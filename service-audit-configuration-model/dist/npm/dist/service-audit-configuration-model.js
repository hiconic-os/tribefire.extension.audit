import '@dev.hiconic/gm_root-model';
import '@dev.hiconic/gm_gm-core-api';

import {T, hc} from '@dev.hiconic/hc-js-base';

export const meta = {
	groupId: "tribefire.extension.audit",
	artifactId: "service-audit-configuration-model",
	version: "2.0.1",
}

function modelAssembler($, P, _) {
//JSE version=4.0
//BEGIN_TYPES
P.a=$.T("com.braintribe.model.meta.GmMetaModel");
P.b=$.T("com.braintribe.model.meta.GmEntityType");
P.c=$.T("com.braintribe.model.meta.GmProperty");
P.d=$.T("com.braintribe.model.meta.GmListType");
P.e=$.T("com.braintribe.model.meta.GmStringType");
//END_TYPES
P.f=$.P(P.a,'name');P.g=$.P(P.a,'types');P.h=$.P(P.a,'version');P.i=$.P(P.b,'globalId');P.j=$.P(P.b,'isAbstract');P.k=$.P(P.b,'properties');P.l=$.P(P.b,'superTypes');
P.m=$.P(P.b,'typeSignature');P.n=$.P(P.c,'declaringType');P.o=$.P(P.c,'globalId');P.p=$.P(P.c,'name');P.q=$.P(P.c,'nullable');P.r=$.P(P.c,'type');P.s=$.P(P.d,'elementType');
P.t=$.P(P.d,'globalId');P.u=$.P(P.d,'typeSignature');P.v=$.P(P.e,'typeSignature');
P.w=$.C(P.a);P.x=$.C(P.b);P.y=$.C(P.b);P.z=$.C(P.c);P.A=$.C(P.b);P.B=$.C(P.c);P.C=$.C(P.c);P.D=$.C(P.d);P.E=$.C(P.e);
_=P.w;
$.s(_,P.f,"tribefire.extension.audit:service-audit-configuration-model");
$.s(_,P.g,$.S([P.x,P.y]));
$.s(_,P.h,"2.0.1");
_=P.x;
$.s(_,P.i,"type:tribefire.extension.audit.model.configuration.ServiceAuditConfiguration");
$.s(_,P.j,$.n);
$.s(_,P.k,$.L([P.z]));
$.s(_,P.l,$.L([P.A]));
$.s(_,P.m,"tribefire.extension.audit.model.configuration.ServiceAuditConfiguration");
_=P.y;
$.s(_,P.i,"type:tribefire.extension.audit.model.configuration.ServiceAuditDomain");
$.s(_,P.j,$.n);
$.s(_,P.k,$.L([P.B,P.C]));
$.s(_,P.l,$.L([P.A]));
$.s(_,P.m,"tribefire.extension.audit.model.configuration.ServiceAuditDomain");
_=P.z;
$.s(_,P.n,P.x);
$.s(_,P.o,"property:tribefire.extension.audit.model.configuration.ServiceAuditConfiguration/domains");
$.s(_,P.p,"domains");
$.s(_,P.q,$.y);
$.s(_,P.r,P.D);
_=P.A;
$.s(_,P.j,$.n);
$.s(_,P.m,"com.braintribe.model.generic.GenericEntity");
_=P.B;
$.s(_,P.n,P.y);
$.s(_,P.o,"property:tribefire.extension.audit.model.configuration.ServiceAuditDomain/auditAccessId");
$.s(_,P.p,"auditAccessId");
$.s(_,P.q,$.y);
$.s(_,P.r,P.E);
_=P.C;
$.s(_,P.n,P.y);
$.s(_,P.o,"property:tribefire.extension.audit.model.configuration.ServiceAuditDomain/serviceDomainId");
$.s(_,P.p,"serviceDomainId");
$.s(_,P.q,$.y);
$.s(_,P.r,P.E);
_=P.D;
$.s(_,P.s,P.y);
$.s(_,P.t,"type:list<tribefire.extension.audit.model.configuration.ServiceAuditDomain>");
$.s(_,P.u,"list<tribefire.extension.audit.model.configuration.ServiceAuditDomain>");
_=P.E;
$.s(_,P.v,"string");
return P.w;
[2317];
}

hc.reflection.internal.ensureModel(modelAssembler)

export const ServiceAuditConfiguration = T.tribefire.extension.audit.model.configuration.ServiceAuditConfiguration;
export const ServiceAuditDomain = T.tribefire.extension.audit.model.configuration.ServiceAuditDomain;
