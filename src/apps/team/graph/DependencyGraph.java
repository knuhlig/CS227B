package apps.team.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import java.util.Map;

import apps.team.game.GameConstants;

public class DependencyGraph extends Digraph<String, Boolean> implements GameConstants {
	
	
	
	
	
	/*
	private Map<NodeType, GraphNode<NodeType, Boolean>> nodes = new HashMap<NodeType, GraphNode<NodeType, Boolean>>();
	
	public void addNode(String name, String type) {
		NodeType t = new NodeType(name, type);
		if (!nodes.containsKey(t)) {
			nodes.put(t, new GraphNode<NodeType, Boolean>(t));
		}
	}
	
	public void addDependency(String name, String type, String depName, String depType, boolean isNegated) {
		addNode(name, type);
		addNode(depName, depType);
		if (depName.equals(T_LEGAL) && !depType.equals(T_LEGAL)) {
			throw new RuntimeException("??");
		}
		NodeType node = new NodeType(name, type);
		NodeType dep = new NodeType(depName, depType);
		nodes.get(node).addEdge(nodes.get(dep), isNegated);
	}
	
	public void mark() {
		List<NodeType> topo = getOrdering();
		for (NodeType type: topo) {
			GraphNode<NodeType, Boolean> node = nodes.get(type);
			NodeType nt = node.getValue();
			if (nt.type.equals(T_FACT)) {
				for (GraphNode<NodeType, Boolean> dep: node.getEdges()) {
					if (dep.getValue().equals(type)) continue;
					if (!dep.getValue().value.equals("purple")) {
						nt.value = "lightblue";
						break;
					}
				}
				if (nt.value == null) {
					nt.value = "purple";
				}
			} else if (nt.type.equals(T_NEXT)) {
				nt.value = "orange";
			} else if (nt.type.equals(T_TRUE)) {
				nt.value = "green";
			} else if (nt.type.equals(T_DOES)) {
				nt.value = "red";
			} else if (nt.type.equals(T_ROLE)) {
				nt.value = "purple";
			} else if (nt.type.equals(T_LEGAL)) {
				nt.value = "pink";
			} else { 
				nt.value = "gray";
			}
		}
	}
	
	private List<NodeType> getOrdering() {
		List<NodeType> ordering = new ArrayList<NodeType>();
		Set<NodeType> marked = new HashSet<NodeType>();
		Set<NodeType> remaining = new HashSet<NodeType>(nodes.keySet());
		
		while (marked.size() < nodes.size()) {
			boolean changed = false;
			Iterator<NodeType> it = remaining.iterator();
			while (it.hasNext()) {
				NodeType type = it.next();
				GraphNode<NodeType, Boolean> node = nodes.get(type);
				boolean ready = true;
				
				for (GraphNode<NodeType, Boolean> dep: node.getEdges()) {
					if (dep.getValue().equals(type)) continue;
					if (!marked.contains(dep.getValue())) {
						ready = false;
						break;
					}
				}
				
				if (ready) {
					it.remove();
					marked.add(type);
					ordering.add(type);
					changed = true;
				}
			}
			
			if (!changed) {
				throw new RuntimeException("stuck!!...");
			}
		}
		return ordering;
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("digraph G {\n");
		b.append("ranksep=\"1.0 equally\";\n");
		b.append("nodesep=\"0.1\";\n");
		for (GraphNode<NodeType, Boolean> node: nodes.values()) {
			b.append(node.getId() + "[");
			b.append("label=\"" + node.getValue().name + "\"");
			if (node.getValue().value != null) {
				b.append(", color=\""+node.getValue().value+"\"");
			}
			b.append(", style=\"filled\"");
			b.append("]\n");
		}
		for (GraphNode<NodeType, Boolean> node: nodes.values()) {
			for (GraphNode<NodeType, Boolean> out: node.getEdges()) {
				b.append(out.getId() + " -> " + node.getId());
				if (node.getEdge(out)) {
					b.append(" [color=\"red\", style=\"bold\"]");
				}
				b.append(";\n");
			}
		}
		b.append("}");
		return b.toString();
	}
	
	private static class NodeType {
		public final String name;
		public final String type;
		public Object value;
		
		public NodeType(String name, String type) {
			this.name = name;
			this.type = type;
		}
		
		@Override
		public int hashCode() {
			return toString().hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof NodeType)) return false;
			return toString().equals(((NodeType) obj).toString());
		}
		
		@Override
		public String toString() {
			return name + "|" + type;
		}
	}
	*/
}
