package apps.pgggppg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import apps.pgggppg.compilation.NativePropNetStateMachine;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;
import apps.pgggppg.compilation.NativePropNetStateMachine;
import apps.pgggppg.uct.UCT;
import apps.team.Pair;

public class UCTGamer extends StateMachineGamer {
		
	@Override
	public String getName() {
		return "PGGGPPG UCT Gamer (Seed)";
	}
	
	private UCT uct;
	private StateMachine machine;
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
