package com.simulation.network.controller;

import com.simulation.network.common.ApiResponse;
import com.simulation.network.dto.TopologyDTO;
import com.simulation.network.service.TopologyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/topology")
@RequiredArgsConstructor
public class TopologyController {
    
    private final TopologyService topologyService;
    
    @PostMapping
    public ApiResponse<Void> saveTopology(
            @PathVariable Long projectId,
            @RequestBody TopologyDTO topologyDTO) {
        topologyService.saveTopology(projectId, topologyDTO);
        return ApiResponse.success();
    }
}
