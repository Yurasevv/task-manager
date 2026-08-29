package com.taskmanager.service;

import com.taskmanager.dto.TaskRequest;
import com.taskmanager.dto.TaskResponse;
import com.taskmanager.entity.Role;
import com.taskmanager.entity.Task;
import com.taskmanager.entity.TaskStatus;
import com.taskmanager.entity.TaskPriority;
import com.taskmanager.entity.User;
import com.taskmanager.exception.ResourceNotFoundException;
import com.taskmanager.mapper.TaskMapper;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.TaskSpecification;
import com.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public TaskResponse createTask(TaskRequest request, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
        
        User assignee = null;
        if (request.assigneeId() != null) {
            assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
        }

        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.TODO);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
        task.setAuthor(author);
        task.setAssignee(assignee);

        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully with ID: {}", savedTask.getId());
        return taskMapper.toResponse(savedTask);
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        return taskMapper.toResponse(task);
    }

    public TaskResponse updateTask(Long id, TaskRequest request, String username) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!task.getAuthor().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            log.warn("User {} is not authorized to update task {}", username, id);
            throw new AccessDeniedException("Not authorized to update this task");
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }

        if (request.assigneeId() != null) {
            User assignee = userRepository.findById(request.assigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found"));
            task.setAssignee(assignee);
        } else {
            task.setAssignee(null);
        }

        Task savedTask = taskRepository.save(task);
        log.info("Task {} updated successfully", savedTask.getId());
        return taskMapper.toResponse(savedTask);
    }

    public void deleteTask(Long id, String username) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!task.getAuthor().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            log.warn("User {} is not authorized to delete task {}", username, id);
            throw new AccessDeniedException("Not authorized to delete this task");
        }

        taskRepository.delete(task);
        log.info("Task {} deleted successfully", id);
    }

    public Page<TaskResponse> getTasks(TaskStatus status, Long assigneeId, Long authorId, Pageable pageable) {
        Specification<Task> spec = Specification.where(TaskSpecification.hasStatus(status))
                .and(TaskSpecification.hasAssignee(assigneeId))
                .and(TaskSpecification.hasAuthor(authorId));

        return taskRepository.findAll(spec, pageable).map(taskMapper::toResponse);
    }
}
