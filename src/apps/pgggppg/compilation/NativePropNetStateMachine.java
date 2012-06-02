package apps.pgggppg.compilation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apps.team.game.GameLoader;

import util.game.Game;
import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
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
	
	@Override
	public void initialize(List<Gdl> description) {
		try {
			PropNet propNet = OptimizingPropNetFactory.create(description);
			//propNet.renderToFile("/Users/knuhlig/Desktop/game.dot");
			
			JavaCodeGenerator gen = new JavaCodeGenerator(propNet);
			gen.generateCode();
			
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
