package com.simulation.network.controller;

import com.simulation.network.common.ApiResponse;
import com.simulation.network.dto.PageResult;
import com.simulation.network.dto.ProjectDTO;
import com.simulation.network.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    
    private final ProjectService projectService;
    
    @GetMapping
    public ApiResponse<PageResult<ProjectDTO>> listProjects(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keywords) {
        return ApiResponse.success(projectService.listProjects(page, pageSize, keywords));
    }
    
    @PostMapping
    public ApiResponse<Void> createProject(@RequestBody ProjectDTO projectDTO) {
        projectService.createProject(projectDTO);
        return ApiResponse.success();
    }
    
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectDTO> getProject(@PathVariable Long projectId) {
        return ApiResponse.success(projectService.getProjectById(projectId));
    }
    
    @PutMapping
    public ApiResponse<Void> updateProject(@RequestBody ProjectDTO projectDTO) {
        projectService.updateProject(projectDTO);
        return ApiResponse.success();
    }
    
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
        return ApiResponse.success();
    }
}
