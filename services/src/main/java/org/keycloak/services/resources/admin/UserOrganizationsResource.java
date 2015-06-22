package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.services.resources.KeycloakApplication;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import org.jboss.resteasy.spi.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

public class UserOrganizationsResource {
    private static Logger logger = Logger.getLogger(OrganizationResource.class);

    protected RealmModel realm;
    private RealmAuth auth;
    protected UserModel user;
    protected KeycloakSession session;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakApplication keycloak;

    protected KeycloakApplication getKeycloakApplication() {
        return keycloak;
    }

    public UserOrganizationsResource(RealmAuth auth, RealmModel realm, KeycloakSession session, UserModel user) {
        this.realm = realm;
        this.session = session;
        this.user = user;
        this.auth = auth;
    }

    @GET
    @NoCache
    @Produces("application/json")
    public List<OrganizationRepresentation> getUserOrganizations() {
        auth.requireView();

        List<OrganizationRepresentation> reps = new ArrayList<>();
        for(OrganizationModel org : user.getOrganizations()) {
            reps.add(ModelToRepresentation.toRepresentation(org));
        }

        return reps;
    }

    @Path("{organizationName}")
    @GET
    @NoCache
    @Produces("application/json")
    public OrganizationRepresentation getUserOrganization(@PathParam("organizationName") String organizationName) {
        auth.requireView();

        OrganizationModel org = realm.getOrganizationByName(organizationName);

        if(org == null || !user.hasOrganization(org)) {
            throw new NotFoundException("Could not find organization: " + organizationName);
        }

        return ModelToRepresentation.toRepresentation(org);
    }

    @Path("{organizationName}")
    @POST
    @NoCache
    @Produces("application/json")
    public Response addOrganization(@PathParam("organizationName") String organizationName) {
        auth.requireManage();

        OrganizationModel org = realm.getOrganizationByName(organizationName);

        if(org == null) {
            throw new NotFoundException("Could not find organization: " + organizationName);
        }

        if(!user.hasOrganization(org)) {
            user.addOrganization(org);
        }

        return Response.noContent().build();
    }

    @Path("{organizationName}")
    @DELETE
    @NoCache
    @Produces("application/json")
    public Response removeOrganization(@PathParam("organizationName") String organizationName) {
        auth.requireManage();

        OrganizationModel org = realm.getOrganizationByName(organizationName);

        if(org == null) {
            throw new NotFoundException("Could not find organization: " + organizationName);
        }

        if(user.hasOrganization(org)) {
            user.removeOrganization(org);
        }

        return Response.noContent().build();
    }
}
