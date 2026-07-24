package tribefire.extension.audit.model.configuration;

import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.SelectiveInformation;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.annotation.meta.Name;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@SelectiveInformation("Audit ${accessId}")
public interface DataAuditAccess extends GenericEntity {
	EntityType<DataAuditAccess> T = EntityTypes.T(DataAuditAccess.class);

	@Mandatory
	@Name("Access ID")
	String getAccessId();
	void setAccessId(String accessId);

	@Name("Audit Access ID")
	@Description("Optional target access for audit records. If omitted, records are stored in the audited access.")
	String getAuditAccessId();
	void setAuditAccessId(String auditAccessId);

	@Mandatory
	@Initializer("'data-audit'")
	@Name("Aspect ID")
	@Description("Stable identifier used by an integration module to order this aspect relative to other access aspects.")
	String getAspectId();
	void setAspectId(String aspectId);

	@Initializer("true")
	boolean getActive();
	void setActive(boolean active);

	@Name("Untracked roles")
	Set<String> getUntrackedRoles();
	void setUntrackedRoles(Set<String> untrackedRoles);
}
