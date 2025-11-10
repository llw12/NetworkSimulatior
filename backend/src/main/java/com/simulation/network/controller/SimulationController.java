package com.simulation.network.controller;

import com.simulation.network.common.ApiResponse;
import com.simulation.network.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/simulations")
@RequiredArgsConstructor
public class SimulationController {
    
    private final SimulationService simulationService;
    
    @GetMapping("/start/{projectId}")
    public ApiResponse<Void> startSimulation(@PathVariable Long projectId) {
        simulationService.startSimulation(projectId);
        return ApiResponse.success();
    }
    
    @GetMapping("/stop/{projectId}")
    public ApiResponse<Void> stopSimulation(@PathVariable Long projectId) {
        simulationService.stopSimulation(projectId);
        return ApiResponse.success();
    }
}
