package com.simulation.network.repository;

import com.simulation.network.entity.Topology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TopologyRepository extends JpaRepository<Topology, Long> {
    Optional<Topology> findByProjectId(Long projectId);
}
