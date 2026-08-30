package com.taskmanager.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.dto.AuthResponse;
import com.taskmanager.dto.LoginRequest;
import com.taskmanager.dto.RegisterRequest;
import com.taskmanager.dto.TaskRequest;
import com.taskmanager.entity.TaskPriority;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskManagerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Helper method to register and return JWT token
    private String registerAndLogin(String username, String email, String password) throws Exception {
        RegisterRequest register = RegisterRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class).token();
    }

    @Test
    @Order(1)
    @DisplayName("Should register a new user and return JWT token")
    void register() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("securePass1")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @Order(2)
    @DisplayName("Should reject registration with duplicate username")
    void register_DuplicateUsername() throws Exception {
        // First registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RegisterRequest.builder()
                                        .username("john_doe")
                                        .email("john@example.com")
                                        .password("securePass1")
                                        .build())))
                .andExpect(status().isCreated());

        // Second with same username
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                RegisterRequest.builder()
                                        .username("john_doe")
                                        .email("other@example.com")
                                        .password("securePass2")
                                        .build())))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    @DisplayName("Should login and receive JWT token")
    void login() throws Exception {
        String token = registerAndLogin("jane_doe", "jane@example.com", "securePass1");
        Assertions.assertNotNull(token);
        Assertions.assertFalse(token.isBlank());
    }

    @Test
    @Order(4)
    @DisplayName("Should reject login with wrong password")
    void login_WrongPassword() throws Exception {
        registerAndLogin("jane_doe", "jane@example.com", "securePass1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                LoginRequest.builder()
                                        .username("jane_doe")
                                        .password("wrongpassword")
                                        .build())))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    @DisplayName("Should create a task")
    void createTask() throws Exception {
        String token = registerAndLogin("alice", "alice@example.com", "securePass1");

        TaskRequest request = TaskRequest.builder()
                .title("Implement login page")
                .description("Create the user login form with validation")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .build();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Implement login page"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.author.username").value("alice"));
    }

    @Test
    @Order(6)
    @DisplayName("Should return paginated task list")
    void getTasks() throws Exception {
        String token = registerAndLogin("alice", "alice@example.com", "securePass1");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                TaskRequest.builder()
                                        .title("Fix bug #42")
                                        .status(TaskStatus.IN_PROGRESS)
                                        .priority(TaskPriority.MEDIUM)
                                        .build())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @Order(7)
    @DisplayName("Should filter tasks by status")
    void filterByStatus() throws Exception {
        String token = registerAndLogin("alice", "alice@example.com", "securePass1");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                TaskRequest.builder().title("Task A").status(TaskStatus.DONE).priority(TaskPriority.LOW).build())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks")
                        .param("status", "DONE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("DONE"))));
    }

    @Test
    @Order(8)
    @DisplayName("Should reject unauthenticated request with 401")
    void unauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @Order(9)
    @DisplayName("Should return 400 when task title is blank")
    void createTask_BlankTitle() throws Exception {
        String token = registerAndLogin("alice", "alice@example.com", "securePass1");

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                TaskRequest.builder().title("").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
