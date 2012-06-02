package apps.team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import player.gamer.statemachine.StateMachineGamer;
import util.gdl.grammar.Gdl;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.propnet.PropNetStateMachine;

public class UctGamer extends StateMachineGamer {
	
	private static final int GOAL_MIN = 0;
	private static final int GOAL_MAX = 100;
	
	// configurable
	private long timeoutBuffer = 1000;
	
	// caches
	private Map<Pair<Role, MachineState>, Map<Move,List<MachineState>>> transitionCache;
	private Map<Pair<Role, Pair<MachineState,Move>>,Double> scoreCache;
	private Map<Pair<Role, Pair<MachineState,Move>>,Double> countCache;
	private Map<MachineState,Integer> stateCounts;
	
	// in-game temporary state
	private long timeout;
	private boolean depthLimited;
	
	// UCT parameter
	private Double C = 5.0;
	private Double gamma = 0.99;
	
	// For debugging purposes
	private Map<Pair<Role, Pair<MachineState,Move>>, List<List<List<Move>>>> utcRuns;
	
	@Override
	public String getName() {
		return "PGGGPPG UCT Gamer";
	}
	
	public void reset() {
		transitionCache = new HashMap<Pair<Role, MachineState>, Map<Move,List<MachineState>>>();
		scoreCache = new HashMap<Pair<Role, Pair<MachineState,Move>>,Double>();
		countCache = new HashMap<Pair<Role, Pair<MachineState,Move>>,Double>();
		stateCounts = new HashMap<MachineState,Integer>();
		utcRuns = new HashMap<Pair<Role, Pair<MachineState,Move>>, List<List<List<Move>>>>();
	}
	
	@Override
	public StateMachine getInitialStateMachine() {
		// initialize
		reset();
		List<Gdl> rules = getMatch().getGame().getRules();
		return new PropNetStateMachine();
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
	
		
	public Pair<List<MachineState>,List<List<Move>>> depthChargeUCT(MachineState state) throws Exception{
		List<MachineState> statePath = new ArrayList<MachineState>();
		List<List<Move>> moveList = new ArrayList<List<Move>>();
		do{
			// maybe only do this every n-th iteration?
        	if (System.currentTimeMillis() + timeoutBuffer >= timeout) {
        		throw new RuntimeException("search timeout");
        	}
			
			List<Move> turnMoves = getStateMachine().getRandomJointMove(state);
			statePath.add(state);
			moveList.add(turnMoves);
			state = getStateMachine().getNextState(state,turnMoves);
			
		}while(!isTerminal(state));
		statePath.add(state);
		return new Pair<List<MachineState>,List<List<Move>>>(statePath, moveList);
	}
	
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
		    /*
			if(unexplored){
				System.out.println(role + "is picking an unexplored move");
			}
			System.out.println(role + " is choosing move with score: " + bestScore);
			*/
			return bestMove;
		}
		catch (Exception e) {
			System.out.println("Something wrong in selectMove: " + e.getMessage());
		}
		// should never get here
		return null;
	}
	
	Pair<List<Double>,List<List<Move>>> search(MachineState state){
		try{
			if(stateCounts.get(state) == null){
				stateCounts.put(state,1);		
			} else{
				stateCounts.put(state,stateCounts.get(state)+1);
			}
			
			if(isTerminal(state)){
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
				playerMoves.add(selectMove(r,state));
			}

			MachineState nextState = getStateMachine().getNextState(state, playerMoves);
			
			// Recurse on next state
			Pair<List<Double>,List<List<Move>>> ret = search(nextState);
			List<Double>scores = ret.fst;
			List<List<Move>> movesList = ret.snd;
			movesList.add(playerMoves);
			
			// Do some backoff to favor closer goals
			for(int i=0; i<scores.size(); i++)
				scores.set(i, gamma*scores.get(i));

			int i=0;
			// Update the scores and counts
			for (Role r : getStateMachine().getRoles()) {
				Move curMove = playerMoves.get(i);
				Pair<Role, Pair<MachineState,Move>> key = new Pair<Role, Pair<MachineState,Move>>(r, new Pair<MachineState,Move>(state,curMove));
				if(scoreCache.get(key) == null){
					scoreCache.put(key,new Double(scores.get(i)));
					countCache.put(key, 1.0);
					/* Debug
					if(utcRuns.get(key)==null){
						List<List<List<Move>>> l = new ArrayList<List<List<Move>>>();
						List<List<Move>> starter = new ArrayList<List<Move>>();
						starter.add(playerMoves);
						l.add(starter);
						utcRuns.put(key,l);
					}else{
						List<List<List<Move>>> l = utcRuns.get(key);
						List<List<Move>> tempList = new ArrayList<List<Move>>();
						for(int j=0; j<movesList.size(); j++)
							tempList.add(movesList.get(j));
						Collections.reverse(tempList);
						l.add(tempList);
						utcRuns.put(key,l);
					}
					// Debug */
				}else{
					Double n = countCache.get(key);
					// update the average score
					scoreCache.put(key,scoreCache.get(key)*(n/(n+1.0)) + scores.get(i)/(n+1.0) );
					countCache.put(key,n+1.0);
					/* Debug
					if(utcRuns.get(key)==null){
						List<List<List<Move>>> l = new ArrayList<List<List<Move>>>();
						List<List<Move>> starter = new ArrayList<List<Move>>();
						starter.add(playerMoves);
						l.add(starter);
						utcRuns.put(key,l);
					}else{
						List<List<List<Move>>> l = utcRuns.get(key);
						List<List<Move>> tempList = new ArrayList<List<Move>>();
						for(int j=0; j<movesList.size(); j++)
							tempList.add(movesList.get(j));
						Collections.reverse(tempList);
						l.add(tempList);
						utcRuns.put(key,l);
					}
					// Debug */
				}
				i++;
			}
			
			return new Pair<List<Double>,List<List<Move>>>(scores, movesList);
		}
		catch (Exception e) {
			System.out.println("Something wrong in search: " + e.getMessage());
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
				if(count > 200)
					break;
				count++;
				search(state);	
			}
			
			// Select the move that has the best cached score
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
				/* DEBUG
				if(bestMove.toString() == "noop")
					return bestMove;
				
				// print out all of the trials for this move
				
				Pair<Role, Pair<MachineState,Move>> firstKey = new Pair<Role, Pair<MachineState,Move>>(role, new Pair<MachineState,Move>(state,bestMove));
				List<List<List<Move>>> runs = utcRuns.get(firstKey);
				
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
				// DEBUG */
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
		
		long startTime = System.currentTimeMillis();
		long duration = timeout - startTime;

		this.timeout = startTime + duration;
		try{
			MachineState start = getStateMachine().getInitialState();
			Integer n = 0;
			// no metagaming right no (switch to true to turn on)
			
			while(true){
				search(start);
				if(System.currentTimeMillis() > this.timeout){
					break;
				}
				n++;
			}
			System.out.println("Performed " + n + " UCT depth charges in " + duration + " milliseconds.");
			
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
		return getNextMove();
	}

	@Override
	public void stateMachineStop() {}

	@Override
	public void stateMachineAbort() {}

}
