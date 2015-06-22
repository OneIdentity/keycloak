package org.keycloak.representations.idm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrganizationRepresentation {

    protected String id;
    protected String name;
    protected String description;
    protected List<String> realmRoles;
    protected Map<String, List<String>> clientRoles;
    protected Map<String, String> attributes;
    protected boolean enabled;

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

    public List<String> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(List<String> realmRoles) {
        this.realmRoles = realmRoles;
    }

    public Map<String, List<String>> getClientRoles() {
        return clientRoles;
    }

    public void setClientRoles(Map<String, List<String>> clientRoles) {
        this.clientRoles = clientRoles;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public OrganizationRepresentation attribute(String name, String value) {
        if (this.attributes == null) {
            attributes = new HashMap<>();
        }

        attributes.put(name, value);
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
