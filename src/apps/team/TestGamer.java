package apps.team;

import java.util.List;

import player.gamer.statemachine.StateMachineGamer;
import player.gamer.statemachine.reflex.event.ReflexMoveSelectionEvent;
import player.gamer.statemachine.reflex.gui.ReflexDetailPanel;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;
import apps.player.detail.DetailPanel;

public class TestGamer extends StateMachineGamer {

	@Override
	public String getName() {
		return "PGGGPPG Player";
	}
	
	@Override
	public StateMachine getInitialStateMachine() {
		return new ProverStateMachine();
	}
	
	/**
	 * Selects the first legal move
	 */
	@Override
	public Move stateMachineSelectMove(long timeout) throws MoveDefinitionException {
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		System.out.println("======== legal moves =========");
		for (Move move: moves) {
			System.out.println(move);
		}
		return moves.get(0);
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

	@Override
	public DetailPanel getDetailPanel() {
		return new ReflexDetailPanel();
	}
}
