package com.simulation.network.service;

import com.simulation.network.common.ErrorCode;
import com.simulation.network.entity.Project;
import com.simulation.network.entity.SimulationResult;
import com.simulation.network.exception.BusinessException;
import com.simulation.network.repository.ProjectRepository;
import com.simulation.network.repository.SimulationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {
    
    private final ProjectRepository projectRepository;
    private final SimulationResultRepository simulationResultRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Value("${simulation.base.dir}")
    private String baseDir;
    
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @Transactional
    public void startSimulation(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        
        // Check if already running
        if (project.getStatus() == 1) {
            throw new BusinessException(ErrorCode.SIMULATION_ALREADY_RUNNING);
        }
        
        // Update project status to running
        project.setStatus(1);
        projectRepository.save(project);
        
        // Start simulation in background
        executorService.submit(() -> runSimulation(projectId, project));
    }
    
    private void runSimulation(Long projectId, Project project) {
        Path projectPath = Paths.get(baseDir, project.getProjectName());
        Path iniFile = projectPath.resolve("omnetpp.ini");
        Path nedFile = projectPath.resolve(project.getProjectName() + ".ned");
        
        if (!Files.exists(iniFile) || !Files.exists(nedFile)) {
            updateProjectStatus(projectId, 3); // Failed
            sendLog(projectId, "[ERROR] INI or NED file not found");
            return;
        }
        
        try {
            // Get the run_sim.sh script path
            Path scriptPath = Paths.get("/home/runner/work/NetworkSimulatior/NetworkSimulatior/run_sim.sh");
            if (!Files.exists(scriptPath)) {
                sendLog(projectId, "[ERROR] run_sim.sh not found");
                updateProjectStatus(projectId, 3);
                return;
            }
            
            sendLog(projectId, "[INFO] Starting simulation for project: " + project.getProjectName());
            
            // Build command
            ProcessBuilder pb = new ProcessBuilder(
                "bash",
                scriptPath.toString(),
                iniFile.toString(),
                nedFile.toString(),
                projectPath.toString()
            );
            
            pb.directory(projectPath.toFile());
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            // Save process PID
            savePid(projectPath, process.pid());
            
            // Read and send output in real-time
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sendLog(projectId, line);
                    log.info("Simulation output: {}", line);
                }
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                sendLog(projectId, "[INFO] Simulation completed successfully");
                
                // Parse SQLite results
                parseSimulationResults(projectId, projectPath);
                
                updateProjectStatus(projectId, 2); // Completed
            } else {
                sendLog(projectId, "[ERROR] Simulation failed with exit code: " + exitCode);
                updateProjectStatus(projectId, 3); // Failed
            }
            
        } catch (Exception e) {
            log.error("Failed to run simulation", e);
            sendLog(projectId, "[ERROR] " + e.getMessage());
            updateProjectStatus(projectId, 3);
        }
    }
    
    @Transactional
    public void stopSimulation(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        
        if (project.getStatus() != 1) {
            throw new BusinessException(40005, "Simulation is not running");
        }
        
        try {
            Path projectPath = Paths.get(baseDir, project.getProjectName());
            Path pidFile = projectPath.resolve("pid");
            
            if (Files.exists(pidFile)) {
                String pid = Files.readString(pidFile).trim();
                
                // Kill the process
                ProcessBuilder pb = new ProcessBuilder("kill", "-9", pid);
                Process process = pb.start();
                process.waitFor();
                
                sendLog(projectId, "[INFO] Simulation stopped by user");
                log.info("Stopped simulation with PID: {}", pid);
                
                Files.deleteIfExists(pidFile);
            }
            
            project.setStatus(3); // Failed/Stopped
            projectRepository.save(project);
            
        } catch (Exception e) {
            log.error("Failed to stop simulation", e);
            throw new BusinessException(ErrorCode.SCRIPT_EXECUTION_FAILED);
        }
    }
    
    private void savePid(Path projectPath, long pid) {
        try {
            Path pidFile = projectPath.resolve("pid");
            Files.writeString(pidFile, String.valueOf(pid));
        } catch (IOException e) {
            log.error("Failed to save PID", e);
        }
    }
    
    private void sendLog(Long projectId, String message) {
        try {
            messagingTemplate.convertAndSend("/topic/sim/log/" + projectId, message);
        } catch (Exception e) {
            log.error("Failed to send log message", e);
        }
    }
    
    private void updateProjectStatus(Long projectId, Integer status) {
        try {
            Project project = projectRepository.findById(projectId).orElse(null);
            if (project != null) {
                project.setStatus(status);
                projectRepository.save(project);
            }
        } catch (Exception e) {
            log.error("Failed to update project status", e);
        }
    }
    
    private void parseSimulationResults(Long projectId, Path projectPath) {
        try {
            // Look for .vec and .sca files
            File[] vecFiles = projectPath.toFile().listFiles((dir, name) -> name.endsWith(".vec"));
            File[] scaFiles = projectPath.toFile().listFiles((dir, name) -> name.endsWith(".sca"));
            
            if (vecFiles != null) {
                for (File vecFile : vecFiles) {
                    parseVectorFile(projectId, vecFile);
                }
            }
            
            if (scaFiles != null) {
                for (File scaFile : scaFiles) {
                    parseScalarFile(projectId, scaFile);
                }
            }
            
            log.info("Parsed simulation results for project: {}", projectId);
            
        } catch (Exception e) {
            log.error("Failed to parse simulation results", e);
            throw new BusinessException(ErrorCode.SQLITE_PARSE_ERROR);
        }
    }
    
    private void parseVectorFile(Long projectId, File vecFile) {
        try {
            String url = "jdbc:sqlite:" + vecFile.getAbsolutePath();
            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {
                
                // Query vector data
                String query = "SELECT v.vectorName, v.moduleName, d.eventNumber, d.simtimeRaw, d.value " +
                              "FROM vector v LEFT JOIN vectorData d ON v.vectorId = d.vectorId";
                
                ResultSet rs = stmt.executeQuery(query);
                
                while (rs.next()) {
                    SimulationResult result = new SimulationResult();
                    result.setProjectId(projectId);
                    result.setMetricType("vector");
                    result.setMetricName(rs.getString("vectorName"));
                    result.setSourceModule(rs.getString("moduleName"));
                    result.setNumericValue(rs.getDouble("value"));
                    result.setValue(String.valueOf(rs.getDouble("value")));
                    
                    simulationResultRepository.save(result);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse vector file: {}", vecFile.getName(), e);
        }
    }
    
    private void parseScalarFile(Long projectId, File scaFile) {
        try {
            String url = "jdbc:sqlite:" + scaFile.getAbsolutePath();
            try (Connection conn = DriverManager.getConnection(url);
                 Statement stmt = conn.createStatement()) {
                
                // Query scalar data
                String query = "SELECT scalarName, moduleName, value FROM scalar";
                ResultSet rs = stmt.executeQuery(query);
                
                while (rs.next()) {
                    SimulationResult result = new SimulationResult();
                    result.setProjectId(projectId);
                    result.setMetricType("scalar");
                    result.setMetricName(rs.getString("scalarName"));
                    result.setSourceModule(rs.getString("moduleName"));
                    result.setNumericValue(rs.getDouble("value"));
                    result.setValue(String.valueOf(rs.getDouble("value")));
                    
                    simulationResultRepository.save(result);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse scalar file: {}", scaFile.getName(), e);
        }
    }
}
