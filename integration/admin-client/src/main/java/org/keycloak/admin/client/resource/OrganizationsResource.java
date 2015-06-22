package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.OrganizationRepresentation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

public interface OrganizationsResource {
    @Path("{name}")
    OrganizationResource get(@PathParam("name") String name);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<OrganizationRepresentation> findAll();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    void create(OrganizationRepresentation organizationRepresentation);
}
