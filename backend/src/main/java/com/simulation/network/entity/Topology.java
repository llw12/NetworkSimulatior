package com.simulation.network.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "topologies")
public class Topology {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long topologyId;
    
    @Column(nullable = false)
    private Long projectId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String topologyData; // JSON format
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
