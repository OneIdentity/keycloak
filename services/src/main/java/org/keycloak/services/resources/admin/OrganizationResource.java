package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientMappingsRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakApplication;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;

public class OrganizationResource {
    private static Logger logger = Logger.getLogger(OrganizationResource.class);

    protected RealmModel realm;
    private RealmAuth auth;
    protected OrganizationModel organizationModel;
    protected KeycloakSession session;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakApplication keycloak;

    protected KeycloakApplication getKeycloakApplication() {
        return keycloak;
    }

    public OrganizationResource(RealmAuth auth, RealmModel realm, KeycloakSession session, OrganizationModel organizationModel) {
        this.realm = realm;
        this.session = session;
        this.organizationModel = organizationModel;
        this.auth = auth;
    }

    @GET
    @NoCache
    @Produces("application/json")
    public OrganizationRepresentation getOrganization() {
        OrganizationRepresentation rep = ModelToRepresentation.toRepresentation(this.organizationModel);
        return rep;
    }

    @DELETE
    @NoCache
    public Response delete() {
        this.auth.requireManage();

        realm.removeOrganizationById(this.organizationModel.getId());

        return Response.noContent().build();
    }

    @PUT
    @Consumes("application/json")
    public Response update(OrganizationRepresentation rep) {
        this.auth.requireManage();

        try {
            this.realm.updateOrganization(RepresentationToModel.toModel(realm, rep));

            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists(String.format("Organization %s already exists", rep.getName()));
        }
    }

    @Path("role-mappings")
    @GET
    @Produces("application/json")
    @NoCache
    public MappingsRepresentation getRoleMappings() {
        auth.requireView();

        MappingsRepresentation all = new MappingsRepresentation();

        //get Realm Mappings
        Set<RoleModel> realmMappings = organizationModel.getRealmRoleMappings();
        if (realmMappings.size() > 0) {
            List<RoleRepresentation> realmRep = new ArrayList<>();
            for (RoleModel roleModel : realmMappings) {
                realmRep.add(ModelToRepresentation.toRepresentation(roleModel));
            }

            all.setRealmMappings(realmRep);
        }

        //Get app mappings
        Set<RoleModel> clientRoleMappings = organizationModel.getClientRoleMappings();
        if (clientRoleMappings.size() > 0) {
            Map<String, ClientMappingsRepresentation> clientMappings = new HashMap<>();

            for (RoleModel roleModel : clientRoleMappings) {
                ClientModel client = (ClientModel) roleModel.getContainer();

                if(!clientMappings.containsKey(client.getClientId())) {
                    ClientMappingsRepresentation mappings = new ClientMappingsRepresentation();
                    mappings.setId(client.getId());
                    mappings.setClient(client.getClientId());
                    mappings.setMappings(new ArrayList<RoleRepresentation>());

                    clientMappings.put(client.getClientId(), mappings);
                }

                clientMappings.get(client.getClientId()).getMappings().add(ModelToRepresentation.toRepresentation(roleModel));
            }

            all.setClientMappings(clientMappings);
        }

        return all;
    }

    @Path("role-mappings/realm")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getRealmRoleMappings() {
        auth.requireView();

        Set<RoleModel> realmMappings = organizationModel.getRealmRoleMappings();
        List<RoleRepresentation> realmMappingsRep = new ArrayList<>();

        for (RoleModel roleModel : realmMappings) {
            realmMappingsRep.add(ModelToRepresentation.toRepresentation(roleModel));
        }

        return realmMappingsRep;
    }

    /**
     * Effective realm-level role mappings for this organization.  Will recurse all composite roles to get this list.
     */
    @Path("role-mappings/realm/composite")
    @GET
    @Produces("application/json")
    @NoCache
    public List<RoleRepresentation> getEffectiveRealmRoleMappings() {
        auth.requireView();

        Set<RoleModel> realmMappings = organizationModel.getRealmRoleMappings();
        List<RoleRepresentation> realmMappingsRep = new ArrayList<>();

        for (RoleModel roleModel : realmMappings) {
            addRole(realmMappingsRep, roleModel);
        }

        return realmMappingsRep;
    }

    private void addRole(List<RoleRepresentation> mappings, RoleModel roleModel) {
        mappings.add(ModelToRepresentation.toRepresentation(roleModel));

        if(roleModel.isComposite()) {
            for(RoleModel compRole : roleModel.getComposites()) {
                addRole(mappings, compRole);
            }
        }
    }

    @Path("role-mappings/realm")
    @POST
    @Consumes("application/json")
    public Response addRealmRoleMappings(List<RoleRepresentation> roles) {
        auth.requireManage();

        logger.debugv("** addRealmRoleMappings: {0}", roles);

        for (RoleRepresentation role : roles) {
            RoleModel roleModel = realm.getRole(role.getName());
            if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                throw new NotFoundException("Role not found");
            }

            organizationModel.grantRole(roleModel);
        }

        this.realm.updateOrganization(organizationModel);

        return Response.noContent().build();
    }

    @Path("role-mappings/realm")
    @DELETE
    @Consumes("application/json")
    public Response deleteRealmRoleMappings(List<RoleRepresentation> roles) {
        auth.requireManage();

        logger.debugv("** deleteRealmRoleMappings: {0}", roles);

        if (roles == null) {
            //Remove all realm role mappings
            Set<RoleModel> roleModels = organizationModel.getRealmRoleMappings();
            for (RoleModel roleModel : roleModels) {
                organizationModel.deleteRoleMapping(roleModel);
            }
        } else {
            //remove specific role mappings
            for (RoleRepresentation role : roles) {
                RoleModel roleModel = realm.getRole(role.getName());
                if (roleModel == null || !roleModel.getId().equals(role.getId())) {
                    throw new NotFoundException("Role not found");
                }
                organizationModel.deleteRoleMapping(roleModel);
            }
        }

        this.realm.updateOrganization(organizationModel);
        return Response.noContent().build();
    }

    @Path("role-mappings/clients/{clientId}")
    public OrganizationClientRoleMappingsResource getOrganizationClientRoleMappingsResource(@PathParam("clientId") String clientId) {
        ClientModel client = realm.getClientByClientId(clientId);

        if (client == null) {
            throw new NotFoundException("Client not found");
        }

        return new OrganizationClientRoleMappingsResource(realm, auth, organizationModel, client);
    }

    @Path("users")
    @GET
    @NoCache
    @Produces("application/json")
    public List<UserRepresentation> getUsers(@QueryParam("first") Integer firstResult,
                                             @QueryParam("max") Integer maxResults) {
        auth.requireView();

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : -1;

        List<UserRepresentation> results = new ArrayList<>();
        List<UserModel> userModels = session.users().getUsersByOrganization(realm, organizationModel, firstResult, maxResults);

        for (UserModel user : userModels) {
            results.add(ModelToRepresentation.toRepresentation(user));
        }
        return results;
    }

    @Path("users/{username}")
    @GET
    @NoCache
    @Produces("application/json")
    public UserRepresentation getUser(@PathParam("username") String username) {
        auth.requireView();

        UserModel user = session.users().getUserByUsername(username, realm);

        if (user == null || !user.hasOrganization(organizationModel)) {
            throw new NotFoundException("User not found");
        }

        return ModelToRepresentation.toRepresentation(user);
    }

    @Path("users/{username}")
    @POST
    @NoCache
    @Produces("application/json")
    public Response addUser(@PathParam("username") String username) {
        auth.requireManage();

        UserModel user = session.users().getUserByUsername(username, realm);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        user.addOrganization(organizationModel);

        return Response.noContent().build();
    }

    @Path("users/{username}")
    @DELETE
    @NoCache
    @Produces("application/json")
    public Response removeUser(@PathParam("username") String username) {
        auth.requireManage();

        UserModel user = session.users().getUserByUsername(username, realm);

        if (user == null) {
            throw new NotFoundException("User not found");
        }

        user.removeOrganization(organizationModel);

        return Response.noContent().build();
    }
}

