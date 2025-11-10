package com.simulation.network.util;

import com.simulation.network.dto.TopologyDTO;
import com.simulation.network.entity.Project;

import java.util.*;

public class IniGenerator {
    
    public static String generate(TopologyDTO topology, Project project) {
        StringBuilder ini = new StringBuilder();
        
        // General section
        ini.append("[General]\n");
        ini.append("network = ").append(project.getProjectName()).append("\n");
        ini.append("sim-time-limit = ").append(project.getSimTimeLimit()).append("\n");
        ini.append("outputvectormanager-class=\"omnetpp::envir::SqliteOutputVectorManager\"\n");
        ini.append("outputscalarmanager-class=\"omnetpp::envir::SqliteOutputScalarManager\"\n\n");
        
        // Misc settings
        ini.append("# misc settings\n");
        ini.append("**.crcMode = \"computed\"\n");
        ini.append("**.fcsMode = \"computed\"\n\n");
        
        // Ethernet settings
        ini.append("*.*.eth[*].bitrate = 100Mbps\n\n");
        
        // Linking settings
        ini.append("#Linking settings\n");
        ini.append("*.*.ethg$o[*].channel.typename = \"inet.node.ethernet.EthernetLink\"\n");
        ini.append("*.*.ethg$o[*].channel.datarate = 100Mbps\n");
        ini.append("*.*.ethg$o[*].channel.ber = 0\n");
        ini.append("*.*.ethg$o[*].channel.per = 0\n");
        ini.append("*.*.ethg$o[*].channel.length = 10m\n\n");
        
        // IP settings
        ini.append("#ip settings\n");
        ini.append("*.*.ip.typename = \"inet.networklayer.ipv4.Ipv4NetworkLayer\"\n");
        
        // Generate IP configuration
        generateIpConfig(ini, topology);
        
        ini.append("*.*.ipv4.arp.typename = \"Arp\"\n\n");
        
        // TCP settings
        ini.append("#tcp settings\n");
        ini.append("**.tcp.typename = \"Tcp\"\n");
        ini.append("**.tcp.advertisedWindow = 65535\n");
        ini.append("**.tcp.delayedAcksEnabled = false\n");
        ini.append("**.tcp.nagleEnabled = false\n");
        ini.append("**.tcp.limitedTransmitEnabled = true\n");
        ini.append("**.tcp.increasedIWEnabled = true\n");
        ini.append("**.tcp.sackSupport = true\n");
        ini.append("**.tcp.windowScalingSupport = true\n");
        ini.append("**.tcp.windowScalingFactor = 3\n");
        ini.append("**.tcp.timestampSupport = true\n");
        ini.append("**.tcp.mss = 1452\n");
        ini.append("**.tcp.tcpAlgorithmClass = \"TcpReno\"\n\n");
        
        // Generate application configurations for each node
        for (TopologyDTO.NodeDTO node : topology.getNodes()) {
            generateNodeConfig(ini, node);
        }
        
        // Configuration sections
        ini.append("\n[unHardInLoop]\n");
        ini.append("*.client[*].numApps = 1\n");
        ini.append("*.client[*].app[0].typename = \"ModbusSlaveApp\"\n");
        ini.append("*.client[*].app[0].localAddress = \"\"\n");
        ini.append("*.client[*].app[0].localPort = 502\n");
        ini.append("*.client[*].app[0].slavesConfigPath = \"SlaveConfig.json\"\n\n");
        
        ini.append("[HardInLoop]\n");
        ini.append("scheduler-class = \"inet::RealTimeScheduler\"\n");
        ini.append("*.client[*].numApps = 1\n");
        ini.append("*.client[*].app[0].typename = \"ModbusSlaveApp\"\n");
        ini.append("*.client[*].app[0].localAddress = \"\"\n");
        ini.append("*.client[*].app[0].localPort = 502\n");
        ini.append("*.client[*].app[0].slavesConfigPath = \"SlaveConfig.json\"\n\n");
        
        // Add HIL specific configurations
        generateHilConfig(ini, topology);
        
        return ini.toString();
    }
    
    private static void generateIpConfig(StringBuilder ini, TopologyDTO topology) {
        StringBuilder xmlConfig = new StringBuilder("<config>");
        
        for (TopologyDTO.NodeDTO node : topology.getNodes()) {
            if (node.getParams() != null && node.getParams().getIpConfig() != null) {
                for (int i = 0; i < node.getParams().getIpConfig().size(); i++) {
                    TopologyDTO.IpConfigDTO ipConfig = node.getParams().getIpConfig().get(i);
                    xmlConfig.append(" \\\n                                ");
                    xmlConfig.append("<interface hosts='").append(node.getNodeName());
                    xmlConfig.append("' names='eth").append(i);
                    xmlConfig.append("' address='").append(ipConfig.getIpAddress());
                    xmlConfig.append("' netmask='").append(ipConfig.getNetmask()).append("'/>");
                }
            }
        }
        
        xmlConfig.append(" \\\n                             </config>");
        
        ini.append("*.configurator.config = xml(\"").append(xmlConfig).append("\")\n");
    }
    
    private static void generateNodeConfig(StringBuilder ini, TopologyDTO.NodeDTO node) {
        if (node.getParams() == null || node.getParams().getApps() == null || 
            node.getParams().getApps().isEmpty()) {
            return;
        }
        
        String nodeName = node.getNodeName();
        ini.append("#").append(nodeName).append(" settings\n");
        ini.append("*.").append(nodeName).append(".numApps = ")
           .append(node.getParams().getApps().size()).append("\n");
        
        for (int i = 0; i < node.getParams().getApps().size(); i++) {
            TopologyDTO.AppDTO app = node.getParams().getApps().get(i);
            String prefix = "*." + nodeName + ".app[" + i + "]";
            
            ini.append(prefix).append(".typename = \"").append(app.getTypename()).append("\"\n");
            ini.append(prefix).append(".localAddress = \"")
               .append(app.getLocalAddress() != null ? app.getLocalAddress() : "").append("\"\n");
            
            if (app.getLocalPort() != null) {
                ini.append(prefix).append(".localPort = ").append(app.getLocalPort()).append("\n");
            }
            
            if (app.getRemoteAddress() != null) {
                ini.append(prefix).append(".connectAddress = \"").append(app.getRemoteAddress()).append("\"\n");
            }
            
            if (app.getRemotePort() != null) {
                ini.append(prefix).append(".connectPort = ").append(app.getRemotePort()).append("\n");
            }
            
            if (app.getConnectport() != null) {
                ini.append(prefix).append(".connectport = ").append(app.getConnectport()).append("\n");
            }
            
            if (app.getNumConnect() != null) {
                ini.append(prefix).append(".numConnect = ").append(app.getNumConnect()).append("\n");
            }
            
            if (app.getReadInterval() != null) {
                ini.append(prefix).append(".readInterval = ").append(app.getReadInterval()).append("\n");
            }
            
            if (app.getInterval() != null) {
                ini.append(prefix).append(".interval = ").append(app.getInterval()).append("\n");
            }
            
            if (app.getStartTime() != null) {
                ini.append(prefix).append(".startTime = ").append(app.getStartTime()).append("\n");
            }
            
            if (app.getReconnectInterval() != null) {
                ini.append(prefix).append(".reconnectInterval = ").append(app.getReconnectInterval()).append("\n");
            }
            
            if (app.getModbusRequest() != null) {
                ini.append(prefix).append(".modbusRequest = \"").append(app.getModbusRequest()).append("\"\n");
            }
            
            if (app.getSendTime() != null) {
                ini.append(prefix).append(".sendTime = \"").append(app.getSendTime()).append("\"\n");
            }
            
            if (app.getSeed() != null) {
                ini.append(prefix).append(".seed = ").append(app.getSeed()).append("\n");
            }
            
            if (app.getConfigFile() != null) {
                ini.append(prefix).append(".configFile = \"").append(app.getConfigFile()).append("\"\n");
            }
            
            if (app.getSlavesConfigPath() != null) {
                ini.append(prefix).append(".slavesConfigPath = \"").append(app.getSlavesConfigPath()).append("\"\n");
            }
            
            ini.append("\n");
        }
    }
    
    private static void generateHilConfig(StringBuilder ini, TopologyDTO topology) {
        for (TopologyDTO.NodeDTO node : topology.getNodes()) {
            if (node.getParams() != null && Boolean.TRUE.equals(node.getParams().getIsHil())) {
                if (node.getParams().getApps() != null) {
                    for (int i = 0; i < node.getParams().getApps().size(); i++) {
                        TopologyDTO.AppDTO app = node.getParams().getApps().get(i);
                        if ("ModbusSlaveHILApp".equals(app.getTypename())) {
                            String prefix = "*." + node.getNodeName() + ".app[" + i + "]";
                            ini.append("#HIL configuration for ").append(node.getNodeName()).append("\n");
                            ini.append(prefix).append(".typename = \"ModbusSlaveHILApp\"\n");
                            ini.append(prefix).append(".localAddress = \"")
                               .append(app.getLocalAddress() != null ? app.getLocalAddress() : "").append("\"\n");
                            ini.append(prefix).append(".localPort = ").append(app.getLocalPort()).append("\n");
                            ini.append(prefix).append(".remoteAddress = \"").append(app.getRemoteAddress()).append("\"\n");
                            ini.append(prefix).append(".remotePort = ").append(app.getRemotePort()).append("\n\n");
                        }
                    }
                }
            }
        }
    }
}
