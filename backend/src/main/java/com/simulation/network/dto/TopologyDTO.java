package com.simulation.network.dto;

import lombok.Data;
import java.util.List;

@Data
public class TopologyDTO {
    private List<NodeDTO> nodes;
    private List<LinkDTO> links;
    
    @Data
    public static class NodeDTO {
        private String nodeId;
        private String nodeType;
        private String nodeName;
        private PositionDTO position;
        private NodeParamsDTO params;
    }
    
    @Data
    public static class PositionDTO {
        private Double x;
        private Double y;
    }
    
    @Data
    public static class NodeParamsDTO {
        private Boolean isHil;
        private List<AppDTO> apps;
        private List<IpConfigDTO> ipConfig;
        private String status;
    }
    
    @Data
    public static class AppDTO {
        private String typename;
        private String localAddress;
        private Integer localPort;
        private String remoteAddress;
        private Integer remotePort;
        private Integer connectport;
        private Integer numConnect;
        private String readInterval;
        private String interval;
        private String startTime;
        private String reconnectInterval;
        private String modbusRequest;
        private String sendTime;
        private Integer seed;
        private String configFile;
        private String slavesConfigPath;
        private Object Masterconfig;
        private Object slavesConfig;
    }
    
    @Data
    public static class IpConfigDTO {
        private String ipAddress;
        private String netmask;
        private String interfaceName; // "interface" is a keyword, so using interfaceName
    }
    
    @Data
    public static class LinkDTO {
        private String linkId;
        private String sourceNodeId;
        private String targetNodeId;
        private String dataRate;
        private Double ber;
        private Double per;
        private String length;
        private String status;
    }
}
