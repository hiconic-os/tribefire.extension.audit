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
package tribefire.extension.audit.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Time to live of audit records, in milliseconds:
 * <ul>
 * <li>on a request type: of the service audit records of its requests,
 * <li>on an entity type: of the manipulation records of its instances (data audit).
 * </ul>
 * Such a record gets an expiry date, which is the time of recording plus the {@link #value() value}. Without this annotation, the record has no
 * expiry date, i.e. it never expires.
 * <ul>
 * <li>A negative value, e.g. {@link tribefire.extension.audit.model.deployment.meta.AuditTtl#NEVER AuditTtl.NEVER}, means the record never expires.
 * Use it to override an inherited TTL.
 * <li>An expiry date after 9999-12-31T23:59:59.999Z (the maximum of common SQL date types) is cut to that date.
 * </ul>
 *
 * @see tribefire.extension.audit.model.deployment.meta.AuditTtl
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface AuditTtl {

	long ONE_HOUR = tribefire.extension.audit.model.deployment.meta.AuditTtl.ONE_HOUR;
	long ONE_DAY = tribefire.extension.audit.model.deployment.meta.AuditTtl.ONE_DAY;
	long ONE_WEEK = tribefire.extension.audit.model.deployment.meta.AuditTtl.ONE_WEEK;
	long ONE_YEAR = tribefire.extension.audit.model.deployment.meta.AuditTtl.ONE_YEAR;

	long NEVER = tribefire.extension.audit.model.deployment.meta.AuditTtl.NEVER;

	String globalId() default "";
	long value();

}
