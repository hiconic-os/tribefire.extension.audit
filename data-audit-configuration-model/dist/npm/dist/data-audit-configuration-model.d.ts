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

export import DataAuditAccess = T.tribefire.extension.audit.model.configuration.DataAuditAccess;
export import DataAuditConfiguration = T.tribefire.extension.audit.model.configuration.DataAuditConfiguration;

declare module '@dev.hiconic/hc-js-base' {

	namespace T.tribefire.extension.audit.model.configuration {

		const DataAuditAccess: hc.reflection.EntityType<DataAuditAccess>;
		type DataAuditAccess = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"tribefire.extension.audit.model.configuration.DataAuditAccess", {
			accessId: string;
			active: P<boolean, { nullable: false }>;
			aspectId: string;
			auditAccessId: string;
			untrackedRoles: set<string>;
		}>;

		const DataAuditConfiguration: hc.reflection.EntityType<DataAuditConfiguration>;
		type DataAuditConfiguration = T.com.braintribe.model.generic.GenericEntity &
		  Entity<"tribefire.extension.audit.model.configuration.DataAuditConfiguration", {
			accesses: list<DataAuditAccess>;
		}>;

	}

}
