package org.keycloak.broker.provider;

import org.jboss.logging.Logger;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class UserOrganizationMapper extends AbstractIdentityProviderMapper {
    private static final Logger log = Logger.getLogger(UserOrganizationMapper.class);

    public static final String[] COMPATIBLE_PROVIDERS = {ANY_PROVIDER};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String ORG_NAME = "organization.name";
    public static final String ORG_ID = "organization.id";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ORG_NAME);
        property.setLabel("Organization Name");
        property.setHelpText("Name of the organizations to associate with the user.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ORG_ID);
        property.setLabel("Organization Id");
        property.setHelpText("Id of the organizations to associate with the user. If specified the Organization Name is ignored");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "user-organization-idp-mapper";

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Organization Mapper";
    }

    @Override
    public String getDisplayType() {
        return "Organization Mapper";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        addUserToOrganization(realm, user, mapperModel);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        addUserToOrganization(realm, user, mapperModel);
    }

    protected void addUserToOrganization(RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel) {
        OrganizationModel org = getOrganization(realm, mapperModel);

        if(org == null) {
            log.error(String.format("Organization specified by mapper %s for IDP %s was not found", mapperModel.getIdentityProviderMapper(), mapperModel.getIdentityProviderAlias()));
            return;
        }

        if(!user.hasOrganization(org)) {
            user.addOrganization(org);
        }
    }

    protected OrganizationModel getOrganization(RealmModel realm, IdentityProviderMapperModel mapperModel) {
        OrganizationModel org = null;
        String orgId = mapperModel.getConfig().get(ORG_ID);
        org = realm.getOrganizationById(orgId);

        if(org == null) {
            orgId = mapperModel.getConfig().get(ORG_NAME);
            org = realm.getOrganizationByName(orgId);
        }

        return org;
    }

    @Override
    public String getHelpText() {
        return "Associate user with specified organization.";
    }
}
