package apps.pgggppg.uct;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import util.statemachine.MachineState;
import util.statemachine.Move;

public class UCTNode {

	private UCTMetadata[] playerData;
	private int[] goalValues;
	
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
	
	public Move sampleAction(int playerIdx) {
		UCTMetadata data = playerData[playerIdx];
		return data.sampleAction();
	}
	
	public double getPayoff(int playerIdx, Move action) {
		UCTMetadata data = playerData[playerIdx];
		ActionMetadata actionData = data.actionMap.get(action);
		double q = actionData.qSum / actionData.actionCount;
		return q;
	}
	
	public void setActions(int playerIdx, List<Move> actions) {
		UCTMetadata data = playerData[playerIdx];
		for (Move action: actions) {
			data.actionMap.put(action, new ActionMetadata());
		}
	}
	
	public void printQValues(int playerIdx) {
		NumberFormat fmt = NumberFormat.getInstance();
		fmt.setMaximumFractionDigits(3);
		fmt.setMinimumFractionDigits(3);
		
		System.out.println("\n========== Move Options ========");
		UCTMetadata data = playerData[playerIdx];
		for (Move action: data.actionMap.keySet()) {
			ActionMetadata actionData = data.actionMap.get(action);
			if (actionData.actionCount > 0) {
				double qValue = actionData.qSum / actionData.actionCount;
				System.out.println("(" + actionData.actionCount + ") " + fmt.format(qValue) + " => " + action);
			} else {
				System.out.println("n/a => " + action);
			}
		}
		System.out.println("================================\n");
	}
	
	public void update(int playerIdx, Move action, double qValue) {
		UCTMetadata data = playerData[playerIdx];
		data.stateCount++;
		
		ActionMetadata actionData = data.actionMap.get(action);
		actionData.actionCount++;
		actionData.qSum += qValue;
	}
	
	private static class UCTMetadata {
		public int stateCount;
		public Map<Move, ActionMetadata> actionMap;
		
		public UCTMetadata() {
			stateCount = 0;
			actionMap = new HashMap<Move, ActionMetadata>();
		}
		
		public Move sampleAction() {
			Move optAction = null;
			double optA = 0;
			
			for (Move action: actionMap.keySet()) {
				ActionMetadata actionData = actionMap.get(action);
				
				// return unexplored actions first
				if (actionData.actionCount == 0) {
					optAction = action;
					break;
				}
				
				double q = actionData.qSum / actionData.actionCount;
				double a = q + UCT.C * Math.sqrt(Math.log(stateCount) / actionData.actionCount);
				
				// maximize
				if (optAction == null || a > optA) {
					optAction = action;
					optA = a;
				}
			}

			return optAction;
		}
		
		public Move getOptimalAction() {
			Move optAction = null;
			double optQ = 0;
			
			for (Move action: actionMap.keySet()) {
				ActionMetadata actionData = actionMap.get(action);
				
				// return unexplored actions first
				if (actionData.actionCount == 0) {
					continue;
				}
				
				double q = actionData.qSum / actionData.actionCount;
				
				// maximize
				if (optAction == null || q > optQ) {
					optAction = action;
					optQ = q;
				}
			}

			// this could only happen if we never did a single depth charge
			if (optAction == null) {
				return actionMap.keySet().iterator().next();
			}
			
			return optAction;
		}
		
	}
	
	private static class ActionMetadata {
		public int actionCount;
		public double qSum;
	}
}
