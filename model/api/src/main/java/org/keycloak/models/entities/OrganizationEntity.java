package org.keycloak.models.entities;

import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:dane.barentine@software.dell.com">Dane Barentine</a>
 */
public class OrganizationEntity {
    private String id;
    private String name;
    private String description;
    private List<String> roleIds;
    private Map<String, String> attributes;
    private boolean enabled;

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

    public List<String> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void updateFromModel(OrganizationModel model) {
        this.setName(model.getName());
        this.setDescription(model.getDescription());

        //Clone attributes
        if (model.getAttributes() != null && !model.getAttributes().isEmpty()) {
            Map<String, String> attrs = new HashMap<>();
            attrs.putAll(model.getAttributes());
            this.setAttributes(attrs);
        } else {
            this.setAttributes(null);
        }

        this.setEnabled(model.isEnabled());

        List<String> roleIds = new ArrayList<>();
        for(RoleModel role : model.getRoleMappings()) {
            roleIds.add(role.getId());
        }

        this.setRoleIds(roleIds);
    }

    public static OrganizationEntity fromModel(OrganizationModel model) {
        OrganizationEntity entity = new OrganizationEntity();

        entity.setId(model.getId());
        entity.setName(model.getName());
        entity.setDescription(model.getDescription());

        //Clone attributes
        if (model.getAttributes() != null && !model.getAttributes().isEmpty()) {
            Map<String, String> attrs = new HashMap<>();
            attrs.putAll(model.getAttributes());
            entity.setAttributes(attrs);
        }

        entity.setEnabled(model.isEnabled());

        List<String> roleIds = new ArrayList<>();
        for(RoleModel role : model.getRoleMappings()) {
            roleIds.add(role.getId());
        }

        entity.setRoleIds(roleIds);

        return entity;
    }

    public OrganizationModel toModel(RealmModel realm) {
        OrganizationModel model = new OrganizationModel();

        model.setId(this.getId());
        model.setName(this.getName());
        model.setDescription(this.getDescription());

        //Clone attributes
        if (this.getAttributes() != null && !this.getAttributes().isEmpty()) {
            Map<String, String> attrs = new HashMap<>();
            attrs.putAll(this.getAttributes());
            model.setAttributes(attrs);
        }

        model.setEnabled(this.isEnabled());

        for(String roleId : this.getRoleIds()) {
            RoleModel role = realm.getRoleById(roleId);

            if(role != null) {
                model.grantRole(role);
            }
        }

        return model;
    }
}
