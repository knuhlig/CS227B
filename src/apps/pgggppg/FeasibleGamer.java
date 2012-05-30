package apps.pgggppg;

import java.util.List;
import java.util.Map;

import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.StateMachine;

public class FeasibleGamer extends BaseGamer {

	private boolean timedOut = false;
	private StateMachine machine;
	private int depthLimit;
	private boolean depthLimited = false;
	private long timerPadding = 1000;
	
	public FeasibleGamer() {
		super("Avoid Losing");
	}
	
	private boolean isLosing(MachineState state, int depth) throws Exception {
		if (timedOut) {
			throw new TimerException();
		}
		
		if (machine.isTerminal(state)) {
			int goal = machine.getGoal(state, getRole());
			return goal == 0;
		}
		
		// non-terminal, can't search any more
		if (depth >= depthLimit) {
			depthLimited = true;
			return false;
		}
		
		Map<Move, List<MachineState>> moveMap = machine.getNextStates(state, getRole());
		for (Move move: moveMap.keySet()) {
			boolean losing = false;
			for (MachineState nextState: moveMap.get(move)) {
				if (isLosing(nextState, depth + 1)) {
					losing = true;
					break;
				}
			}
			// can make a move that doesn't lead to a losing game
			if (!losing) {
				return false;
			}
		}
		// didn't find any acceptable moves
		return true;
	}
	
	private Move getFeasibleMove(MachineState state) throws Exception {
		Map<Move, List<MachineState>> moveMap = machine.getNextStates(state, getRole());
		for (Move move: moveMap.keySet()) {
			boolean losing = false;
			for (MachineState nextState: moveMap.get(move)) {
				if (isLosing(nextState, 1)) {
					losing = true;
					break;
				}
			}
			if (!losing) {
				return move;
			}
		}
		return null;
	}
	
	@Override
	public Move selectNextMove(long timeout) throws Exception {
		timedOut = false;
		machine = getStateMachine();
		
		// start timer
		long duration = timeout - System.currentTimeMillis() - timerPadding;
		new TimerThread(duration).start();
		
		Move move = null;
		try {
			for (int depth = 1; true; depth++) {
				depthLimit = depth;
				depthLimited = false;
				move = getFeasibleMove(getCurrentState());
				System.out.println(">> searched depth " + depth);
				if (!depthLimited) {
					System.out.println(">> searched full tree");
					break;
				}
			}
		} catch (TimerException e) {
			System.out.println(">> timed out");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		if (move == null) {
			System.out.println(">> BOO: no moves. playing randomly");
			move = machine.getRandomMove(getCurrentState(), getRole());
		}
		return move;
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
