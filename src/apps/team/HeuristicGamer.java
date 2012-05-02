package apps.team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public abstract class HeuristicGamer extends StateMachineGamer {
	
	private static final int GOAL_MIN = 0;
	private static final int GOAL_MAX = 100;
	
	private long timeout;
	private int count;
	private long timeoutBuffer = 2000;
	private int timeoutCheckInterval = 200;
	
	private Map<MachineState, Integer> terminalCache;
	private Map<MachineState, Integer> stateCache;
	
	public HeuristicGamer() {
		terminalCache = new HashMap<MachineState, Integer>();
		stateCache = new HashMap<MachineState, Integer>();
	}
	
	public abstract int getHeuristicValue(MachineState state) throws Exception;

	@Override
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}
	
	public int getCachedValue(MachineState state) {
		boolean isTerminal = getStateMachine().isTerminal(state);
		return getCachedValue(state, isTerminal);
	}
	
	public int getCachedValue(MachineState state, boolean isTerminal) {
		if (isTerminal) {
			return terminalCache.get(state);
		}
		return stateCache.get(state);
	}
	
	private int dlsMin(List<MachineState> states, int depth) throws Exception {
		int min = GOAL_MAX;
		for (MachineState state: states) {
			min = Math.min(min, dls(state, depth));
		}
		return min;
	}
	
	private int dls(MachineState state, int depth) throws Exception {
		// check periodically for a timeout
		if (++count % timeoutCheckInterval == 0) {
			if (System.currentTimeMillis() + timeoutBuffer >= timeout) {
				throw new RuntimeException("search timeout");
			}
		}
		
		StateMachine sm = getStateMachine();
		
		// terminal state, cached
		if (terminalCache.containsKey(state)) {
			return terminalCache.get(state);
		}
		
		// terminal state, uncached
		if (sm.isTerminal(state)) {
			int value = sm.getGoal(state, getRole());
			terminalCache.put(state, value);
			return value;
		}
		
		// depth limit reached
		if (depth == 0) {
			// return heuristic value, don't cache?
			int value = getHeuristicValue(state);
			return value;
		}
	
		// recurse: minimax
		Map<Move, List<MachineState>> legalMoves = sm.getNextStates(getCurrentState(), getRole());
		int max = GOAL_MIN;
		for (Move move: legalMoves.keySet()) {
			int cur = dlsMin(legalMoves.get(move), depth - 1);
			max = Math.max(max, cur);
		}
		
		// cache?
		stateCache.put(state, max);
		return max;
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	}
	
	public Move findMove(MachineState state, int value) throws Exception {
		Map<Move, List<MachineState>> nextStates = getStateMachine().getNextStates(state, getRole());
		for (Move move: nextStates.keySet()) {
			int min = GOAL_MAX;
			for (MachineState next: nextStates.get(move)) {
				min = Math.min(min, getCachedValue(state));
			}
			if (min == value) {
				return move;
			}
		}
		return null;
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		stateCache.clear();
		count = 0;
		Move optMove = null;
		int optVal = GOAL_MIN - 1;
		try {
			this.timeout = timeout;
			for (int depth = 1; ; depth++) {
				System.out.println(">> exploring depth " + depth);
				int val = dls(getCurrentState(), depth);
				if (val > optVal) {
					optVal = val;
					optMove = findMove(getCurrentState(), val);
				}
			}
		} catch (Exception e) {
			System.out.println(">> " + e.getMessage());
		}
		
		if (optMove != null) {
			return optMove;
		}
		
		// didn't evaluate any moves, shouldn't happen in practice
		System.out.println(">> returning random move");
		return getStateMachine().getRandomMove(getCurrentState(), getRole());
	}

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub
		
	}

}
