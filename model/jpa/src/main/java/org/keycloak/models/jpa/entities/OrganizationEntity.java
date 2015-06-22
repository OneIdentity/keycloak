package org.keycloak.models.jpa.entities;

import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name="ORGANIZATION")
@NamedQueries({
        @NamedQuery(name="findOrganizationByName", query="select organization from OrganizationEntity organization where organization.name = :name")
})
public class OrganizationEntity {
    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Column(name="NAME")
    protected String name;

    @Column(name="DESCRIPTION")
    protected String description;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy="organization")
    protected Collection<OrganizationRoleMappingEntity> roles = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy="organization")
    protected Collection<OrganizationAttributeEntity> attributes = new ArrayList<>();

    @Column(name="ENABLED")
    private boolean enabled;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RealmEntity getRealm() {
        return this.realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
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

    public Collection<OrganizationAttributeEntity> getAttributes() {
        return attributes;
    }

    public void setAttributes(Collection<OrganizationAttributeEntity> attributes) {
        this.attributes = attributes;
    }

    public Collection<OrganizationRoleMappingEntity> getRoles() {
        return roles;
    }

    public void setRoles(Collection<OrganizationRoleMappingEntity> roles) {
        this.roles = roles;
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
        replaceRoles(model.getRoleMappings());
        replaceAttributes(model.getAttributes());
        this.setEnabled(model.isEnabled());
    }

    protected void replaceAttributes(Map<String, String> newAttributes) {
        //TODO: This seems a little quirky. It seems like we should be able to just clear the collection and then add the new ones. But JPA doesn't like that
        //because of the PK constraint. It seems like it adds items first and then remove items which can lead to duplicate keys by clearing/adding.

        //We need a map so that we can search by name
        Map<String, OrganizationAttributeEntity> attributesByName = new HashMap<>();
        for(OrganizationAttributeEntity attributeEntity : this.attributes) {
            attributesByName.put(attributeEntity.getName(), attributeEntity);
        }

        //if model contains an attribute that is not in this.attributes then add it to this.attributes
        //store off ids so we can check the remove case next
        Set<String> attributeNames = new HashSet<>();
        for(Map.Entry<String, String> entry : newAttributes.entrySet()) {
            attributeNames.add(entry.getKey());

            if(!attributesByName.containsKey(entry.getKey())) {
                OrganizationAttributeEntity attribute = new OrganizationAttributeEntity();
                attribute.setOrganization(this);
                attribute.setName(entry.getKey());
                attribute.setValue(entry.getValue());

                this.attributes.add(attribute);
            }
            else {
                //If it does exist already than update the value
                attributesByName.get(entry.getKey()).setValue(entry.getValue());
            }
        }

        //if this.attributes contains an attribute that is not in model then remove it from this.attributes
        //use the map instead of the original collection so that we can remove items from the original collection
        for(OrganizationAttributeEntity attribute : attributesByName.values()) {
            if(!attributeNames.contains(attribute.getName())) {
                this.attributes.remove(attribute);
            }
        }
    }

    protected void replaceRoles(Set<RoleModel> newRoles) {
        //TODO: This seems a little quirky. It seems like we should be able to just clear the collection and then add the new ones. But JPA doesn't like that
        //because of the PK constraint. It seems like it adds items first and then remove items which can lead to duplicate keys by clearing/adding.
        //We need a map so that we can search by ID
        Map<String, OrganizationRoleMappingEntity> roleEntityIds = new HashMap<>();
        for(OrganizationRoleMappingEntity mappingEntity : this.roles) {
            roleEntityIds.put(mappingEntity.getRoleId(), mappingEntity);
        }

        //if model contains a role that is not in currentMappingEntities then add it to currentMappingEntities
        //store off ids so we can check the remove case next
        Set<String> roleModelIds = new HashSet<>();
        for(RoleModel role : newRoles) {
            roleModelIds.add(role.getId());

            if(!roleEntityIds.containsKey(role.getId())) {
                OrganizationRoleMappingEntity mapping = new OrganizationRoleMappingEntity();
                mapping.setOrganization(this);
                mapping.setRoleId(role.getId());

                this.roles.add(mapping);
            }
        }

        //if currentMappingEntities contains a role that is not in model then remove it from currentMappingEntities
        //use the map instead of the original collection so that we can remove items from the original collection
        for(OrganizationRoleMappingEntity role : roleEntityIds.values()) {
            if(!roleModelIds.contains(role.getRoleId())) {
                this.roles.remove(role);
            }
        }
    }

    public static OrganizationEntity fromModel(OrganizationModel model) {
        OrganizationEntity entity = new OrganizationEntity();

        entity.setId(model.getId());
        entity.setName(model.getName());
        entity.setDescription(model.getDescription());
        entity.setEnabled(model.isEnabled());

        List<OrganizationAttributeEntity> attributes = new ArrayList<>();
        for(Map.Entry<String, String> entry: model.getAttributes().entrySet()) {
            OrganizationAttributeEntity attribute = new OrganizationAttributeEntity();
            attribute.setOrganization(entity);
            attribute.setName(entry.getKey());
            attribute.setValue(entry.getValue());

            attributes.add(attribute);
        }

        entity.setAttributes(attributes);

        List<OrganizationRoleMappingEntity> roleMappingEntities = new ArrayList<>();
        for(RoleModel role : model.getRoleMappings()) {
            OrganizationRoleMappingEntity mapping = new OrganizationRoleMappingEntity();
            mapping.setOrganization(entity);
            mapping.setRoleId(role.getId());

            roleMappingEntities.add(mapping);
        }

        entity.setRoles(roleMappingEntities);

        return entity;
    }

    public OrganizationModel toModel(RealmModel realm) {
        OrganizationModel model = new OrganizationModel();

        model.setId(this.getId());
        model.setName(this.getName());
        model.setDescription(this.getDescription());
        model.setEnabled(this.isEnabled());

        for(OrganizationAttributeEntity attribute : this.getAttributes()) {
            model.setAttribute(attribute.getName(), attribute.getValue());
        }

        for(OrganizationRoleMappingEntity roleMappingEntity : this.getRoles()) {
            RoleModel role = realm.getRoleById(roleMappingEntity.getRoleId());

            if(role != null) {
                model.grantRole(role);
            }
        }

        return model;
    }

}