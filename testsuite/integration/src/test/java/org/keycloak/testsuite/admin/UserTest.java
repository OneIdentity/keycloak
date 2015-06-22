package org.keycloak.testsuite.admin;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserTest extends AbstractClientTest {

    public String createUser() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        user.setEmail("user1@localhost");

        Response response = realm.users().create(user);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();
        return createdId;
    }

    @Test
    public void verifyCreateUser() {
        createUser();
    }

    @Test
    public void createDuplicatedUser1() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user1");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());

        // Just to show how to retrieve underlying error message
        ErrorRepresentation error = response.readEntity(ErrorRepresentation.class);
        Assert.assertEquals("User exists with same username", error.getErrorMessage());

        response.close();
    }

    @Test
    public void createDuplicatedUser2() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("user1@localhost");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());
        response.close();
    }
    
    @Test
    public void createDuplicatedUser3() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("User1");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());
        response.close();
    }
    
    @Test
    public void createDuplicatedUser4() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("USER1");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());
        response.close();
    }

    @Test
    public void createDuplicatedUser5() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("User1@localhost");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());
        response.close();
    }
    
    @Test
    public void createDuplicatedUser6() {
        createUser();

        UserRepresentation user = new UserRepresentation();
        user.setUsername("user2");
        user.setEmail("user1@LOCALHOST");
        Response response = realm.users().create(user);
        assertEquals(409, response.getStatus());
        response.close();
    }
    
    private void createUsers() {
        for (int i = 1; i < 10; i++) {
            UserRepresentation user = new UserRepresentation();
            user.setUsername("username" + i);
            user.setEmail("user" + i + "@localhost");
            user.setFirstName("First" + i);
            user.setLastName("Last" + i);

            realm.users().create(user).close();
        }
    }

    @Test
    public void searchByEmail() {
        createUsers();

        List<UserRepresentation> users = realm.users().search(null, null, null, "user1@localhost", null, null);
        assertEquals(1, users.size());

        users = realm.users().search(null, null, null, "@localhost", null, null);
        assertEquals(9, users.size());
    }

    @Test
    public void searchByUsername() {
        createUsers();

        List<UserRepresentation> users = realm.users().search("username1", null, null, null, null, null);
        assertEquals(1, users.size());

        users = realm.users().search("user", null, null, null, null, null);
        assertEquals(9, users.size());
    }

    @Test
    public void search() {
        createUsers();

        List<UserRepresentation> users = realm.users().search("username1", null, null);
        assertEquals(1, users.size());

        users = realm.users().search("first1", null, null);
        assertEquals(1, users.size());

        users = realm.users().search("last", null, null);
        assertEquals(9, users.size());
    }

    @Test
    public void searchPaginated() {
        createUsers();

        List<UserRepresentation> users = realm.users().search("username", 0, 1);
        assertEquals(1, users.size());
        assertEquals("username1", users.get(0).getUsername());

        users = realm.users().search("username", 5, 2);
        assertEquals(2, users.size());
        assertEquals("username6", users.get(0).getUsername());
        assertEquals("username7", users.get(1).getUsername());

        users = realm.users().search("username", 7, 20);
        assertEquals(2, users.size());
        assertEquals("username8", users.get(0).getUsername());
        assertEquals("username9", users.get(1).getUsername());

        users = realm.users().search("username", 0, 20);
        assertEquals(9, users.size());
    }

    @Test
    public void getFederatedIdentities() {
        // Add sample identity provider
        addSampleIdentityProvider();

        // Add sample user
        String id = createUser();
        UserResource user = realm.users().get(id);
        assertEquals(0, user.getFederatedIdentity().size());

        // Add social link to the user
        FederatedIdentityRepresentation link = new FederatedIdentityRepresentation();
        link.setUserId("social-user-id");
        link.setUserName("social-username");
        Response response = user.addFederatedIdentity("social-provider-id", link);
        assertEquals(204, response.getStatus());

        // Verify social link is here
        user = realm.users().get(id);
        List<FederatedIdentityRepresentation> federatedIdentities = user.getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        link = federatedIdentities.get(0);
        assertEquals("social-provider-id", link.getIdentityProvider());
        assertEquals("social-user-id", link.getUserId());
        assertEquals("social-username", link.getUserName());

        // Remove social link now
        user.removeFederatedIdentity("social-provider-id");
        assertEquals(0, user.getFederatedIdentity().size());

        removeSampleIdentityProvider();
    }

    private void addSampleIdentityProvider() {
        List<IdentityProviderRepresentation> providers = realm.identityProviders().findAll();
        Assert.assertEquals(0, providers.size());

        IdentityProviderRepresentation rep = new IdentityProviderRepresentation();
        rep.setAlias("social-provider-id");
        rep.setProviderId("social-provider-type");
        realm.identityProviders().create(rep);
    }

    private void removeSampleIdentityProvider() {
        IdentityProviderResource resource = realm.identityProviders().get("social-provider-id");
        assertNotNull(resource);
        resource.remove();
    }

    @Test
    public void addRequiredAction() {
        String id = createUser();

        UserResource user = realm.users().get(id);
        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());

        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add("UPDATE_PASSWORD");
        user.update(userRep);

        assertEquals(1, user.toRepresentation().getRequiredActions().size());
        assertEquals("UPDATE_PASSWORD", user.toRepresentation().getRequiredActions().get(0));
    }

    @Test
    public void removeRequiredAction() {
        String id = createUser();

        UserResource user = realm.users().get(id);
        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());

        UserRepresentation userRep = user.toRepresentation();
        userRep.getRequiredActions().add("UPDATE_PASSWORD");
        user.update(userRep);

        user = realm.users().get(id);
        userRep = user.toRepresentation();
        userRep.getRequiredActions().clear();
        user.update(userRep);

        assertTrue(user.toRepresentation().getRequiredActions().isEmpty());
    }

    @Test
    public void attributes() {
        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("user1");
        user1.attribute("attr1", "value1user1");
        user1.attribute("attr2", "value2user1");

        Response response = realm.users().create(user1);
        String user1Id = ApiUtil.getCreatedId(response);
        response.close();

        UserRepresentation user2 = new UserRepresentation();
        user2.setUsername("user2");
        user2.attribute("attr1", "value1user2");
        user2.attribute("attr2", "value2user2");

        response = realm.users().create(user2);
        String user2Id = ApiUtil.getCreatedId(response);
        response.close();
        user1 = realm.users().get(user1Id).toRepresentation();
        assertEquals(2, user1.getAttributes().size());
        assertEquals("value1user1", user1.getAttributes().get("attr1"));
        assertEquals("value2user1", user1.getAttributes().get("attr2"));

        user2 = realm.users().get(user2Id).toRepresentation();
        assertEquals(2, user2.getAttributes().size());
        assertEquals("value1user2", user2.getAttributes().get("attr1"));
        assertEquals("value2user2", user2.getAttributes().get("attr2"));

        user1.attribute("attr1", "value3user1");
        user1.attribute("attr3", "value4user1");

        realm.users().get(user1Id).update(user1);

        user1 = realm.users().get(user1Id).toRepresentation();
        assertEquals(3, user1.getAttributes().size());
        assertEquals("value3user1", user1.getAttributes().get("attr1"));
        assertEquals("value2user1", user1.getAttributes().get("attr2"));
        assertEquals("value4user1", user1.getAttributes().get("attr3"));

        user1.getAttributes().remove("attr1");
        realm.users().get(user1Id).update(user1);

        user1 = realm.users().get(user1Id).toRepresentation();
        assertEquals(2, user1.getAttributes().size());
        assertEquals("value2user1", user1.getAttributes().get("attr2"));
        assertEquals("value4user1", user1.getAttributes().get("attr3"));

        user1.getAttributes().clear();
        realm.users().get(user1Id).update(user1);

        user1 = realm.users().get(user1Id).toRepresentation();
        assertNull(user1.getAttributes());
    }

    @Test
    public void sendResetPasswordEmail() {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("user1");
        Response response = realm.users().create(userRep);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        UserResource user = realm.users().get(id);

        try {
            user.resetPasswordEmail();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User email missing", error.getErrorMessage());
        }
        try {
            userRep = user.toRepresentation();
            userRep.setEmail("user1@localhost");
            userRep.setEnabled(false);
            user.update(userRep);
            user.resetPasswordEmail();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User is disabled", error.getErrorMessage());
        }
        try {
            userRep.setEnabled(true);
            user.update(userRep);
            user.resetPasswordEmail("invalidClientId");
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("invalidClientId not enabled", error.getErrorMessage());
        }
    }

    @Test
    public void sendVerifyEmail() {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("user1");
        Response response = realm.users().create(userRep);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        UserResource user = realm.users().get(id);

        try {
            user.sendVerifyEmail();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User email missing", error.getErrorMessage());
        }
        try {
            userRep = user.toRepresentation();
            userRep.setEmail("user1@localhost");
            userRep.setEnabled(false);
            user.update(userRep);
            user.sendVerifyEmail();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User is disabled", error.getErrorMessage());
        }
        try {
            userRep.setEnabled(true);
            user.update(userRep);
            user.sendVerifyEmail("invalidClientId");
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("invalidClientId not enabled", error.getErrorMessage());
        }
    }

    @Test
    public void updateUserWithNewUsername() {
        switchEditUsernameAllowedOn();
        String id = createUser();

        UserResource user = realm.users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setUsername("user11");
        user.update(userRep);

        userRep = realm.users().get(id).toRepresentation();
        assertEquals("user11", userRep.getUsername());
    }

    @Test
    public void updateUserWithNewUsernameNotPossible() {
        String id = createUser();

        UserResource user = realm.users().get(id);
        UserRepresentation userRep = user.toRepresentation();
        userRep.setUsername("user11");
        user.update(userRep);

        userRep = realm.users().get(id).toRepresentation();
        assertEquals("user1", userRep.getUsername());
    }

    @Test
    public void updateUserWithNewUsernameAccessingViaOldUsername() {
        switchEditUsernameAllowedOn();
        createUser();

        try {
            UserResource user = realm.users().get("user1");
            UserRepresentation userRep = user.toRepresentation();
            userRep.setUsername("user1");
            user.update(userRep);

            realm.users().get("user11").toRepresentation();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(404, e.getResponse().getStatus());
        }
    }

    @Test
    public void updateUserWithExistingUsername() {
        switchEditUsernameAllowedOn();
        createUser();

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("user2");
        Response response = realm.users().create(userRep);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();

        try {
            UserResource user = realm.users().get(createdId);
            userRep = user.toRepresentation();
            userRep.setUsername("user1");
            user.update(userRep);
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(409, e.getResponse().getStatus());
        }
    }

    private void switchEditUsernameAllowedOn() {
        RealmRepresentation rep = realm.toRepresentation();
        rep.setEditUsernameAllowed(true);
        realm.update(rep);
    }

    @Test
    public void organizations() {
        OrganizationRepresentation org1 = createExampleOrganization("Organization 1");
        OrganizationRepresentation org2 = createExampleOrganization("Organization 2");

        UserRepresentation rep = new UserRepresentation();
        rep.setUsername("tuser");
        rep.setFirstName("Test");
        rep.setLastName("User");
        rep.attribute("badge_number", "12345");

        rep.setOrganizations(new ArrayList<String>());
        rep.getOrganizations().add(org1.getName());

        //Create and User with single org
        Response response = realm.users().create(rep);
        String userId = ApiUtil.getCreatedId(response);
        response.close();

        UserResource userResource = realm.users().get(userId);
        rep = userResource.toRepresentation();
        assertEquals(1, rep.getOrganizations().size());
        assertEquals(org1.getName(), rep.getOrganizations().get(0));

        //Add second org
        rep.getOrganizations().add(org2.getName());
        userResource.update(rep);
        rep = userResource.toRepresentation();
        assertEquals(2, rep.getOrganizations().size());
        assertNames(rep.getOrganizations(), org1.getName(), org2.getName());

        //remove org1
        rep.getOrganizations().remove(org1.getName());
        userResource.update(rep);
        rep = userResource.toRepresentation();
        assertEquals(1, rep.getOrganizations().size());
        assertEquals(org2.getName(), rep.getOrganizations().get(0));

        //Delete organization which should also delete org from user
        realm.organizations().get(org2.getName()).remove();
        rep = userResource.toRepresentation();
        assertEquals(0, rep.getOrganizations().size());
    }

    @Test
    public void organizationsResource() {
        OrganizationRepresentation org1 = createExampleOrganization("Organization 1");
        OrganizationRepresentation org2 = createExampleOrganization("Organization 2");

        UserRepresentation rep = new UserRepresentation();
        rep.setUsername("tuser");
        rep.setFirstName("Test");
        rep.setLastName("User");
        rep.attribute("badge_number", "12345");

        Response response = realm.users().create(rep);
        String userId = ApiUtil.getCreatedId(response);
        response.close();

        UserResource userResource = realm.users().get(userId);

        assertEquals(0, userResource.organizations().getAll().size());

        //Test add
        userResource.organizations().add(org1.getName());
        assertEquals(1, userResource.organizations().getAll().size());
        OrganizationRepresentation testRep = userResource.organizations().get(org1.getName());
        assertNotNull(testRep);
        assertEquals(org1.getId(), testRep.getId());

        //test second add
        userResource.organizations().add(org2.getName());
        assertEquals(2, userResource.organizations().getAll().size());
        testRep = userResource.organizations().get(org2.getName());
        assertNotNull(testRep);
        assertEquals(org2.getId(), testRep.getId());

        //test remove
        userResource.organizations().remove(org1.getName());
        assertEquals(1, userResource.organizations().getAll().size());

        //second one should still exist
        testRep = userResource.organizations().get(org2.getName());
        assertNotNull(testRep);
        assertEquals(org2.getId(), testRep.getId());

        try {
            testRep = userResource.organizations().get(org1.getName());
            fail(org1.getName() + " should no longer exist in the collection");
        }
        catch(NotFoundException ex) {
            ;
        }

        userResource.organizations().remove(org2.getName());
        assertEquals(0, userResource.organizations().getAll().size());
    }

    protected OrganizationRepresentation createExampleOrganization(String name) {
        OrganizationRepresentation rep = new OrganizationRepresentation();
        rep.setName(name);
        rep.setDescription("Organization Description");

        realm.organizations().create(rep);

        return realm.organizations().get(rep.getName()).toRepresentation();
    }
}
