package org.keycloak.models.jpa.entities;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="organizationHasRole", query="select m from OrganizationRoleMappingEntity m where m.organization = :organization and m.roleId = :roleId"),
        @NamedQuery(name="organizationRoleMappings", query="select m from OrganizationRoleMappingEntity m where m.organization = :organization"),
        @NamedQuery(name="organizationRoleMappingIds", query="select m.roleId from OrganizationRoleMappingEntity m where m.organization = :organization"),
        @NamedQuery(name="deleteOrganizationRoleMappingsByRealm", query="delete from OrganizationRoleMappingEntity m where m.organization IN (select o from OrganizationEntity o where o.realm=:realm)"),
        @NamedQuery(name="deleteOrganizationRoleMappingsByRole", query="delete from OrganizationRoleMappingEntity m where m.roleId = :roleId"),
        @NamedQuery(name="deleteOrganizationRoleMappingsByUser", query="delete from OrganizationRoleMappingEntity m where m.organization = :organization")
})
@Table(name="ORGANIZATION_ROLE_MAPPING")
@Entity
@IdClass(OrganizationRoleMappingEntity.Key.class)
public class OrganizationRoleMappingEntity {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="ORGANIZATION_ID")
    protected OrganizationEntity organization;

    @Id
    @Column(name = "ROLE_ID")
    protected String roleId;

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public static class Key implements Serializable {

        protected OrganizationEntity organization;

        protected String roleId;

        public Key() {
        }

        public Key(OrganizationEntity organization, String roleId) {
            this.organization = organization;
            this.roleId = roleId;
        }

        public OrganizationEntity getOrganization() {
            return organization;
        }

        public String getRoleId() {
            return roleId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!roleId.equals(key.roleId)) return false;
            if (!organization.equals(key.organization)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = organization.hashCode();
            result = 31 * result + roleId.hashCode();
            return result;
        }
    }
}
