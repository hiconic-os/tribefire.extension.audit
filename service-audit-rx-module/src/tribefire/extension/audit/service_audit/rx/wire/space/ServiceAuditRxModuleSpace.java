package tribefire.extension.audit.service_audit.rx.wire.space;

import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.AccessDataModelConfiguration;
import hiconic.rx.access.module.api.AccessServiceModelConfiguration;
import hiconic.rx.access.module.impl.RxModelAccessoryFactory;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ServiceProcessorRegistration;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import tribefire.extension.audit._ServiceAuditModel_;
import tribefire.extension.audit.model.ServiceAuditRecord;
import tribefire.extension.audit.model.configuration.ServiceAuditConfiguration;
import tribefire.extension.audit.model.configuration.ServiceAuditDomain;
import tribefire.extension.audit.model.deployment.meta.CreateServiceAuditRecordWithKey;
import tribefire.extension.audit.model.service.audit.api.CreateServiceAuditRecord;
import tribefire.extension.audit.processing.ServiceAuditInterceptor;

@Managed
public class ServiceAuditRxModuleSpace implements RxModuleContract {
	public static final String INTERCEPTOR_ID = "service-audit";

	@Import private RxPlatformContract platform;
	@Import private AccessContract access;

	@Override
	public void configureModels(ModelConfigurations configurations) {
		for (ServiceAuditDomain domain : configuration().getDomains()) {
			AccessServiceModelConfiguration serviceModel =
					access.accessModelConfigurations().serviceModelConfiguration(domain.getServiceDomainId());
			serviceModel.bindInterceptor(INTERCEPTOR_ID).forType(ServiceRequest.T).bind(() -> serviceAuditInterceptor(domain));

			AccessDataModelConfiguration auditDataModel =
					access.accessModelConfigurations().dataModelConfiguration(domain.getAuditAccessId());
			auditDataModel.addModel(_ServiceAuditModel_.reflection);
		}
	}

	@Managed
	private ServiceAuditConfiguration configuration() {
		return platform.configuration().readConfig(ServiceAuditConfiguration.T).get();
	}

	private ServiceAuditInterceptor serviceAuditInterceptor(ServiceAuditDomain domain) {
		ServiceAuditInterceptor bean = new ServiceAuditInterceptor();
		bean.setAuditAccessId(domain.getAuditAccessId());
		bean.setModelAccessoryFactory(RxModelAccessoryFactory.createContextMaf(platform, access));
		bean.setMarshallerRegistry(platform.marshalling().marshallers());
		bean.setSystemSessionFactory(access.systemSessionFactory());
		bean.setRecordFactoryResolver((cmdResolver, request) -> {
			CreateServiceAuditRecordWithKey createWith = cmdResolver.getMetaData().entity(request)
					.meta(CreateServiceAuditRecordWithKey.T).exclusive();
			if (createWith == null || isBlank(createWith.getRecordFactoryKey()))
				return null;

			ServiceProcessorRegistration registration = platform.serviceProcessing().serviceProcessorRegistry()
					.require(createWith.getRecordFactoryKey());
			return (requestContext, createRecord) -> createRecord(registration, requestContext, createRecord);
		});
		return bean;
	}

	@SuppressWarnings("unchecked")
	private ServiceAuditRecord createRecord(ServiceProcessorRegistration registration, ServiceRequestContext context,
			CreateServiceAuditRecord request) {
		ServiceProcessor<CreateServiceAuditRecord, ServiceAuditRecord> processor =
				(ServiceProcessor<CreateServiceAuditRecord, ServiceAuditRecord>) registration.processorSupplier().get();
		return processor.process(context, request);
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
