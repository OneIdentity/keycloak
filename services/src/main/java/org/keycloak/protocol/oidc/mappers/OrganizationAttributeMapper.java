package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappings UserModel.attribute to an ID Token claim.  Token claim name can be a full qualified nested object name,
 * i.e. "address.country".  This will create a nested
 * json object within the toke claim.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OrganizationAttributeMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String ORGANIZATION_ATTRIBUTE = "organization.attribute";
    public static final String ORGANIZATION_MODEL_ATTRIBUTE_LABEL = "Organization Attribute";
    public static final String ORGANIZATION_MODEL_ATTRIBUTE_HELP_TEXT = "Name of stored organization attribute which is the name of an attribute within the OrganizationModel.attribute map.";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ORGANIZATION_ATTRIBUTE);
        property.setLabel(ORGANIZATION_MODEL_ATTRIBUTE_LABEL);
        property.setHelpText(ORGANIZATION_MODEL_ATTRIBUTE_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties);

    }

    public static final String PROVIDER_ID = "oidc-organization-attribute-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Organization Attribute";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a custom organization attribute to a token claim.";
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) return token;

        setClaim(token, mappingModel, userSession);
        return token;
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        UserModel user = userSession.getUser();
        String attributeName = mappingModel.getConfig().get(ORGANIZATION_ATTRIBUTE);

        for(OrganizationModel org : user.getOrganizations()) {
            String attributeValue = org.getAttribute(attributeName);

            if(attributeValue != null) {
                OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attributeValue);
            }
        }
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) return token;
        setClaim(token, mappingModel, userSession);
        return token;
    }
}
