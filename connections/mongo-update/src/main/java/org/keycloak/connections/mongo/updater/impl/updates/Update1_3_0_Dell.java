package org.keycloak.connections.mongo.updater.impl.updates;

import com.mongodb.*;
import org.keycloak.Config;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Update1_3_0_Dell extends Update {

    @Override
    public String getId() {
        return "1.3.0.Dell";
    }

    @Override
    public void update(KeycloakSession session) {
        addNewAdminRoles();
    }


    private void addNewAdminRoles() {
        DBCollection realms = db.getCollection("realms");
        String adminRealmName = Config.getAdminRealm();

        DBCursor realmsCursor = realms.find();
        try {
            while (realmsCursor.hasNext()) {
                BasicDBObject realm = (BasicDBObject) realmsCursor.next();
                if (adminRealmName.equals(realm.get("name"))) {
                    addNewAdminRolesToMasterRealm(realm);
                } else {
                    addNewAdminRolesToRealm(realm);
                }
            }
        } finally {
            realmsCursor.close();
        }
    }

    private void addNewAdminRolesToMasterRealm(BasicDBObject adminRealm) {
        DBCollection realms = db.getCollection("realms");
        DBCollection clients = db.getCollection("clients");
        DBCollection roles = db.getCollection("roles");

        DBCursor realmsCursor = realms.find();
        try {
            while (realmsCursor.hasNext()) {
                BasicDBObject currentRealm = (BasicDBObject) realmsCursor.next();
                String masterAdminClientName = currentRealm.getString("name") + "-realm";

                BasicDBObject masterAdminClient = (BasicDBObject) clients.findOne(new BasicDBObject().append("realmId", adminRealm.get("_id")).append("clientId", masterAdminClientName));

                String viewIdProvidersRoleId = insertClientRole(roles, AdminRoles.VIEW_ORGANIZATIONS, masterAdminClient.getString("_id"));
                String manageIdProvidersRoleId = insertClientRole(roles, AdminRoles.MANAGE_ORGANIZATIONS, masterAdminClient.getString("_id"));

                BasicDBObject adminRole = (BasicDBObject) roles.findOne(new BasicDBObject().append("realmId", adminRealm.get("_id")).append("name", AdminRoles.ADMIN));
                BasicDBList adminCompositeRoles = (BasicDBList) adminRole.get("compositeRoleIds");
                adminCompositeRoles.add(viewIdProvidersRoleId);
                adminCompositeRoles.add(manageIdProvidersRoleId);
                roles.save(adminRole);

                log.debugv("Added roles {0} and {1} to client {2}", AdminRoles.VIEW_ORGANIZATIONS, AdminRoles.MANAGE_ORGANIZATIONS, masterAdminClientName);
            }
        } finally {
            realmsCursor.close();
        }
    }

    private void addNewAdminRolesToRealm(BasicDBObject currentRealm) {
        DBCollection clients = db.getCollection("clients");
        DBCollection roles = db.getCollection("roles");

        BasicDBObject adminClient = (BasicDBObject) clients.findOne(new BasicDBObject().append("realmId", currentRealm.get("_id")).append("clientId", "realm-management"));

        String viewIdProvidersRoleId = insertClientRole(roles, AdminRoles.VIEW_ORGANIZATIONS, adminClient.getString("_id"));
        String manageIdProvidersRoleId = insertClientRole(roles, AdminRoles.MANAGE_ORGANIZATIONS, adminClient.getString("_id"));

        BasicDBObject adminRole = (BasicDBObject) roles.findOne(new BasicDBObject().append("clientId", adminClient.get("_id")).append("name", AdminRoles.REALM_ADMIN));
        BasicDBList adminCompositeRoles = (BasicDBList) adminRole.get("compositeRoleIds");
        adminCompositeRoles.add(viewIdProvidersRoleId);
        adminCompositeRoles.add(manageIdProvidersRoleId);

        roles.save(adminRole);
        log.debugv("Added roles {0} and {1} to client realm-management of realm {2}", AdminRoles.VIEW_ORGANIZATIONS, AdminRoles.MANAGE_ORGANIZATIONS, currentRealm.get("name"));
    }

    private String insertClientRole(DBCollection roles, String roleName, String clientId) {
        BasicDBObject role = new BasicDBObject();
        String roleId = KeycloakModelUtils.generateId();
        role.append("_id", roleId);
        role.append("name", roleName);
        role.append("clientId", clientId);
        role.append("nameIndex", clientId + "//" + roleName);
        roles.insert(role);
        return roleId;
    }
}
