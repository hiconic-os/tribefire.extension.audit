package tribefire.extension.audit.data_audit.rx.wire.space;

import java.util.Collections;
import java.util.Set;

import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.AccessDataModelConfiguration;
import hiconic.rx.module.api.common.RxPlatform;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import tribefire.extension.audit.model.ManipulationRecord;
import tribefire.extension.audit.model.configuration.DataAuditAccess;
import tribefire.extension.audit.model.configuration.DataAuditConfiguration;
import tribefire.extension.audit.processing.AuditAspect;
import tribefire.extension.audit.processing.ManipulationRecordCreator;

@Managed
public class DataAuditRxModuleSpace implements RxModuleContract {
	@Import private RxPlatformContract platform;
	@Import private AccessContract access;

	@Override
	public void configureModels(ModelConfigurations configurations) {
		for (DataAuditAccess auditedAccess : configuration().getAccesses()) {
			AccessDataModelConfiguration dataModel =
					access.accessModelConfigurations().dataModelConfiguration(auditedAccess.getAccessId());
			dataModel.bindAspect(auditedAccess.getAspectId()).bind(() -> auditAspect(auditedAccess));

			String auditAccessId = auditedAccess.getAuditAccessId();
			AccessDataModelConfiguration auditDataModel = isBlank(auditAccessId)
					? dataModel
					: access.accessModelConfigurations().dataModelConfiguration(auditAccessId);
			auditDataModel.addModel(ManipulationRecord.T.getModel());
		}
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
