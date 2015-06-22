package org.keycloak.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:dane.barentine@software.dell.com">Dane Barentine</a>
 */

public class OrganizationModel {
    private String id;
    private String name;
    private String description;
    protected Set<RoleModel> roleMappings = new HashSet<>();
    private Map<String, String> attributes = new HashMap<>();
    private boolean enabled;

    public OrganizationModel() {

    }

    public OrganizationModel(OrganizationModel model) {
        this.id = model.getId();
        this.name = model.getName();
        this.description = model.getDescription();
        this.roleMappings = model.getRoleMappings();
        this.attributes = model.getAttributes();
        this.enabled = model.isEnabled();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Set<RoleModel> getRealmRoleMappings() {
        Set<RoleModel> roleMappings = getRoleMappings();
        Set<RoleModel> realmMappings = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof RealmModel) {
                realmMappings.add(role);
            }
        }
        return realmMappings;
    }

    public Set<RoleModel> getClientRoleMappings() {
        Set<RoleModel> roleMappings = getRoleMappings();
        Set<RoleModel> clientMappings = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof ClientModel) {
                clientMappings.add(role);
            }
        }
        return clientMappings;
    }

    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        Set<RoleModel> roleMappings = getRoleMappings();
        Set<RoleModel> clientMappings = new HashSet<RoleModel>();
        for (RoleModel role : roleMappings) {
            RoleContainerModel container = role.getContainer();
            if (container instanceof ClientModel) {
                if (((ClientModel) container).getId().equals(app.getId())) {
                    clientMappings.add(role);
                }
            }
        }
        return clientMappings;
    }

    public boolean hasRole(RoleModel role) {
        if (getRoleMappings().contains(role)) {
            return true;
        }

        //check composite roles
        Set<RoleModel> mappings = getRoleMappings();
        for (RoleModel mapping: mappings) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }

    public void grantRole(RoleModel role) {
        getRoleMappings().add(role);
    }

    public Set<RoleModel> getRoleMappings() {
        return roleMappings;
    }

    public void setRoleMappings(Set<RoleModel> roleMappings) {
        this.roleMappings = roleMappings;
    }

    public void deleteRoleMapping(RoleModel role) {
        getRoleMappings().remove(role);
    }
}
