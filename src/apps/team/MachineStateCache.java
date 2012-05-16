package apps.team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class MachineStateCache
{
	private Map<MachineState, Integer> terminalCache;
	private Map<MachineState, Map<Move, List<MachineState>>> transitionCache;
	private StateMachine stateMachine;
	private Role role;

	private int numLookups;
	private int numHits;
	
	public MachineStateCache(StateMachine stateMachine, Role role) {
		this.stateMachine = stateMachine;
		this.role = role;
		reset();
	}

	public void reset() {
		terminalCache = new HashMap<MachineState, Integer>();
		transitionCache = new HashMap<MachineState, Map<Move, List<MachineState>>>();
		numLookups = 0;
		numHits = 0;
	}

	public Map<Move, List<MachineState>> getTransitions(MachineState state)  {
		numLookups++;
		try {
			Map<Move, List<MachineState>> transitions = transitionCache.get(state);
			if (transitions == null) {
				transitions = stateMachine.getNextStates(state, role);
				transitionCache.put(state, transitions);
			} else {
				numHits++;
			}
			if (numLookups % 1024 == 1) {
				System.out.println("Hit ratio is " + ((double)numHits)/numLookups);
			}
			return transitions;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	public List<MachineState> getTransitionResults(MachineState state, Move move) {
		return getTransitions(state).get(move);
	}
	public MachineState getTransitionResult(MachineState state, Move move, int subMoveNum) {
		return getTransitionResults(state,move).get(subMoveNum);
	}
	public boolean isTerminalState(MachineState state) {
		if (terminalCache.containsKey(state)) {
			return true;
		}
		return stateMachine.isTerminal(state);
	}
	public int terminalValue(MachineState state) {
		try {
			Integer value = terminalCache.get(state);
			if (value == null) {
				value = stateMachine.getGoal(state, role);
				terminalCache.put(state, value);
			}
			return value;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}