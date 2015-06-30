package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllAttributeMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    private static final String ORGANIZATION_ATTRIBUTE_URI_FORMAT = "%s.%s.%s";
    private static final String ORGANIZATION_ATTRIBUTE_ID = "id";

    private static final String MAP_USER_ATTRIBUTES_NAME = "map.user";
    private static final String MAP_USER_ATTRIBUTES_LABEL = "Map User Attributes";
    private static final String MAP_ORG_ATTRIBUTES_NAME = "map.organization";
    private static final String MAP_ORG_ATTRIBUTES_LABEL = "Map Organization Attributes";

    static {
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties);
        for(ProviderConfigProperty prop : configProperties) {
            if(prop.getName().compareTo(OIDCAttributeMapperHelper.JSON_TYPE) == 0) {
                configProperties.remove(prop);
                break;
            }
        }

        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(MAP_USER_ATTRIBUTES_NAME);
        property.setLabel(MAP_USER_ATTRIBUTES_LABEL);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("Should the user attributes be added to the token?");
        property.setDefaultValue(true);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(MAP_ORG_ATTRIBUTES_NAME);
        property.setLabel(MAP_ORG_ATTRIBUTES_LABEL);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("Should the users organization attributes be added to the token?");
        property.setDefaultValue(true);
        configProperties.add(property);
    }

    public static final String PROVIDER_ID = "oidc-all-attribute-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "All Attributes";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map all user attribute to a token claim.";
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

        if(Boolean.parseBoolean(mappingModel.getConfig().get(MAP_USER_ATTRIBUTES_NAME))) {
            for (Map.Entry<String, String> attribute : user.getAttributes().entrySet()) {
                OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attribute.getKey(), attribute.getValue());
            }
        }

        if(Boolean.parseBoolean(mappingModel.getConfig().get(MAP_ORG_ATTRIBUTES_NAME))) {
            String attributePrefix = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
            if (attributePrefix == null || attributePrefix.isEmpty()) {
                attributePrefix = "organizations";
            }

            for (OrganizationModel org : user.getOrganizations()) {
                //Add organization ID that this user is associated with
                String attributeName = String.format(ORGANIZATION_ATTRIBUTE_URI_FORMAT, attributePrefix, org.getName(), ORGANIZATION_ATTRIBUTE_ID);
                OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attributeName, org.getId());

                //Add any additional attributes from this organization
                for (Map.Entry<String, String> attribute : org.getAttributes().entrySet()) {
                    attributeName = String.format(ORGANIZATION_ATTRIBUTE_URI_FORMAT, attributePrefix, org.getName(), attribute.getKey());
                    OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attributeName, attribute.getValue());
                }
            }
        }
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) return token;
        setClaim(token, mappingModel, userSession);
        return token;
    }

    public static ProtocolMapperModel createClaimMapper(String name,
                                                        String tokenClaimName, String claimType,
                                                        boolean consentRequired, String consentText,
                                                        boolean mapUserAttributes, boolean mapOrganizationAttributes,
                                                        boolean accessToken, boolean idToken) {
        ProtocolMapperModel mapper = OIDCAttributeMapperHelper.createClaimMapper(name, null,
                                        tokenClaimName, claimType,
                                        consentRequired, consentText,
                                        accessToken, idToken,
                                        PROVIDER_ID);

        if (mapUserAttributes) mapper.getConfig().put(MAP_USER_ATTRIBUTES_NAME, "true");
        if (mapOrganizationAttributes) mapper.getConfig().put(MAP_ORG_ATTRIBUTES_NAME, "true");

        return mapper;
    }
}
