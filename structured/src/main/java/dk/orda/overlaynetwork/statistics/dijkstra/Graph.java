package dk.orda.overlaynetwork.statistics.dijkstra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Graph {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Set<Node> nodes = new HashSet<>();

    public void addNode(Node nodeA) {
        nodes.add(nodeA);
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public void setNodes(Set<Node> nodes) {
        this.nodes = nodes;
    }

    public Graph clone() {
        Set<Node> clone = new HashSet<>(nodes.size());
        Map<String, Node> cloneNodes = new HashMap<>(nodes.size());
        for (Node node : nodes) {
            Node n = new Node(node.getName());
            clone.add(n);
            cloneNodes.put(node.getName(), n);
        }
        for (Node node : nodes) {
            Node node1 = cloneNodes.get(node.getName());
            for (Map.Entry<Node, Integer> entry : node.getAdjacentNodes().entrySet()) {
                node1.addDestination(cloneNodes.get(entry.getKey().getName()), entry.getValue());
            }
        }
        Graph g = new Graph();
        g.setNodes(clone);
        return g;
    }

    public Graph cloneWithHelperNodeMap(HashMap<String, Node> emptyMap) {
        if (emptyMap == null || emptyMap.size() != 0)
            throw new IllegalArgumentException("Hashmap must be initialized empty map!");

        Set<Node> clone = new HashSet<>(nodes.size());
        for (Node node : nodes) {
            Node n = new Node(node.getName());
            clone.add(n);
            emptyMap.put(node.getName(), n);
        }
        for (Node node : nodes) {
            Node node1 = emptyMap.get(node.getName());
            for (Map.Entry<Node, Integer> entry : node.getAdjacentNodes().entrySet()) {
                node1.addDestination(emptyMap.get(entry.getKey().getName()), entry.getValue());
            }
        }
        Graph g = new Graph();
        g.setNodes(clone);
        return g;
    }
}
