package com.example.petpooja_clone.controller;

import com.example.petpooja_clone.entity.Task;
import com.example.petpooja_clone.entity.User;
import com.example.petpooja_clone.repository.TaskRepository;
import com.example.petpooja_clone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<Task> getAllTasks() {
        return taskRepository.findAllOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public Task getTask(@PathVariable UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    @GetMapping("/assigned-to/{userId}")
    public List<Task> getTasksByUser(@PathVariable UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found");
        }
        return taskRepository.findByAssignedToIdOrderByCreatedAtDesc(userId);
    }

    @GetMapping("/status/{status}")
    public List<Task> getTasksByStatus(@PathVariable String status) {
        return taskRepository.findByStatus(status);
    }

    @GetMapping("/overdue")
    public List<Task> getOverdueTasks() {
        return taskRepository.findOverdueTasks(LocalDateTime.now());
    }

    @PostMapping
    public Task createTask(@RequestBody Map<String, Object> payload) {
        UUID assignedToId = payload.get("assignedToId") != null 
            ? UUID.fromString(payload.get("assignedToId").toString()) 
            : null;
        UUID createdById = payload.get("createdById") != null 
            ? UUID.fromString(payload.get("createdById").toString()) 
            : null;

        User assignedTo = null;
        if (assignedToId != null) {
            assignedTo = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
        }

        User createdBy = null;
        if (createdById != null) {
            createdBy = userRepository.findById(createdById)
                    .orElseThrow(() -> new RuntimeException("Creator user not found"));
        }

        LocalDateTime dueDate = null;
        if (payload.get("dueDate") != null) {
            dueDate = LocalDateTime.parse(payload.get("dueDate").toString());
        }

        Task task = Task.builder()
                .title(payload.get("title").toString())
                .description(payload.get("description") != null ? payload.get("description").toString() : null)
                .assignedTo(assignedTo)
                .createdBy(createdBy)
                .status(payload.get("status") != null ? payload.get("status").toString() : "PENDING")
                .priority(payload.get("priority") != null ? payload.get("priority").toString() : "MEDIUM")
                .dueDate(dueDate)
                .createdAt(LocalDateTime.now())
                .notes(payload.get("notes") != null ? payload.get("notes").toString() : null)
                .build();

        return taskRepository.save(task);
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable UUID id, @RequestBody Map<String, Object> payload) {
        Task existing = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (payload.containsKey("title")) {
            existing.setTitle(payload.get("title").toString());
        }
        if (payload.containsKey("description")) {
            existing.setDescription(payload.get("description").toString());
        }
        if (payload.containsKey("assignedToId")) {
            UUID assignedToId = UUID.fromString(payload.get("assignedToId").toString());
            User assignedTo = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            existing.setAssignedTo(assignedTo);
        }
        if (payload.containsKey("status")) {
            String newStatus = payload.get("status").toString();
            existing.setStatus(newStatus);
            if ("COMPLETED".equals(newStatus) && existing.getCompletedAt() == null) {
                existing.setCompletedAt(LocalDateTime.now());
            } else if (!"COMPLETED".equals(newStatus)) {
                existing.setCompletedAt(null);
            }
        }
        if (payload.containsKey("priority")) {
            existing.setPriority(payload.get("priority").toString());
        }
        if (payload.containsKey("dueDate")) {
            if (payload.get("dueDate") != null) {
                existing.setDueDate(LocalDateTime.parse(payload.get("dueDate").toString()));
            } else {
                existing.setDueDate(null);
            }
        }
        if (payload.containsKey("notes")) {
            existing.setNotes(payload.get("notes").toString());
        }

        return taskRepository.save(existing);
    }

    @PutMapping("/{id}/status")
    public Task updateTaskStatus(@PathVariable UUID id, @RequestParam String status) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(status);
        if ("COMPLETED".equals(status) && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        } else if (!"COMPLETED".equals(status)) {
            task.setCompletedAt(null);
        }
        return taskRepository.save(task);
    }

    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable UUID id) {
        if (!taskRepository.existsById(id)) {
            throw new RuntimeException("Task not found");
        }
        taskRepository.deleteById(id);
    }
}

