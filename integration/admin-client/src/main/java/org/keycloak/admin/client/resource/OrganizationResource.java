package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

public interface OrganizationResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    OrganizationRepresentation toRepresentation();

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    void update(OrganizationRepresentation organizationRepresentation);

    @DELETE
    void remove();

    @Path("role-mappings")
    RoleMappingResource roles();

    @GET
    @Path("users")
    List<UserRepresentation> getUsers(@QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults);

    @GET
    @Path("users/{username}")
    UserRepresentation getUser(@PathParam("username") String username);

    @POST
    @Path("users/{username}")
    void addUser(@PathParam("username") String username);

    @DELETE
    @Path("users/{username}")
    void removeUser(@PathParam("username") String username);
}
