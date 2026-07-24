import '@dev.hiconic/gm_root-model';
import '@dev.hiconic/gm_gm-core-api';

import {T, hc} from '@dev.hiconic/hc-js-base';

export const meta = {
	groupId: "tribefire.extension.audit",
	artifactId: "data-audit-configuration-model",
	version: "2.0.1",
}

function modelAssembler($, P, _) {
//JSE version=4.0
//BEGIN_TYPES
P.a=$.T("com.braintribe.model.meta.GmMetaModel");
P.b=$.T("com.braintribe.model.meta.GmEntityType");
P.c=$.T("com.braintribe.model.meta.GmProperty");
P.d=$.T("com.braintribe.model.meta.GmStringType");
P.e=$.T("com.braintribe.model.meta.GmBooleanType");
P.f=$.T("com.braintribe.model.meta.GmSetType");
P.g=$.T("com.braintribe.model.meta.GmListType");
//END_TYPES
P.h=$.P(P.a,'name');P.i=$.P(P.a,'types');P.j=$.P(P.a,'version');P.k=$.P(P.b,'globalId');P.l=$.P(P.b,'isAbstract');P.m=$.P(P.b,'properties');P.n=$.P(P.b,'superTypes');
P.o=$.P(P.b,'typeSignature');P.p=$.P(P.c,'declaringType');P.q=$.P(P.c,'globalId');P.r=$.P(P.c,'name');P.s=$.P(P.c,'nullable');P.t=$.P(P.c,'type');P.u=$.P(P.c,'initializer');
P.v=$.P(P.d,'typeSignature');P.w=$.P(P.e,'typeSignature');P.x=$.P(P.f,'elementType');P.y=$.P(P.f,'globalId');P.z=$.P(P.f,'typeSignature');P.A=$.P(P.g,'elementType');
P.B=$.P(P.g,'globalId');P.C=$.P(P.g,'typeSignature');
P.D=$.C(P.a);P.E=$.C(P.b);P.F=$.C(P.b);P.G=$.C(P.c);P.H=$.C(P.c);P.I=$.C(P.c);P.J=$.C(P.c);P.K=$.C(P.c);P.L=$.C(P.b);P.M=$.C(P.c);P.N=$.C(P.d);P.O=$.C(P.e);P.P=$.C(P.f);
P.Q=$.C(P.g);
_=P.D;
$.s(_,P.h,"tribefire.extension.audit:data-audit-configuration-model");
$.s(_,P.i,$.S([P.E,P.F]));
$.s(_,P.j,"2.0.1");
_=P.E;
$.s(_,P.k,"type:tribefire.extension.audit.model.configuration.DataAuditAccess");
$.s(_,P.l,$.n);
$.s(_,P.m,$.L([P.G,P.H,P.I,P.J,P.K]));
$.s(_,P.n,$.L([P.L]));
$.s(_,P.o,"tribefire.extension.audit.model.configuration.DataAuditAccess");
_=P.F;
$.s(_,P.k,"type:tribefire.extension.audit.model.configuration.DataAuditConfiguration");
$.s(_,P.l,$.n);
$.s(_,P.m,$.L([P.M]));
$.s(_,P.n,$.L([P.L]));
$.s(_,P.o,"tribefire.extension.audit.model.configuration.DataAuditConfiguration");
_=P.G;
$.s(_,P.p,P.E);
$.s(_,P.q,"property:tribefire.extension.audit.model.configuration.DataAuditAccess/accessId");
$.s(_,P.r,"accessId");
$.s(_,P.s,$.y);
$.s(_,P.t,P.N);
_=P.H;
$.s(_,P.p,P.E);
$.s(_,P.q,"property:tribefire.extension.audit.model.configuration.DataAuditAccess/active");
$.s(_,P.u,$.y);
$.s(_,P.r,"active");
$.s(_,P.s,$.n);
$.s(_,P.t,P.O);
_=P.I;
$.s(_,P.p,P.E);
$.s(_,P.q,"property:tribefire.extension.audit.model.configuration.DataAuditAccess/aspectId");
$.s(_,P.u,"data-audit");
$.s(_,P.r,"aspectId");
$.s(_,P.s,$.y);
$.s(_,P.t,P.N);
_=P.J;
$.s(_,P.p,P.E);
$.s(_,P.q,"property:tribefire.extension.audit.model.configuration.DataAuditAccess/auditAccessId");
$.s(_,P.r,"auditAccessId");
$.s(_,P.s,$.y);
$.s(_,P.t,P.N);
_=P.K;
$.s(_,P.p,P.E);
$.s(_,P.q,"property:tribefire.extension.audit.model.configuration.DataAuditAccess/untrackedRoles");
$.s(_,P.r,"untrackedRoles");
$.s(_,P.s,$.y);
$.s(_,P.t,P.P);
_=P.L;
$.s(_,P.l,$.n);
$.s(_,P.o,"com.braintribe.model.generic.GenericEntity");
_=P.M;
$.s(_,P.p,P.F);
$.s(_,P.q,"property:tribefire.extension.audit.model.configuration.DataAuditConfiguration/accesses");
$.s(_,P.r,"accesses");
$.s(_,P.s,$.y);
$.s(_,P.t,P.Q);
_=P.N;
$.s(_,P.v,"string");
_=P.O;
$.s(_,P.w,"boolean");
_=P.P;
$.s(_,P.x,P.N);
$.s(_,P.y,"type:set<string>");
$.s(_,P.z,"set<string>");
_=P.Q;
$.s(_,P.A,P.E);
$.s(_,P.B,"type:list<tribefire.extension.audit.model.configuration.DataAuditAccess>");
$.s(_,P.C,"list<tribefire.extension.audit.model.configuration.DataAuditAccess>");
return P.D;
[3264];
}

hc.reflection.internal.ensureModel(modelAssembler)

export const DataAuditAccess = T.tribefire.extension.audit.model.configuration.DataAuditAccess;
export const DataAuditConfiguration = T.tribefire.extension.audit.model.configuration.DataAuditConfiguration;
