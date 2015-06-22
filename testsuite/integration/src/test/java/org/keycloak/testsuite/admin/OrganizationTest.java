package org.keycloak.testsuite.admin;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.rule.WebRule;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OrganizationTest extends AbstractClientTest {

    @Rule
    public WebRule webRule = new WebRule(this);

    @Before
    public void before() {
        super.before();
        realm.roles().create(new RoleRepresentation("admin", null));
    }

    @Test
    public void testFindAll() {
        OrganizationRepresentation org1 = create(KeycloakModelUtils.generateId(), "Org 1", "Desc 1", true);
        OrganizationRepresentation org2 = create(KeycloakModelUtils.generateId(), "Org 2", "Desc 2", false);

        realm.organizations().create(org1);
        realm.organizations().create(org2);

        List<OrganizationRepresentation> orgs = realm.organizations().findAll();

        for( OrganizationRepresentation org : orgs ) {
            if(org1.getName().contentEquals(org.getName())) {
                assertOrganization(org1, org, false);
            }
            else if(org2.getName().contentEquals(org.getName())) {
                assertOrganization(org2, org, false);
            }
            else {
                fail("Organization returned was not one that we created.");
            }
        }
    }

    @Test
    public void testCreate() {
        OrganizationRepresentation org1 = create(KeycloakModelUtils.generateId(), "Org 1", "Desc 1", true);

        org1.setRealmRoles(new ArrayList<String>());
        org1.getRealmRoles().add("admin");

        ClientRepresentation client = createClient();
        List<RoleRepresentation> clientRoles = realm.clients().get(client.getId()).roles().list();
        org1.setClientRoles(new HashMap<String, List<String>>());
        org1.getClientRoles().put(client.getClientId(), new ArrayList<String>());
        org1.getClientRoles().get(client.getClientId()).add(clientRoles.get(0).getName());

        realm.organizations().create(org1);
        OrganizationResource organizationResource = realm.organizations().get(org1.getName());

        assertNotNull(organizationResource);

        assertOrganization(org1, organizationResource.toRepresentation(), false);
    }

    @Test
    public void testCreateDuplicate() {
        OrganizationRepresentation org1 = create("Org 1", "Desc 1", true);

        realm.organizations().create(org1);

        try {
            realm.organizations().create(org1);
        }
        catch(ClientErrorException ex) {
            assertEquals(409, ex.getResponse().getStatus());
        }
    }

    @Test
    public void testUpdate() {
        OrganizationRepresentation org1 = create(KeycloakModelUtils.generateId(), "Org 1", "Desc 1", true);

        org1.setRealmRoles(new ArrayList<String>());
        org1.getRealmRoles().add("create-realm");
        org1.getRealmRoles().add("admin");

        realm.organizations().create(org1);
        OrganizationResource organizationResource = realm.organizations().get(org1.getName());

        assertNotNull(organizationResource);
        OrganizationRepresentation newOrg = organizationResource.toRepresentation();
        assertOrganization(org1, newOrg, false);

        newOrg.setName("Org 2");
        newOrg.setDescription("Desc 2");
        newOrg.getRealmRoles().remove("admin");

        organizationResource.update(newOrg);
        organizationResource = realm.organizations().get(newOrg.getName());
        assertNotNull(organizationResource);

        assertOrganization(newOrg, organizationResource.toRepresentation(), true);
    }

    @Test(expected = NotFoundException.class)
    public void testRemove() {
        OrganizationRepresentation org1 = create(KeycloakModelUtils.generateId(), "Org 1", "Desc 1", true);
        realm.organizations().create(org1);

        OrganizationResource organizationResource = realm.organizations().get(org1.getName());
        assertNotNull(organizationResource);
        assertOrganization(org1, organizationResource.toRepresentation(), false);

        organizationResource.remove();
        realm.organizations().get(org1.getName()).toRepresentation();
    }

    @Test
    public void testAddRealmRole() {
        OrganizationRepresentation org1 = create(KeycloakModelUtils.generateId(), "Org 1", "Desc 1", true);
        realm.organizations().create(org1);

        OrganizationResource organizationResource = realm.organizations().get(org1.getName());
        assertTrue(isNullOrEmpty(organizationResource.toRepresentation().getRealmRoles()));

        //Add realm role directly
        List<RoleRepresentation> roles = organizationResource.roles().realmLevel().listAll();
        assertTrue(isNullOrEmpty(roles));

        RoleRepresentation realmAdminRole = realm.roles().get("admin").toRepresentation();
        roles.add(realmAdminRole);
        organizationResource.roles().realmLevel().add(roles);

        roles = organizationResource.roles().realmLevel().listAll();
        assertEquals(1, roles.size());
        assertRole(realmAdminRole, roles.get(0), true);
    }

    @Test
    public void testDeleteRealmRole() {
        testAddRealmRole();

        OrganizationResource organizationResource = realm.organizations().get("Org 1");
        List<RoleRepresentation> roles = organizationResource.roles().realmLevel().listAll();

        //remove the roles
        organizationResource.roles().realmLevel().remove(roles);
        roles = organizationResource.roles().realmLevel().listAll();
        assertTrue(isNullOrEmpty(roles));
    }

    @Test
    public void testAddClientRole() {
        OrganizationRepresentation org1 = create(KeycloakModelUtils.generateId(), "Org 1", "Desc 1", true);
        realm.organizations().create(org1);

        OrganizationResource organizationResource = realm.organizations().get(org1.getName());
        assertTrue(isNullOrEmpty(organizationResource.toRepresentation().getClientRoles()));

        ClientRepresentation client = createClient();
        List<RoleRepresentation> clientRoles = realm.clients().get(client.getId()).roles().list();

        List<RoleRepresentation> roles = organizationResource.roles().clientLevel(client.getClientId()).listAll();
        assertTrue(isNullOrEmpty(roles));

        organizationResource.roles().clientLevel(client.getClientId()).add(clientRoles);

        roles = organizationResource.roles().clientLevel(client.getClientId()).listAll();
        assertEquals(clientRoles.size(), roles.size());
        assertRole(clientRoles.get(0), roles.get(0), true);
    }

    @Test
    public void testDeleteClientRole() {
        testAddClientRole();

        OrganizationResource organizationResource = realm.organizations().get("Org 1");
        List<RoleRepresentation> roles = organizationResource.roles().clientLevel("my-test-app").listAll();

        //remove the roles
        organizationResource.roles().clientLevel("my-test-app").remove(roles);
        roles = organizationResource.roles().clientLevel("my-test-app").listAll();
        assertTrue(isNullOrEmpty(roles));
    }

    @Test
    public void testGetAllRoles() {
        OrganizationRepresentation org1 = create(KeycloakModelUtils.generateId(), "Org 1", "Desc 1", true);
        realm.organizations().create(org1);

        OrganizationResource organizationResource = realm.organizations().get(org1.getName());
        assertTrue(isNullOrEmpty(organizationResource.toRepresentation().getRealmRoles()));
        assertTrue(isNullOrEmpty(organizationResource.toRepresentation().getClientRoles()));

        assertTrue(isNullOrEmpty(organizationResource.roles().getAll().getRealmMappings()));
        assertTrue(isNullOrEmpty(organizationResource.roles().getAll().getClientMappings()));

        //add roles
        List<RoleRepresentation> roles = new ArrayList<>();
        RoleRepresentation realmAdminRole = realm.roles().get("admin").toRepresentation();
        roles.add(realmAdminRole);
        organizationResource.roles().realmLevel().add(roles);

        ClientRepresentation client = createClient();
        List<RoleRepresentation> clientRoles = realm.clients().get(client.getId()).roles().list();
        organizationResource.roles().clientLevel(client.getClientId()).add(clientRoles);

        MappingsRepresentation mappings = organizationResource.roles().getAll();

        assertEquals(1, mappings.getRealmMappings().size());
        assertRole(realmAdminRole, mappings.getRealmMappings().get(0), true);

        assertEquals(1, mappings.getClientMappings().size());
        assertEquals(1, mappings.getClientMappings().get("my-test-app").getMappings().size());
        assertRole(clientRoles.get(0), mappings.getClientMappings().get("my-test-app").getMappings().get(0), true);
    }

    @Test
    public void testAttributes() {
        OrganizationRepresentation org1 = create(KeycloakModelUtils.generateId(), "Org 1", "Desc 1", true);
        org1.attribute("attr1", "value1org1");
        org1.attribute("attr2", "value2org1");

        realm.organizations().create(org1);

        OrganizationRepresentation createdOrg = realm.organizations().get(org1.getName()).toRepresentation();
        assertOrganization(org1, createdOrg, false);

        org1 = createdOrg;

        //Test adding and updating
        org1.attribute("attr1", "value3org1");
        org1.attribute("attr3", "value4org1");

        realm.organizations().get(org1.getName()).update(org1);
        createdOrg = realm.organizations().get(org1.getName()).toRepresentation();
        assertOrganization(org1, createdOrg, true);

        //Test removing
        org1.getAttributes().remove("attr1");

        realm.organizations().get(org1.getName()).update(org1);
        createdOrg = realm.organizations().get(org1.getName()).toRepresentation();
        assertOrganization(org1, createdOrg, true);

        //Test clearing
        org1.getAttributes().clear();

        realm.organizations().get(org1.getName()).update(org1);
        createdOrg = realm.organizations().get(org1.getName()).toRepresentation();
        assertOrganization(org1, createdOrg, true);
    }

    @Test
    public void testGetUsers() {
        int totalUsers = 20;
        String usernamePrefix = "tuser";
        OrganizationRepresentation org1 = create(KeycloakModelUtils.generateId(), "Org 1", "Desc 1", true);
        realm.organizations().create(org1);

        OrganizationResource organizationResource = realm.organizations().get(org1.getName());

        //Create users
        for(int a = 0; a < totalUsers; a++) {
            UserRepresentation rep = createExampleUser(getUserName(usernamePrefix, a), usernamePrefix, String.valueOf(a));
            assertNotNull(rep);
        }

        assertEquals(0, organizationResource.getUsers(0, totalUsers + 1).size());

        //Add users to organization
        for(int a = 0; a < totalUsers; a++) {
            organizationResource.addUser(getUserName(usernamePrefix, a));
            assertEquals(a + 1, organizationResource.getUsers(0, totalUsers + 1).size());

            //verify that we can actually get this specific user
            UserRepresentation rep = organizationResource.getUser(getUserName(usernamePrefix, a));
            assertNotNull(rep);
            assertEquals(rep.getUsername(), getUserName(usernamePrefix, a));
        }

        //test paging
        int pageSize = totalUsers/4;
        int currentIndex = 0;

        while(currentIndex < totalUsers) {
            List<UserRepresentation> users = organizationResource.getUsers(currentIndex, pageSize);
            assertPage(currentIndex, pageSize, usernamePrefix, users);
            currentIndex += pageSize;
        }
    }

    @Test
    public void testManageUser() {
        OrganizationRepresentation org1 = create(KeycloakModelUtils.generateId(), "Org 1", "Desc 1", true);
        realm.organizations().create(org1);

        OrganizationResource organizationResource = realm.organizations().get(org1.getName());
        UserRepresentation rep = createExampleUser("tmuser", "Test", "Manage");

        assertEquals(0, organizationResource.getUsers(0, 30).size());
        organizationResource.addUser(rep.getUsername());

        //Test get all users
        List<UserRepresentation> users = organizationResource.getUsers(0, 30);
        assertEquals(1, users.size());

        assertEquals(rep.getId(), users.get(0).getId());
        assertEquals(rep.getUsername(), users.get(0).getUsername());

        //check to see if this specific user is part of the organization
        UserRepresentation rep2 = organizationResource.getUser(rep.getUsername());
        assertNotNull(rep2);
        assertEquals(rep.getId(), rep2.getId());
        assertEquals(rep.getUsername(), rep2.getUsername());

        //remove user
        organizationResource.removeUser(rep.getUsername());
        assertEquals(0, organizationResource.getUsers(0, 30).size());

        try {
            rep2 = organizationResource.getUser(rep.getUsername());
            fail("getUser should throw exception");
        }
        catch(NotFoundException ex) {
            ;
        }
    }

    protected void assertPage(int currentIndex, int pageSize, String usernamePrefix, List<UserRepresentation> users) {
        assertTrue(users.size() <= pageSize);

        for(int a = 0; a < users.size(); a++) {
            assertEquals(getUserName(usernamePrefix, a + currentIndex), users.get(a).getUsername());
        }
    }

    protected String getUserName(String usernamePrefix, int currentIndex) {
        return String.format("%s%05d", usernamePrefix, currentIndex);
    }

    protected UserRepresentation createExampleUser(String username, String firstName, String lastName) {
        UserRepresentation rep = new UserRepresentation();
        rep.setUsername(username);
        rep.setFirstName(firstName);
        rep.setLastName(lastName);

        rep.setEnabled(true);

        Response response = realm.users().create(rep);
        String userId = ApiUtil.getCreatedId(response);
        response.close();

        return realm.users().get(userId).toRepresentation();
    }

    public ClientRepresentation createClient() {
        ClientRepresentation rep = new ClientRepresentation();
        rep.setClientId("my-test-app");
        rep.setEnabled(true);

        Response response = realm.clients().create(rep);
        String clientId = ApiUtil.getCreatedId(response);
        response.close();

        //realm.clients().create(rep);

        realm.clients().get(clientId).roles().create(new RoleRepresentation("my-test-app-role", null));

        return realm.clients().get(clientId).toRepresentation();
    }

    private OrganizationRepresentation create(String name, String desc, Boolean enabled) {
        return create(null, name, desc, enabled);
    }

    private OrganizationRepresentation create(String id, String name, String desc, Boolean enabled) {
        OrganizationRepresentation organizationRepresentation = new OrganizationRepresentation();
        organizationRepresentation.setId(id);
        organizationRepresentation.setName(name);
        organizationRepresentation.setDescription(desc);
        organizationRepresentation.setEnabled(enabled);

        return organizationRepresentation;
    }

    private void assertOrganization(OrganizationRepresentation expected, OrganizationRepresentation actual, Boolean validateId) {
        if(validateId) {
            assertEquals(expected.getId(), actual.getId());
        }

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertRoles(expected.getRealmRoles(), actual.getRealmRoles());
        assertAttributes(expected.getAttributes(), actual.getAttributes());
        assertClientRoles(expected.getClientRoles(), actual.getClientRoles());
    }

    private void assertAttributes(Map<String, String> expected, Map<String, String> actual) {
        if(isNullOrEmpty(expected)) {
            assertEquals(isNullOrEmpty(expected), isNullOrEmpty(actual));
            return;
        }

        assertEquals(expected.size(), actual.size());
        for(Map.Entry<String, String> entry : expected.entrySet()) {
            assertTrue(actual.containsKey(entry.getKey()));
            assertEquals(entry.getValue(), actual.get(entry.getKey()));
        }
    }

    private void assertClientRoles(Map<String, List<String>> expected, Map<String, List<String>> actual) {
        if(isNullOrEmpty(expected)) {
            assertEquals(isNullOrEmpty(expected), isNullOrEmpty(actual));
            return;
        }

        assertEquals(expected.size(), actual.size());

        for(Map.Entry<String, List<String>> entry : expected.entrySet()) {
            assertTrue(actual.containsKey(entry.getKey()));
            assertRoles(entry.getValue(), actual.get(entry.getKey()));
        }
    }

    private void assertRoles(List<String> expected, List<String> actual) {
        if(isNullOrEmpty(expected)) {
            assertEquals(isNullOrEmpty(expected), isNullOrEmpty(actual));
            return;
        }

        assertEquals(expected.size(), actual.size());

        Collections.sort(expected);
        Collections.sort(actual);

        for(int a = 0; a < expected.size(); a++) {
            assertEquals(expected.get(a), actual.get(a));
        }
    }

    private void assertRole(RoleRepresentation expected, RoleRepresentation actual, boolean validateId) {
        if(validateId) {
            assertEquals(expected.getId(), actual.getId());
        }

        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.isComposite(), actual.isComposite());
    }

    private <T> Boolean isNullOrEmpty(List<T> obj) {
        return (obj == null || obj.size() == 0);
    }

    private <K, V> Boolean isNullOrEmpty(Map<K, V> obj) {
        return (obj == null || obj.size() == 0);
    }
}

