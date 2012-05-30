package apps.pgggppg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.StateMachine;

public class SimpleGamer extends BaseGamer {

	public SimpleGamer() {
		super("Simple Gamer");
	}
	
	private StateMachine machine;
	private boolean timedOut = false;
	private int depthLimit = 0;
	private boolean depthLimited = false;
	
	long timerPadding = 1000;
	
	private Comparator<MachineState> stateCmp = new Comparator<MachineState>() {
		@Override
		public int compare(MachineState a, MachineState b) {
			int aVal = machine.isTerminal(a) ? 0 : 1;
			int bVal = machine.isTerminal(b) ? 0 : 1;
			return aVal - bVal;
		}
	};
	
	private void checkTimeout() {
		if (timedOut) {
			throw new TimerException();
		}
	}
	
	public Outcome worstOutcome(List<MachineState> states, int depth, Outcome alpha, Outcome beta) throws Exception {
		Collections.sort(states, stateCmp);
		for (MachineState state: states) {
			Outcome cur = bestOutcome(state, depth, alpha, beta);
			if (cur.isWorseThan(beta)) {
				beta = cur;
			}
			if (!beta.isBetterThan(alpha)) {
				break;
			}
		}
		return beta;
	}
	
	public Outcome bestOutcome(MachineState state, int depth, Outcome alpha, Outcome beta) throws Exception {
		checkTimeout();
		
		// terminal
		if (machine.isTerminal(state)) {
			int goal = machine.getGoal(state, getRole());
			return new Outcome(goal, depth);
		}
		
		// non-terminal, can't search further
		if (depth >= depthLimit) {
			depthLimited = true;
			return new Outcome(50, depth);
		}
		
		Map<Move, List<MachineState>> moveMap = machine.getNextStates(state, getRole());
		for (Move move: moveMap.keySet()) {
			Outcome min = worstOutcome(moveMap.get(move), depth + 1, alpha, beta);
			if (min.isBetterThan(alpha)) {
				alpha = min;
			}
			if (!beta.isBetterThan(alpha)) {
				break;
			}
		}
		
		return alpha;
	}
	
	public Move alphaBeta(MachineState state, int searchDepth) throws Exception {
		depthLimit = searchDepth;
		Outcome bestOutcome = null;
		Move bestMove = null;

		Map<Move, List<MachineState>> moveMap = machine.getNextStates(state, getRole());
		Outcome alpha = new Outcome(0, 0);
		Outcome beta = new Outcome(100, 0);
		
		for (Move move: moveMap.keySet()) {
			Outcome min = worstOutcome(moveMap.get(move), 1, alpha, beta);
			if (bestOutcome == null || min.isBetterThan(bestOutcome)) {
				bestOutcome = min;
				bestMove = move;
			}
		}
		
		System.out.println("alpha beta: " + bestOutcome);
		return bestMove;
	}
	
	@Override
	public Move selectNextMove(long timeout) throws Exception {
		timedOut = false;
		machine = getStateMachine();
		
		// start timer
		long duration = timeout - System.currentTimeMillis() - timerPadding;
		new TimerThread(duration).start();

		Move bestMove = null;
		try {
			for (int depth = 0; true; depth++) {
				depthLimited = false;
				Move cur = alphaBeta(getCurrentState(), depth);
				bestMove = cur;
				System.out.println(">> done exploring depth " + depth + ", candidate: " + bestMove);
				if (!depthLimited) {
					System.out.println(">> game tree is fully explored.");
					break;
				}
			}
		} catch (TimerException e) {
			System.out.println(">> timeout");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (bestMove == null) {
			System.out.println("BOO: didn't find any reasonable moves to make... playing randomly");
			return machine.getRandomMove(getCurrentState(), getRole());
		}
		return bestMove;
	}
	
	private class TimerThread extends Thread {
		
		private long duration;
		
		public TimerThread(long duration) {
			this.duration = duration;
		}
		
		public void run() {
			try {
				Thread.sleep(duration);
				timedOut = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static class TimerException extends RuntimeException {
		
	}
	
}
