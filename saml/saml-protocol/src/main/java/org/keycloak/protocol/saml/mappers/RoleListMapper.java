package org.keycloak.protocol.saml.mappers;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.services.managers.ClientSessionCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RoleListMapper extends AbstractSAMLProtocolMapper implements SAMLRoleListMapper {
    protected static final Logger logger = Logger.getLogger(RoleListMapper.class);
    public static final String PROVIDER_ID = "saml-role-list-mapper";
    public static final String SINGLE_ROLE_ATTRIBUTE = "single";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
    private static final String RESOURCE_ROLE_URI_FORMAT = "https://schemas.org/keycloak/saml/role/%s/%s";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(AttributeStatementHelper.SAML_ATTRIBUTE_NAME);
        property.setLabel("Role attribute name");
        property.setDefaultValue("Role");
        property.setHelpText("Name of the SAML attribute you want to put your roles into.  i.e. 'Role', 'memberOf'.");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(AttributeStatementHelper.FRIENDLY_NAME);
        property.setLabel(AttributeStatementHelper.FRIENDLY_NAME_LABEL);
        property.setHelpText(AttributeStatementHelper.FRIENDLY_NAME_HELP_TEXT);
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
        property = new ProviderConfigProperty();
        property.setName(SINGLE_ROLE_ATTRIBUTE);
        property.setLabel("Single Role Attribute");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText("If true, all roles will be stored under one attribute with multiple attribute values.");
        configProperties.add(property);

    }


    @Override
    public String getDisplayCategory() {
        return "Role Mapper";
    }

    @Override
    public String getDisplayType() {
        return "Role list";
    }

    @Override
    public String getHelpText() {
        return "Role names are stored in an attribute value.  There is either one attribute with multiple attribute values, or an attribute per role name depending on how you configure it.  You can also specify the attribute name i.e. 'Role' or 'memberOf' being examples.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void mapRoles(AttributeStatementType roleAttributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        String single = mappingModel.getConfig().get(SINGLE_ROLE_ATTRIBUTE);
        boolean singleAttribute = Boolean.parseBoolean(single);

        List<SamlProtocol.ProtocolMapperProcessor<SAMLRoleNameMapper>> roleNameMappers = new LinkedList<>();
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        AttributeType singleAttributeType = null;
        Set<ProtocolMapperModel> requestedProtocolMappers = new ClientSessionCode(clientSession.getRealm(), clientSession).getRequestedProtocolMappers();
        if(singleAttribute) {
            singleAttributeType = getNewAttributeType(roleAttributeStatement, mappingModel);
        }

        for (ProtocolMapperModel mapping : requestedProtocolMappers) {
            if (!mapping.getProtocol().equals(SamlProtocol.LOGIN_PROTOCOL)) continue;

            ProtocolMapper mapper = (ProtocolMapper)sessionFactory.getProviderFactory(ProtocolMapper.class, mapping.getProtocolMapper());
            if (mapper == null) continue;

            //Get role name mappers to use
            if (mapper instanceof SAMLRoleNameMapper) {
                roleNameMappers.add(new SamlProtocol.ProtocolMapperProcessor<>((SAMLRoleNameMapper) mapper,mapping));
            }

            //get hardcoded role mappers to add
            if (mapper instanceof HardcodedRole) {
                AttributeType attributeType = (singleAttribute ? singleAttributeType : getNewAttributeType(roleAttributeStatement, mappingModel));
                attributeType.addAttributeValue(mapping.getConfig().get("role"));
            }
        }

        Set<String> roles = new HashSet<>();

        //Add user roles
        for (String roleId : clientSession.getRoles()) {
            RoleModel roleModel = clientSession.getRealm().getRoleById(roleId);
            roles.addAll(getComposites(roleModel, roleNameMappers));
        }

        //Add organization roles
        for (OrganizationModel organizationModel : userSession.getUser().getOrganizations()) {
            if(organizationModel.isEnabled()) {
                for (RoleModel roleModel : organizationModel.getRoleMappings()) {
                    roles.addAll(getComposites(roleModel, roleNameMappers));
                }
            }
            else {
                logger.debugf("Skipping organization %s as it is currently disabled", organizationModel.getName());
            }
        }

        for(String roleName : roles) {
            AttributeType attributeType = (singleAttribute ? singleAttributeType : getNewAttributeType(roleAttributeStatement, mappingModel));
            attributeType.addAttributeValue(roleName);
        }
    }

    protected Set<String> getComposites(RoleModel role, List<SamlProtocol.ProtocolMapperProcessor<SAMLRoleNameMapper>> roleNameMappers) {
        Set<String> roles = new HashSet<>();

        String roleName = getRoleName(role, roleNameMappers);

        if (role.getContainer() instanceof ClientModel) {
            ClientModel app = (ClientModel) role.getContainer();
            roleName = String.format(RESOURCE_ROLE_URI_FORMAT, app.getClientId(), roleName);
        }

        roles.add(roleName);

        if (role.isComposite()) {
            for (RoleModel composite : role.getComposites()) {
                Set<String> compositeRoles = getComposites(composite, roleNameMappers);
                roles.addAll(compositeRoles);
            }
        }

        return roles;
    }

    protected String getRoleName(RoleModel role, List<SamlProtocol.ProtocolMapperProcessor<SAMLRoleNameMapper>> roleNameMappers) {
        String roleName = role.getName();
        for (SamlProtocol.ProtocolMapperProcessor<SAMLRoleNameMapper> entry : roleNameMappers) {
            String newName = entry.mapper.mapName(entry.model, role);
            if (newName != null) {
                roleName = newName;
                break;
            }
        }

        return roleName;
    }

    protected AttributeType getNewAttributeType(AttributeStatementType roleAttributeStatement, ProtocolMapperModel mappingModel) {
        AttributeType attributeType = AttributeStatementHelper.createAttributeType(mappingModel);
        roleAttributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attributeType));

        return attributeType;
    }

    public static ProtocolMapperModel create(String name, String samlAttributeName, String nameFormat, String friendlyName, boolean singleAttribute) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        mapper.setConsentRequired(false);
        Map<String, String> config = new HashMap<String, String>();
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, samlAttributeName);
        if (friendlyName != null) {
            config.put(AttributeStatementHelper.FRIENDLY_NAME, friendlyName);
        }
        if (nameFormat != null) {
            config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, nameFormat);
        }
        config.put(SINGLE_ROLE_ATTRIBUTE, Boolean.toString(singleAttribute));
        mapper.setConfig(config);

        return mapper;
    }

}
