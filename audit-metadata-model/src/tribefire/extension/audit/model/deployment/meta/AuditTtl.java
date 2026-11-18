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
package tribefire.extension.audit.model.deployment.meta;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

/**
 * Time to live of audit records:
 * <ul>
 * <li>on a request type: of the service audit records of its requests,
 * <li>on an entity type: of the manipulation records of its instances (data audit).
 * </ul>
 * Such a record gets an expiry date, which is the time of recording plus {@link #getTtlInMs() ttlInMs}. Without this metadata, the record has no
 * expiry date, i.e. it never expires.
 * <ul>
 * <li>A negative value, e.g. {@link #NEVER}, means the record never expires. Use it to override an inherited TTL.
 * <li>An expiry date after 9999-12-31T23:59:59.999Z (the maximum of common SQL date types) is cut to that date.
 * </ul>
 * In a model, declare it with the annotation <code>tribefire.extension.audit.annotation.AuditTtl</code> (artifact
 * <code>audit-metadata-annotations</code>).
 */
@Description("Time to live of the audit records of a request type (service audit) or an entity type (data audit), in milliseconds. "
		+ "A negative value means the records never expire, and so does a type without this metadata.")
public interface AuditTtl extends EntityTypeMetaData {

	EntityType<AuditTtl> T = EntityTypes.T(AuditTtl.class);

	long ONE_HOUR = 60 * 60 * 1000;
	long ONE_DAY = 24 * ONE_HOUR;
	long ONE_WEEK = 7 * ONE_DAY;
	long ONE_YEAR = 365 * ONE_DAY;

	long NEVER = -1; // any negative number means never

	String ttlInMs = "ttlInMs";

	@Description("Time to live of the audit record in milliseconds. A negative value means the record never expires.")
	long getTtlInMs();
	void setTtlInMs(long ttlInMs);

	static AuditTtl create(long ttlInMs) {
		AuditTtl result = AuditTtl.T.create();
		result.setTtlInMs(ttlInMs);
		return result;
	}

}
