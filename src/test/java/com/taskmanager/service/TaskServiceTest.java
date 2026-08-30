package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.*;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.mapper.TaskMapper;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private User adminUser;
    private User otherUser;
    private Task testTask;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encoded_password")
                .role(Role.USER)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("admin")
                .email("admin@example.com")
                .password("encoded_password")
                .role(Role.ADMIN)
                .build();

        otherUser = User.builder()
                .id(3L)
                .username("otheruser")
                .email("other@example.com")
                .password("encoded_password")
                .role(Role.USER)
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .author(testUser)
                .assignee(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        lenient().when(taskMapper.toResponse(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            return TaskResponse.builder()
                    .id(t.getId())
                    .title(t.getTitle())
                    .status(t.getStatus())
                    .priority(t.getPriority())
                    .author(t.getAuthor() != null ? com.taskmanager.dto.UserResponse.builder().username(t.getAuthor().getUsername()).build() : null)
                    .assignee(t.getAssignee() != null ? com.taskmanager.dto.UserResponse.builder().username(t.getAssignee().getUsername()).build() : null)
                    .build();
        });
    }

    @Test
    @DisplayName("Should create task successfully")
    void createTask_Success() {
        TaskRequest request = TaskRequest.builder()
                .title("New Task")
                .description("New Description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.HIGH)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        TaskResponse response = taskService.createTask(request, "testuser");

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("New Task");
        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.author().username()).isEqualTo("testuser");
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("Should create task with default status and priority when null")
    void createTask_DefaultStatusAndPriority() {
        TaskRequest request = TaskRequest.builder()
                .title("Task without status")
                .description("Description")
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(2L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        TaskResponse response = taskService.createTask(request, "testuser");

        assertThat(response.status()).isEqualTo(TaskStatus.TODO);
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    @DisplayName("Should create task with assignee")
    void createTask_WithAssignee() {
        TaskRequest request = TaskRequest.builder()
                .title("Assigned Task")
                .description("Description")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.MEDIUM)
                .assigneeId(3L)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(otherUser));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(3L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return saved;
        });

        TaskResponse response = taskService.createTask(request, "testuser");

        assertThat(response.assignee()).isNotNull();
        assertThat(response.assignee().username()).isEqualTo("otheruser");
    }

    @Test
    @DisplayName("Should throw exception when author not found")
    void createTask_AuthorNotFound() {
        TaskRequest request = TaskRequest.builder()
                .title("Task")
                .build();

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(request, "unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Author not found");
    }

    @Test
    @DisplayName("Should get task by ID")
    void getTaskById_Success() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        TaskResponse response = taskService.getTaskById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("Test Task");
    }
}
