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
import com.braintribe.model.generic.annotation.SelectiveInformation;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.resource.Resource;

/**
 * A zip with the audit records which one archive run removed from the audit access, because they had expired.
 * <p>
 * The zip contains a manifest, CSV files with the records, and the payloads of the records (their {@link Resource} properties, if any). Each kind of
 * audit record has its own sub type.
 */
@Abstract
@SelectiveInformation("${resource.name}")
@Description("A zip with audit records which were removed from the audit access because they had expired.")
public interface AuditArchive extends GenericEntity {

	EntityType<AuditArchive> T = EntityTypes.T(AuditArchive.class);

	String resource = "resource";
	String formatVersion = "formatVersion";
	String cutoff = "cutoff";
	String recordCount = "recordCount";
	String firstRecordDate = "firstRecordDate";
	String lastRecordDate = "lastRecordDate";
	String creationDate = "creationDate";

	@Description("The zip with the manifest, the records as CSV files, and their payloads.")
	Resource getResource();
	void setResource(Resource resource);

	@Description("The version of the layout of the zip and its files.")
	int getFormatVersion();
	void setFormatVersion(int formatVersion);

	@Description("The archive run took the records whose expiry date was before this time.")
	Date getCutoff();
	void setCutoff(Date cutoff);

	@Description("Number of records in the zip.")
	long getRecordCount();
	void setRecordCount(long recordCount);

	@Description("The date of the oldest record in the zip.")
	Date getFirstRecordDate();
	void setFirstRecordDate(Date firstRecordDate);

	@Description("The date of the newest record in the zip.")
	Date getLastRecordDate();
	void setLastRecordDate(Date lastRecordDate);

	Date getCreationDate();
	void setCreationDate(Date creationDate);

}
