package apps.team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public class AlphaBetaGamer extends StateMachineGamer {
	
	private Map<MachineState, Integer> stateCache = new HashMap<MachineState, Integer>();
	private Map<MachineState, Boolean> exactValue = new HashMap<MachineState, Boolean>();
	
	@Override
	public String getName() {
		return "PGGGPPG Alpha-Beta";
	}
	
	@Override
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}
	
	@Override
	public Move stateMachineSelectMove(long timeout) throws MoveDefinitionException, TransitionDefinitionException {
		try {
			Move opt = alphaBetaMove(getCurrentState());
			System.out.println(">> cache size: " + stateCache.size());
			return opt;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// shouldn't happen, but just in case, return a random move
		return getStateMachine().getRandomMove(getCurrentState(), getRole());
	}
	
	private int alphaBetaValue(MachineState state, int alpha, int beta) throws Exception {
		if (stateCache.containsKey(state) && exactValue.get(state)) {
			return stateCache.get(state);
		}
		
		// terminal state
		if (getStateMachine().isTerminal(state)) {
			int goal = getStateMachine().getGoal(state, getRole());
			stateCache.put(state, goal);
			exactValue.put(state, true);
			return goal;
		}
		
		// get moves and potential new states
		Map<Move, List<MachineState>> moveStates = getStateMachine().getNextStates(state, getRole());
		boolean isExact = true;
		for (Move move: moveStates.keySet()) {
			// prune
			if (beta <= alpha) {
				isExact = false;
				break;
			}
			
			int curBeta = beta;
			for (MachineState nextState: moveStates.get(move)) {
				// prune
				if (curBeta <= alpha) {
					isExact = false;
					break;
				}
				int value = alphaBetaValue(nextState, alpha, curBeta);
				if (!exactValue.get(nextState)) {
					isExact = false;
				}
				curBeta = Math.min(curBeta, value);
			}
			alpha = Math.max(alpha, curBeta);
		}
		
		stateCache.put(state, alpha);
		exactValue.put(state, isExact);
		return alpha;
	}
	
	/**
	 * Wrapper to evaluate legal moves and compute opt via minimaxValue.
	 * @param state
	 * @return
	 * @throws Exception
	 */
	private Move alphaBetaMove(MachineState state) throws Exception {
		// unroll the first step
		int optVal = -1;
		Move optMove = null;
		
		Map<Move, List<MachineState>> moveStates = getStateMachine().getNextStates(state, getRole());		
		for (Move move: moveStates.keySet()) {
			int min = 100;
			for (MachineState nextState: moveStates.get(move)) {
				min = Math.min(min, alphaBetaValue(nextState, optVal, min));
			}
			
			if (min > optVal) {
				optVal = min;
				optMove = move;
			}
		}
		
		return optMove;
	}
	
	
	/**
	 * Does nothing for the metagame
	 */
	@Override
	public void stateMachineMetaGame(long timeout) {
		// Do nothing.
	}

	@Override
	public void stateMachineStop() {
		// Do nothing.
	}

	@Override
	public void stateMachineAbort() {
		// Do nothing
	}
}
