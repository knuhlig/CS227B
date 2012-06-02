package util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.game.Game;
import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.Constant;
import util.propnet.architecture.components.Proposition;
import util.propnet.factory.OptimizingPropNetFactory;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.query.ProverQueryBuilder;
import apps.pgggppg.compilation.NativePropNetStateMachine;
import apps.pgggppg.optimizations.PassthroughNodeRemover;
import apps.team.game.GameLoader;

@SuppressWarnings("unused")
public class PropNetStateMachine extends StateMachine {
	
	public static void main(String[] args) {
		GameLoader loader = new GameLoader();
		System.out.println(loader.getAvailableGames());
		Game game = loader.loadGdlGame("lightsOnParallel");
		PropNetStateMachine machine = new PropNetStateMachine();
		machine.initialize(game.getRules());
	}

	/** The underlying proposition network  */
	private PropNet propNet;
	/** The topological ordering of the propositions */
	private List<Proposition> ordering;
	private List<Component> componentOrdering;

	/** The player roles */
	private List<Role> roles;

	/**
	 * Initializes the PropNetStateMachine. You should compute the topological
	 * ordering here. Additionally you may compute the initial state here, at
	 * your discretion.
	 */
	@Override
	public void initialize(List<Gdl> description) {
		try {
			propNet = OptimizingPropNetFactory.create(description);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		propNet.renderToFile("/Users/knuhlig/Desktop/unopt.dot");
		//new DeadNodeEliminator(propNet).runPass();
		//new NotSquasher(propNet).runPass();
		//new DeadNodeEliminator(propNet).runPass();
		//new PassthroughNodeRemover(propNet).runPass();
		//new DeadNodeEliminator(propNet).runPass();
		//new EquivalenceMerger(propNet).runPass();
		//new DeadNodeEliminator(propNet).runPass();
		new PassthroughNodeRemover(propNet).runPass();
		//new DeadNodeEliminator(propNet).runPass();
		System.out.println("made aaaas!");
		propNet.renderToFile("/Users/knuhlig/Desktop/opt.dot");
		System.out.println("made it!");
		
		roles = propNet.getRoles();
		componentOrdering = new ArrayList<Component>();
		ordering = getOrdering(componentOrdering);
	}


	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public boolean isTerminal(MachineState state) {
		//clear();
		setState(state);
		return propNet.getTerminalProposition().getValue();
	}

	/**
	 * Computes the goal for a role in the current state.
	 * Should return the value of the goal proposition that
	 * is true for that role. If there is not exactly one goal
	 * proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined. 
	 */
	@Override
	public int getGoal(MachineState state, Role role) throws GoalDefinitionException {
		setState(state);
		Set<Proposition> set = propNet.getGoalPropositions().get(role);
		int val = 0;
		boolean found = false;
		for (Proposition p: set) {
			if (p.getValue()) {
				if (found) {
					throw new GoalDefinitionException(state, role);
				}
				found = true;
				val = getGoalValue(p);
			}
		}
		return val;
	}

	/**
	 * Returns the initial state. The initial state can be computed
	 * by only setting the truth value of the INIT proposition to true,
	 * and then computing the resulting state.
	 */
	@Override
	public MachineState getInitialState() {
		for (Proposition p: propNet.getPropositions()) {
			p.setValue(false);
		}
		propNet.getInitProposition().setValue(true);

		Map<GdlTerm, Proposition> map = propNet.getBasePropositions();
		for (GdlTerm term: map.keySet()) {
			Proposition p = map.get(term);
			p.setValue(p.getSingleInput().getValue());
		}

		return getStateFromBase();
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
			throws MoveDefinitionException {
		setState(state);
		Set<Proposition> set = propNet.getLegalPropositions().get(role);
		List<Move> moves = new ArrayList<Move>();
		for (Proposition p: set) {
			if (p.getValue()) {
				moves.add(getMoveFromProposition(p));
			}
		}
		return moves;
	}

	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
			throws TransitionDefinitionException {
		setState(state, moves);		
		for (Proposition p: propNet.getBasePropositions().values()) {
			p.setValue(p.getSingleInput().getValue());
		}
		return getStateFromBase();
	}

	private void addOutputs(Component c, Set<Component> set) {
		for (Component output: c.getOutputs()) {
			set.add(output);
		}
	}

	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 * 
	 * The base propositions and input propositions should always be exempt
	 * from this ordering.
	 * 
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from
	 * the Moves that operations are performed on as well (if any).
	 * 
	 * @return The order in which the truth values of propositions need to be set.
	 */
	public List<Proposition> getOrdering(List<Component> componentOrdering)
	{
		// List to contain the topological ordering.
		List<Proposition> order = new ArrayList<Proposition>();

		// All of the components in the PropNet
		List<Component> components = new ArrayList<Component>(propNet.getComponents());

		// All of the propositions in the PropNet.
		List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

		Set<Component> marked = new HashSet<Component>();
		Set<Component> fringe = new HashSet<Component>();
		
		// constants
		for (Component term : propNet.getComponents()) {
			if (term instanceof Constant) {
				marked.add(term);
				addOutputs(term, fringe);
			}
		}

		// input
		Map<GdlTerm, Proposition> baseProps = propNet.getBasePropositions();
		for (GdlTerm term: baseProps.keySet()) {
			marked.add(baseProps.get(term));
			addOutputs(baseProps.get(term), fringe);
		}

		// moves
		baseProps = propNet.getInputPropositions();
		for (GdlTerm term: baseProps.keySet()) {
			marked.add(baseProps.get(term));
			addOutputs(baseProps.get(term), fringe);
		}
		// initial
		marked.add(propNet.getInitProposition());
		addOutputs(propNet.getInitProposition(), fringe);

		while (!fringe.isEmpty()) {
			Iterator<Component> it = fringe.iterator();
			Component c = it.next();
			it.remove();

			boolean ready = true;
			for (Component input: c.getInputs()) {
				if (!marked.contains(input)) {
					ready = false;
					break;
				}
			}

			if (ready) {
				marked.add(c);
				if (c instanceof Proposition) {
					order.add((Proposition) c);
				}
				componentOrdering.add(c);
				for (Component output: c.getOutputs()) {
					if (!marked.contains(output)) {
						fringe.add(output);
					}
				}
			}
		}
		return order;
	}

	/* Already implemented for you */
	@Override
	public List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 * 
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlTerms that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with 
	 * setting input values, feel free to change this for a more efficient implementation.
	 * 
	 * @param moves
	 * @return
	 */
	private List<GdlTerm> toDoes(List<Move> moves)
	{
		List<GdlTerm> doeses = new ArrayList<GdlTerm>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)).toTerm());
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().toSentence().get(1).toSentence());
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */	
	private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName().toSentence();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	private void setState(MachineState state, List<Move> moves) {
		propNet.getInitProposition().setValue(false);
		
		// Uncache everything
		for (Component c : propNet.getComponents()) {
			c.uncacheValue();
		}

		// set base propositions
		Set<GdlSentence> contents = state.getContents();
		for (Proposition p: propNet.getBasePropositions().values()) {
			if (contents.contains(p.getName().toSentence())) {
				p.setValue(true);
			} else {
				p.setValue(false);
			}
		}

		// set input propositions
		Set<GdlSentence> sentences = new HashSet<GdlSentence>();
		if (moves != null) {
			List<GdlTerm> terms = toDoes(moves);
			for (GdlTerm term: terms) {
				sentences.add(term.toSentence());
			}
		}
		for (Proposition p: propNet.getInputPropositions().values()) {
			if (sentences.contains(p.getName().toSentence())) {
				p.setValue(true);
			} else {
				p.setValue(false);
			}
		}

		// set inner propositions
		for (Proposition p: ordering) {
			p.setValue(p.getSingleInput().getValue());
		}
	}

	private void setState(MachineState state) {
		setState(state, null);
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */	
	public MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{
			p.setValue(p.getSingleInput().getValue());
			if (p.getValue())
			{
				contents.add(p.getName().toSentence());
			}

		}
		return new MachineState(contents);
	}

}