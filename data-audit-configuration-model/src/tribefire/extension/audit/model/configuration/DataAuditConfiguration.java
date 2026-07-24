package tribefire.extension.audit.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.SelectiveInformation;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Name;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@SelectiveInformation("Data Audit Configuration")
public interface DataAuditConfiguration extends GenericEntity {
	EntityType<DataAuditConfiguration> T = EntityTypes.T(DataAuditConfiguration.class);

	@Name("Audited accesses")
	@Description("Accesses whose entity manipulations are recorded according to the audit metadata of their data models.")
	List<DataAuditAccess> getAccesses();
	void setAccesses(List<DataAuditAccess> accesses);
}
