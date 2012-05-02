package apps.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import player.gamer.statemachine.StateMachineGamer;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.StateMachine;
import util.statemachine.implementation.prover.ProverStateMachine;

public class Engine extends StateMachineGamer {
    // Those levels in the tree which have not been expanded.
    List<TreeLevel> levelsToExpand;
    
    // A depth in a BFS search (i.e. a TreeLevel is all the GameNodes at depth
    // N in a BFS for some N
    private class TreeLevel
    {
	TreeLevel()
	{
	    this.nodesToExpand = new ArrayList<GameNode>();
	}
	
	void addNode(GameNode node) {
	    this.nodesToExpand.add(node);
	}
	
	List<GameNode> nodesToExpand;
	boolean finished;
    }
    
    // A node in the search tree. Note that this is *not* isomorphic to a
    // MachineState. Multiple GameNodes can have the same MachineState. This
    // is because arriving in a MachineState through different paths may let
    // heuristics evaluate the situation differently.
    
    // Note: We never store a MachineState in this class; this lets us move all
    // the caching decisions elsewhere.
    private class GameNode
    {
	GameNode(GameNode parent, int moveNum)
	{
	    this.parent = parent;
	    this.moveNum = moveNum;
	}
	
	// Which GameNode led to us
	GameNode parent;
	// Which move number in the parent's MachineState led to us 
	int moveNum;
	
	// The children of this GameNode.
	List<GameNode> children;
    }

    @Override
    public String getName() {
	return "PGGGPPG";
    }

    @Override
    public StateMachine getInitialStateMachine() {
	return new ProverStateMachine();
    }
    
    public Engine()
    {
	this.levelsToExpand = new ArrayList<TreeLevel>();
    }
    
    public Move stateMachineSelectMove(long timeout) {
	MachineState currentState = super.getCurrentState();
	GameNode rootNode = new GameNode(null, 0);
	TreeLevel rootLevel = new TreeLevel();
	rootLevel.addNode(rootNode);
	levelsToExpand.add(rootLevel);
	
	expandNode(rootNode, 1);
	return null;
    }
    
    void expandNode(GameNode node, int intoLevel)
    {
	
    }
    
    /*
    @Override
    public Move stateMachineSelectMove(long timeout)
	    throws MoveDefinitionException, TransitionDefinitionException {
	try {
	    Move opt = minimaxMove(getCurrentState());
	    System.out.println(">> cache size: " + stateCache.size());
	    return opt;
	} catch (Exception e) {
	    e.printStackTrace();
	}

	// shouldn't happen, but just in case, return a random move
	return getStateMachine().getRandomMove(getCurrentState(), getRole());
    }
    */

    private int minimaxValue(MachineState state) throws Exception {
	// already computed
	if (stateCache.containsKey(state)) {
	    return stateCache.get(state);
	}

	// terminal state
	if (getStateMachine().isTerminal(state)) {
	    int goal = getStateMachine().getGoal(state, getRole());
	    stateCache.put(state, goal);
	    return goal;
	}

	// get moves and potential new states
	Map<Move, List<MachineState>> moveStates = getStateMachine()
		.getNextStates(state, getRole());
	// BEGIN HW EX
	numStatesExpanded += moveStates.size();
	// END HW EX

	int max = -1;
	for (Move move : moveStates.keySet()) {
	    // minimize
	    int min = -1;
	    for (MachineState nextState : moveStates.get(move)) {
		int value = minimaxValue(nextState);
		if (min < 0 || value < min) {
		    min = value;
		}
	    }

	    // maximize
	    if (max < 0 || min > max) {
		max = min;
	    }
	}

	stateCache.put(state, max);
	return max;
    }

    /**
     * Wrapper to evaluate legal moves and compute opt via minimaxValue.
     * 
     * @param state
     * @return
     * @throws Exception
     */
    private Move minimaxMove(MachineState state) throws Exception {
	int max = -1;
	Move optMove = null;

	numStatesExpanded = 0;
	stateCache.clear();

	Map<Move, List<MachineState>> moveStates = getStateMachine()
		.getNextStates(state, getRole());
	numStatesExpanded += moveStates.size();

	for (Move move : moveStates.keySet()) {
	    // minimize
	    int min = -1;
	    for (MachineState nextState : moveStates.get(move)) {
		int value = minimaxValue(nextState);
		if (min < 0 || value < min) {
		    min = value;
		}
	    }

	    // maximize
	    if (max < 0 || min > max) {
		max = min;
		optMove = move;
	    }
	}
	System.out.println("Expanded " + numStatesExpanded + " states.");
	return optMove;
    }

    /**
     * Does nothing for the metagame
     */
    @Override
    public void stateMachineMetaGame(long timeout) {
	// Do nothing.
    }

    @Override
    public void stateMachineStop() {
	// Do nothing.
    }

    @Override
    public void stateMachineAbort() {
	// Do nothing
    }
}
