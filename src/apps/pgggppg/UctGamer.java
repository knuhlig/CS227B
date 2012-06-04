package apps.pgggppg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import apps.pgggppg.compilation.NativePropNetStateMachine;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;
import apps.pgggppg.compilation.NativePropNetStateMachine;
import apps.team.Pair;

public class UctGamer extends StateMachineGamer {
		
	// Caches
	private Map<Pair<Role, MachineState>, Map<Move,List<MachineState>>> transitionCache;
	private Map<Pair<Role, Pair<MachineState,Move>>,Double> scoreCache;
	private Map<Pair<Role, Pair<MachineState,Move>>,Double> countCache;
	private Map<MachineState,Integer> stateCounts;
	
	// Timeout Handling
	private long timeout;
	private long stopTime;
	private long timeoutBuffer = 1000;
	private boolean breakout = false;
	
	// UCT parameter
	private Double C = 40.0;
	private Double gamma = 0.99;
	
	// For debugging purposes
	private boolean debug = false;
	private Map<Pair<Role, Pair<MachineState,Move>>, List<List<List<Move>>>> uctRuns;
	
	@Override
	public String getName() {
		return "PGGGPPG UCT Gamer";
	}
	
	public void reset() {
		transitionCache = new HashMap<Pair<Role, MachineState>, Map<Move,List<MachineState>>>();
		scoreCache = new HashMap<Pair<Role, Pair<MachineState,Move>>,Double>();
		countCache = new HashMap<Pair<Role, Pair<MachineState,Move>>,Double>();
		stateCounts = new HashMap<MachineState,Integer>();
		uctRuns = new HashMap<Pair<Role, Pair<MachineState,Move>>, List<List<List<Move>>>>();
	}
	
	@Override
	public StateMachine getInitialStateMachine() {
		reset();
		return new NativePropNetStateMachine();
		//return new ProverStateMachine();
	}
		
	public Map<Move, List<MachineState>> getTransitions(Role role, MachineState state, boolean cache) throws Exception {
		Pair<Role,MachineState> key = new Pair<Role,MachineState>(role,state); 
		if (transitionCache.containsKey(key)) {
			return transitionCache.get(key);
		}

		Map<Move, List<MachineState>> transitions = getStateMachine().getNextStates(state, role);
		if (cache) {
			transitionCache.put(key,transitions);
		}
		return transitions;
	}
	
	public boolean isTerminal(MachineState state) {
		return getStateMachine().isTerminal(state);
	}
	
	public int randomInt(int n) {
		return (int) (Math.random() * n);
	}
		
	// Computes the UCT score
	Double computeUCTScore(Pair<Role, Pair<MachineState,Move>> key){
		return scoreCache.get(key) + C*Math.sqrt(Math.log(stateCounts.get(key.snd.fst)) / countCache.get(key) );
	}
	
	// Computes the UCT score for this role at this state. Returns the move that maximizes the UCT score
	Move selectMove(Role role, MachineState state) throws Exception{
		try{
			Double bestScore = -1.0;
			Move bestMove = null;
			Boolean unexplored = false;
			List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(state);
			
			for(int moveNum=0; moveNum<jointMoves.size(); moveNum++){
				List<Move> curMoveSet = jointMoves.get(moveNum);
				int i=0;
				for (Role r : getStateMachine().getRoles()) {
					if (r.equals(role)) {
						
						if(System.currentTimeMillis() > this.stopTime){
							this.breakout = true;
							return null;
						}
							
						
						Move curMove = curMoveSet.get(i);
						Pair<Role, Pair<MachineState,Move>> key = new Pair<Role, Pair<MachineState,Move>>(r, new Pair<MachineState,Move>(state, curMove ));
						
						if(countCache.get(key) == null ){
							unexplored = true;
							bestMove = curMove;
							break;
						}
						
						// Compute UCT score
						Double score = computeUCTScore(key);
						
						if(score > bestScore){
							bestScore = score;
							bestMove = curMove;
						}
					}
					i++;
				}
				if(unexplored)
					break;
			}
			
		    if(this.debug){
		    	if(unexplored){
		    		System.out.println(role + "is picking an unexplored move");
		    	}
		    	System.out.println(role + " is choosing move with score: " + bestScore);
		    }
		    
			return bestMove;
		}
		catch (Exception e) {
			System.out.println("Something wrong in selectMove: " + e.getMessage());
		}
		// should never get here
		return null;
	}
	
	
	// Does the recursive UCT search (basically UCT depth charge)
	Pair<List<Double>,List<List<Move>>> search(MachineState state){
		try{
			if(stateCounts.get(state) == null){
				stateCounts.put(state,1);		
			} else{
				stateCounts.put(state,stateCounts.get(state)+1);
			}
			
			if(isTerminal(state)){
				if(System.currentTimeMillis() > this.stopTime){
					this.breakout = true;
					return null;
				}
				List<Integer> scores = getStateMachine().getGoals(state);
				List<Double> ret = new ArrayList<Double>();
				for(int i=0; i<scores.size(); i++)
					ret.add(new Double(scores.get(i)));
				return new Pair<List<Double>,List<List<Move>>>(ret, new ArrayList<List<Move>>());
			}
			
			// All players select move based on UCT rule
			// Works for both simultaneous and alternating
			List<Move> playerMoves = new ArrayList<Move>();
			for (Role r : getStateMachine().getRoles()) {
				
				// if move returned is null, break out
				Move curMove = selectMove(r,state);
				if(this.breakout)
					return null;
				playerMoves.add(curMove);
			}
			
			MachineState nextState = getStateMachine().getNextState(state, playerMoves);
			
			// Recurse on next state
			Pair<List<Double>,List<List<Move>>> ret = search(nextState);
			if(this.breakout)
				return null;
			List<Double>scores = ret.fst;
			List<List<Move>> movesList = ret.snd;
			movesList.add(playerMoves);
			
			// Do some backoff to favor closer goals
			for(int i=0; i<scores.size(); i++)
				scores.set(i, gamma*scores.get(i));

			int i=0;
			// Update the scores and counts
			for (Role r : getStateMachine().getRoles()) {
				
				if(System.currentTimeMillis() > this.stopTime){
					this.breakout = true;
					return null;
				}
				
				Move curMove = playerMoves.get(i);
				Pair<Role, Pair<MachineState,Move>> key = new Pair<Role, Pair<MachineState,Move>>(r, new Pair<MachineState,Move>(state,curMove));
				if(scoreCache.get(key) == null){
					scoreCache.put(key,new Double(scores.get(i)));
					countCache.put(key, 1.0);
					
					// begin debug
					if(this.debug){
						if(uctRuns.get(key)==null){
							List<List<List<Move>>> l = new ArrayList<List<List<Move>>>();
							List<List<Move>> starter = new ArrayList<List<Move>>();
							starter.add(playerMoves);
							l.add(starter);
							uctRuns.put(key,l);
						}else{
							List<List<List<Move>>> l = uctRuns.get(key);
							List<List<Move>> tempList = new ArrayList<List<Move>>();
							for(int j=0; j<movesList.size(); j++)
								tempList.add(movesList.get(j));
							Collections.reverse(tempList);
							l.add(tempList);
							uctRuns.put(key,l);
						}
					}
					// end debug
					
				}else{
					Double n = countCache.get(key);
					// update the average score
					scoreCache.put(key,scoreCache.get(key)*(n/(n+1.0)) + scores.get(i)/(n+1.0) );
					countCache.put(key,n+1.0);
					
					// begin debug
					if(this.debug){
						if(uctRuns.get(key)==null){
							List<List<List<Move>>> l = new ArrayList<List<List<Move>>>();
							List<List<Move>> starter = new ArrayList<List<Move>>();
							starter.add(playerMoves);
							l.add(starter);
							uctRuns.put(key,l);
						}else{
							List<List<List<Move>>> l = uctRuns.get(key);
							List<List<Move>> tempList = new ArrayList<List<Move>>();
							for(int j=0; j<movesList.size(); j++)
								tempList.add(movesList.get(j));
							Collections.reverse(tempList);
							l.add(tempList);
							uctRuns.put(key,l);
						}
					}
					// end debug
				}
				i++;
			}
			
			return new Pair<List<Double>,List<List<Move>>>(scores, movesList);
		}
		catch (Exception e) {
			System.out.println("Something wrong in search: " + e.getMessage());
			e.printStackTrace();
		}
		
		// should never get here
		return null;
		
	}
		
	private Move getNextMove() throws MoveDefinitionException {
		try{
			MachineState state = getCurrentState();
			
			// Try UCT until times out
			int count = 0;
			while(true){
				this.breakout = false;
				search(state);
				count++;
				if(this.breakout)
					break;
			}
			
			System.out.println("Performed " + count + " UCT searches this turn");
			System.out.println("Cache has size " + scoreCache.size());
			
			// Select the move that has the best cached score. How long does this take?
			Double bestScore = -1.0;
			Move bestMove = null;
			Role role = getRole();
			List<Move> nextJointMoves = new ArrayList<Move>();
			List<List<Move>> jointMoves = getStateMachine().getLegalJointMoves(state);
			for(int moveNum=0; moveNum<jointMoves.size(); moveNum++){
				List<Move> curMoveSet = jointMoves.get(moveNum);
				int i=0;
				for (Role r : getStateMachine().getRoles()) {
					if (r.equals(role)) {
						Move curMove = curMoveSet.get(i);
						Pair<Role, Pair<MachineState,Move>> key = new Pair<Role, Pair<MachineState,Move>>(r, new Pair<MachineState,Move>(state, curMove ));
					
						if(scoreCache.get(key) == null)
							continue;
						
						Double score = scoreCache.get(key);
				
						if(this.debug)
							System.out.println(role + " has move " + curMove + " with score " + score);
						
						if(score > bestScore){
							bestScore = score;
							bestMove = curMove;
							nextJointMoves = curMoveSet;
						}
					
					}
					i++;
				}
			}
			
			// shouldn't happen
			if(bestMove==null){
				System.out.println("Returning null move. Something is wrong!");
			}else{
				System.out.println("Found a move for " + role + " with score: " + bestScore + " at move " + bestMove);
				// begin debug
				if(this.debug){
					if(bestMove.toString() == "noop")
						return bestMove;

					// print out all of the trials for this move

					Pair<Role, Pair<MachineState,Move>> firstKey = new Pair<Role, Pair<MachineState,Move>>(role, new Pair<MachineState,Move>(state,bestMove));
					List<List<List<Move>>> runs = uctRuns.get(firstKey);

					System.out.println("Printing UTC runs");
					for(int i=0; i<runs.size(); i++){
						System.out.println("First move for " + role + " is " + bestMove);
						for(int j=0; j<runs.get(i).size(); j++){
							System.out.println(runs.get(i).get(j));
						}
						System.out.println("");
					}


					System.out.println("Move that will be made: " + nextJointMoves);
					MachineState nextState = getStateMachine().getNextState(state, nextJointMoves);
					jointMoves = getStateMachine().getLegalJointMoves(nextState);
					Move opponentBestMove = null;
					Double opponentBestScore = -1.0;
					for(int moveNum=0; moveNum<jointMoves.size(); moveNum++){
						List<Move> curMoveSet = jointMoves.get(moveNum);
						int i=0;
						for (Role r : getStateMachine().getRoles()) {
							if (!r.equals(role)) {
								Move curMove = curMoveSet.get(i);
								Pair<Role, Pair<MachineState,Move>> key = new Pair<Role, Pair<MachineState,Move>>(r, new Pair<MachineState,Move>(nextState, curMove ));

								if(scoreCache.get(key) == null)
									continue;

								Double score = scoreCache.get(key);

								// print out the opponent scores and UCT scores. Which move will opponent pick?
								System.out.println(r + " considers move " + curMove);
								Double UCTscore = computeUCTScore(key);
								System.out.println("UCT score: " + UCTscore);
								System.out.println("score: " + scoreCache.get(key));
								System.out.println("Do this move: " + countCache.get(key) + " times ");
								System.out.println("In this state: " + stateCounts.get(nextState) + "times ");
								System.out.println("");

								if(score > opponentBestScore){
									opponentBestScore = score;
									opponentBestMove = curMove;
								}
							}
							i++;
						}
					}
					//System.out.println("After this, found a move for opponent with score: " + opponentBestScore + " with move " + opponentBestMove);
				}
				// end debug
				return bestMove;
			}
			
		}
		catch (Exception e){
			
		}
		// should never get here
		return null;
	}
	
	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		System.out.println(">> metagaming");
		
		
		
		this.timeout = timeout;
		long startTime = System.currentTimeMillis();
		long duration = timeout - startTime;
		System.out.println(">> start time: " + startTime);
		System.out.println(">> timeout: " + timeout);
		System.out.println(">> duration: " + duration);
		
		stopTime = startTime + duration - timeoutBuffer;
		try{
			MachineState start = getStateMachine().getInitialState();
			Integer n = 0;

			// Drop UCT charges until we run out of time
			while(true){
				this.breakout = false;
				search(start);
				if(this.breakout){
					break;
				}
				n++;
			}
			System.out.println(">>Performed " + n + " UCT depth charges in " + duration + " milliseconds.");
			System.out.println(">>Score cache size: " + scoreCache.size());
			
		}catch (Exception e){
			System.out.println(">> metagaming killed");
			System.out.println("Size of cache: " + scoreCache.size());
		}
		
		System.out.println(">> done metagaming");
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		this.timeout = timeout;
		this.stopTime = this.timeout - this.timeoutBuffer;
		System.out.println(">>Search time: " + (this.stopTime - System.currentTimeMillis()) + " ms");
		return getNextMove();
	}

	@Override
	public void stateMachineStop() {}

	@Override
	public void stateMachineAbort() {}

}
