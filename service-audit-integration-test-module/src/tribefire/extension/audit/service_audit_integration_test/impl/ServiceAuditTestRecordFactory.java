// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package tribefire.extension.audit.service_audit_integration_test.impl;

import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import tribefire.extension.audit.model.service.audit.api.CreateServiceAuditRecord;
import tribefire.extension.audit.model.test.TestServiceAuditRecord;
import tribefire.extension.audit.model.test.api.GetPersonData;

public class ServiceAuditTestRecordFactory implements ServiceProcessor<CreateServiceAuditRecord, TestServiceAuditRecord> {

	@Override
	public TestServiceAuditRecord process(ServiceRequestContext requestContext, CreateServiceAuditRecord request) {
		GetPersonData payloadRequest = (GetPersonData) request.getRequest();
		
		TestServiceAuditRecord record = TestServiceAuditRecord.T.create();
		
		record.setPersonId(payloadRequest.getPersonId());
		
		return record;
	}
}
