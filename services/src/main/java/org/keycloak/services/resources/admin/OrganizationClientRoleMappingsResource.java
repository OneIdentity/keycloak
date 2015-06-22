package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OrganizationClientRoleMappingsResource {
    protected static final Logger logger = Logger.getLogger(OrganizationClientRoleMappingsResource.class);

    protected RealmModel realm;
    protected RealmAuth auth;
    protected OrganizationModel organization;
    protected ClientModel client;

    public OrganizationClientRoleMappingsResource(RealmModel realm, RealmAuth auth, OrganizationModel organization, ClientModel client) {
        this.realm = realm;
        this.auth = auth;
        this.organization = organization;
        this.client = client;
    }

    /**
     * Get application-level role mappings for this user for a specific app
     *
     * @return
     */
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getClientRoleMappings() {
        auth.requireView();

        logger.debug("getClientRoleMappings");

        Set<RoleModel> mappings = organization.getClientRoleMappings(client);
        List<RoleRepresentation> mapRep = new ArrayList<RoleRepresentation>();
        for (RoleModel roleModel : mappings) {
            mapRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }
        logger.debugv("getClientRoleMappings.size() = {0}", mapRep.size());
        return mapRep;
    }

    /**
     * Get effective application-level role mappings.  This recurses any composite roles
     *
     * @return
     */
    @Path("composite")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getEffectiveClientRoleMappings() {
        auth.requireView();

        Set<RoleModel> clientMappings = organization.getClientRoleMappings(client);
        List<RoleRepresentation> clientMappingsRep = new ArrayList<>();

        for (RoleModel roleModel : clientMappings) {
            addRole(clientMappingsRep, roleModel);
        }

        return clientMappingsRep;
    }

    private void addRole(List<RoleRepresentation> mappings, RoleModel roleModel) {
        mappings.add(ModelToRepresentation.toRepresentation(roleModel));

        if(roleModel.isComposite()) {
            for(RoleModel compRole : roleModel.getComposites()) {
                addRole(mappings, compRole);
            }
        }
    }

    /**
     * Add application-level roles to the user role mapping.
     *
      * @param roles
     */
    @POST
    @Consumes("application/json")
    public Response addClientRoleMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        logger.debug("addClientRoleMapping");
        for (RoleRepresentation role : roles) {
            RoleModel roleModel = client.getRole(role.getName());
            if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                throw new NotFoundException("Role not found");
            }
            organization.grantRole(roleModel);
        }

        this.realm.updateOrganization(organization);
        return Response.noContent().build();
    }

    /**
     * Delete application-level roles from user role mapping.
     *
     * @param roles
     */
    @DELETE
    @Consumes("application/json")
    public Response deleteClientRoleMapping(List<RoleRepresentation> roles) {
        auth.requireManage();

        if (roles == null) {
            Set<RoleModel> roleModels = organization.getClientRoleMappings(client);
            for (RoleModel roleModel : roleModels) {
                if (!(roleModel.getContainer() instanceof ClientModel)) {
                    ClientModel clientModel = (ClientModel) roleModel.getContainer();
                    if (!clientModel.getId().equals(client.getId())) continue;
                }
                organization.deleteRoleMapping(roleModel);
            }

        } else {
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = client.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }
                organization.deleteRoleMapping(roleModel);
            }
        }

        this.realm.updateOrganization(organization);
        return Response.noContent().build();
    }
}
