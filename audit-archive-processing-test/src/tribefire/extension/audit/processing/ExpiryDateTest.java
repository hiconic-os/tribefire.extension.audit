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

import static org.assertj.core.api.Assertions.assertThat;
import static tribefire.extension.audit.processing.AuditExpiry.MAX_EXPIRY_MS;
import static tribefire.extension.audit.processing.AuditExpiry.computeExpiryDate;

import java.util.Date;

import org.junit.Test;

import tribefire.extension.audit.model.deployment.meta.AuditTtl;

/** Tests {@link AuditExpiry}. */
public class ExpiryDateTest {

	private static final long NOW = 1_763_200_000_000L; // 2025-11-15

	@Test
	public void ttlIsAddedToNow() {
		assertThat(computeExpiryDate(NOW, 90_000L)).isEqualTo(new Date(NOW + 90_000L));
	}

	@Test
	public void zeroTtlExpiresNow() {
		assertThat(computeExpiryDate(NOW, 0)).isEqualTo(new Date(NOW));
	}

	@Test
	public void negativeTtlNeverExpires() {
		assertThat(computeExpiryDate(NOW, AuditTtl.NEVER)).isNull();
		assertThat(computeExpiryDate(NOW, Long.MIN_VALUE)).isNull();
	}

	@Test
	public void maxExpiryIsKept() {
		assertThat(computeExpiryDate(NOW, MAX_EXPIRY_MS - NOW)).isEqualTo(new Date(MAX_EXPIRY_MS));
	}

	@Test
	public void expiryAfterMaxIsCut() {
		assertThat(computeExpiryDate(NOW, MAX_EXPIRY_MS - NOW + 1)).isEqualTo(new Date(MAX_EXPIRY_MS));
		assertThat(computeExpiryDate(NOW, Long.MAX_VALUE)).isEqualTo(new Date(MAX_EXPIRY_MS));
	}

}
