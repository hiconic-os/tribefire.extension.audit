package tribefire.extension.audit.data_audit.rx.wire.space;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.AccessDataModelConfiguration;
import hiconic.rx.access.module.api.AccessServiceModelConfiguration;
import hiconic.rx.module.api.common.RxPlatform;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import tribefire.extension.audit._DataAuditApiModel_;
import tribefire.extension.audit.model.DataAuditArchive;
import tribefire.extension.audit.model.ManipulationRecord;
import tribefire.extension.audit.model.configuration.DataAuditAccess;
import tribefire.extension.audit.model.configuration.DataAuditConfiguration;
import tribefire.extension.audit.model.data.audit.api.ArchiveExpiredDataAuditRecords;
import tribefire.extension.audit.processing.ArchiveExpiredAuditRecordsProcessor;
import tribefire.extension.audit.processing.AuditAspect;
import tribefire.extension.audit.processing.ManipulationRecordCreator;

@Managed
public class DataAuditRxModuleSpace implements RxModuleContract {
	@Import private RxPlatformContract platform;
	@Import private AccessContract access;

	@Override
	public void configureModels(ModelConfigurations configurations) {
		Set<String> archivingAccessIds = new HashSet<>();

		for (DataAuditAccess auditedAccess : configuration().getAccesses()) {
			AccessDataModelConfiguration dataModel =
					access.accessModelConfigurations().dataModelConfiguration(auditedAccess.getAccessId());
			dataModel.bindAspect(auditedAccess.getAspectId()).bind(() -> auditAspect(auditedAccess));

			String auditAccessId = isBlank(auditedAccess.getAuditAccessId()) ? auditedAccess.getAccessId() : auditedAccess.getAuditAccessId();
			AccessDataModelConfiguration auditDataModel = access.accessModelConfigurations().dataModelConfiguration(auditAccessId);
			auditDataModel.addModel(ManipulationRecord.T.getModel());

			// several audited accesses may share one audit access, which needs the archive request only once
			if (archivingAccessIds.add(auditAccessId)) {
				AccessServiceModelConfiguration auditServiceModel = access.accessModelConfigurations().serviceModelConfiguration(auditAccessId);
				auditServiceModel.addModel(_DataAuditApiModel_.reflection);
				auditServiceModel.bindAccessRequest(ArchiveExpiredDataAuditRecords.T, this::archiveProcessor);
			}
		}
	}

	@Managed
	private ArchiveExpiredAuditRecordsProcessor archiveProcessor() {
		ArchiveExpiredAuditRecordsProcessor bean = new ArchiveExpiredAuditRecordsProcessor();
		bean.setSystemSessionFactory(access.systemSessionFactory());
		bean.setRecordType(ManipulationRecord.T);
		bean.setArchiveType(DataAuditArchive.T);
		bean.setKind("data-audits");
		return bean;
	}

	@Managed
	private DataAuditConfiguration configuration() {
		return platform.configuration().readConfig(DataAuditConfiguration.T).get();
	}

	private AuditAspect auditAspect(DataAuditAccess configuration) {
		AuditAspect bean = new AuditAspect();
		bean.setActive(configuration.getActive());
		bean.setUntrackedRoles(configuration.getUntrackedRoles());
		bean.setUserRolesProvider(this::currentUserRoles);
		bean.setManipulationRecordCreator(manipulationRecordCreator());

		String auditAccessId = configuration.getAuditAccessId();
		if (!isBlank(auditAccessId))
			bean.setAuditSessionProvider(() -> auditSession(auditAccessId));

		return bean;
	}

	private PersistenceGmSession auditSession(String accessId) {
		return access.systemSessionFactory().newSession(accessId);
	}

	private Set<String> currentUserRoles() {
		UserSession userSession = RxPlatform.currentUserSession();
		return userSession == null ? Collections.emptySet() : userSession.getEffectiveRoles();
	}

	@Managed
	private ManipulationRecordCreator manipulationRecordCreator() {
		return new ManipulationRecordCreator();
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
