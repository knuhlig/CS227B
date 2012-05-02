package apps.team;

import util.statemachine.MachineState;

public class Assign4Gamer extends HeuristicGamer {
	
	private int numTrials = 3;
	
	@Override
	public String getName() {
		return "PGGGPPG Assign4";
	}

	@Override
	public int getHeuristicValue(MachineState state) throws Exception {
		int sum = 0;
		for (int i = 0; i < numTrials; i++) {
			MachineState terminal = getStateMachine().performDepthCharge(state, null);
			sum += getStateMachine().getGoal(terminal, getRole());
		}
		return sum / numTrials;
	}
}
