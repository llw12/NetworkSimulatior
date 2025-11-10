package com.simulation.network.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProjectDTO {
    private Long projectId;
    private String projectName;
    private String simTimeLimit;
    private String description;
    private String createUser;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
