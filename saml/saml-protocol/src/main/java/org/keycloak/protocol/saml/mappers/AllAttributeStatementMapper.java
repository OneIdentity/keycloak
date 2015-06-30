package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.*;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllAttributeStatementMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    private static final String ORGANIZATION_ATTRIBUTE_URI_FORMAT = "https://schemas.org/keycloak/saml/attribute/%s/%s/%s";
    private static final String ORGANIZATION_ATTRIBUTE_ID = "id";

    public static final String TOKEN_CLAIM_NAME = "claim.name";
    public static final String TOKEN_CLAIM_NAME_LABEL = "Token Claim Prefix";
    private static final String MAP_USER_ATTRIBUTES_NAME = "map.user";
    private static final String MAP_USER_ATTRIBUTES_LABEL = "Map User Attributes";
    private static final String MAP_ORG_ATTRIBUTES_NAME = "map.organization";
    private static final String MAP_ORG_ATTRIBUTES_LABEL = "Map Organization Attributes";

    public static final String PROVIDER_ID = "saml-all-attribute-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(TOKEN_CLAIM_NAME);
        property.setLabel(TOKEN_CLAIM_NAME_LABEL);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Name of the claim to insert into the token.  This will precede the claims for all organizations. i.e. organizations");
        property.setDefaultValue("organizations");
        configProperties.add(property);

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
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map all user attributes to a to a SAML attribute.";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        UserModel user = userSession.getUser();

        if(Boolean.parseBoolean(mappingModel.getConfig().get(MAP_USER_ATTRIBUTES_NAME))) {
            for (Map.Entry<String, String> attribute : user.getAttributes().entrySet()) {
                AttributeStatementHelper.addAttribute(attributeStatement, attribute.getKey(), null, AttributeStatementHelper.BASIC, attribute.getValue());
            }
        }

        if(Boolean.parseBoolean(mappingModel.getConfig().get(MAP_ORG_ATTRIBUTES_NAME))) {
            String attributePrefix = mappingModel.getConfig().get(TOKEN_CLAIM_NAME);
            if (attributePrefix == null || attributePrefix.isEmpty()) {
                attributePrefix = "organizations";
            }

            for (OrganizationModel org : user.getOrganizations()) {
                //Add organization ID that this user is associated with
                String attributeName = String.format(ORGANIZATION_ATTRIBUTE_URI_FORMAT, attributePrefix, org.getName(), ORGANIZATION_ATTRIBUTE_ID);
                AttributeStatementHelper.addAttribute(attributeStatement, attributeName, null, AttributeStatementHelper.BASIC, org.getId());

                //Add any additional attributes from this organization
                for (Map.Entry<String, String> attribute : org.getAttributes().entrySet()) {
                    attributeName = String.format(ORGANIZATION_ATTRIBUTE_URI_FORMAT, attributePrefix, org.getName(), attribute.getKey());
                    AttributeStatementHelper.addAttribute(attributeStatement, attributeName, null, AttributeStatementHelper.BASIC, attribute.getValue());
                }
            }
        }
    }

    public static ProtocolMapperModel createAttributeMapper(String name, boolean mapUserAttributes, boolean mapOrganizationAttributes, String tokenClaimName) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        mapper.setConfig(new HashMap<String, String>());
        if (mapUserAttributes) mapper.getConfig().put(MAP_USER_ATTRIBUTES_NAME, "true");
        if (mapOrganizationAttributes) mapper.getConfig().put(MAP_ORG_ATTRIBUTES_NAME, "true");
        mapper.getConfig().put(TOKEN_CLAIM_NAME, tokenClaimName);
        return mapper;
    }
}
