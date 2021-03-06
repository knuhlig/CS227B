package apps.pgggppg;

import player.gamer.statemachine.StateMachineGamer;
import util.gdl.grammar.GdlPool;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;
import apps.pgggppg.compilation.NativeMachineState;
import apps.pgggppg.compilation.NativePropNetStateMachine;
import apps.pgggppg.uct.UCT;

public class UctGamer extends StateMachineGamer {
		
	@Override
	public String getName() {
		return "PGGGPPG UCT Gamer (Seed)";
	}
	
	private UCT uct;
	private StateMachine machine;
	private long timeoutPadding = 1200;
	
	@Override
	public StateMachine getInitialStateMachine() {
		try {
			machine = new NativePropNetStateMachine(getMatch().getGame().getRules());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(">> falling back on ProverStateMachine!");
			machine = new ProverStateMachine();
		}
		return machine;
	}
	
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		try {
			collectGarbage();
			System.out.println(">> metagaming");
			uct = new UCT(machine, getRole());
			uct.searchRepeatedly(machine.getInitialState(), timeout - timeoutPadding);
			System.out.println(">> done metagaming. Spare time: " + (timeout - System.currentTimeMillis()));
		} catch (Exception e) {
			System.out.println(">> metagaming: unhandled exception");
			e.printStackTrace();
		}
	}
	
	private void collectGarbage() {
		if (true) {
			System.out.println(">> skipping GC");
			return;
		}
		System.out.print(">> garbage collection...");
		System.gc();
		System.out.println("done.");
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		MachineState currentState = getCurrentState();
		try {
			collectGarbage();
			Move move = uct.selectBestMove(currentState, timeout - timeoutPadding);
			System.out.println(">> move chosen. Spare time: " + (timeout - System.currentTimeMillis()));
			return move;
		} catch (Exception e) {
			System.out.println(">> search: unhandled exception");
			e.printStackTrace();
		}
		
		return machine.getRandomMove(currentState, getRole());
	}

	@Override
	public void stateMachineStop() {
		System.out.println("==== GAME OVER ====\n");
		//GdlPool.drainPool();
	}

	@Override
	public void stateMachineAbort() {}

}
