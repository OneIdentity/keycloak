package org.keycloak.models.jpa.entities;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@NamedQueries({
        @NamedQuery(name="userHasOrganization", query="select o from UserOrganizationMappingEntity o where o.user = :user and o.organizationId = :organizationId"),
        @NamedQuery(name="getUsersByOrganization", query="select o from UserOrganizationMappingEntity o where o.organizationId = :organizationId order by o.user.username"),
        @NamedQuery(name="userOrganizations", query="select o from UserOrganizationMappingEntity o where o.user = :user"),
        @NamedQuery(name="userOrganizationIds", query="select o.organizationId from UserOrganizationMappingEntity o where o.user = :user"),
        @NamedQuery(name="deleteUserOrganizationMappingsByRealm", query="delete from UserOrganizationMappingEntity o where o.user IN (select u from UserEntity u where u.realmId=:realmId)"),
        @NamedQuery(name="deleteUserOrganizationMappingsByRealmAndLink", query="delete from UserOrganizationMappingEntity o where o.user IN (select u from UserEntity u where u.realmId=:realmId and u.federationLink=:link)"),
        @NamedQuery(name="deleteUserOrganizationMappingsByOrganization", query="delete from UserOrganizationMappingEntity o where o.organizationId = :organizationId"),
        @NamedQuery(name="deleteUserOrganizationMappingsByUser", query="delete from UserOrganizationMappingEntity o where o.user = :user")
})
@Table(name="USER_ORGANIZATION_MAPPING")
@Entity
@IdClass(UserOrganizationMappingEntity.Key.class)
public class UserOrganizationMappingEntity  {

    @Id
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="USER_ID")
    protected UserEntity user;

    @Id
    @Column(name = "ORGANIZATION_ID")
    private String organizationId;

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public static class Key implements Serializable {

        protected UserEntity user;

        protected String organizationId;

        public Key() {
        }

        public Key(UserEntity user, String organizationId) {
            this.user = user;
            this.organizationId = organizationId;
        }

        public UserEntity getUser() {
            return user;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (!organizationId.equals(key.organizationId)) return false;
            if (user != null ? !user.getId().equals(key.user != null ? key.user.getId() : null) : key.user != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = user != null ? user.getId().hashCode() : 0;
            result = 31 * result + organizationId.hashCode();
            return result;
        }
    }
}
