package apps.team;

import java.util.List;
import java.util.Map;

import util.statemachine.MachineState;
import util.statemachine.Move;

public class WeightedGamer extends HeuristicGamer {

	@Override
	public String getName() {
		return "PGGGPPG Weighted";
	}

	private int numTrials = 5;

	private double sigmoid(double t) {
		return 1 / (1 + Math.exp(-t));
	}

	@Override
	public int getHeuristicValue(MachineState state) throws Exception {

		int playerMobilityScore, playerFocusScore, opponentMobilityScore, opponentFocusScore;

		Map<Move, List<MachineState>> successors = getTransitions(state);
		int numPossibleSuccessors = successors.size();

		int numResultantStates = 0;
		for (List<MachineState> successorGroup : successors.values()) {
			numResultantStates += successorGroup.size();
		}

		// Maximize Player Mobility
		playerMobilityScore = (int) (100 * sigmoid(0.3 * numPossibleSuccessors));

		// Maximize Player Focus
		playerFocusScore = (int) (100 * sigmoid(-0.3 * numPossibleSuccessors));

		// Maximize Opponent Mobility
		opponentMobilityScore = (int) (100 * sigmoid(0.3 * numResultantStates));

		// Maximize Opponent Focus
		opponentFocusScore = (int) (100 * sigmoid(-0.3 * numResultantStates));

		// Monte Carlo Heuristic
		int sum = 0;
		for (int i = 0; i < numTrials; i++) {
			MachineState terminal = depthCharge(state);
			sum += getStateMachine().getGoal(terminal, getRole());
		}
		sum /= numTrials;
		
		// Weights
		double w1 = 0.2, w2 = 0.2, w3 = 0.2, w4 = 0.15, w5 = 1 - (w1 + w2+ w3 + w4);
		return (int) (w1 * playerMobilityScore + w2 * playerFocusScore + w3
				* opponentMobilityScore + w4 * opponentFocusScore + w5 * sum);

	}
}
