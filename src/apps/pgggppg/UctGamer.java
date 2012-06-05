package apps.pgggppg;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import apps.pgggppg.compilation.NativeMachineState;
import apps.pgggppg.compilation.NativePropNetStateMachine;
import apps.pgggppg.uct.UCT;

public class UctGamer extends StateMachineGamer {
		
	@Override
	public String getName() {
		return "PGGGPPG UCT Gamer (Seed)";
	}
	
	private UCT uct;
	private NativePropNetStateMachine machine;
	private long timeoutPadding = 1000;
	
	@Override
	public StateMachine getInitialStateMachine() {
		machine = new NativePropNetStateMachine();
		return machine;
	}
	
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		try {
			System.out.println(">> metagaming");
			uct = new UCT(machine, getRole());
			uct.searchRepeatedly(machine.getInitialState(), timeout - timeoutPadding);
		} catch (Exception e) {
			System.out.println(">> metagaming: unhandled exception");
			e.printStackTrace();
		}
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		MachineState currentState = getCurrentState();
		try {
			Move move = uct.selectBestMove(currentState, timeout - timeoutPadding);
			return move;
		} catch (Exception e) {
			System.out.println(">> search: unhandled exception");
			e.printStackTrace();
		}
		
		return machine.getRandomMove(currentState, getRole());
	}

	@Override
	public void stateMachineStop() {}

	@Override
	public void stateMachineAbort() {}

}
