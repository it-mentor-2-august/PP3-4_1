package com.itm.space.backendresources;

import com.itm.space.backendresources.api.request.UserRequest;
import com.itm.space.backendresources.exception.BackendResourcesException;
import com.itm.space.backendresources.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest extends BaseIntegrationTest {

    @Value("${keycloak.realm}")
    private String realmItm;

    @MockBean
    private Keycloak keycloak;
    @MockBean
    List<RoleRepresentation> userRoles;
    @MockBean
    List<GroupRepresentation> userGroups;

    @SpyBean
    private UserService userService;

    @Nested
    class Create {

        private UserRequest userRequest;


        @BeforeEach
        void init() {
            when(keycloak.realm(realmItm)).thenReturn(mock(RealmResource.class));
            when(keycloak.realm(realmItm).users()).thenReturn(mock(UsersResource.class));
            userRequest = new UserRequest("username", "email@example.com", "password", "firstName", "lastName");
        }

        @Test
        @DisplayName("Test BackendResourcesException")
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnBackendResourcesExceptionMessageAndStatusWhenCreateUserFails() throws Exception {
            doThrow(new BackendResourcesException("User creation failed", HttpStatus.INTERNAL_SERVER_ERROR))
                    .when(userService).createUser(userRequest);
            when(keycloak.realm(realmItm).users().create(any())).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

            mvc.perform(requestWithContent(post("/api/users"), userRequest))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string("User creation failed"));
            verify(userService, times(1)).createUser(any(UserRequest.class));
        }


        @Test
        @DisplayName("Test create endpoint")
        @WithMockUser(username = "tmp", roles = "MODERATOR")
        void testCreateUser_ShouldReturnSuccessStatus() throws Exception {
            Response response = Response.status(Response.Status.CREATED).location(new URI("/550e8400-e29b-41d4-a716-446655440000")).build();
            when(keycloak.realm(realmItm).users().create(any())).thenReturn(response);
            mvc.perform(requestWithContent(post("/api/users"), userRequest))
                    .andExpect(status().isOk());
            verify(keycloak.realm(realmItm).users(), times(1)).create(any(UserRepresentation.class));

        }

        @Test
        @DisplayName("Test MethodArgumentNotValidException")
        @WithMockUser(roles = "MODERATOR")
        void testCreateUser_MethodArgumentNotValidException() throws Exception {
            userRequest = new UserRequest("a", "asdasd", "123", "", "");
            mvc.perform(requestWithContent(post("/api/users"), userRequest))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.username").value("Username should be between 2 and 30 characters long"))
                    .andExpect(jsonPath("$.email").value("Email should be valid"))
                    .andExpect(jsonPath("$.password").value("Password should be greater than 4 characters long"))
                    .andExpect(jsonPath("$.firstName").value("must not be blank"))
                    .andExpect(jsonPath("$.lastName").value("must not be blank"));
        }

    }

    @Nested
    class GetUserById {

        private UUID userId;

        @BeforeEach
        void init() {
            userId = UUID.randomUUID();
            when(keycloak.realm(anyString())).thenReturn(mock(RealmResource.class));
            when(keycloak.realm(anyString()).users()).thenReturn(mock(UsersResource.class));
        }

        @Test
        @DisplayName("Test getUserById endpoint")
        @WithMockUser(roles = "MODERATOR")
        void shouldReturnUserResponseWhenValidId() throws Exception {
            UserRepresentation user = new UserRepresentation();
            user.setId(String.valueOf(userId));
            user.setFirstName("tmp");
            user.setLastName("tmp_lastName");


            when(keycloak.realm(anyString()).users().get(any())).thenReturn(mock(UserResource.class));
            when(keycloak.realm(anyString()).users().get(any()).toRepresentation()).thenReturn(user);
            when(keycloak.realm(anyString()).users().get(any()).roles()).thenReturn(mock(RoleMappingResource.class));
            when(keycloak.realm(anyString()).users().get(any()).roles().getAll()).thenReturn(mock(MappingsRepresentation.class));
            when(keycloak.realm(anyString()).users().get(any()).roles().getAll().getRealmMappings()).thenReturn(userRoles);
            when(keycloak.realm(anyString()).users().get(any()).groups()).thenReturn(userGroups);
            mvc.perform(get("/api/users/{id}", userId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.firstName").value("tmp"))
                    .andExpect(jsonPath("$.lastName").value("tmp_lastName"));
            verify(keycloak.realm(anyString()).users(), times(8)).get(any());
        }

        @Test
        @DisplayName("Test getUserById is Forbidden ")
        @WithMockUser(roles = "USER")
        void shouldReturnForbiddenWhenNotModerator() throws Exception {
            mvc.perform(get("/api/users/{id}", userId))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class Hello {
        @Test
        @DisplayName("Test hello endpoint")
        @WithMockUser(username = "tmp", roles = "MODERATOR")
        void shouldReturnUsernameWhenModerator() throws Exception {
            mvc.perform(get("/api/users/hello"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("tmp"));
        }

        @Test
        @DisplayName("Test hello is Forbidden")
        @WithMockUser(username = "tmp", roles = "ADMIN")
        void shouldReturnForbiddenWhenNotModerator() throws Exception {
            mvc.perform(get("/api/users/hello"))
                    .andExpect(status().isForbidden());
        }

    }

}
