package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;
import org.keycloak.Config;
import org.keycloak.migration.MigrationProvider;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClaimMask;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class JpaUpdate1_3_0_Dell extends CustomKeycloakTask {

    private String realmTableName;

    @Override
    protected String getTaskId() {
        return "Update 1.3.0.Dell";
    }

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        realmTableName = database.correctObjectName("REALM", Table.class);

        try {
            addNewAdminRoles();
        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
        }
    }

    private void addNewAdminRoles() throws SQLException, DatabaseException{
        addNewMasterAdminRoles();
        addNewRealmAdminRoles();

        confirmationMessage.append("Adding new admin roles. ");
    }

    protected void addNewMasterAdminRoles() throws SQLException, DatabaseException {
        // Retrieve ID of admin role of master realm
        String adminRoleId = getAdminRoleId();
        String masterRealmId = Config.getAdminRealm();

        PreparedStatement statement = jdbcConnection.prepareStatement("select NAME from REALM");
        try {
            ResultSet resultSet = statement.executeQuery();
            try {
                while (resultSet.next()) {
                    String realmName = resultSet.getString("NAME");
                    String masterAdminAppName = realmName + "-realm";

                    PreparedStatement statement2 = jdbcConnection.prepareStatement("select ID from CLIENT where REALM_ID = ? AND NAME = ?");
                    statement2.setString(1, masterRealmId);
                    statement2.setString(2, masterAdminAppName);

                    try {
                        ResultSet resultSet2 = statement2.executeQuery();
                        try {
                            if (resultSet2.next()) {
                                String masterAdminAppId = resultSet2.getString("ID");

                                addAdminRole(AdminRoles.VIEW_ORGANIZATIONS, masterRealmId, masterAdminAppId, adminRoleId);
                                addAdminRole(AdminRoles.MANAGE_ORGANIZATIONS, masterRealmId, masterAdminAppId, adminRoleId);
                            } else {
                                throw new IllegalStateException("Couldn't find ID of '" + masterAdminAppName + "' application in 'master' realm. ");
                            }
                        } finally {
                            resultSet2.close();
                        }
                    } finally {
                        statement2.close();
                    }
                }
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    private String getAdminRoleId() throws SQLException, DatabaseException {
        PreparedStatement statement = jdbcConnection.prepareStatement("select ID from KEYCLOAK_ROLE where NAME = ? AND REALM = ?");
        statement.setString(1, AdminRoles.ADMIN);
        statement.setString(2, Config.getAdminRealm());

        try {
            ResultSet resultSet = statement.executeQuery();
            try {
                if (resultSet.next()) {
                    return resultSet.getString("ID");
                } else {
                    throw new IllegalStateException("Couldn't find ID of 'admin' role in 'master' realm");
                }
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }


    protected void addNewRealmAdminRoles() throws SQLException, DatabaseException {
        PreparedStatement statement = jdbcConnection.prepareStatement("select CLIENT.ID REALM_ADMIN_APP_ID, CLIENT.REALM_ID REALM_ID, KEYCLOAK_ROLE.ID ADMIN_ROLE_ID from CLIENT,KEYCLOAK_ROLE where KEYCLOAK_ROLE.APPLICATION = CLIENT.ID AND CLIENT.NAME = 'realm-management' AND KEYCLOAK_ROLE.NAME = ?");
        statement.setString(1, AdminRoles.REALM_ADMIN);

        try {
            ResultSet resultSet = statement.executeQuery();
            try {

                while (resultSet.next()) {
                    String realmAdminAppId = resultSet.getString("REALM_ADMIN_APP_ID");
                    String realmId = resultSet.getString("REALM_ID");
                    String adminRoleId = resultSet.getString("ADMIN_ROLE_ID");

                    addAdminRole(AdminRoles.VIEW_ORGANIZATIONS, realmId, realmAdminAppId, adminRoleId);
                    addAdminRole(AdminRoles.MANAGE_ORGANIZATIONS, realmId, realmAdminAppId, adminRoleId);
                }
            } finally {
                resultSet.close();
            }
        } finally {
            statement.close();
        }
    }

    private void addAdminRole(String roleName, String realmId, String applicationId, String realmAdminAppRoleId) {
        String roleTableName = database.correctObjectName("KEYCLOAK_ROLE", Table.class);
        String compositeRoleTableName = database.correctObjectName("COMPOSITE_ROLE", Table.class);
        String newRoleId = KeycloakModelUtils.generateId();

        InsertStatement insertRole = new InsertStatement(null, null, roleTableName)
                .addColumnValue("ID", newRoleId)
                .addColumnValue("APP_REALM_CONSTRAINT", applicationId)
                .addColumnValue("APPLICATION_ROLE", true)
                .addColumnValue("NAME", roleName)
                .addColumnValue("REALM_ID", realmId)
                .addColumnValue("APPLICATION", applicationId);

        // Add newly created role to the composite roles of 'realm-admin' role
        InsertStatement insertCompRole = new InsertStatement(null, null, compositeRoleTableName)
                .addColumnValue("COMPOSITE", realmAdminAppRoleId)
                .addColumnValue("CHILD_ROLE", newRoleId);

        statements.add(insertRole);
        statements.add(insertCompRole);
    }
}
