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
package tribefire.extension.audit.data_audit.rx.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
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
import tribefire.extension.audit.model.DataAuditArchive;
import tribefire.extension.audit.model.ManipulationRecord;
import tribefire.extension.audit.model.archive.api.AuditArchivingResponse;
import tribefire.extension.audit.model.data.audit.api.ArchiveExpiredDataAuditRecords;
import tribefire.extension.audit.model.data.test.ExpiringAuditedEntity;
import tribefire.extension.audit.model.data.test.KeptAuditedEntity;

/**
 * The audited access <code>audit-test.data</code> writes its manipulation records into <code>audit-test.audit</code>. The records of
 * {@link ExpiringAuditedEntity} expire immediately (AuditTtl 0), the records of {@link KeptAuditedEntity} never.
 */
public class ArchiveExpiredDataAuditRecordsTest extends AbstractRxTest {

	private static final String DATA_ACCESS_ID = "audit-test.data";
	private static final String AUDIT_ACCESS_ID = "audit-test.audit";

	@Test
	public void expiryDateFollowsAuditTtlOfManipulatedType() {
		createAuditedEntities();

		List<ManipulationRecord> expiring = records(ExpiringAuditedEntity.T.getTypeSignature());
		assertThat(expiring).isNotEmpty();
		for (ManipulationRecord record : expiring)
			assertThat(record.getExpiryDate()).as("expiry date with TTL 0").isEqualTo(record.getDate());

		List<ManipulationRecord> kept = records(KeptAuditedEntity.T.getTypeSignature());
		assertThat(kept).isNotEmpty();
		assertThat(kept).extracting(ManipulationRecord::getExpiryDate).containsOnlyNulls();
	}

	@Test
	public void archivesOnlyExpiredRecords() throws Exception {
		createAuditedEntities();

		int expiringCount = records(ExpiringAuditedEntity.T.getTypeSignature()).size();
		int keptCount = records(KeptAuditedEntity.T.getTypeSignature()).size();

		// the archive run takes the records which expired before its start
		Thread.sleep(10);

		assertThat(archive().getArchivedRecordCount()).isEqualTo(expiringCount);

		// records
		assertThat(records(ExpiringAuditedEntity.T.getTypeSignature())).isEmpty();
		assertThat(records(KeptAuditedEntity.T.getTypeSignature())).hasSize(keptCount);

		// archive
		PersistenceGmSession session = auditSession();
		DataAuditArchive archive = session.query().entities(EntityQueryBuilder.from(DataAuditArchive.T).done()).unique();
		assertThat(archive.getRecordCount()).isEqualTo(expiringCount);

		String zipName = archive.getResource().getName();
		assertThat(zipName).matches("audit-test\\.audit-data-audits-\\d{4}-\\d{2}-\\d{2}--\\d{2}-\\d{2}\\.zip");
		String prefix = zipName.substring(0, zipName.length() - ".zip".length());

		Map<String, String> entries = readZip(session, archive.getResource());
		assertThat(entries).containsOnlyKeys(prefix + "--manifest.yaml", prefix + "--01.csv");

		// CSV
		List<CSVRecord> rows = CSVFormat.RFC4180.parse(new StringReader(entries.get(prefix + "--01.csv"))).getRecords();
		assertThat(rows).hasSize(expiringCount + 1);

		int instanceTypeColumn = rows.get(0).toList().indexOf(ManipulationRecord.instanceType);
		for (CSVRecord row : rows.subList(1, rows.size())) {
			assertThat(row.get(0)).isEqualTo(ManipulationRecord.T.getTypeSignature());
			assertThat(row.get(instanceTypeColumn)).isEqualTo(ExpiringAuditedEntity.T.getTypeSignature());
		}
	}

	private void createAuditedEntities() {
		PersistenceGmSession session = resolveExportContract(AccessContract.class).systemSessionFactory().newSession(DATA_ACCESS_ID);

		ExpiringAuditedEntity expiring = session.create(ExpiringAuditedEntity.T);
		expiring.setName("expiring");

		KeptAuditedEntity kept = session.create(KeptAuditedEntity.T);
		kept.setName("kept");

		session.commit();
	}

	private List<ManipulationRecord> records(String instanceType) {
		return auditSession().query().entities(EntityQueryBuilder.from(ManipulationRecord.T) //
				.where().property(ManipulationRecord.instanceType).eq(instanceType) //
				.done()).list();
	}

	private AuditArchivingResponse archive() {
		ArchiveExpiredDataAuditRecords request = ArchiveExpiredDataAuditRecords.T.create();
		request.setDomainId(AUDIT_ACCESS_ID);
		return request.eval(evaluator).get();
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

	private PersistenceGmSession auditSession() {
		return resolveExportContract(AccessContract.class).systemSessionFactory().newSession(AUDIT_ACCESS_ID);
	}

}
