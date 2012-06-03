package apps.pgggppg.optimizations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.Transition;
import apps.team.Pair;

public class ImplicationGraph {
	PropNet propnet;
	
	public ImplicationGraph (PropNet propnet) {
		this.propnet = propnet;
	}
	
	static class Graph {
		Set<Node> nodes = new HashSet<Node>();
		Map<Pair<Component, Boolean>, Node> nodeFromComponent = new HashMap<Pair<Component, Boolean>, Node>();
		Map<Node, Pair<Component, Boolean>> componentFromNode = new HashMap<Node, Pair<Component, Boolean>>();
	}
	
	static class Node {
		boolean isTransition;
		List<Node> outEdges = new ArrayList<Node>();
		List<Node> inEdges = new ArrayList<Node>();
	}
	
	void addComponent(Graph graph, Component c, boolean outputValue) {
		Node n = new Node();
		graph.nodes.add(n);
		Pair<Component, Boolean> associatedValue = new Pair<Component, Boolean>(c, outputValue);
		graph.nodeFromComponent.put(associatedValue, n);
		graph.componentFromNode.put(n, associatedValue);
		
		n.isTransition = c instanceof Transition;
	}
	
	Graph createGraph() {
		Graph graph = new Graph();
		for (Component c : propnet.getComponents()) {
			addComponent(graph, c, true);
			addComponent(graph, c, false);
		}
		return graph;
	}
}
