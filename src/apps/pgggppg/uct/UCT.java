package apps.pgggppg.uct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;

public class UCT {
	
	public static final double C = 40.0;
	public static final double GAMMA = 0.99;

	private StateMachine machine;
	private List<Role> roles;
	private int myPlayerIdx;
	private int numPlayers;
	
	private long timeout;
	private boolean timedOut;
	
	private Map<MachineState, UCTNode> nodes = new HashMap<MachineState, UCTNode>();
	
	public UCT(StateMachine machine, Role myRole) {
		this.machine = machine;
		roles = machine.getRoles();
		numPlayers = roles.size();
		for (int i = 0; i < roles.size(); i++) {
			Role role = roles.get(i);
			if (role.equals(myRole)) {
				myPlayerIdx = i;
				break;
			}
		}
	}
	
	public Move selectBestMove(MachineState state, long timeout) throws Exception {
		UCTNode node = getOrCreateNode(state);
		searchRepeatedly(state, timeout);
		node.printQValues(myPlayerIdx);
		Move action = node.getOptimalAction(myPlayerIdx);
		System.out.println(">> optimal action:");
		System.out.println("        move: " + action);
		System.out.println("      payoff: " + node.getPayoff(myPlayerIdx, action));
		return action;
	}
	
	public void searchRepeatedly(MachineState state, long timeout) throws Exception {
		timedOut = false;
		this.timeout = timeout;
		double[] qValues = new double[numPlayers];
		int it = 0;
		
		while (!timedOut) {
			search(state, qValues);
			it++;
		}
		
		System.out.println(">> UCT simulations: " + it);
	}
	
	private void search(MachineState state, double[] qValues) throws Exception {
		// check for timeouts
		if (System.currentTimeMillis() >= timeout) {
			timedOut = true;
			return;
		}
		
		UCTNode node = getOrCreateNode(state);
		
		// terminal state
		if (node.isTerminal()) {
			for (int i = 0; i < numPlayers; i++) {
				qValues[i] = node.getGoalValue(i);
			}
			return;
		}
		
		List<Move> sampledActions = new ArrayList<Move>();
		for (int i = 0; i < numPlayers; i++) {
			sampledActions.add(node.sampleAction(i));
		}
		
		search(machine.getNextState(state, sampledActions), qValues);
		if (!timedOut) {
			for (int i = 0; i < numPlayers; i++) {
				qValues[i] *= GAMMA;
				node.update(i, sampledActions.get(i), qValues[i]);
			}
		}
	}

	private UCTNode getOrCreateNode(MachineState state) throws Exception {
		// already have it
		if (nodes.containsKey(state)) {
			return nodes.get(state);
		}
		
		// create and add
		UCTNode node = createNode(state);
		nodes.put(state, node);
		return node;
	}
	
	private UCTNode createNode(MachineState state) throws Exception {
		UCTNode node = new UCTNode(numPlayers);
		
		// set actions
		for (int i = 0; i < numPlayers; i++) {
			List<Move> legalMoves = machine.getLegalMoves(state, roles.get(i));
			node.setActions(i, legalMoves);
		}
		
		// set goal values
		if (machine.isTerminal(state)) {
			for (int i = 0; i < numPlayers; i++) {
				int goal = machine.getGoal(state, roles.get(i));
				node.setGoalValue(i, goal);
			}
		}
		return node;
	}
}
