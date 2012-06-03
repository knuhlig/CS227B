package apps.pgggppg.optimizations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Not;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;
import apps.pgggppg.util.FileUtil;
import apps.team.Pair;
import apps.team.graph.Digraph;

public class ImplicationGraph {
	
	private PropNet propnet;
	private Digraph<Node, Void> graph;
	
	public ImplicationGraph (PropNet propnet) {
		this.propnet = propnet;
		graph = new Digraph<Node, Void>();
		
		for (Component c : propnet.getComponents()) {
			if (c instanceof Or) {
				for (Component in: c.getInputs()) {
					graph.addEdge(new Node(in, true), new Node(c, true));
				}
				if (c.getInputs().size() == 1) {
					graph.addEdge(new Node(c.getSingleInput(), false), new Node(c, false));
				}
			}
			if (c instanceof And) {
				for (Component in: c.getInputs()) {
					graph.addEdge(new Node(in, false), new Node(c, false));
				}
				if (c.getInputs().size() == 1) {
					graph.addEdge(new Node(c.getSingleInput(), true), new Node(c, true));
				}
			}
			if (c instanceof Not) {
				graph.addEdge(new Node(c.getSingleInput(), true), new Node(c, false));
				graph.addEdge(new Node(c.getSingleInput(), false), new Node(c, true));
			}
			if (c instanceof Proposition || c instanceof Transition) {
				if (c.getInputs().size() == 1) {
					graph.addEdge(new Node(c.getSingleInput(), true), new Node(c, true));
					graph.addEdge(new Node(c.getSingleInput(), false), new Node(c, false));
				}
			}
		}
		
		Map<Integer, Set<Node>> sccMap = new HashMap<Integer, Set<Node>>();
		Digraph<Integer, Void> sccs = graph.computeSCCs(sccMap);
		
		FileUtil.writeToFile(graph.toDot(), "/Users/knuhlig/Desktop/implication.dot");
	}
	
	private static class Node {
		Component c;
		boolean b;
		
		public Node(Component c, Boolean b) {
			this.c = c;
			this.b = b;
		}
		
		@Override
		public int hashCode() {
			return c.hashCode() + (b ? 1 : 0);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof Node)) return false;
			Node node = (Node) obj;
			return b == node.b && c.equals(node.c);
		}
		
		@Override
		public String toString() {
			if (c instanceof Proposition) {
				return ((Proposition) c).getName() + "_" + b;
			}
			return c.getClass().getSimpleName() + "_" + b;
		}
	}
}
