package com.simulation.network.service;

import com.simulation.network.common.ErrorCode;
import com.simulation.network.dto.PageResult;
import com.simulation.network.dto.ProjectDTO;
import com.simulation.network.entity.Project;
import com.simulation.network.exception.BusinessException;
import com.simulation.network.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    
    @Value("${simulation.base.dir}")
    private String baseDir;
    
    public PageResult<ProjectDTO> listProjects(Integer page, Integer pageSize, String keywords) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Project> projectPage;
        
        if (keywords != null && !keywords.isEmpty()) {
            projectPage = projectRepository.findByProjectNameContainingOrDescriptionContaining(
                keywords, keywords, pageable);
        } else {
            projectPage = projectRepository.findAll(pageable);
        }
        
        List<ProjectDTO> dtoList = projectPage.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return new PageResult<>(dtoList, projectPage.getTotalElements());
    }
    
    @Transactional
    public void createProject(ProjectDTO projectDTO) {
        // Check if project name already exists
        if (projectRepository.findByProjectName(projectDTO.getProjectName()).isPresent()) {
            throw new BusinessException(ErrorCode.PROJECT_NAME_EXISTS);
        }
        
        // Create project entity
        Project project = new Project();
        BeanUtils.copyProperties(projectDTO, project);
        project.setStatus(0); // Not running
        
        // Set default simulation time if not provided
        if (project.getSimTimeLimit() == null || project.getSimTimeLimit().isEmpty()) {
            project.setSimTimeLimit("10s");
        }
        
        projectRepository.save(project);
        
        // Create project directory
        try {
            Path projectPath = Paths.get(baseDir, project.getProjectName());
            Files.createDirectories(projectPath);
            log.info("Created project directory: {}", projectPath);
        } catch (Exception e) {
            log.error("Failed to create project directory", e);
            throw new BusinessException(50001, "Failed to create project directory");
        }
    }
    
    public ProjectDTO getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        return convertToDTO(project);
    }
    
    @Transactional
    public void updateProject(ProjectDTO projectDTO) {
        Project project = projectRepository.findById(projectDTO.getProjectId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        
        // Check if new name conflicts with existing project
        if (projectDTO.getProjectName() != null && 
            !projectDTO.getProjectName().equals(project.getProjectName())) {
            if (projectRepository.findByProjectName(projectDTO.getProjectName()).isPresent()) {
                throw new BusinessException(ErrorCode.PROJECT_NAME_EXISTS);
            }
            project.setProjectName(projectDTO.getProjectName());
        }
        
        if (projectDTO.getSimTimeLimit() != null) {
            project.setSimTimeLimit(projectDTO.getSimTimeLimit());
        }
        if (projectDTO.getDescription() != null) {
            project.setDescription(projectDTO.getDescription());
        }
        
        projectRepository.save(project);
    }
    
    @Transactional
    public void deleteProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        
        // Check if simulation is running
        if (project.getStatus() == 1) {
            throw new BusinessException(ErrorCode.SIMULATION_ALREADY_RUNNING);
        }
        
        // Delete project directory
        try {
            Path projectPath = Paths.get(baseDir, project.getProjectName());
            if (Files.exists(projectPath)) {
                deleteDirectory(projectPath.toFile());
                log.info("Deleted project directory: {}", projectPath);
            }
        } catch (Exception e) {
            log.error("Failed to delete project directory", e);
        }
        
        projectRepository.delete(project);
    }
    
    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
    
    private ProjectDTO convertToDTO(Project project) {
        ProjectDTO dto = new ProjectDTO();
        BeanUtils.copyProperties(project, dto);
        return dto;
    }
}
