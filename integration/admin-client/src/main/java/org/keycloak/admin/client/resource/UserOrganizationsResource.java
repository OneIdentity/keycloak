package org.keycloak.admin.client.resource;

import org.keycloak.representations.idm.OrganizationRepresentation;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface UserOrganizationsResource {
    @GET
    public List<OrganizationRepresentation> getAll();

    @Path("{organizationName}")
    @GET
    public OrganizationRepresentation get(@PathParam("organizationName") String organizationName);

    @Path("{organizationName}")
    @POST
    public void add(@PathParam("organizationName") String organizationName);

    @Path("{organizationName}")
    @DELETE
    public void remove(@PathParam("organizationName") String organizationName);
}
