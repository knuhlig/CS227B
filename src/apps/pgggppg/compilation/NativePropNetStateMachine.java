package apps.pgggppg.compilation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apps.pgggppg.optimizations.Optimization;
import apps.pgggppg.optimizations.PassthroughNodeRemover;
import apps.team.game.GameLoader;

import util.game.Game;
import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.Proposition;
import util.propnet.factory.OptimizingPropNetFactory;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class NativePropNetStateMachine extends StateMachine {
	
	public static Move getMove(Proposition p) {
		return new Move(p.getName().toSentence().get(1).toSentence());
	}
	
	public static Role getRole(Proposition p) {
		String roleName = p.getName().toSentence().get(0).toString();
		return new Role(GdlPool.getProposition(GdlPool.getConstant(roleName)));
	}
	
	private MachineState initialState;
	private List<Role> roles = new ArrayList<Role>();
	private Map<Role, Integer> roleToIndex = new HashMap<Role, Integer>();
	
	private List<Map<Move, Integer>> doesToIndex = new ArrayList<Map<Move, Integer>>();
	private Map<Integer, Move> indexToLegal = new HashMap<Integer, Move>();
	
	private Map<Integer, Component> translation = new HashMap<Integer, Component>();
	
	public void addRole(Role role) {
		int idx = roles.size();
		roles.add(role);
		roleToIndex.put(role, idx);
		doesToIndex.add(new HashMap<Move, Integer>());
	}
	
	public void addDoes(Role role, Move move, int idx) {
		int roleIdx = roleToIndex.get(role);
		doesToIndex.get(roleIdx).put(move, idx);
	}
	
	public void addLegal(Role role, Move move, int idx) {
		indexToLegal.put(idx, move);
	}
	
	private void reset() {
		initialState = null;
		roles.clear();
		roleToIndex.clear();
		doesToIndex.clear();
		indexToLegal.clear();
		translation.clear();
	}
	
	@Override
	public void initialize(List<Gdl> description) {
		try {
			reset();
			PropNet propNet = OptimizingPropNetFactory.create(description);
			propNet.renderToFile("/Users/knuhlig/Desktop/unopt.dot");
			Optimization.runPasses(propNet);
			
			System.out.println(">> rendering propNet to file");
			propNet.renderToFile("/Users/knuhlig/Desktop/opt.dot");
			
			//BooleanCodeGenerator gen = new BooleanCodeGenerator(propNet);
			JavaCodeGenerator gen = new JavaCodeGenerator(propNet);
			gen.generateCode();
			
			for (Component c: propNet.getComponents()) {
				int idx = gen.getNativeIdx(c);
				if (idx >= 0) {
					translation.put(idx, c);
				}
			}
			
			// add role mappings
			for (Role role: propNet.getRoles()) {
				addRole(role);
			}
			
			// add input mappings
			Map<GdlTerm, Proposition> inputs = propNet.getInputPropositions();
			for (GdlTerm term: inputs.keySet()) {
				Proposition p = inputs.get(term);
				addDoes(getRole(p), getMove(p), gen.getNativeIdx(p));
			}
			
			// add legal mappings
			Map<Role, Set<Proposition>> legalProps = propNet.getLegalPropositions();
			for (Role role: legalProps.keySet()) {
				for (Proposition p: legalProps.get(role)) {
					Move move = getMove(p);
					addLegal(role, move, gen.getNativeIdx(p));
				}
			}
			
			// add initial state
			initialState = gen.getInitialState();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Set<Component> getComponents(MachineState state, boolean bitType) {
		Set<Component> set = new HashSet<Component>();
		Set<Integer> bits = ((NativeMachineState) state).getAllBits(bitType);
		for (int bit: bits) {
			set.add(translation.get(bit));
		}
		return set;
	}
	
	@Override
	public int getGoal(MachineState state, Role role) throws GoalDefinitionException {
		int roleIdx = roleToIndex.get(role);
		return ((NativeMachineState) state).getGoal(roleIdx);
	}

	@Override
	public boolean isTerminal(MachineState state) {
		return ((NativeMachineState) state).isTerminal();
	}

	@Override
	public List<Role> getRoles() {
		return roles;
	}

	@Override
	public MachineState getInitialState() {
		return initialState;
	}

	@Override
	public List<Move> getLegalMoves(MachineState state, Role role) throws MoveDefinitionException {
		int roleIdx = roleToIndex.get(role);
		List<Integer> moveList = ((NativeMachineState) state).getLegalMoves(roleIdx);
		List<Move> legalMoves = new ArrayList<Move>();
		for (int moveIdx: moveList) {
			legalMoves.add(indexToLegal.get(moveIdx));
		}
		return legalMoves;
	}

	@Override
	public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException {
		List<Integer> moveList = new ArrayList<Integer>();
		for (int i = 0; i < moves.size(); i++) {
			moveList.add(doesToIndex.get(i).get(moves.get(i)));
		}
		return ((NativeMachineState) state).getNextState(moveList);
	}
	
	
}
