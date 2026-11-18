// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package tribefire.extension.audit.processing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.accessrequest.api.AccessRequestContext;
import com.braintribe.model.processing.accessrequest.api.ReasonedAccessRequestProcessor;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.query.fluent.SelectQueryBuilder;
import com.braintribe.model.processing.query.tools.PreparedTcs;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.query.EntityQuery;
import com.braintribe.model.query.OrderingDirection;
import com.braintribe.model.query.SelectQuery;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resourceapi.persistence.DeletionScope;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipeFactory;
import com.braintribe.utils.stream.api.StreamPipes;

import tribefire.extension.audit.model.AuditArchive;
import tribefire.extension.audit.model.archive.api.ArchiveExpiredAuditRecords;
import tribefire.extension.audit.model.archive.api.AuditArchivingResponse;

/**
 * Processes {@link ArchiveExpiredAuditRecords} for one kind of audit records: moves the expired records of the configured
 * {@link #setRecordType(EntityType) record type} in the audit access (the domain of the request) into a zip, stores it as an instance of the
 * configured {@link #setArchiveType(EntityType) archive type} in the same access, and deletes the archived records with their payload resources.
 * <p>
 * Layout of the zip, where <code>prefix</code> is <code>&lt;accessId&gt;-&lt;kind&gt;-yyyy-MM-dd--HH-mm</code> with the cutoff in UTC (for
 * <code>kind</code> see {@link #setKind(String)}):
 *
 * <pre>
 * prefix.zip
 *     prefix--manifest.yaml        format version, cutoff, columns, and record count and date range of each CSV
 *     prefix--01.csv               RFC 4180, header row with the columns, at most recordsPerCsv records (see the request)
 *     prefix--payloads/            the Resources of the records, e.g. preserved request and result
 * </pre>
 */
public class ArchiveExpiredAuditRecordsProcessor implements ReasonedAccessRequestProcessor<ArchiveExpiredAuditRecords, AuditArchivingResponse> {

	private static final Logger log = Logger.getLogger(ArchiveExpiredAuditRecordsProcessor.class);

	/** The property with the time of the record, by which the records are ordered. */
	public static final String DATE_PROPERTY = "date";

	/** The property with the time after which the record is archived, null if it never expires. */
	public static final String EXPIRY_DATE_PROPERTY = "expiryDate";

	/** Version of the layout of the zip and its files, see {@link AuditArchive#getFormatVersion()}. */
	public static final int FORMAT_VERSION = 1;

	/** The first column of each CSV, with the type signature of the record. */
	public static final String TYPE_COLUMN = "_type";

	private static final int QUERY_PAGE_SIZE = 500;
	private static final int DELETE_BATCH_SIZE = 100;

	private static final DateTimeFormatter NAME_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm").withZone(ZoneOffset.UTC);
	private static final DateTimeFormatter VALUE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

	private PersistenceGmSessionFactory systemSessionFactory;
	private StreamPipeFactory streamPipeFactory = StreamPipes.simpleFactory();
	private EntityType<?> recordType;
	private EntityType<? extends AuditArchive> archiveType;
	private String kind;

	@Required
	@Configurable
	public void setSystemSessionFactory(PersistenceGmSessionFactory systemSessionFactory) {
		this.systemSessionFactory = systemSessionFactory;
	}

	@Configurable
	public void setStreamPipeFactory(StreamPipeFactory streamPipeFactory) {
		this.streamPipeFactory = streamPipeFactory;
	}

	/**
	 * The type of the archived records. The zip has the records of all its instantiable sub types too. The type must have the properties
	 * {@value #DATE_PROPERTY} and {@value #EXPIRY_DATE_PROPERTY}, both of type {@link Date}.
	 */
	@Required
	@Configurable
	public void setRecordType(EntityType<?> recordType) {
		requireDateProperty(recordType, DATE_PROPERTY);
		requireDateProperty(recordType, EXPIRY_DATE_PROPERTY);
		this.recordType = recordType;
	}

	/** The type of the entity which holds the zip. */
	@Required
	@Configurable
	public void setArchiveType(EntityType<? extends AuditArchive> archiveType) {
		this.archiveType = archiveType;
	}

	/**
	 * The kind of the records in the file names, e.g. <code>service-audits</code>. It makes the names unique when one audit access holds several
	 * kinds of records.
	 */
	@Required
	@Configurable
	public void setKind(String kind) {
		this.kind = kind;
	}

	private static void requireDateProperty(EntityType<?> type, String name) {
		Property property = type.findProperty(name);
		if (property == null || property.getType().getJavaType() != Date.class)
			throw new IllegalArgumentException("Audit record type " + type.getTypeSignature() + " has no Date property: " + name);
	}

	@Override
	public Maybe<AuditArchivingResponse> processReasoned(AccessRequestContext<ArchiveExpiredAuditRecords> context) {
		int recordsPerCsv = context.getRequest().getRecordsPerCsv();
		if (recordsPerCsv < 1)
			return Reasons.build(InvalidArgument.T).text("recordsPerCsv must be at least 1, but was: " + recordsPerCsv).toMaybe();

		long archived = new ArchiveRun(context.getDomainId(), recordsPerCsv).run();

		AuditArchivingResponse response = AuditArchivingResponse.T.create();
		response.setArchivedRecordCount(archived);
		return Maybe.complete(response);
	}

	/** The file name of a zip entry, with every character outside <code>[A-Za-z0-9._-]</code> replaced by an underscore. */
	/* package */ static String sanitizeFileName(String s) {
		return s.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	/** At least 2 digits, so that the CSVs of a zip sort alphabetically in their order. */
	/* package */ static int counterWidth(long recordCount, int recordsPerCsv) {
		long csvCount = (recordCount + recordsPerCsv - 1) / recordsPerCsv;
		return Math.max(2, String.valueOf(csvCount).length());
	}

	private class ArchiveRun {

		private final String accessId;
		private final int recordsPerCsv;

		/*
		 * Records with expiryDate < cutoff are archived. A record which is created during the run gets an expiryDate after the cutoff, so the set
		 * cannot grow, and the count at the start is exact, which the counter width of the CSV names relies on.
		 */
		private final Date cutoff = new Date();
		private final String prefix;

		private final List<String> columns;

		/** The CSVs which are written, for the manifest. */
		private final List<CsvInfo> csvInfos = new ArrayList<>();

		private ZipOutputStream zip;
		private Writer idWriter;
		private Function<String, Object> idParser;

		private CsvInfo currentCsv;
		private StreamPipe currentCsvPipe;
		private CSVPrinter currentCsvPrinter;

		private long recordCount;
		private int counterWidth;

		private final Property dateProperty = recordType.getProperty(DATE_PROPERTY);

		public ArchiveRun(String accessId, int recordsPerCsv) {
			this.accessId = accessId;
			this.recordsPerCsv = recordsPerCsv;
			this.prefix = sanitizeFileName(accessId) + "-" + kind + "-" + NAME_TIME_FORMAT.format(cutoff.toInstant());
			this.columns = resolveColumns();
		}

		public long run() {
			long expectedCount = countExpiredRecords();
			if (expectedCount == 0)
				return 0;

			counterWidth = counterWidth(expectedCount, recordsPerCsv);

			StreamPipe zipPipe = streamPipeFactory.newPipe(prefix + ".zip");
			// The ids of the exported records, one per line, so that exactly these are deleted, without keeping them in memory.
			StreamPipe idPipe = streamPipeFactory.newPipe(prefix + "-ids");

			try {
				writeZip(zipPipe, idPipe, expectedCount);

				if (recordCount == 0)
					return 0;

				storeArchive(zipPipe);
				deleteRecords(idPipe);

				log.info("Archived " + recordCount + " expired records of type " + recordType.getTypeSignature() + " of access " + accessId + " into "
						+ prefix + ".zip");
				return recordCount;

			} finally {
				zipPipe.close();
				idPipe.close();
			}
		}

		/*
		 * Every query also says "expiryDate != null" explicitly: SQL excludes null from "<" by itself, but other accesses, e.g. Smood, treat
		 * null as less than every date, and would archive the records which never expire.
		 */
		private long countExpiredRecords() {
			SelectQuery query = new SelectQueryBuilder().from(recordType, "r") //
					.where().conjunction() //
						.property("r", EXPIRY_DATE_PROPERTY).ne(null) //
						.property("r", EXPIRY_DATE_PROPERTY).lt(cutoff) //
					.close() //
					.select().count("r") //
					.done();

			Long count = newSession().query().select(query).first();
			return count == null ? 0 : count;
		}

		/** <code>_type</code>, then the union of the properties of all instantiable record types of the access, in a stable order. */
		private List<String> resolveColumns() {
			Set<EntityType<?>> recordTypes = newSession().getModelAccessory().getOracle() //
					.getEntityTypeOracle(recordType) //
					.getSubTypes().transitive().includeSelf().onlyInstantiable() //
					.asTypes();

			// the record type first, so that its properties come first, then the sub types by signature
			List<EntityType<?>> sortedTypes = recordTypes.stream() //
					.sorted(Comparator.comparing((EntityType<?> t) -> t != recordType).thenComparing(EntityType::getTypeSignature)) //
					.collect(Collectors.toList());

			Set<String> names = new LinkedHashSet<>();
			names.add(TYPE_COLUMN);
			names.add(GenericEntity.id);
			for (EntityType<?> type : sortedTypes)
				for (Property p : type.getProperties())
					if (!p.getName().equals(GenericEntity.globalId) && !p.getName().equals(GenericEntity.partition))
						names.add(p.getName());

			return new ArrayList<>(names);
		}

		// ##############################################
		// ## . . . . . . . Writing the zip . . . . . .##
		// ##############################################

		private void writeZip(StreamPipe zipPipe, StreamPipe idPipe, long expectedCount) {
			try (ZipOutputStream zipOut = new ZipOutputStream(zipPipe.openOutputStream(), StandardCharsets.UTF_8);
					Writer idOut = new OutputStreamWriter(idPipe.openOutputStream(), StandardCharsets.UTF_8)) {
				zip = zipOut;
				idWriter = idOut;

				exportRecords(expectedCount);
				finishCsv();

				if (recordCount > 0)
					writeManifest();

			} catch (IOException e) {
				throw new UncheckedIOException("Error while writing the audit archive " + prefix + ".zip", e);
			}
		}

		/*
		 * Keyset paging by (date, id) rather than offset paging: a record which is committed late (see the cutoff) must not shift the pages. At most
		 * expectedCount records are written, so the counter width stays correct; any further record waits for the next run.
		 */
		private void exportRecords(long expectedCount) throws IOException {
			GenericEntity last = null;

			while (recordCount < expectedCount) {
				PersistenceGmSession session = newSession();
				List<GenericEntity> page = session.query().entities(pageQuery(last)).list();
				if (page.isEmpty())
					return;

				for (GenericEntity record : page) {
					if (recordCount == expectedCount)
						return;

					exportRecord(session, record);
					last = record;
				}
			}
		}

		private EntityQuery pageQuery(GenericEntity last) {
			EntityQueryBuilder builder = EntityQueryBuilder.from(recordType);

			if (last == null)
				builder.where().conjunction() //
						.property(EXPIRY_DATE_PROPERTY).ne(null) //
						.property(EXPIRY_DATE_PROPERTY).lt(cutoff) //
						.close();
			else
				// @formatter:off
				builder.where()
					.conjunction()
						.property(EXPIRY_DATE_PROPERTY).ne(null)
						.property(EXPIRY_DATE_PROPERTY).lt(cutoff)
						.disjunction()
							.property(DATE_PROPERTY).gt(dateOf(last))
							.conjunction()
								.property(DATE_PROPERTY).eq(dateOf(last))
								.property(GenericEntity.id).gt(last.getId())
							.close()
						.close()
					.close();
				// @formatter:on

			return builder //
					.orderByCascade() //
					.dir(OrderingDirection.ascending).property(DATE_PROPERTY) //
					.dir(OrderingDirection.ascending).property(GenericEntity.id) //
					.close() //
					.limit(QUERY_PAGE_SIZE) //
					.tc(PreparedTcs.everythingTc) //
					.done();
		}

		private Date dateOf(GenericEntity record) {
			return dateProperty.get(record);
		}

		private void exportRecord(PersistenceGmSession session, GenericEntity record) throws IOException {
			if (currentCsv == null || currentCsv.recordCount == recordsPerCsv) {
				finishCsv();
				startCsv();
			}

			List<String> row = new ArrayList<>(columns.size());
			row.add(record.entityType().getTypeSignature());

			for (String column : columns.subList(1, columns.size())) {
				Property property = record.entityType().findProperty(column);
				row.add(property == null ? "" : cellValue(session, record, property));
			}

			currentCsvPrinter.printRecord(row);
			currentCsv.add(dateOf(record));

			rememberId(record.getId());
			recordCount++;
		}

		private void rememberId(Object id) throws IOException {
			if (idParser == null)
				idParser = id instanceof Long ? Long::valueOf : id instanceof Integer ? Integer::valueOf : s -> s;

			idWriter.write(String.valueOf(id));
			idWriter.write('\n');
		}

		/*
		 * The CSV is buffered in its own pipe and copied into the zip once it is complete, because the payload files are written into the zip in the
		 * meantime, and a zip stream can only write one entry at a time.
		 */
		private void startCsv() throws IOException {
			String counter = String.format("%0" + counterWidth + "d", csvInfos.size() + 1);
			currentCsv = new CsvInfo(prefix + "--" + counter + ".csv");
			currentCsvPipe = streamPipeFactory.newPipe(currentCsv.name);
			currentCsvPrinter = new CSVPrinter(new OutputStreamWriter(currentCsvPipe.openOutputStream(), StandardCharsets.UTF_8), CSVFormat.RFC4180);
			currentCsvPrinter.printRecord(columns);
		}

		private void finishCsv() throws IOException {
			if (currentCsv == null)
				return;

			currentCsvPrinter.close();

			zip.putNextEntry(new ZipEntry(currentCsv.name));
			try (InputStream in = currentCsvPipe.openInputStream()) {
				transfer(in, zip);
			}
			zip.closeEntry();

			currentCsvPipe.close();
			csvInfos.add(currentCsv);

			currentCsv = null;
			currentCsvPipe = null;
			currentCsvPrinter = null;
		}

		private String cellValue(PersistenceGmSession session, GenericEntity record, Property property) throws IOException {
			Object value = property.get(record);
			if (value instanceof Resource)
				return writePayload(session, record, property, (Resource) value);
			else
				return formatValue(value);
		}

		/** @return the path of the payload file in the zip, which is the cell value */
		private String writePayload(PersistenceGmSession session, GenericEntity record, Property property, Resource resource) throws IOException {
			// typed as Object, so that String.valueOf(Object) is used rather than String.valueOf(char[]) for the generic getId()
			Object id = record.getId();
			String path = prefix + "--payloads/" + sanitizeFileName(id + "-" + property.getName()) + extension(resource);

			zip.putNextEntry(new ZipEntry(path));
			try (InputStream in = session.resources().openStream(resource)) {
				transfer(in, zip);
			}
			zip.closeEntry();

			return path;
		}

		private void writeManifest() throws IOException {
			StringBuilder sb = new StringBuilder();
			sb.append("formatVersion: ").append(FORMAT_VERSION).append('\n');
			sb.append("accessId: ").append(yamlString(accessId)).append('\n');
			sb.append("cutoff: ").append(yamlString(formatDate(cutoff))).append('\n');
			sb.append("exportTime: ").append(yamlString(formatDate(new Date()))).append('\n');
			sb.append("recordCount: ").append(recordCount).append('\n');
			sb.append("recordsPerCsv: ").append(recordsPerCsv).append('\n');

			sb.append("columns:\n");
			for (String column : columns)
				sb.append("  - ").append(yamlString(column)).append('\n');

			sb.append("csvFiles:\n");
			for (CsvInfo csv : csvInfos) {
				sb.append("  - name: ").append(yamlString(csv.name)).append('\n');
				sb.append("    recordCount: ").append(csv.recordCount).append('\n');
				sb.append("    firstRecordDate: ").append(yamlString(formatDate(csv.firstDate))).append('\n');
				sb.append("    lastRecordDate: ").append(yamlString(formatDate(csv.lastDate))).append('\n');
			}

			zip.putNextEntry(new ZipEntry(prefix + "--manifest.yaml"));
			zip.write(sb.toString().getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}

		// ##############################################
		// ## . . . . . Storing and deleting . . . . . ##
		// ##############################################

		private void storeArchive(StreamPipe zipPipe) {
			PersistenceGmSession session = newSession();

			Resource resource = session.resources().create() //
					.name(prefix + ".zip") //
					.mimeType("application/zip") //
					.store(zipPipe::openInputStream);

			AuditArchive archive = session.create(archiveType);
			archive.setResource(resource);
			archive.setFormatVersion(FORMAT_VERSION);
			archive.setCutoff(cutoff);
			archive.setRecordCount(recordCount);
			archive.setFirstRecordDate(csvInfos.get(0).firstDate);
			archive.setLastRecordDate(csvInfos.get(csvInfos.size() - 1).lastDate);
			archive.setCreationDate(new Date());

			session.commit();
		}

		/*
		 * Deletes exactly the exported records, by id, rather than by "expiryDate < cutoff": a record which is committed late can match the criterion
		 * without having been exported. The payload resources are deleted after their records, which reference them.
		 */
		private void deleteRecords(StreamPipe idPipe) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(idPipe.openInputStream(), StandardCharsets.UTF_8))) {
				Set<Object> batch = new HashSet<>();

				String line;
				while ((line = reader.readLine()) != null) {
					batch.add(idParser.apply(line));
					if (batch.size() == DELETE_BATCH_SIZE) {
						deleteBatch(batch);
						batch.clear();
					}
				}

				if (!batch.isEmpty())
					deleteBatch(batch);

			} catch (IOException e) {
				throw new UncheckedIOException("Error while reading the ids of the archived audit records", e);
			}
		}

		private void deleteBatch(Set<Object> ids) {
			PersistenceGmSession session = newSession();

			EntityQuery query = EntityQueryBuilder.from(recordType) //
					.where().property(GenericEntity.id).in(ids) //
					.tc(PreparedTcs.everythingTc) //
					.done();

			List<GenericEntity> records = session.query().entities(query).list();

			List<Resource> payloads = new ArrayList<>();
			for (GenericEntity record : records) {
				for (Property p : record.entityType().getProperties()) {
					Object value = p.get(record);
					if (value instanceof Resource)
						payloads.add((Resource) value);
				}

				session.deleteEntity(record);
			}

			session.commit();

			for (Resource payload : payloads)
				session.resources().delete(payload).scope(DeletionScope.resource).delete();

			session.commit();
		}

		private PersistenceGmSession newSession() {
			return systemSessionFactory.newSession(accessId);
		}
	}

	private static class CsvInfo {
		final String name;
		long recordCount;
		Date firstDate;
		Date lastDate;

		CsvInfo(String name) {
			this.name = name;
		}

		void add(Date date) {
			if (recordCount == 0)
				firstDate = date;
			lastDate = date;
			recordCount++;
		}
	}

	// ##############################################
	// ## . . . . . . . Value formatting . . . . . ##
	// ##############################################

	/** Null is empty, dates are ISO-8601 in UTC, entities are their id, and the elements of collections and maps are separated by ';'. */
	/* package */ static String formatValue(Object value) {
		if (value == null)
			return "";
		if (value instanceof Date)
			return formatDate((Date) value);
		if (value instanceof GenericEntity) {
			// typed as Object, so that String.valueOf(Object) is used rather than String.valueOf(char[]) for the generic getId()
			Object id = ((GenericEntity) value).getId();
			return String.valueOf(id);
		}
		if (value instanceof Enum)
			return ((Enum<?>) value).name();
		if (value instanceof Collection)
			return ((Collection<?>) value).stream().map(ArchiveExpiredAuditRecordsProcessor::formatValue).collect(Collectors.joining(";"));
		if (value instanceof Map)
			return ((Map<?, ?>) value).entrySet().stream() //
					.map(e -> formatValue(e.getKey()) + "=" + formatValue(e.getValue())) //
					.collect(Collectors.joining(";"));

		return value.toString();
	}

	private static String formatDate(Date date) {
		return date == null ? "" : VALUE_TIME_FORMAT.format(date.toInstant());
	}

	private static String yamlString(String s) {
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private static final Map<String, String> EXTENSION_BY_MIME_TYPE = new LinkedHashMap<>();

	static {
		EXTENSION_BY_MIME_TYPE.put("application/json", ".json");
		EXTENSION_BY_MIME_TYPE.put("application/xml", ".xml");
		EXTENSION_BY_MIME_TYPE.put("text/xml", ".xml");
		EXTENSION_BY_MIME_TYPE.put("application/yaml", ".yaml");
		EXTENSION_BY_MIME_TYPE.put("text/yaml", ".yaml");
		EXTENSION_BY_MIME_TYPE.put("application/x-yaml", ".yaml");
		EXTENSION_BY_MIME_TYPE.put("text/plain", ".txt");
	}

	private static String extension(Resource resource) {
		String mimeType = resource.getMimeType();
		if (mimeType != null) {
			String extension = EXTENSION_BY_MIME_TYPE.get(mimeType.split(";")[0].trim().toLowerCase());
			if (extension != null)
				return extension;
		}

		return ".bin";
	}

	private static void transfer(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[64 * 1024];
		int n;
		while ((n = in.read(buffer)) != -1)
			out.write(buffer, 0, n);
	}

}
