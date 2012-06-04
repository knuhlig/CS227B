package apps.pgggppg.uct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import util.statemachine.MachineState;
import util.statemachine.Move;

public class UCTNode {

	private UCTMetadata[] playerData;
	private int[] goalValues;
	private boolean isTerminal;
	
	
	public UCTNode(int numPlayers) {
		playerData = new UCTMetadata[numPlayers];
		for (int i = 0; i < playerData.length; i++) {
			playerData[i] = new UCTMetadata();
		}
	}
	
	public void setGoalValue(int playerIdx, int goal) {
		if (goalValues == null) {
			goalValues = new int[playerData.length];
		}
		goalValues[playerIdx] = goal;
	}
	
	public int getGoalValue(int playerIdx) {
		return goalValues[playerIdx];
	}
	
	public boolean isTerminal() {
		return goalValues != null;
	}
	
	public Move getOptimalAction(int playerIdx) {
		UCTMetadata data = playerData[playerIdx];
		return data.getOptimalAction();
	}
	
	public double getPayoff(int playerIdx, Move action) {
		UCTMetadata data = playerData[playerIdx];
		ActionMetadata actionData = data.map.get(action);
		return actionData.payoffSum / actionData.actionCount;
	}
	
	public void setActions(int playerIdx, List<Move> actions) {
		UCTMetadata data = playerData[playerIdx];
		for (Move action: actions) {
			data.map.put(action, new ActionMetadata());
		}
	}
	
	public void update(int playerIdx, Move action, double qValue) {
		UCTMetadata data = playerData[playerIdx];
		data.stateCount++;
		
		ActionMetadata actionData = data.map.get(action);
		actionData.actionCount++;
		actionData.payoffSum += qValue;
	}
	
	private static class UCTMetadata {
		public int stateCount;
		public Map<Move, ActionMetadata> map;
		
		public UCTMetadata() {
			stateCount = 0;
			map = new HashMap<Move, ActionMetadata>();
		}
		
		public Move getOptimalAction() {
			Move optAction = null;
			double optA = 0;
			
			for (Move action: map.keySet()) {
				ActionMetadata actionData = map.get(action);
				
				// return unexplored actions first
				if (actionData.actionCount == 0) {
					optAction = action;
					break;
				}
				
				double q = actionData.payoffSum / actionData.actionCount;
				double a = q + UCT.C * Math.sqrt(Math.log(stateCount) / actionData.actionCount);
				
				// maximize
				if (optAction == null || a > optA) {
					optAction = action;
					optA = a;
				}
			}
			
			return optAction;
		}
		
	}
	
	private static class ActionMetadata {
		public int actionCount;
		public double payoffSum;
	}
}
