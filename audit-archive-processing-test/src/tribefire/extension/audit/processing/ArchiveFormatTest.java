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
import static tribefire.extension.audit.processing.ArchiveExpiredAuditRecordsProcessor.counterWidth;
import static tribefire.extension.audit.processing.ArchiveExpiredAuditRecordsProcessor.formatValue;
import static tribefire.extension.audit.processing.ArchiveExpiredAuditRecordsProcessor.sanitizeFileName;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.braintribe.model.resource.Resource;

/** Tests the static helpers of {@link ArchiveExpiredAuditRecordsProcessor}; in the same package, as they are package private. */
public class ArchiveFormatTest {

	@Test
	public void fileNameKeepsSafeCharacters() {
		assertThat(sanitizeFileName("access.proventem")).isEqualTo("access.proventem");
		assertThat(sanitizeFileName("my access/1:x")).isEqualTo("my_access_1_x");
	}

	@Test
	public void counterHasAtLeastTwoDigits() {
		assertThat(counterWidth(1, 10_000)).isEqualTo(2);
		assertThat(counterWidth(990_000, 10_000)).isEqualTo(2);
	}

	@Test
	public void counterGrowsWithCsvCount() {
		assertThat(counterWidth(990_001, 10_000)).isEqualTo(3);
		assertThat(counterWidth(9_990_001, 10_000)).isEqualTo(4);
		assertThat(counterWidth(100, 1)).isEqualTo(3);
	}

	@Test
	public void valuesAreFormatted() {
		assertThat(formatValue(null)).isEmpty();
		assertThat(formatValue(42L)).isEqualTo("42");
		assertThat(formatValue(new Date(0))).isEqualTo("1970-01-01T00:00:00.000Z");
		assertThat(formatValue(Arrays.asList("a", "b"))).isEqualTo("a;b");

		Map<String, Integer> map = new LinkedHashMap<>();
		map.put("x", 1);
		map.put("y", 2);
		assertThat(formatValue(map)).isEqualTo("x=1;y=2");
	}

	@Test
	public void entityIsFormattedAsId() {
		Resource resource = Resource.T.create();
		resource.setId("resource-1");

		assertThat(formatValue(resource)).isEqualTo("resource-1");
	}

}
