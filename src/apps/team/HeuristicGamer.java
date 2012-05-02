package apps.team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.tools.javac.util.Pair;

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
	private boolean depthLimited;
	
	private long timeoutBuffer = 2000;
	private int timeoutCheckInterval = 200;
	
	private Map<MachineState, Integer> terminalCache;
	private Map<MachineState, Map<Move, List<MachineState>>> transitionCache;
	
	public HeuristicGamer() {
		terminalCache = new HashMap<MachineState, Integer>();
		transitionCache = new HashMap<MachineState, Map<Move,List<MachineState>>>();
	}
	
	public abstract int getHeuristicValue(MachineState state) throws Exception;

	@Override
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}
	
	private Map<Move, List<MachineState>> getTransitions(MachineState state) throws Exception {
		if (!transitionCache.containsKey(state)) {
			Map<Move, List<MachineState>> transitions = getStateMachine().getNextStates(state, getRole());
			transitionCache.put(state, transitions);
		}
		return transitionCache.get(state);
	}
	
	private int dlsMin(List<MachineState> states, int depth) throws Exception {
		int min = GOAL_MAX;
		for (MachineState state: states) {
			min = Math.min(min, dls(state, depth).fst);
		}
		return min;
	}
	
	private Pair<Integer, Move> dls(MachineState state, int depth) throws Exception {
		// check periodically for a timeout
		if (++count % timeoutCheckInterval == 0) {
			if (System.currentTimeMillis() + timeoutBuffer >= timeout) {
				throw new RuntimeException("search timeout");
			}
		}
		
		StateMachine sm = getStateMachine();
		
		// terminal state, cached
		if (terminalCache.containsKey(state)) {
			return new Pair<Integer, Move>(terminalCache.get(state), null);
		}
		
		// terminal state, uncached
		if (sm.isTerminal(state)) {
			int value = sm.getGoal(state, getRole());
			terminalCache.put(state, value);
			return new Pair<Integer, Move>(value, null);
		}
		
		// depth limit reached
		if (depth == 0) {
			// return heuristic value, don't cache?
			depthLimited = true;
			int value = getHeuristicValue(state);
			return new Pair<Integer, Move>(value, null);
		}
	
		// recurse: minimax
		Map<Move, List<MachineState>> legalMoves = getTransitions(state);
		Move optMove = null;
		Integer optValue = GOAL_MIN;
		
		for (Move move: legalMoves.keySet()) {
			int cur = dlsMin(legalMoves.get(move), depth - 1);
			if (cur > optValue) {
				optValue = cur;
				optMove = move;
			}
		}
		
		// cache?
		return new Pair<Integer, Move>(optValue, optMove);
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		depthLimited = true;
		count = 0;
		Pair<Integer, Move> opt = null;
		int depth = 0;
		
		try {
			while (true) {
				depthLimited = false;
				depth++;
				System.out.println(">> exploring to depth " + depth);
				opt = dls(getCurrentState(), depth);
				if (!depthLimited) {
					System.out.println(">> explored full depth");
					break;
				}
			}
		} catch (Exception e) {
			System.out.println(">> stopping: " + e.getMessage());
		}
		
		if (opt != null) {
			return opt.snd;
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
