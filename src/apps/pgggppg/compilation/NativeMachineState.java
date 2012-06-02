package apps.pgggppg.compilation;

import java.util.List;

import util.statemachine.MachineState;


public abstract class NativeMachineState extends MachineState {

	public abstract boolean isTerminal();
	public abstract int getGoal(int roleIdx);
	public abstract List<Integer> getLegalMoves(int roleIdx);
	public abstract MachineState getNextState(List<Integer> moves);
	public abstract MachineState getInitialState();
}
