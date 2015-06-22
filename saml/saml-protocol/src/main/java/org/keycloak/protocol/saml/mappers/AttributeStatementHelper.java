package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AttributeStatementHelper {
    public static final String SAML_ATTRIBUTE_NAME = "attribute.name";
    public static final String ATTRIBUTE_STATEMENT_CATEGORY = "AttributeStatement Mapper";
    public static final String FRIENDLY_NAME = "friendly.name";
    public static final String FRIENDLY_NAME_LABEL = "Friendly Name";
    public static final String FRIENDLY_NAME_HELP_TEXT = "Standard SAML attribute setting.  An optional, more human-readable form of the attribute's name that can be provided if the actual attribute name is cryptic.";
    public static final String SAML_ATTRIBUTE_NAMEFORMAT = "attribute.nameformat";
    public static final String BASIC = "Basic";
    public static final String URI_REFERENCE = "URI Reference";
    public static final String UNSPECIFIED = "Unspecified";

    public static void addAttribute(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel,
                                    String attributeValue) {
        AttributeType attribute = createAttributeType(mappingModel);
        attribute.addAttributeValue(attributeValue);
        attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attribute));
    }

    public static void addAttribute(AttributeStatementType attributeStatement, String attributeName, String friendlyName, String attributeType, String attributeValue) {
        AttributeType attribute = AttributeStatementHelper.createAttributeType(attributeName, friendlyName, attributeType);
        attribute.addAttributeValue(attributeValue);
        attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attribute));
    }

    public static AttributeType createAttributeType(ProtocolMapperModel mappingModel) {
        String attributeName = mappingModel.getConfig().get(SAML_ATTRIBUTE_NAME);
        String attributeType = mappingModel.getConfig().get(SAML_ATTRIBUTE_NAMEFORMAT);
        String friendlyName = mappingModel.getConfig().get(FRIENDLY_NAME);

        return createAttributeType(attributeName, friendlyName, attributeType);
    }

    public static AttributeType createAttributeType(String attributeName, String friendlyName, String attributeType) {
        AttributeType attribute = new AttributeType(attributeName);
        String attributeNameFormat = JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.get();
        if ("URI Reference".equals(attributeType)) attributeNameFormat = JBossSAMLURIConstants.ATTRIBUTE_FORMAT_URI.get();
        else if ("Unspecified".equals(attributeType)) attributeNameFormat = "urn:oasis:names:tc:SAML2.0:attrname-format:unspecified";
        attribute.setNameFormat(attributeNameFormat);
        if (friendlyName != null && !friendlyName.trim().equals("")) attribute.setFriendlyName(friendlyName);
        return attribute;
    }

    public static void setConfigProperties(List<ProviderConfigProperty> configProperties) {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(AttributeStatementHelper.FRIENDLY_NAME);
        property.setLabel(AttributeStatementHelper.FRIENDLY_NAME_LABEL);
        property.setHelpText(AttributeStatementHelper.FRIENDLY_NAME_HELP_TEXT);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(AttributeStatementHelper.SAML_ATTRIBUTE_NAME);
        property.setLabel("SAML Attribute Name");
        property.setHelpText("SAML Attribute Name");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT);
        property.setLabel("SAML Attribute NameFormat");
        property.setHelpText("SAML Attribute NameFormat.  Can be basic, URI reference, or unspecified.");
        List<String> types = new ArrayList(3);
        types.add(AttributeStatementHelper.BASIC);
        types.add(AttributeStatementHelper.URI_REFERENCE);
        types.add(AttributeStatementHelper.UNSPECIFIED);
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setDefaultValue(types);
        configProperties.add(property);

    }
    public static ProtocolMapperModel createAttributeMapper(String name, String userAttribute, String samlAttributeName, String nameFormat,  String friendlyName, boolean consentRequired, String consentText, String mapperId) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(mapperId);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        mapper.setConsentRequired(consentRequired);
        mapper.setConsentText(consentText);
        Map<String, String> config = new HashMap<String, String>();
        if (userAttribute != null) config.put(ProtocolMapperUtils.USER_ATTRIBUTE, userAttribute);
        config.put(SAML_ATTRIBUTE_NAME, samlAttributeName);
        if (friendlyName != null) {
            config.put(FRIENDLY_NAME, friendlyName);
        }
        if (nameFormat != null) {
            config.put(SAML_ATTRIBUTE_NAMEFORMAT, nameFormat);
        }
        mapper.setConfig(config);
        return mapper;
    }
}
