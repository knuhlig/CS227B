package apps.team.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

public class Digraph<T, E> {

	private Map<T, Properties> nodeProperties = new HashMap<T, Properties>();
	protected Map<T, Map<T, E>> graph = new HashMap<T, Map<T, E>>();
	protected Map<T, Map<T, E>> reverse = new HashMap<T, Map<T, E>>();
	
	public void addEdge(T a, T b) {
		addEdge(a, b, null);
	}
	
	public Set<T> getNodes() {
		return nodeProperties.keySet();
	}
	
	public void addEdge(T a, T b, E edgeValue) {
		addNode(a);
		addNode(b);
		graph.get(a).put(b, edgeValue);
		reverse.get(b).put(a, edgeValue);
	}
	
	public boolean containsNode(T node) {
		return nodeProperties.containsKey(node);
	}
	
	public void addNode(T nodeValue) {
		if (!graph.containsKey(nodeValue)) {
			graph.put(nodeValue, new HashMap<T, E>());
			reverse.put(nodeValue, new HashMap<T, E>());
			nodeProperties.put(nodeValue, new Properties());
		}
	}
	
	public E getEdgeValue(T a, T b) {
		return graph.get(a).get(b);
	}
	
	public void setProperty(T node, Object key, Object value) {
		nodeProperties.get(node).put(key, value);
	}
	
	public Object getProperty(T node, Object key) {
		return nodeProperties.get(node).get(key);
	}
	
	public boolean hasProperty(T node, Object key) {
		return nodeProperties.get(node).containsKey(key);
	}
	
	public void removeProperty(T node, Object key) {
		nodeProperties.get(node).remove(key);
	}
	
	public Set<T> getEdges(T node) {
		return graph.get(node).keySet();
	}
	
	public Set<T> getReverseEdges(T node) {
		return reverse.get(node).keySet();
	}
	
	public Set<T> getSinks(boolean allowSelfLoops) {
		return getSinks(graph, allowSelfLoops);
	}
	
	private Set<T> getSinks(Map<T, Map<T, E>> edges, boolean allowSelfLoops) {
		Set<T> sinks = new HashSet<T>();
		for (T node: edges.keySet()) {
			boolean empty = true;
			for (T node2: edges.get(node).keySet()) {
				if (!(node.equals(node2) && allowSelfLoops)) {
					empty = false;
					break;
				}
			}
			if (empty) {
				sinks.add(node);
			}
		}
		return sinks;
	}
	
	private void sccDFS(T node, Stack<T> stack, Set<T> visited) {
		visited.add(node);
		for (T neighbor: graph.get(node).keySet()) {
			if (!visited.contains(neighbor)) {
				sccDFS(neighbor, stack, visited);
			}
		}
		stack.push(node);
	}
	
	private void sccDFS2(T node, Set<T> scc, Map<T, Map<T, E>> gRev, Set<T> visited) {
		visited.add(node);
		scc.add(node);
		for (T neighbor: gRev.get(node).keySet()) {
			if (!visited.contains(neighbor)) {
				sccDFS2(neighbor, scc, gRev, visited);
			}
		}
	}
	
	public Digraph<Integer, Void> computeSCCs(Map<Integer, Set<T>> sccMap) {
		Digraph<Integer, Void> sccs = new Digraph<Integer, Void>();
		
		Stack<T> stack = new Stack<T>();
		Set<T> visited = new HashSet<T>();
		for (T node: nodeProperties.keySet()) {
			if (!visited.contains(node)) {
				sccDFS(node, stack, visited);
			}
		}
		
		Map<T, Map<T, E>> gRev = new HashMap<T, Map<T, E>>(reverse);
		Map<T, Integer> nodeMap = new HashMap<T, Integer>();
		
		visited.clear();
		int idx = 0;
		
		while (!stack.isEmpty()) {
			T node = stack.pop();
			if (!visited.contains(node)) {
				Set<T> scc = new HashSet<T>();
				sccDFS2(node, scc, gRev, visited);
				sccMap.put(idx, scc);
				for (T member: scc) {
					nodeMap.put(member, idx);
				}
				idx++;
			}
		}
		
		for (T node: graph.keySet()) {
			int sccIdx = nodeMap.get(node);
			sccs.addNode(sccIdx);
			for (T neighbor: graph.get(node).keySet()) {
				int neighborIdx = nodeMap.get(neighbor);
				if (sccIdx != neighborIdx) {
					sccs.addEdge(sccIdx, neighborIdx);
				}
			}
		}
		
		return sccs;
	}
	
	public List<T> getTopologicalOrder(boolean allowSelfLoops) {
		List<T> order = new ArrayList<T>();
		
		Stack<T> stack = new Stack<T>();
		Set<T> marked = new HashSet<T>();
		Set<T> done = new HashSet<T>();
		
		// intiialize with sink nodes
		for (T sink: getSinks(allowSelfLoops)) {
			stack.push(sink);
		}
		
		while (!stack.isEmpty()) {
			T cur = stack.peek();
			if (marked.contains(cur)) {
				done.add(cur);
				order.add(cur);
				stack.pop();
				continue;
			}
			
			marked.add(cur);
			// find unvisited parents
			Set<T> toVisit = new HashSet<T>();
			for (T in: reverse.get(cur).keySet()) {
				if (!marked.contains(in)) {
					toVisit.add(in);
				} else if (!done.contains(in) && !allowSelfLoops) {
					throw new RuntimeException("no topo exists: " + stack);
				}
			}
			
			if (toVisit.isEmpty()) {
				// all parents are visited
				order.add(stack.pop());
				done.add(cur);
			} else {
				// visit parents first
				for (T next: toVisit) {
					stack.push(next);
				}
			}
		}
		
		return order;
	}
	
	public String toDot() {
		StringBuilder b = new StringBuilder();
		Map<T, Integer> ids = new HashMap<T, Integer>();
		
		b.append("digraph G {\n");
		b.append("ranksep=\"1.0 equally\";\n");
		b.append("nodesep=\"0.1\";\n");
		for (T node: graph.keySet()) {
			int id = ids.size() + 1;
			ids.put(node, id);
			b.append(id + " [");
			
			String label = node.toString();
			if (hasProperty(node, "label")) {
				label = (String) getProperty(node, "label");
			}
			b.append("label=\"" + label + "\"");
			
			if (hasProperty(node, "color")) {
				b.append(", color=\""+getProperty(node, "color")+"\"");
			}
			b.append(", style=\"filled\"");
			b.append("]\n");
		}
		for (T node: graph.keySet()) {
			Map<T, E> edges = graph.get(node);
			for (T dst: edges.keySet()) {
				b.append(ids.get(node) + " -> " + ids.get(dst));
				if (edges.get(dst) != null) {
					b.append(" [color=\"red\", style=\"bold\"]");
				}
				b.append(";\n");
			}
		}
		b.append("}");
		return b.toString();
	}
}
