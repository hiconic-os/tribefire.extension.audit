package tribefire.extension.audit.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.SelectiveInformation;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Name;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@SelectiveInformation("Service Audit Configuration")
public interface ServiceAuditConfiguration extends GenericEntity {
	EntityType<ServiceAuditConfiguration> T = EntityTypes.T(ServiceAuditConfiguration.class);

	@Name("Audited service domains")
	@Description("Service domains whose audited requests are recorded in the associated audit access.")
	List<ServiceAuditDomain> getDomains();
	void setDomains(List<ServiceAuditDomain> domains);
}
