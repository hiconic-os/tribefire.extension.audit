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
package tribefire.extension.audit.processing;

import java.time.Instant;
import java.util.Date;

import tribefire.extension.audit.model.deployment.meta.AuditTtl;

/**
 * The expiry date of an audit record, from the {@link AuditTtl} of the audited request type or manipulated entity type.
 * <p>
 * The expiry date must be computed from the time when the record is stored: an archive run selects the records with an expiry date before its start,
 * and a record which is stored after that start must not have an earlier expiry date (see {@link ArchiveExpiredAuditRecordsProcessor}).
 */
public class AuditExpiry {

	/** The latest expiry date, which is the maximum of common SQL date types. */
	public static final long MAX_EXPIRY_MS = Instant.parse("9999-12-31T23:59:59.999Z").toEpochMilli();

	/**
	 * @param auditTtl
	 *            the resolved metadata, may be null
	 * @return null if there is no TTL, otherwise see {@link #computeExpiryDate(long, long)}
	 */
	public static Date expiryDate(AuditTtl auditTtl, long nowMs) {
		return auditTtl == null ? null : computeExpiryDate(nowMs, auditTtl.getTtlInMs());
	}

	/**
	 * @param nowMs
	 *            the current time; a parameter rather than read here, so that a test can pass a fixed time
	 * @return null for a negative TTL (never expires), otherwise <code>nowMs + ttlInMs</code>, but at most {@link #MAX_EXPIRY_MS}
	 */
	public static Date computeExpiryDate(long nowMs, long ttlInMs) {
		if (ttlInMs < 0)
			return null;

		// the subtraction cannot overflow, unlike the sum
		if (ttlInMs > MAX_EXPIRY_MS - nowMs)
			return new Date(MAX_EXPIRY_MS);

		return new Date(nowMs + ttlInMs);
	}

}
