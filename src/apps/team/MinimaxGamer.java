package apps.team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.tools.javac.util.Pair;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;
import apps.player.detail.DetailPanel;

public class MinimaxGamer extends StateMachineGamer {
	
	private Map<MachineState, Integer> stateCache = new HashMap<MachineState, Integer>();
	
	@Override
	public String getName() {
		return "PGGGPPG Minimax";
	}
	
	@Override
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}
	
	/**
	 * Selects the first legal move
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws MoveDefinitionException, TransitionDefinitionException {
		try {
			return minimaxMove(getCurrentState());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return getStateMachine().getRandomMove(getCurrentState(), getRole());
	}
	
	private int minimaxValue(MachineState state) throws Exception {
		// cache
		if (stateCache.containsKey(state)) {
			return stateCache.get(state);
		}
		
		if (getStateMachine().isTerminal(state)) {
			int goal = getStateMachine().getGoal(state, getRole());
			stateCache.put(state, goal);
			return goal;
		}
		
		Map<Move, List<MachineState>> moveStates = getStateMachine().getNextStates(state, getRole());
		int max = -1;
		
		for (Move move: moveStates.keySet()) {
			int min = -1;
			for (MachineState nextState: moveStates.get(move)) {
				int value = minimaxValue(nextState);
				if (min < 0 || value < min) {
					min = value;
				}
			}
			if (max < 0 || min > max) {
				max = min;
			}
		}
		
		stateCache.put(state, max);
		return max;
	}
	
	private Move minimaxMove(MachineState state) throws Exception {
		Map<Move, List<MachineState>> moveStates = getStateMachine().getNextStates(state, getRole());
		int max = -1;
		Move optMove = null;
		for (Move move: moveStates.keySet()) {
			int min = -1;
			for (MachineState nextState: moveStates.get(move)) {
				int value = minimaxValue(nextState);
				if (min < 0 || value < min) {
					min = value;
				}
			}
			if (max < 0 || min > max) {
				max = min;
				optMove = move;
			}
		}
		System.out.println(">> cache size: " + stateCache.size());
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
