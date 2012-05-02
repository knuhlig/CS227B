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
	private boolean depthLimited;
	
	private long timeoutBuffer = 1000;
	
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
	
	private int dlsMin(List<MachineState> states, int depth, int alpha, int beta) throws Exception {
		for (MachineState state: states) {
			beta = Math.min(beta, dls(state, depth, alpha, beta).fst);
			if (beta <= alpha) {
				break;
			}
		}
		return beta;
	}
	
	private Pair<Integer, Move> dls(MachineState state, int depth, int alpha, int beta) throws Exception {
		// check periodically for a timeout
		if (System.currentTimeMillis() + timeoutBuffer >= timeout) {
			System.out.println("timing out: " + System.currentTimeMillis() + " getting close to " + timeout);
			throw new RuntimeException("search timeout");
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
		
		for (Move move: legalMoves.keySet()) {
			int cur = dlsMin(legalMoves.get(move), depth - 1, alpha, beta);
			if (cur > alpha) {
				alpha = cur;
				optMove = move;
			}
			if (beta <= alpha) {
				break;
			}
		}
		
		// cache?
		return new Pair<Integer, Move>(alpha, optMove);
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		this.timeout = timeout;
		Pair<Integer, Move> opt = null;
		try {
			int depth = 0;
			while (true) {
				depthLimited = false;
				depth++;
				System.out.println(">> exploring to depth " + depth);
				opt = dls(getCurrentState(), depth, GOAL_MIN, GOAL_MAX);
				if (!depthLimited) {
					System.out.println(">> explored full depth");
					break;
				}
			}
		} catch (Exception e) {
			System.out.println(">> stopping: " + e.getMessage());
		}
		
		if (opt != null && opt.snd != null) {
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
