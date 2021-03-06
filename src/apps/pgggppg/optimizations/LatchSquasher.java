package apps.pgggppg.optimizations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Constant;
import util.propnet.architecture.components.Not;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;
import apps.team.Pair;
import apps.team.graph.Digraph;

public class LatchSquasher extends Optimization {
	Set<Component> trueComponents;
	Set<Component> falseComponents;
	
	public LatchSquasher (PropNet propnet, Set<Component> trueComponents, Set<Component> falseComponents) {
		super (propnet);
		this.trueComponents = trueComponents;
		this.falseComponents = falseComponents;
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
	
	Set<Set<Node>> getImplicationSCCs() {
		Digraph<Node, Void> graph;
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
		
		Set<Set<Node>> latchSccs = new HashSet<Set<Node>>();
		
		for (Integer i : sccs.getNodes()) {
			Set<Node> nodesInScc = sccMap.get(i);
			int numTransitions = 0;
			for (Node n : nodesInScc) {
				if (n.c instanceof Transition) {
					++numTransitions;
				}
			}
			if (numTransitions == 1) {
				latchSccs.add(nodesInScc);
				System.out.println("Found latch of size " + nodesInScc.size());
			} else if (numTransitions > 1) {
				System.out.println("More than 1 transition in SCC!");
			}
		}
		
		return latchSccs;
	}
	
	void addConstantComponents(Set<Component> componentsWithValue, boolean value, Map<Pair<Component, Boolean>, Set<Node>> nodesImpliedByComponentMarking, Set<Node> outputNodesToConstantize) {
		for (Component c : componentsWithValue) {
			Set<Node> nodesImplicated = nodesImpliedByComponentMarking.get(new Pair<Component, Boolean>(c,value));
			if (nodesImplicated == null) {
				continue;
			}
			for (Node n : nodesImplicated) {
				outputNodesToConstantize.add(n);
				// We now remove all the possible nodes to avoid a potential O(n^2) blowup where each node in the SCC triggers this loop
				nodesImpliedByComponentMarking.remove(new Pair<Component, Boolean>(n.c,n.b));
			}
		}
	}
	
	void constantizeNodes(Set<Node> nodes) {
		// Ensure that we don't add True or False constants if one already exists
		// Add them if they don't exist
		Component trueConstant = null;
		Component falseConstant = null;
		for (Component c : propnet.getComponents()) {
			if (c instanceof Constant) {
				if (c.getValue()) {
					trueConstant = c;
				} else {
					falseConstant = c;
				}
			}
		}
		if (trueConstant == null) {
			trueConstant = new Constant(true);
			propnet.addComponent(trueConstant);
		}
		if (falseConstant == null) {
			falseConstant = new Constant(false);
			propnet.addComponent(falseConstant);
		}
		
		for (Node constantNode : nodes) {
			propnet.removeComponent(constantNode.c);
			Component constant = (constantNode.b ? trueConstant : falseConstant);
			for (Component output : constantNode.c.getOutputs()) {
				constant.addOutput(output);
				output.addInput(constant);
			}
		}
	}
	
	public void runPass() {
		Set<Set<Node>> implicationSCCs = getImplicationSCCs();
		Map<Pair<Component, Boolean>, Set<Node>> nodesImpliedByComponentMarking = new HashMap<Pair<Component, Boolean>, Set<Node>>(); 
		for (Set<Node> s : implicationSCCs) {
			for (Node n : s) {
				nodesImpliedByComponentMarking.put(new Pair<Component, Boolean>(n.c,n.b), s);
			}
		}
		Set<Node> nodesToConstantize = new HashSet<Node>();
		addConstantComponents(trueComponents, true, nodesImpliedByComponentMarking, nodesToConstantize);
		addConstantComponents(falseComponents, false, nodesImpliedByComponentMarking, nodesToConstantize);
		
		constantizeNodes(nodesToConstantize);
	}
}
