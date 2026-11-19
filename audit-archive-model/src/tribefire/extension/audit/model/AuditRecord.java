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
package tribefire.extension.audit.model;

import java.util.Date;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.meta.Indexed;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * An audit record which can expire and then be archived (see {@link AuditArchive}).
 */
@Abstract
public interface AuditRecord extends GenericEntity {

	EntityType<AuditRecord> T = EntityTypes.T(AuditRecord.class);

	String date = "date";
	String expiryDate = "expiryDate";

	/** The time of recording. */
	Date getDate();
	void setDate(Date date);

	/**
	 * The time after which the record may be archived and removed from the audit access, derived from the AuditTtl metadata of the audited type:
	 * the request type for a service audit record, the manipulated type for a manipulation record. Null means the record never expires.
	 */
	@Indexed
	Date getExpiryDate();
	void setExpiryDate(Date expiryDate);

}
