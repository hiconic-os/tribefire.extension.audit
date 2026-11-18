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
package tribefire.extension.audit.model.service.audit.api;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import tribefire.extension.audit.model.ServiceAuditArchive;
import tribefire.extension.audit.model.ServiceAuditRecord;
import tribefire.extension.audit.model.archive.api.ArchiveExpiredAuditRecords;

/**
 * {@link ArchiveExpiredAuditRecords} for {@link ServiceAuditRecord}s, see {@link ServiceAuditRecord#getExpiryDate() expiryDate}. The archive is a
 * {@link ServiceAuditArchive}.
 */
@Description("Moves the expired service audit records of the audit access into a zip (held by ServiceAuditArchive).")
public interface ArchiveExpiredServiceAuditRecords extends ArchiveExpiredAuditRecords {

	EntityType<ArchiveExpiredServiceAuditRecords> T = EntityTypes.T(ArchiveExpiredServiceAuditRecords.class);

}
