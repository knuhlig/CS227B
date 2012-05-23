package apps.team.util;

import java.util.List;
import java.util.Map;

import apps.team.Connect4Machine;

import util.game.Game;
import util.game.GameRepository;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.implementation.propnet.PropNetStateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;

public class MachineEvaluator {
	
	public static void main(String[] args) {
		String gameName = "connectFour";
		GameRepository repository = GameRepository.getDefaultRepository(); 
		Game game = repository.getGame(gameName);
		
		StateMachine[] machines = new StateMachine[] {
			//new ProverStateMachine(),
			new PropNetStateMachine(),
			new Connect4Machine()
		};
		int depth = 6;
		
		MachineEvaluator eval = new MachineEvaluator();
		for (StateMachine machine: machines) {
			machine.initialize(game.getRules());
			eval.evaluate(machine, depth);
		}
	}

	private StateMachine machine;
	private int stateCount;
	private Role role;
	
	public void evaluate(StateMachine machine, int depth) {
		this.machine = machine;
		stateCount = 0;
		role = machine.getRoles().get(0);
		long start = System.currentTimeMillis();
		
		try {
			evaluate(machine.getInitialState(), depth);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		long end = System.currentTimeMillis();
		System.out.println("states: " + stateCount);
		System.out.println("total time: " + (end - start)/1000.0 + " seconds");
		System.out.println("avg: " + (end - start) * 1000.0 / stateCount + " us per state");
		System.out.println();
	}
	
	private void evaluate(MachineState state, int depth) throws Exception {
		stateCount++;
		// base case
		if (depth == 0) {
			return;
		}
		// terminal
		if (machine.isTerminal(state)) {
			return;
		}
		
		Map<Move, List<MachineState>> moves = machine.getNextStates(state, role);
		for (Move move: moves.keySet()) {
			for (MachineState next: moves.get(move)) {
				evaluate(next, depth - 1);
			}
		}
	}
}
