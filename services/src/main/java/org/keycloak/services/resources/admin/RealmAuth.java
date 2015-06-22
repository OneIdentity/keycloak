package org.keycloak.services.resources.admin;

import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.services.ForbiddenException;


/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RealmAuth {

    private Resource resource;

    public enum Resource {
        CLIENT, USER, REALM, EVENTS, IDENTITY_PROVIDER, ORGANIZATION
    }

    private AdminAuth auth;
    private ClientModel realmAdminApp;

    public RealmAuth(AdminAuth auth, ClientModel realmAdminApp) {
        this.auth = auth;
        this.realmAdminApp = realmAdminApp;
    }

    public RealmAuth init(Resource resource) {
        this.resource = resource;
        return this;
    }

    public void requireAny() {
        if (!auth.hasOneOfAppRole(realmAdminApp, AdminRoles.ALL_REALM_ROLES)) {
            throw new ForbiddenException();
        }
    }

    public boolean hasView() {
        return auth.hasOneOfAppRole(realmAdminApp, getViewRole(resource), getManageRole(resource));
    }

    public boolean hasManage() {
        return auth.hasOneOfAppRole(realmAdminApp, getManageRole(resource));
    }

    public void requireView() {
        if (!hasView()) {
            throw new ForbiddenException();
        }
    }

    public void requireManage() {
        if (!hasManage()) {
            throw new ForbiddenException();
        }
    }

    private String getViewRole(Resource resource) {
        switch (resource) {
            case CLIENT:
                return AdminRoles.VIEW_CLIENTS;
            case USER:
                return AdminRoles.VIEW_USERS;
            case REALM:
                return AdminRoles.VIEW_REALM;
            case EVENTS:
                return AdminRoles.VIEW_EVENTS;
            case IDENTITY_PROVIDER:
                return AdminRoles.VIEW_IDENTITY_PROVIDERS;
            case ORGANIZATION:
                return AdminRoles.VIEW_ORGANIZATIONS;
            default:
                throw new IllegalStateException();
        }
    }

    private String getManageRole(Resource resource) {
        switch (resource) {
            case CLIENT:
                return AdminRoles.MANAGE_CLIENTS;
            case USER:
                return AdminRoles.MANAGE_USERS;
            case REALM:
                return AdminRoles.MANAGE_REALM;
            case EVENTS:
                return AdminRoles.MANAGE_EVENTS;
            case IDENTITY_PROVIDER:
                return AdminRoles.MANAGE_IDENTITY_PROVIDERS;
            case ORGANIZATION:
                return AdminRoles.MANAGE_ORGANIZATIONS;
            default:
                throw new IllegalStateException();
        }
    }

}
