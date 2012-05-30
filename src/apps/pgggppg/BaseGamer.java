package apps.pgggppg;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.propnet.PropNetStateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;

public abstract class BaseGamer extends StateMachineGamer {
	
	protected String gamerName;
	
	public BaseGamer(String gamerName) {
		this.gamerName = gamerName;
	}
	
	public abstract Move selectNextMove(long timeout) throws Exception;
	
	@Override
	public String getName() {
		return gamerName;
	}
	
	@Override
	public StateMachine getInitialStateMachine() {
		return new PropNetStateMachine();
	}
	
	@Override
	public Move stateMachineSelectMove(long timeout) throws MoveDefinitionException, TransitionDefinitionException {
		try {
			return selectNextMove(timeout);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getStateMachine().getRandomMove(getCurrentState(), getRole());
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
