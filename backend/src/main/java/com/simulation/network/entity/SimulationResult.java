package com.simulation.network.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "simulation_results")
public class SimulationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resultId;
    
    @Column(nullable = false)
    private Long projectId;
    
    @Column(nullable = false)
    private String metricType; // scalar or vector
    
    @Column(nullable = false)
    private String metricName;
    
    private String sourceModule;
    
    @Column(columnDefinition = "TEXT")
    private String value; // For scalar: numeric value, for vector: JSON array or reference
    
    private Double numericValue; // For scalar values and aggregations
}
