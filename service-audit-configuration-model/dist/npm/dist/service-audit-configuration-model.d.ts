// ************
// Types
// ************

import '@dev.hiconic/gm_root-model';
import '@dev.hiconic/gm_gm-core-api';

import { T } from '@dev.hiconic/hc-js-base';

export declare namespace meta {
	const groupId: string;
	const artifactId: string;
	const version: string;
}

export import ServiceAuditConfiguration = T.tribefire.extension.audit.model.configuration.ServiceAuditConfiguration;
export import ServiceAuditDomain = T.tribefire.extension.audit.model.configuration.ServiceAuditDomain;

declare module '@dev.hiconic/hc-js-base' {

	namespace T.tribefire.extension.audit.model.configuration {

		const ServiceAuditConfiguration: hc.reflection.EntityType<ServiceAuditConfiguration>;
		type ServiceAuditConfiguration = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"tribefire.extension.audit.model.configuration.ServiceAuditConfiguration", {
			domains: list<ServiceAuditDomain>;
		}>;

		const ServiceAuditDomain: hc.reflection.EntityType<ServiceAuditDomain>;
		type ServiceAuditDomain = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"tribefire.extension.audit.model.configuration.ServiceAuditDomain", {
			auditAccessId: string;
			serviceDomainId: string;
		}>;

	}

}
