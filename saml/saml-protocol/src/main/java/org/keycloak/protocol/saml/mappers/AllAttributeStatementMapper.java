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
    private static final String ORGANIZATION_ATTRIBUTE_URI_FORMAT = "https://schemas.org/keycloak/saml/attribute/organizations/%s/%s";
    private static final String ORGANIZATION_ATTRIBUTE_ID = "id";

    public static final String PROVIDER_ID = "saml-all-attribute-mapper";


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
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map all user attributes to a to a SAML attribute.";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        UserModel user = userSession.getUser();

        for(Map.Entry<String, String> attribute : user.getAttributes().entrySet()) {
            AttributeStatementHelper.addAttribute(attributeStatement, attribute.getKey(), null, AttributeStatementHelper.BASIC, attribute.getValue());
        }

        for(OrganizationModel org : user.getOrganizations()) {
            //Add organization ID that this user is associated with
            String attributeName = String.format(ORGANIZATION_ATTRIBUTE_URI_FORMAT, org.getName(), ORGANIZATION_ATTRIBUTE_ID);
            AttributeStatementHelper.addAttribute(attributeStatement, attributeName, null, AttributeStatementHelper.BASIC, org.getId());

            //Add any additional attributes from this organization
            for(Map.Entry<String, String> attribute : org.getAttributes().entrySet()) {
                attributeName = String.format(ORGANIZATION_ATTRIBUTE_URI_FORMAT, org.getName(), attribute.getKey());
                AttributeStatementHelper.addAttribute(attributeStatement, attributeName, null, AttributeStatementHelper.BASIC, attribute.getValue());
            }
        }
    }

    public static ProtocolMapperModel createAttributeMapper(String name) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        mapper.setConfig(new HashMap<String, String>());

        return mapper;
    }
}
