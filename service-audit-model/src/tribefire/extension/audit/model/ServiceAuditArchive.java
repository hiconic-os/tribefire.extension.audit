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

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * An {@link AuditArchive} with expired {@link ServiceAuditRecord}s. The payloads in the zip are the preserved requests and results.
 */
@Description("A zip with service audit records which were removed from the audit access because they had expired.")
public interface ServiceAuditArchive extends AuditArchive {

	EntityType<ServiceAuditArchive> T = EntityTypes.T(ServiceAuditArchive.class);

}
