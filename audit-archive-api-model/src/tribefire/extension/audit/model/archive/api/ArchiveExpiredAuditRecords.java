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
package tribefire.extension.audit.model.archive.api;

import com.braintribe.model.accessapi.AccessRequest;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Min;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.AuthorizedRequest;
import com.braintribe.model.service.api.ServiceRequest;

import tribefire.extension.audit.model.AuditArchive;

/**
 * Moves the expired audit records of the audit access (the domain of this request) into a new {@link AuditArchive} instance (i.e. it also deletes
 * the expired records). Each kind of audit record has its own sub type, which says which records and which archive type.
 * <p>
 * A record is expired if its expiry date is before the start of the request. Records without an expiry date are never archived.
 * <p>
 * If no record is expired, no archive is created.
 * <p>
 * The request requires an authenticated session. Restrict it further with role-based metadata (e.g. AllowRoles) where needed.
 * <p>
 * Result is {@link AuditArchivingResponse}
 */
@Abstract
public interface ArchiveExpiredAuditRecords extends AccessRequest, AuthorizedRequest {

	EntityType<ArchiveExpiredAuditRecords> T = EntityTypes.T(ArchiveExpiredAuditRecords.class);

	String recordsPerCsv = "recordsPerCsv";

	@Description("Maximum number of records in one CSV file of the archive. Must be at least 1.")
	@Initializer("10000")
	@Min("1")
	int getRecordsPerCsv();
	void setRecordsPerCsv(int recordsPerCsv);

	@Override
	EvalContext<? extends AuditArchivingResponse> eval(Evaluator<ServiceRequest> evaluator);

}
