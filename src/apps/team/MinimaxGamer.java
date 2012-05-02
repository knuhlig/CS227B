package apps.team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	private long timeout;
	private int count;
	
	@Override
	public String getName() {
		return "PGGGPPG Minimax";
	}
	
	@Override
	public StateMachine getInitialStateMachine() {
		stateCache.clear();
		return new ProverStateMachine();
	}
	
	@Override
	public Move stateMachineSelectMove(long timeout) throws MoveDefinitionException, TransitionDefinitionException {
		this.timeout = timeout;
		count = 0;
		
		try {
			Move opt = minimaxMove(getCurrentState());
			System.out.println(">> cache size: " + stateCache.size());
			return opt;
		} catch (Exception e) {
			e.printStackTrace();
		}

		// shouldn't happen, but just in case, return a random move
		return getStateMachine().getRandomMove(getCurrentState(), getRole());
	}
	
	private int minimaxValue(MachineState state) throws Exception {
		count++;
		if (count % 2 == 0) {
			if (System.currentTimeMillis() > timeout - 500) {
				throw new RuntimeException("timeout");
			}
			System.out.println("considered: " + count);
		}
		
		// already computed
		if (stateCache.containsKey(state)) {
			return stateCache.get(state);
		}
		
		// terminal state
		if (getStateMachine().isTerminal(state)) {
			int goal = getStateMachine().getGoal(state, getRole());
			stateCache.put(state, goal);
			return goal;
		}
		
		// get moves and potential new states
		System.out.print("computing legal moves");
		Map<Move, List<MachineState>> moveStates = getStateMachine().getNextStates(state, getRole());
		System.out.println("...done");
		int max = 0;
		for (Move move: moveStates.keySet()) {
			// minimize
			int min = 100;
			for (MachineState nextState: moveStates.get(move)) {
				int value = minimaxValue(nextState);
				if (value < min) {
					min = value;
				}
			}
			
			// maximize
			if (min > max) {
				max = min;
			}
		}
		
		stateCache.put(state, max);
		return max;
	}
	
	/**
	 * Wrapper to evaluate legal moves and compute opt via minimaxValue.
	 * @param state
	 * @return
	 * @throws Exception
	 */
	private Move minimaxMove(MachineState state) throws Exception {
		int max = 0;
		Move optMove = null;
		Map<Move, List<MachineState>> moveStates = getStateMachine().getNextStates(state, getRole());

		for (Move move: moveStates.keySet()) {
			// minimize
			int min = 100;
			for (MachineState nextState: moveStates.get(move)) {
				int value = minimaxValue(nextState);
				if (value < min) {
					min = value;
				}
			}
			
			// maximize
			if (min > max) {
				max = min;
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
