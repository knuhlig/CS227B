package util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.components.Proposition;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public abstract class NativePropNetStateMachine extends StateMachine {
	
	protected List<Role> roles;
	protected Map<String, Integer> roleIndices;
	protected Map<String, Integer> moveMap;
	protected Map<Integer, Move> moveRMap;
	
	protected boolean[] components;
	protected int terminalIdx;
	protected int initIdx;
	
	public NativePropNetStateMachine() {
		roles = new ArrayList<Role>();
		roleIndices = new HashMap<String, Integer>();
		moveMap = new HashMap<String, Integer>();
		moveRMap = new HashMap<Integer, Move>();
	}
	
	public void clear() {
		Arrays.fill(components, false);
	}

	public abstract void mark();
	public abstract int getGoal(int roleIdx);
	public abstract NativeMachineState getStateFromBase();
	public abstract Set<Integer> getLegalMoves(int roleIdx);
	public abstract void updateBase();
	
	public void setState(MachineState state) {
		setState(state, null);
	}
	
	public void setState(MachineState state, List<Move> moves) {
		clear();
		NativeMachineState ns = (NativeMachineState) state;
		for (int idx: ns.getState()) {	
			components[idx] = true;
		}
		if (moves != null) {
			
			for (int i = 0; i < moves.size(); i++) {
				Role role = roles.get(i);
				Move move = moves.get(i);
				String key = "( does " + role + " " + move.toString() + " )";
				int idx = moveMap.get(key);
				components[idx] = true;
			}
		}
		mark();
	}
	
	public void initialize(List<Gdl> description) {}
	
	public boolean isTerminal(MachineState state) {
		setState(state);
		return components[terminalIdx];
	}
	
	public void addRole(String name) {
		roles.add(new Role(GdlPool.getProposition(GdlPool.getConstant(name))));
	}
	
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	public int getGoal(MachineState state, Role role) {
		setState(state);
		return getGoal(roleIndices.get(role.toString()));
	}
	
	public NativeMachineState getInitialState() {
		clear();
		components[0] = true;
		mark();
		updateBase();
		NativeMachineState ns = getStateFromBase();
		return ns;
	}
	
	public void addMapping(int index, Move move) {
		moveRMap.put(index, move);
	}
	
	public List<Move> getLegalMoves(MachineState state, Role role) {
		setState(state);
		int roleIdx = roleIndices.get(role.toString());
		List<Move> moves = new ArrayList<Move>();
		for (int idx: getLegalMoves(roleIdx)) {
			moves.add(moveRMap.get(idx));
		}
		return moves;
	}
	
	public MachineState getNextState(MachineState state, List<Move> moves) throws TransitionDefinitionException {
		setState(state, moves);
		updateBase();
		NativeMachineState ns = getStateFromBase();
		return ns;
	}
}
