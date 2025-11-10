package com.simulation.network.repository;

import com.simulation.network.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByProjectName(String projectName);
    
    Page<Project> findByProjectNameContainingOrDescriptionContaining(
        String projectName, String description, Pageable pageable);
}
