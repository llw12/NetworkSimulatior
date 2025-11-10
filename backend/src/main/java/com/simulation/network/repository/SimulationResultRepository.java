package com.simulation.network.repository;

import com.simulation.network.entity.SimulationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationResultRepository extends JpaRepository<SimulationResult, Long> {
    Page<SimulationResult> findByProjectId(Long projectId, Pageable pageable);
    
    Page<SimulationResult> findByProjectIdAndMetricType(Long projectId, String metricType, Pageable pageable);
    
    Page<SimulationResult> findByProjectIdAndMetricTypeAndMetricNameContaining(
        Long projectId, String metricType, String metricName, Pageable pageable);
    
    @Query("SELECT sr FROM SimulationResult sr WHERE sr.projectId = :projectId " +
           "AND (:metricType IS NULL OR sr.metricType = :metricType) " +
           "AND (:metricName IS NULL OR sr.metricName LIKE %:metricName%)")
    Page<SimulationResult> findByFilters(Long projectId, String metricType, String metricName, Pageable pageable);
    
    List<SimulationResult> findByProjectIdAndMetricTypeAndMetricName(
        Long projectId, String metricType, String metricName);
}
