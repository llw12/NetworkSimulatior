package com.simulation.network.util;

import com.simulation.network.dto.TopologyDTO;

import java.util.*;
import java.util.stream.Collectors;

public class NedGenerator {
    
    public static String generate(TopologyDTO topology, String networkName) {
        StringBuilder ned = new StringBuilder();
        
        // Imports
        ned.append("import inet.networks.base.TsnNetworkBase;\n");
        ned.append("import inet.node.ethernet.EthernetLink;\n");
        ned.append("import inet.node.tsn.TsnDevice;\n");
        ned.append("import inet.node.tsn.TsnSwitch;\n\n");
        
        // Network definition
        ned.append("network ").append(networkName).append(" extends TsnNetworkBase\n{\n");
        ned.append("    parameters:\n");
        
        // Count array nodes (clients)
        Map<String, List<TopologyDTO.NodeDTO>> nodesByType = new HashMap<>();
        for (TopologyDTO.NodeDTO node : topology.getNodes()) {
            nodesByType.computeIfAbsent(getNodeCategory(node), k -> new ArrayList<>()).add(node);
        }
        
        if (nodesByType.containsKey("client")) {
            ned.append("        int numClients = ").append(nodesByType.get("client").size()).append(";\n");
        }
        
        // Submodules
        ned.append("    submodules:\n");
        
        for (TopologyDTO.NodeDTO node : topology.getNodes()) {
            String category = getNodeCategory(node);
            if ("client".equals(category)) {
                continue; // Handle clients as array
            }
            
            ned.append("        ").append(node.getNodeName()).append(": ");
            ned.append(node.getNodeType()).append(" {\n");
            
            if (node.getPosition() != null) {
                ned.append("            @display(\"p=")
                   .append(node.getPosition().getX()).append(",")
                   .append(node.getPosition().getY()).append("\");\n");
            }
            
            ned.append("        }\n");
        }
        
        // Client array
        if (nodesByType.containsKey("client")) {
            ned.append("        client[numClients]: TsnDevice {\n");
            ned.append("            @display(\"p=652,329,r,80\");\n");
            ned.append("        }\n");
        }
        
        // Connections
        ned.append("    connections allowunconnected:\n");
        
        if (topology.getLinks() != null) {
            for (TopologyDTO.LinkDTO link : topology.getLinks()) {
                String source = convertNodeIdToName(link.getSourceNodeId(), topology.getNodes());
                String target = convertNodeIdToName(link.getTargetNodeId(), topology.getNodes());
                
                ned.append("        ").append(source).append(".ethg++ <--> EthernetLink <--> ");
                ned.append(target).append(".ethg++;\n");
            }
        }
        
        ned.append("}\n");
        
        return ned.toString();
    }
    
    private static String getNodeCategory(TopologyDTO.NodeDTO node) {
        String name = node.getNodeName().toLowerCase();
        if (name.startsWith("client")) {
            return "client";
        } else if (name.startsWith("server")) {
            return "server";
        } else if (name.startsWith("switch")) {
            return "switch";
        } else if (name.contains("operator") || name.contains("station")) {
            return "operatorStation";
        }
        return node.getNodeName();
    }
    
    private static String convertNodeIdToName(String nodeId, List<TopologyDTO.NodeDTO> nodes) {
        for (TopologyDTO.NodeDTO node : nodes) {
            if (node.getNodeId().equals(nodeId)) {
                // Check if it's a client array member
                if (getNodeCategory(node).equals("client")) {
                    // Extract index from client name (e.g., "client01" -> "client[1]")
                    String name = node.getNodeName();
                    String index = name.replaceAll("[^0-9]", "");
                    if (!index.isEmpty()) {
                        return "client[" + Integer.parseInt(index) + "]";
                    }
                    return "client[0]";
                }
                return node.getNodeName();
            }
        }
        return nodeId;
    }
}
