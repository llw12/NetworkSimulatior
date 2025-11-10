package com.simulation.network.controller;

import com.simulation.network.common.ApiResponse;
import com.simulation.network.dto.PageResult;
import com.simulation.network.service.ResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulation-results")
@RequiredArgsConstructor
public class ResultController {
    
    private final ResultService resultService;
    
    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> queryResults(
            @RequestParam Long projectId,
            @RequestParam(required = false) String metricType,
            @RequestParam(required = false) String metricName,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        return ApiResponse.success(resultService.queryResults(
            projectId, metricType, metricName, page, pageSize));
    }
    
    @GetMapping("/export")
    public ResponseEntity<String> exportResults(
            @RequestParam Long projectId,
            @RequestParam String format) {
        String content = resultService.exportResults(projectId, format);
        
        HttpHeaders headers = new HttpHeaders();
        if ("csv".equalsIgnoreCase(format)) {
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "results.csv");
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", "results.json");
        }
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(content);
    }
    
    @GetMapping("/aggregate")
    public ApiResponse<Map<String, Map<String, Double>>> aggregateResults(
            @RequestParam Long projectId,
            @RequestParam String metrics,
            @RequestParam(required = false) String percentiles) {
        return ApiResponse.success(resultService.aggregateResults(projectId, metrics, percentiles));
    }
    
    @GetMapping("/vector")
    public ApiResponse<Map<String, Object>> getVectorData(
            @RequestParam Long projectId,
            @RequestParam String metricName,
            @RequestParam(required = false, defaultValue = "0") Integer offset,
            @RequestParam(required = false, defaultValue = "500") Integer limit) {
        return ApiResponse.success(resultService.getVectorData(projectId, metricName, offset, limit));
    }
}
