package com.simulation.network.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Project related
    PROJECT_NAME_EXISTS(40001, "项目名已存在"),
    NODE_TYPE_NOT_SUPPORTED(40002, "节点类型不支持"),
    TOPOLOGY_MISSING_REQUIRED_NODE(40003, "拓扑缺少必需节点"),
    SIMULATION_PARAM_MISSING(40004, "启动参数缺失"),
    SIMULATION_ALREADY_RUNNING(40005, "仿真运行中禁止重复启动"),
    HIL_CONFIG_MISSING(40006, "HIL 客户端配置缺失"),
    MASTER_CONFIG_INVALID(40007, "MasterConfig 内容非法"),
    SLAVE_CONFIG_INVALID(40008, "SlaveConfig 内容非法"),
    PORT_CONFLICT(40009, "端口冲突"),
    NO_VALID_CLIENT(40010, "无有效客户端"),
    
    // Not found
    PROJECT_NOT_FOUND(40401, "项目不存在"),
    NODE_NOT_FOUND(40402, "节点不存在"),
    TOPOLOGY_NOT_FOUND(40403, "拓扑版本/文件不存在"),
    SIMULATION_NOT_FOUND(40404, "仿真实例不存在"),
    
    // Server errors
    SCRIPT_EXECUTION_FAILED(50001, "脚本执行失败"),
    SQLITE_PARSE_ERROR(50002, "SQLite 解析异常"),
    INI_GENERATION_FAILED(50003, "INI 生成失败"),
    NED_GENERATION_FAILED(50004, "NED 生成失败");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
