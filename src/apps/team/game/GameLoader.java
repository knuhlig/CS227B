package apps.team.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apps.team.Pair;
import apps.team.graph.DependencyGraph;
import apps.team.graph.Digraph;
import apps.team.util.FileUtil;


import util.game.Game;
import util.game.GameRepository;
import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlDistinct;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlLiteral;
import util.gdl.grammar.GdlNot;
import util.gdl.grammar.GdlOr;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlRule;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.gdl.grammar.GdlVariable;

public class GameLoader implements GameConstants {


	private GameRepository repository;
	
	private Game gdlGame;
	private GameDescription desc;	
	private DependencyGraph typeGraph;
	private Map<String, ObjectDomain> typeDomains;
	
	public GameLoader() {
		repository = GameRepository.getDefaultRepository(); 
	}

	public List<String> getAvailableGames() {
		return new ArrayList<String>(repository.getGameKeys());
	}
	
	public Game loadGdlGame(String name) {
		return repository.getGame(name);
	}

	public GameDescription loadGame(String name) {
		this.gdlGame = loadGdlGame(name);
		
		/*
		// write gdl to file
		StringBuilder b = new StringBuilder();
		for (Gdl rule: gdlGame.getRules()) {
			b.append(rule.toString() + "\n");
		}
		FileUtil.writeToFile(b.toString(), KIF_DIR + name + ".kif");
		*/
		
		desc = new GameDescription();
		desc.name = name;
		
		typeGraph = new DependencyGraph();
		typeDomains = new HashMap<String, ObjectDomain>();
		computeTypeGraph();
		computeTypeDomains();
		
		for (String typeName: typeDomains.keySet()) {
			System.out.println(typeName + ": " + typeDomains.get(typeName));
		}
		
		loadRules();
		return desc;
	}
	
	
	private void computeTypeGraph() {
		for (Gdl gdl: gdlGame.getRules()) {
			if (gdl instanceof GdlRelation) {
				GdlRelation rel = (GdlRelation) gdl;
				addConstants(rel);
			} else if (gdl instanceof GdlRule) {
				GdlRule rule = (GdlRule) gdl;
				
				addConstants(rule.getHead());
				Map<GdlVariable, List<Pair<String, Integer>>> headVars = new HashMap<GdlVariable, List<Pair<String,Integer>>>();
				getVariables(rule.getHead(), headVars);
				
				Map<GdlVariable, List<Pair<String, Integer>>> bodyVars = new HashMap<GdlVariable, List<Pair<String,Integer>>>();
				getVariables(rule.getBody(), bodyVars);
				
				for (GdlVariable headVar: headVars.keySet()) {
					for (Pair<String, Integer> headType: headVars.get(headVar)) {
						for (Pair<String,Integer> bodyType: bodyVars.get(headVar)) {
							typeGraph.addEdge(bodyType.fst + ":" + bodyType.snd, headType.fst + ":" + headType.snd, false);
						}
					}
				}
			} else {
				throw new RuntimeException("unexpected top-level gdl: " + gdl.getClass().getName());
			}
		}
	}
	
	private void computeTypeDomains() {
		Map<Integer, Set<String>> domains = new HashMap<Integer, Set<String>>();
		Map<Integer, Set<String>> sccMap = new HashMap<Integer, Set<String>>();
		Digraph<Integer, Void> sccs = typeGraph.computeSCCs(sccMap);
		
		// write to file
		for (Integer node: sccMap.keySet()) {
			String label = "";
			for (String name: sccMap.get(node)) {
				if (!label.isEmpty()) {
					label += ", ";
				}
				label += name;
			}
			sccs.setProperty(node, "label", label);
		}
		FileUtil.writeToFile(sccs.toDot(), DOT_DIR + "types/" + desc.name + ".dot");
		
		for (int idx: sccs.getTopologicalOrder(false)) {
			Set<String> scc = sccMap.get(idx);
			
			Set<String> cur = new HashSet<String>();
			for (int parent: sccs.getReverseEdges(idx)) {
				cur.addAll(domains.get(parent));
			}
			
			for (String type: scc) {
				String[] parts = type.split(":");
				if (parts.length < 3) {
					cur.add(type);
				} else {
					String typeName = parts[0] + ":" + parts[1];
					int pos = Integer.parseInt(parts[2]);
					if (!typeDomains.containsKey(typeName)) {
						continue;
					}
					cur.addAll(typeDomains.get(typeName).getDomain(pos));
				}
			}

			for (String type: scc) {
				String[] parts = type.split(":");
				if (parts.length < 3) continue;
				String typeName = parts[0] + ":" + parts[1];
				int pos = Integer.parseInt(parts[2]);
				if (!typeDomains.containsKey(typeName)) {
					typeDomains.put(typeName, new ObjectDomain());
				}
				typeDomains.get(typeName).addAll(pos, cur);
			}
			
			domains.put(idx, cur);
		}
	}
	
	private void addToDomain(String value, String type, int idx) {
		if (!typeDomains.containsKey(type)) {
			typeDomains.put(type, new ObjectDomain());
		}
		typeDomains.get(type).add(idx, value);
	}
	
	private void addTerm(GdlTerm term, String typeName, int idx) {
		if (term instanceof GdlConstant) {
			addToDomain(term.toString(), typeName, idx);
		} else if (term instanceof GdlFunction) {
			GdlFunction fn = (GdlFunction) term;
			String fnType = "obj:" + fn.getName().getValue();
			typeGraph.addEdge(fnType, typeName + ":" + idx, false);
			List<GdlTerm> children = fn.getBody();
			for (int i = 0; i < children.size(); i++) {
				addTerm(children.get(i), fnType, i);
			}
		}
	}
	
	private void addConstants(GdlLiteral literal) {
		if (literal instanceof GdlRelation) {
			GdlRelation rel = (GdlRelation) literal;
			String typeName = "rel:" + rel.getName().getValue();
			for (int i = 0; i < rel.arity(); i++) {
				GdlTerm term = rel.get(i);
				addTerm(term, typeName, i);
			}
		}
	}
	
	private void getVariables(List<GdlLiteral> body, Map<GdlVariable, List<Pair<String, Integer>>> varMap) {
		for (GdlLiteral literal: body) {
			if (literal instanceof GdlSentence) {
				getVariables((GdlSentence) literal, varMap);
			} else if (literal instanceof GdlOr) {
				GdlOr or = (GdlOr) literal;
				getVariables(or.getDisjuncts(), varMap);
			} else if (literal instanceof GdlNot) {
				List<GdlLiteral> children = new ArrayList<GdlLiteral>();
				children.add(((GdlNot) literal).getBody());
				getVariables(children, varMap);
			}
		}
	}
	
	private void getVariables(GdlSentence literal, Map<GdlVariable, List<Pair<String, Integer>>> varMap) {
		String typeName = "rel:" + literal.getName().getValue();
		for (int i = 0; i < literal.arity(); i++) {
			getVariables(literal.get(i), typeName, i, varMap);
		}
	}
	
	private void getVariables(GdlTerm term, String parentType, int idx, Map<GdlVariable, List<Pair<String, Integer>>> varMap) {
		if (term instanceof GdlVariable) {
			GdlVariable var = (GdlVariable) term;
			if (!varMap.containsKey(var)) {
				varMap.put(var, new ArrayList<Pair<String,Integer>>());
			}
			varMap.get(var).add(new Pair<String, Integer>(parentType, idx));
		} else if (term instanceof GdlFunction) {
			GdlFunction fn = (GdlFunction) term;
			String typeName = "obj:" + fn.getName().getValue();
			for (int i = 0; i < fn.arity(); i++) {
				GdlTerm child = fn.get(i);
				getVariables(child, typeName, i, varMap);
			}
		}
	}

	private void loadRules() {
		for (Gdl gdl: gdlGame.getRules()) {
			if (gdl instanceof GdlRelation) {
				loadFact((GdlRelation) gdl);
			}
		}
		for (Gdl gdl: gdlGame.getRules()) {
			if (gdl instanceof GdlRule) {
				loadRule((GdlRule) gdl);
			}
		}
	}
	
	private void loadInit(GdlTerm term) {
		String typeName = null;
		if (term instanceof GdlFunction) {
			GdlFunction fn = (GdlFunction) term;
			typeName = fn.getName().getValue();
		} else if (term instanceof GdlConstant) {
			typeName = term.toString();
		} else {
			throw new RuntimeException("unexpected init statement: " + term);
		}
		if (!desc.init.containsKey(typeName)) {
			desc.init.put(typeName, new ArrayList<GameObject>());
		}
		desc.init.get(typeName).add(generateObject(term));
	}
	
	private void loadFact(GdlRelation fact) {
		String typeName = fact.getName().getValue();
		if (typeName.equals(T_INIT)) {
			loadInit(fact.get(0));
		} else if (typeName.equals(T_ROLE)) {
			desc.roles.add(fact.get(0).toString());
		} else {
			if (!desc.facts.containsKey(typeName)) {
				desc.facts.put(typeName, new ArrayList<GameObject>());
			}
			desc.facts.get(typeName).add(generateObject(fact));
		}
	}
	
	private void loadRule(GdlRule rule) {
		GdlSentence head = rule.getHead();
		String headName = head.getName().getValue();
		
		if (headName.equals(T_NEXT)) {
			loadTransition(rule);
		} else if (headName.equals(T_GOAL)) {
			loadGoal(rule);
		} else if (headName.equals(T_TERMINAL)) {
			loadTerminal(rule);
		} else if (headName.equals(T_LEGAL)) {
			loadLegal(rule);
		} else {
			loadInternal(rule);
		}
	}
	
	private void loadInternal(GdlRule rule) {
		GdlSentence head = rule.getHead();
		String typeName = head.getName().getValue();
		if (typeName == null) {
			throw new RuntimeException("ewps: " + rule);
		}
		if (!desc.internals.containsKey(typeName)) {
			desc.internals.put(typeName, new HashMap<GameCondition, GameObject>());
		}
		List<GameCondition> conds = generateConditions(rule);
		GameObject pattern = generateObject(head);
		for (GameCondition cond: conds) {
			desc.internals.get(typeName).put(cond, pattern);	
		}
	}
	
	private void loadTransition(GdlRule rule) {
		GdlTerm nextState = rule.getHead().get(0);
		String typeName = null;
		if (nextState instanceof GdlFunction) {
			GdlFunction fn = (GdlFunction) nextState;
			typeName = fn.getName().getValue();
		} else if (nextState instanceof GdlConstant) {
			typeName = nextState.toString();
		} else if (nextState instanceof GdlVariable) {
			throw new RuntimeException("variable transitions not yet implemented");
		} else {
			throw new RuntimeException("transition head has type: " + nextState.getClass().getName() + ": " + rule);
		}
		
		if (!desc.transitions.containsKey(typeName)) {
			desc.transitions.put(typeName, new HashMap<GameCondition, GameObject>());
		}
		List<GameCondition> conds = generateConditions(rule);
		GameObject pattern = generateObject(nextState);
		for (GameCondition cond: conds) {
			desc.transitions.get(typeName).put(cond, pattern);			
		}
	}
	
	private void addLegalMove(String roleName, GameCondition cond, GameObject pattern) {
		if (!desc.legals.containsKey(roleName)) {
			desc.legals.put(roleName, new HashMap<GameCondition, GameObject>());
		}
		desc.legals.get(roleName).put(cond, pattern);
	}
	
	private void loadLegal(GdlRule rule) {
		GdlSentence head = rule.getHead();
		GdlTerm role = head.get(0);
		GdlTerm move = head.get(1);
		List<GameCondition> conds = generateConditions(rule);
		GameObject pattern = generateObject(move);
		
		for (GameCondition cond: conds) {
			if (role instanceof GdlVariable) {
				// ground out moves
				for (String roleName: desc.roles) {
					GameCondition roleCond = cond.substitute(role.toString(), new GameObject(roleName));
					GameObject rolePattern = pattern.substitute(role.toString(), new GameObject(roleName));
					addLegalMove(roleName, roleCond, rolePattern);
				}
			} else {
				addLegalMove(role.toString(), cond, pattern);
			}
		}
	}
	
	
	private void loadTerminal(GdlRule rule) {
		List<GameCondition> conds = generateConditions(rule);
		desc.terminals.addAll(conds);
	}
	
	private void addGoal(String roleName, GameCondition cond, GameObject value) {
		if (!desc.goals.containsKey(roleName)) {
			desc.goals.put(roleName, new HashMap<GameCondition, GameObject>());
		}
		desc.goals.get(roleName).put(cond, value);
	}
	
	private void loadGoal(GdlRule goal) {
		GdlTerm role = goal.getHead().get(0);
		GameObject value = generateObject(goal.getHead().get(1));
		List<GameCondition> conds = generateConditions(goal);
		
		for (GameCondition cond: conds) {
			if (role instanceof GdlVariable) {
				// ground out roles
				for (String roleName: desc.roles) {
					GameCondition roleCond = cond.substitute(role.toString(), new GameObject(roleName));
					addGoal(roleName, roleCond, value);
				}
			} else {
				addGoal(role.toString(), cond, value);
			}
		}
	}
	
	private void addCondition(GameCondition cond, GdlSentence s, boolean isNegated) {
		String typeName = s.getName().getValue();
		if (typeName.equals(T_TRUE)) {
			cond.addCondition(generateObject(s.get(0)), COND_TRUE, isNegated);
		} else if (typeName.equals(T_DOES)) {
			cond.addCondition(generateObject(s), COND_DOES, isNegated);
		} else if (typeName.equals(T_ROLE)) {
			cond.addCondition(generateObject(s.get(0)), COND_ROLE, isNegated);
		} else if (typeName.equals(T_LEGAL)) {
			cond.addCondition(generateObject(s.get(1)), COND_LEGAL, isNegated);
		} else if (typeName.equals(T_GOAL)) {
			cond.addCondition(generateObject(s), COND_GOAL, isNegated);
		} else {
			cond.addCondition(generateObject(s), COND_INTERNAL, isNegated);
		}
	}
	
	private void addDistinct(GameCondition cond, GdlDistinct d, boolean isNegated) {
		cond.addDistinct(generateObject(d.getArg1()), generateObject(d.getArg2()), isNegated);
	}
	
	private void addLiteral(GdlLiteral literal, GameCondition cond) {
		if (literal instanceof GdlNot) {
			GdlNot not = (GdlNot) literal;
			GdlLiteral body = not.getBody();
			if (body instanceof GdlSentence) {
				addCondition(cond, (GdlSentence) body, true);
			} else if (body instanceof GdlDistinct) {
				addDistinct(cond, (GdlDistinct) body, true);
			} else {
				throw new RuntimeException("unexpected negated literal: " + literal);
			}
		} else if (literal instanceof GdlDistinct) {
			addDistinct(cond, (GdlDistinct) literal, false);
		} else if (literal instanceof GdlSentence) {
			GdlSentence s = (GdlSentence) literal;
			addCondition(cond, s, false);
		} else {
			throw new RuntimeException("bad literal? " + literal.getClass().getName());
		}
	}
	
	private List<GameCondition> generateConditions(GdlRule rule) {
		List<GdlOr> ors = new ArrayList<GdlOr>();
		List<GdlLiteral> literals = new ArrayList<GdlLiteral>();
		
		// distribute out OR literals
		for (GdlLiteral literal: rule.getBody()) {
			if (literal instanceof GdlOr) {
				ors.add((GdlOr) literal);
			} else {
				literals.add(literal);
			}
		}
		List<GameCondition> conds = new ArrayList<GameCondition>();
		
		GameCondition cur = new GameCondition();
		for (GdlLiteral literal: literals) {
			addLiteral(literal, cur);
		}
		conds.add(cur);
		
		for (GdlOr or: ors) {
			addOr(or, conds);
		}
		return conds;
	}
	
	private void addOr(GdlOr or, List<GameCondition> conds) {
		List<GameCondition> tmp = new ArrayList<GameCondition>(conds);
		conds.clear();
		for (int i = 0; i < or.arity(); i++) {
			GdlLiteral literal = or.get(i);
			for (GameCondition cond: tmp) {
				GameCondition next = new GameCondition(cond);
				addLiteral(literal, next);
				conds.add(next);
			}
		}
	}
	
	private GameObject generateObject(GdlSentence term) {
		GameObject obj = new GameObject(term.getName().getValue());
		for (int i = 0; i < term.arity(); i++) {
			obj.addChild(generateObject(term.get(i)));
		}
		return obj;
	}
	
	private GameObject generateObject(GdlRelation term) {
		GameObject obj = new GameObject(term.getName().getValue());
		for (int i = 0; i < term.arity(); i++) {
			obj.addChild(generateObject(term.get(i)));
		}
		return obj;
	}
	
	private GameObject generateObject(GdlFunction term) {
		GameObject obj = new GameObject(term.getName().getValue());
		for (int i = 0; i < term.arity(); i++) {
			obj.addChild(generateObject(term.get(i)));
		}
		return obj;
	}
	
	private GameObject generateObject(GdlTerm term) {
		if (term instanceof GdlFunction) {
			return generateObject((GdlFunction) term);
		}
		
		if (term instanceof GdlConstant) {
			return new GameObject(term.toString());
		}
		
		if (term instanceof GdlVariable) {
			return new GameObject(term.toString());
		}
		
		throw new RuntimeException("unexpected term: " + term.getClass().getName());
	}
}
