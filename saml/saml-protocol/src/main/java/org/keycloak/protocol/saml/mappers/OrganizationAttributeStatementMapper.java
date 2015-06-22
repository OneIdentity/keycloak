package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Mappings UserModel property (the property name of a getter method) to an AttributeStatement.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OrganizationAttributeStatementMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {
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
        configProperties.add(property);
        AttributeStatementHelper.setConfigProperties(configProperties);

    }

    public static final String PROVIDER_ID = "saml-organization-attribute-mapper";


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
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map a custom organization attribute to a to a SAML attribute.";
    }

    @Override
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        UserModel user = userSession.getUser();
        String attributeName = mappingModel.getConfig().get(ORGANIZATION_ATTRIBUTE);

        for(OrganizationModel org : user.getOrganizations()) {
            String attributeValue = org.getAttribute(attributeName);

            if(attributeValue != null) {
                AttributeStatementHelper.addAttribute(attributeStatement, mappingModel, attributeValue);
            }
        }
    }
}
