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
package tribefire.extension.audit.service_audit.rx.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;

import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.resource.Resource;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.test.common.AbstractRxTest;
import tribefire.extension.audit.model.ServiceAuditArchive;
import tribefire.extension.audit.model.ServiceAuditRecord;
import tribefire.extension.audit.model.archive.api.AuditArchivingResponse;
import tribefire.extension.audit.model.service.audit.api.ArchiveExpiredServiceAuditRecords;

public class ArchiveExpiredServiceAuditRecordsTest extends AbstractRxTest {

	private static final String ACCESS_ID = "audit-test.access";
	private static final String PAYLOAD = "{\"requestType\": \"Test\"}";

	@Test
	public void archivesOnlyExpiredRecords() throws IOException {
		PersistenceGmSession session = newSession();

		ServiceAuditRecord expired = newRecord(session, "Expired", new Date(System.currentTimeMillis() - 60_000));
		expired.setRequest(session.resources().create() //
				.name("payload.json") //
				.mimeType("application/json") //
				.store(new ByteArrayInputStream(PAYLOAD.getBytes(StandardCharsets.UTF_8))));

		ServiceAuditRecord notYetExpired = newRecord(session, "NotYetExpired", new Date(System.currentTimeMillis() + 3_600_000));
		ServiceAuditRecord neverExpires = newRecord(session, "NeverExpires", null);
		session.commit();

		Object expiredId = expired.getId();
		Object payloadId = expired.getRequest().getId();

		assertThat(archive().getArchivedRecordCount()).isEqualTo(1);

		// records
		PersistenceGmSession checkSession = newSession();
		List<ServiceAuditRecord> remaining = checkSession.query().entities(EntityQueryBuilder.from(ServiceAuditRecord.T).done()).list();
		assertThat(remaining).extracting(ServiceAuditRecord::getId).containsExactlyInAnyOrder(notYetExpired.getId(), neverExpires.getId());

		Resource deletedPayload = checkSession.query().entities(EntityQueryBuilder.from(Resource.T).where().property(Resource.id).eq(payloadId).done())
				.first();
		assertThat(deletedPayload).as("payload resource of the archived record").isNull();

		// archive
		ServiceAuditArchive archive = checkSession.query().entities(EntityQueryBuilder.from(ServiceAuditArchive.T).done()).unique();
		assertThat(archive.getRecordCount()).isEqualTo(1);
		assertThat(archive.getFormatVersion()).isEqualTo(1);
		assertThat(archive.getFirstRecordDate()).isEqualTo(expired.getDate());

		String zipName = archive.getResource().getName();
		assertThat(zipName).matches("audit-test\\.access-service-audits-\\d{4}-\\d{2}-\\d{2}--\\d{2}-\\d{2}\\.zip");
		String prefix = zipName.substring(0, zipName.length() - ".zip".length());

		Map<String, String> entries = readZip(checkSession, archive.getResource());
		String payloadPath = prefix + "--payloads/" + expiredId + "-request.json";
		assertThat(entries).containsOnlyKeys(prefix + "--manifest.yaml", prefix + "--01.csv", payloadPath);
		assertThat(entries.get(payloadPath)).isEqualTo(PAYLOAD);
		assertThat(entries.get(prefix + "--manifest.yaml")).contains("formatVersion: 1", "recordCount: 1", "\"" + prefix + "--01.csv\"");

		// CSV
		List<CSVRecord> rows = CSVFormat.RFC4180.parse(new StringReader(entries.get(prefix + "--01.csv"))).getRecords();
		assertThat(rows).hasSize(2);

		CSVRecord header = rows.get(0);
		CSVRecord row = rows.get(1);
		assertThat(header.get(0)).isEqualTo("_type");
		assertThat(header.get(1)).isEqualTo("id");
		assertThat(row.get(0)).isEqualTo(ServiceAuditRecord.T.getTypeSignature());
		assertThat(row.get(1)).isEqualTo(String.valueOf(expiredId));
		assertThat(row.get(header.toList().indexOf(ServiceAuditRecord.requestType))).isEqualTo("Expired");
		assertThat(row.get(header.toList().indexOf(ServiceAuditRecord.request))).isEqualTo(payloadPath);
	}

	@Test
	public void noExpiredRecordMakesNoArchive() {
		PersistenceGmSession session = newSession();
		newRecord(session, "NeverExpires", null);
		session.commit();

		assertThat(archive().getArchivedRecordCount()).isZero();

		List<ServiceAuditArchive> archives = newSession().query().entities(EntityQueryBuilder.from(ServiceAuditArchive.T).done()).list();
		assertThat(archives).isEmpty();
	}

	@Test
	public void splitsRecordsIntoCsvsOfConfiguredSize() throws IOException {
		PersistenceGmSession session = newSession();
		Date expiry = new Date(System.currentTimeMillis() - 60_000);
		for (int i = 1; i <= 3; i++) {
			ServiceAuditRecord record = newRecord(session, "Expired" + i, expiry);
			record.setDate(new Date(1_000_000L * i));
		}
		session.commit();

		ArchiveExpiredServiceAuditRecords request = ArchiveExpiredServiceAuditRecords.T.create();
		request.setDomainId(ACCESS_ID);
		request.setRecordsPerCsv(2);
		assertThat(request.eval(evaluator).get().getArchivedRecordCount()).isEqualTo(3);

		PersistenceGmSession checkSession = newSession();
		ServiceAuditArchive archive = checkSession.query().entities(EntityQueryBuilder.from(ServiceAuditArchive.T).done()).unique();
		String zipName = archive.getResource().getName();
		String prefix = zipName.substring(0, zipName.length() - ".zip".length());

		Map<String, String> entries = readZip(checkSession, archive.getResource());
		assertThat(entries).containsOnlyKeys(prefix + "--manifest.yaml", prefix + "--01.csv", prefix + "--02.csv");
		assertThat(entries.get(prefix + "--manifest.yaml")).contains("recordsPerCsv: 2");

		// in the order of the record dates, 2 in the first CSV, 1 in the second
		assertThat(requestTypes(entries.get(prefix + "--01.csv"))).containsExactly("Expired1", "Expired2");
		assertThat(requestTypes(entries.get(prefix + "--02.csv"))).containsExactly("Expired3");
	}

	@Test
	public void recordsPerCsvBelowOneIsRejected() {
		ArchiveExpiredServiceAuditRecords request = ArchiveExpiredServiceAuditRecords.T.create();
		request.setDomainId(ACCESS_ID);
		request.setRecordsPerCsv(0);

		assertThat(request.eval(evaluator).getReasoned().isUnsatisfied()).isTrue();
	}

	private static List<String> requestTypes(String csv) throws IOException {
		List<CSVRecord> rows = CSVFormat.RFC4180.parse(new StringReader(csv)).getRecords();
		int column = rows.get(0).toList().indexOf(ServiceAuditRecord.requestType);

		List<String> result = new ArrayList<>();
		for (CSVRecord row : rows.subList(1, rows.size()))
			result.add(row.get(column));
		return result;
	}

	private AuditArchivingResponse archive() {
		ArchiveExpiredServiceAuditRecords request = ArchiveExpiredServiceAuditRecords.T.create();
		request.setDomainId(ACCESS_ID);
		return request.eval(evaluator).get();
	}

	private static ServiceAuditRecord newRecord(PersistenceGmSession session, String requestType, Date expiryDate) {
		ServiceAuditRecord record = session.create(ServiceAuditRecord.T);
		record.setRequestType(requestType);
		record.setDate(new Date());
		record.setExpiryDate(expiryDate);
		return record;
	}

	private static Map<String, String> readZip(PersistenceGmSession session, Resource zip) throws IOException {
		Map<String, String> entries = new LinkedHashMap<>();
		try (ZipInputStream in = new ZipInputStream(session.resources().openStream(zip), StandardCharsets.UTF_8)) {
			ZipEntry entry;
			while ((entry = in.getNextEntry()) != null)
				entries.put(entry.getName(), readAll(in));
		}
		return entries;
	}

	private static String readAll(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int n;
		while ((n = in.read(buffer)) != -1)
			out.write(buffer, 0, n);
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}

	private PersistenceGmSession newSession() {
		return resolveExportContract(AccessContract.class).systemSessionFactory().newSession(ACCESS_ID);
	}

}
