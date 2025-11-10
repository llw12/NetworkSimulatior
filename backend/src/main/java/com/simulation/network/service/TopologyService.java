package com.simulation.network.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulation.network.common.ErrorCode;
import com.simulation.network.dto.TopologyDTO;
import com.simulation.network.entity.Project;
import com.simulation.network.entity.Topology;
import com.simulation.network.exception.BusinessException;
import com.simulation.network.repository.ProjectRepository;
import com.simulation.network.repository.TopologyRepository;
import com.simulation.network.util.NedGenerator;
import com.simulation.network.util.IniGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class TopologyService {
    
    private final TopologyRepository topologyRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${simulation.base.dir}")
    private String baseDir;
    
    @Transactional
    public void saveTopology(Long projectId, TopologyDTO topologyDTO) {
        // Check if project exists
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        
        // Validate topology
        validateTopology(topologyDTO);
        
        try {
            // Convert topology to JSON
            String topologyJson = objectMapper.writeValueAsString(topologyDTO);
            
            // Save or update topology in database
            Topology topology = topologyRepository.findByProjectId(projectId)
                .orElse(new Topology());
            topology.setProjectId(projectId);
            topology.setTopologyData(topologyJson);
            topologyRepository.save(topology);
            
            // Generate NED and INI files
            Path projectPath = Paths.get(baseDir, project.getProjectName());
            generateNedFile(projectPath, topologyDTO, project);
            generateIniFile(projectPath, topologyDTO, project);
            generateConfigFiles(projectPath, topologyDTO);
            
            log.info("Topology saved and files generated for project: {}", projectId);
            
        } catch (Exception e) {
            log.error("Failed to save topology", e);
            throw new BusinessException(50001, "Failed to save topology: " + e.getMessage());
        }
    }
    
    private void validateTopology(TopologyDTO topologyDTO) {
        if (topologyDTO.getNodes() == null || topologyDTO.getNodes().isEmpty()) {
            throw new BusinessException(ErrorCode.TOPOLOGY_MISSING_REQUIRED_NODE);
        }
        
        // Check for required node types
        boolean hasDevice = topologyDTO.getNodes().stream()
            .anyMatch(node -> "TsnDevice".equals(node.getNodeType()));
        
        if (!hasDevice) {
            throw new BusinessException(ErrorCode.NO_VALID_CLIENT);
        }
        
        // Validate port conflicts within each node
        for (TopologyDTO.NodeDTO node : topologyDTO.getNodes()) {
            if (node.getParams() != null && node.getParams().getApps() != null) {
                long uniquePorts = node.getParams().getApps().stream()
                    .map(TopologyDTO.AppDTO::getLocalPort)
                    .filter(port -> port != null)
                    .distinct()
                    .count();
                
                long totalPorts = node.getParams().getApps().stream()
                    .map(TopologyDTO.AppDTO::getLocalPort)
                    .filter(port -> port != null)
                    .count();
                
                if (uniquePorts < totalPorts) {
                    throw new BusinessException(ErrorCode.PORT_CONFLICT);
                }
            }
        }
        
        // Validate HIL configurations
        for (TopologyDTO.NodeDTO node : topologyDTO.getNodes()) {
            if (node.getParams() != null && Boolean.TRUE.equals(node.getParams().getIsHil())) {
                if (node.getParams().getApps() != null) {
                    for (TopologyDTO.AppDTO app : node.getParams().getApps()) {
                        if ("ModbusSlaveHILApp".equals(app.getTypename())) {
                            if (app.getRemoteAddress() == null || app.getRemotePort() == null) {
                                throw new BusinessException(ErrorCode.HIL_CONFIG_MISSING);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void generateNedFile(Path projectPath, TopologyDTO topologyDTO, Project project) {
        try {
            String nedContent = NedGenerator.generate(topologyDTO, project.getProjectName());
            Path nedFile = projectPath.resolve(project.getProjectName() + ".ned");
            Files.writeString(nedFile, nedContent);
            log.info("Generated NED file: {}", nedFile);
        } catch (Exception e) {
            log.error("Failed to generate NED file", e);
            throw new BusinessException(ErrorCode.NED_GENERATION_FAILED);
        }
    }
    
    private void generateIniFile(Path projectPath, TopologyDTO topologyDTO, Project project) {
        try {
            String iniContent = IniGenerator.generate(topologyDTO, project);
            Path iniFile = projectPath.resolve("omnetpp.ini");
            Files.writeString(iniFile, iniContent);
            log.info("Generated INI file: {}", iniFile);
        } catch (Exception e) {
            log.error("Failed to generate INI file", e);
            throw new BusinessException(ErrorCode.INI_GENERATION_FAILED);
        }
    }
    
    private void generateConfigFiles(Path projectPath, TopologyDTO topologyDTO) {
        try {
            // Generate MasterConfig.json and SlaveConfig.json
            for (TopologyDTO.NodeDTO node : topologyDTO.getNodes()) {
                if (node.getParams() != null && node.getParams().getApps() != null) {
                    for (TopologyDTO.AppDTO app : node.getParams().getApps()) {
                        // Generate MasterConfig.json
                        if ("ModbusMasterApp".equals(app.getTypename()) && app.getMasterconfig() != null) {
                            String masterConfig = objectMapper.writeValueAsString(app.getMasterconfig());
                            Path masterConfigFile = projectPath.resolve("MasterConfig.json");
                            Files.writeString(masterConfigFile, masterConfig);
                            log.info("Generated MasterConfig.json");
                        }
                        
                        // Generate SlaveConfig.json
                        if (app.getSlavesConfig() != null) {
                            String slaveConfig = objectMapper.writeValueAsString(app.getSlavesConfig());
                            Path slaveConfigFile = projectPath.resolve("SlaveConfig.json");
                            Files.writeString(slaveConfigFile, slaveConfig);
                            log.info("Generated SlaveConfig.json");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate config files", e);
        }
    }
}
