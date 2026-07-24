package tribefire.extension.audit.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.SelectiveInformation;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.annotation.meta.Name;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@SelectiveInformation("Audit ${serviceDomainId} into ${auditAccessId}")
public interface ServiceAuditDomain extends GenericEntity {
	EntityType<ServiceAuditDomain> T = EntityTypes.T(ServiceAuditDomain.class);

	@Mandatory
	@Name("Service domain ID")
	String getServiceDomainId();
	void setServiceDomainId(String serviceDomainId);

	@Mandatory
	@Name("Audit access ID")
	@Description("Persistence access in which service audit records are stored.")
	String getAuditAccessId();
	void setAuditAccessId(String auditAccessId);
}
