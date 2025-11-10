package com.simulation.network.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simulation.network.common.ErrorCode;
import com.simulation.network.dto.PageResult;
import com.simulation.network.entity.SimulationResult;
import com.simulation.network.exception.BusinessException;
import com.simulation.network.repository.SimulationResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {
    
    private final SimulationResultRepository resultRepository;
    private final ObjectMapper objectMapper;
    
    public PageResult<Map<String, Object>> queryResults(Long projectId, String metricType, 
                                                        String metricName, Integer page, Integer pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<SimulationResult> resultPage = resultRepository.findByFilters(
            projectId, metricType, metricName, pageable);
        
        List<Map<String, Object>> list = resultPage.getContent().stream()
            .map(this::convertToMap)
            .collect(Collectors.toList());
        
        return new PageResult<>(list, resultPage.getTotalElements());
    }
    
    public String exportResults(Long projectId, String format) {
        List<SimulationResult> results = resultRepository.findByProjectId(projectId, Pageable.unpaged()).getContent();
        
        if ("csv".equalsIgnoreCase(format)) {
            return exportToCsv(results);
        } else if ("json".equalsIgnoreCase(format)) {
            return exportToJson(results);
        } else {
            throw new BusinessException(40002, "Unsupported export format: " + format);
        }
    }
    
    private String exportToCsv(List<SimulationResult> results) {
        try (StringWriter writer = new StringWriter();
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("MetricType", "MetricName", "SourceModule", "Value"))) {
            
            for (SimulationResult result : results) {
                csvPrinter.printRecord(
                    result.getMetricType(),
                    result.getMetricName(),
                    result.getSourceModule(),
                    result.getValue()
                );
            }
            
            csvPrinter.flush();
            return writer.toString();
            
        } catch (IOException e) {
            log.error("Failed to export to CSV", e);
            throw new BusinessException(50001, "Failed to export results");
        }
    }
    
    private String exportToJson(List<SimulationResult> results) {
        try {
            List<Map<String, Object>> list = results.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Failed to export to JSON", e);
            throw new BusinessException(50001, "Failed to export results");
        }
    }
    
    public Map<String, Map<String, Double>> aggregateResults(Long projectId, String metrics, String percentiles) {
        String[] metricArray = metrics.split(",");
        List<Integer> percentileList = new ArrayList<>();
        
        if (percentiles != null && !percentiles.isEmpty()) {
            for (String p : percentiles.split(",")) {
                percentileList.add(Integer.parseInt(p.trim()));
            }
        }
        
        Map<String, Map<String, Double>> aggregation = new HashMap<>();
        
        for (String metric : metricArray) {
            String metricName = metric.trim();
            List<SimulationResult> results = resultRepository.findByProjectIdAndMetricTypeAndMetricName(
                projectId, "scalar", metricName);
            
            if (results.isEmpty()) {
                continue;
            }
            
            List<Double> values = results.stream()
                .map(SimulationResult::getNumericValue)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
            
            if (values.isEmpty()) {
                continue;
            }
            
            Map<String, Double> stats = new HashMap<>();
            stats.put("min", values.get(0));
            stats.put("max", values.get(values.size() - 1));
            stats.put("avg", values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            
            // Calculate percentiles
            for (Integer p : percentileList) {
                int index = (int) Math.ceil(p / 100.0 * values.size()) - 1;
                if (index >= 0 && index < values.size()) {
                    stats.put("p" + p, values.get(index));
                }
            }
            
            aggregation.put(metricName, stats);
        }
        
        return aggregation;
    }
    
    public Map<String, Object> getVectorData(Long projectId, String metricName, Integer offset, Integer limit) {
        List<SimulationResult> results = resultRepository.findByProjectIdAndMetricTypeAndMetricName(
            projectId, "vector", metricName);
        
        List<Double> allValues = results.stream()
            .map(SimulationResult::getNumericValue)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        int start = offset != null ? offset : 0;
        int end = Math.min(start + (limit != null ? limit : 500), allValues.size());
        
        List<Double> slice = allValues.subList(start, end);
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", allValues.size());
        result.put("slice", slice);
        result.put("offset", start);
        result.put("limit", limit != null ? limit : 500);
        
        return result;
    }
    
    private Map<String, Object> convertToMap(SimulationResult result) {
        Map<String, Object> map = new HashMap<>();
        map.put("metricType", result.getMetricType());
        map.put("metricName", result.getMetricName());
        map.put("sourceModule", result.getSourceModule());
        map.put("value", result.getNumericValue() != null ? result.getNumericValue() : result.getValue());
        return map;
    }
}
