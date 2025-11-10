package com.example.restrosuite.repository;

import com.example.restrosuite.entity.Task;
import com.example.restrosuite.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    
    List<Task> findByAssignedTo(User user);
    
    List<Task> findByStatus(String status);
    
    List<Task> findByPriority(String priority);
    
    List<Task> findByDueDateBefore(LocalDateTime date);
    
    @Query("SELECT t FROM Task t ORDER BY t.createdAt DESC")
    List<Task> findAllOrderByCreatedAtDesc();
    
    @Query("SELECT t FROM Task t WHERE t.assignedTo.id = :userId ORDER BY t.createdAt DESC")
    List<Task> findByAssignedToIdOrderByCreatedAtDesc(UUID userId);
    
    @Query("SELECT t FROM Task t WHERE t.status != 'COMPLETED' AND t.dueDate < :now ORDER BY t.dueDate ASC")
    List<Task> findOverdueTasks(LocalDateTime now);
}

