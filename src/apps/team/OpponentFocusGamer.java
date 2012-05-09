package apps.team;

import java.util.List;
import java.util.Map;

import util.statemachine.MachineState;
import util.statemachine.Move;


// Maximizes opponent focus
public class OpponentFocusGamer extends HeuristicGamer{
	private double sigmoid(double t) {
		return 1/(1 + Math.exp(-t));
	}

	@Override
	public int getHeuristicValue(MachineState state) throws Exception {
		Map<Move, List<MachineState>> successors = getStateMachine().getNextStates(state, getRole());
		int numPossibleSuccessors = 0;
		for(List<MachineState> successorGroup : successors.values()) {
			numPossibleSuccessors += successorGroup.size();
		}
		return (int)(100*sigmoid(-0.3*numPossibleSuccessors));
	}

}
