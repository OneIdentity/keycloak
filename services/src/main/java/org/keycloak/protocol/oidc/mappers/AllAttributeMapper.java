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

    static {
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties);
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

        for(Map.Entry<String, String> attribute : user.getAttributes().entrySet()) {
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attribute.getKey(), attribute.getValue());
        }

        String attributePrefix = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        if(attributePrefix == null || attributePrefix.isEmpty()) {
            attributePrefix = "organization";
        }

        for(OrganizationModel org : user.getOrganizations()) {
            for (Map.Entry<String, String> attribute : org.getAttributes().entrySet()) {
                String attributeName = String.format(ORGANIZATION_ATTRIBUTE_URI_FORMAT, attributePrefix, org.getName(), attribute.getKey());
                OIDCAttributeMapperHelper.mapClaim(token, mappingModel, attributeName, attribute.getValue());
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
                                                        boolean accessToken, boolean idToken) {
        return OIDCAttributeMapperHelper.createClaimMapper(name, null,
                tokenClaimName, claimType,
                consentRequired, consentText,
                accessToken, idToken,
                PROVIDER_ID);
    }
}
