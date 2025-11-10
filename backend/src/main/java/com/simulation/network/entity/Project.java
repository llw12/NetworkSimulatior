package com.simulation.network.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;
    
    @Column(unique = true, nullable = false)
    private String projectName;
    
    private String simTimeLimit;
    
    @Column(length = 500)
    private String description;
    
    private String createUser;
    
    @Column(nullable = false)
    private Integer status = 0; // 0: not running, 1: running, 2: completed, 3: failed
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    @Column(nullable = false)
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
