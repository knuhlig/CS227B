package apps.team.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apps.pgggppg.util.FileUtil;
import apps.team.graph.DependencyGraph;

public class GameDescription implements GameConstants {

	protected String name;
	protected List<String> roles = new ArrayList<String>();
	protected Map<String, List<GameObject>> facts = new HashMap<String, List<GameObject>>();
	protected Map<String, List<GameObject>> init = new HashMap<String, List<GameObject>>();
	
	// maps: role -> condition -> value
	protected Map<String, Map<GameCondition, GameObject>> goals = new HashMap<String, Map<GameCondition, GameObject>>();
	protected Map<String, Map<GameCondition, GameObject>> legals = new HashMap<String, Map<GameCondition,GameObject>>();
		
	// conditions that signal end of game
	protected List<GameCondition> terminals = new ArrayList<GameCondition>();
	
	// state transitions, indexed by type name
	protected Map<String, Map<GameCondition, GameObject>> transitions = new HashMap<String, Map<GameCondition, GameObject>>();
	protected Map<String, Map<GameCondition, GameObject>> internals = new HashMap<String, Map<GameCondition,GameObject>>();

	
	public String getName() {
		return name;
	}
	
	public void analyze() {
		DependencyGraph g = computeDependencyGraph();
		List<String> order = g.getTopologicalOrder(true);
		System.out.println("order: " + order);
		Map<String, String> colors = new HashMap<String, String>();
		colors.put(T_TRUE, "green");
		colors.put(T_NEXT, "orange");
		colors.put(T_LEGAL, "pink");
		colors.put(T_FACT, "purple");
		colors.put(T_DOES, "red");
		colors.put(T_INTERNAL, "lightblue");
		
		for (String node: order) {
			String label = node;
			String type = node;
			if (node.contains(":")) {
				label = node.substring(node.indexOf(":") + 1);
				type = node.substring(0, node.indexOf(":"));
			}
			if (type.equals(T_FACT)) {
				for (String dep: g.getReverseEdges(node)) {
					if (dep.equals(node)) continue;
					if (!g.getProperty(dep, "type").equals(T_FACT)) {
						type = T_INTERNAL;
						break;
					}
				}
			}
			
			g.setProperty(node, "label", label);
			g.setProperty(node, "type", type);
			if (colors.containsKey(type)) {
				g.setProperty(node, "color", colors.get(type));
			}
		}
		FileUtil.writeToFile(g.toDot(), DOT_DIR + getName() + ".dot");
	}
	
	public DependencyGraph computeDependencyGraph() {
		DependencyGraph g = new DependencyGraph();
		for (Map<GameCondition, GameObject> map: goals.values()) {
			for (GameCondition cond: map.keySet()) {
				cond.addDependencies(g, T_GOAL);
			}
		}
		for (String roleName: legals.keySet()) {
			Map<GameCondition, GameObject> map = legals.get(roleName);
			for (GameCondition cond: map.keySet()) {
				GameObject obj = map.get(cond);
				cond.addDependencies(g, T_LEGAL + ":" + obj.getType());
			}
		}
		
		for (GameCondition cond: terminals) {
			cond.addDependencies(g, T_TERMINAL);
		}
		for (String typeName: transitions.keySet()) {
			Map<GameCondition, GameObject> map = transitions.get(typeName);
			for (GameCondition cond: map.keySet()) {
				cond.addDependencies(g, T_NEXT+":"+typeName);
			}
		}
		for (Map<GameCondition, GameObject> map: internals.values()) {
			for (GameCondition cond: map.keySet()) {
				cond.addDependencies(g, T_FACT+":"+map.get(cond).getType());
			}
		}
		return g;
	}
	
	public void printInfo() {
		System.out.println(" -- roles --");
		for (String role: roles) {
			System.out.print(role + " ");
		}
		System.out.println();
		
		System.out.println("\n -- transitions --");
		for (String type: transitions.keySet()) {
			System.out.println(" " + type);
			Map<GameCondition, GameObject> map = transitions.get(type);
			for (GameCondition cond: map.keySet()) {
				System.out.println("    " + cond + " -> " + map.get(cond));
			}
		}

		System.out.println("\n -- internals --");
		for (String type: internals.keySet()) {
			System.out.println(" " + type);
			Map<GameCondition, GameObject> map = internals.get(type);
			for (GameCondition cond: map.keySet()) {
				System.out.println("    " + cond + " -> " + map.get(cond));
			}
		}
		
		System.out.println("\n -- legals --");
		for (String type: legals.keySet()) {
			System.out.println(" " + type);
			Map<GameCondition, GameObject> map = legals.get(type);
			for (GameCondition cond: map.keySet()) {
				System.out.println("    " + cond + " -> " + map.get(cond));
			}
		}
		
		System.out.println("\n -- goals --");
		for (String type: goals.keySet()) {
			System.out.println(" " + type);
			Map<GameCondition, GameObject> map = goals.get(type);
			for (GameCondition cond: map.keySet()) {
				System.out.println("    " + cond + " -> " + map.get(cond));
			}
		}
		
		System.out.println("\n -- terminals --");
		for (GameCondition cond: terminals) {
			System.out.println("    " + cond);
		}
		System.out.println("\n -- facts --");
		for (String type: facts.keySet()) {
			System.out.println(" " + type);
			for (GameObject obj: facts.get(type)) {
				System.out.println("    " + obj);
			}
		}
		
		System.out.println("\n -- init --");
		for (String type: init.keySet()) {
			System.out.println(" " + type);
			for (GameObject obj: init.get(type)) {
				System.out.println("    " + obj);
			}
		}
	}
}
