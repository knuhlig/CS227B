package apps.team;

import java.util.ArrayList;
import java.util.List;

import player.gamer.Gamer;
import player.gamer.exception.MetaGamingException;
import player.gamer.exception.MoveSelectionException;
import util.game.Game;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlSentence;
import util.match.Match;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import util.statemachine.implementation.prover.ProverStateMachine;

public class RandomGamer extends Gamer {

	private Match match;
	private Role role;
	private StateMachine stateMachine;
	private MachineState currentState;
	
	@Override
	public String getName() {
		return "PGGGPPG Random";
	}
	
	@Override
	public boolean start(String matchId, GdlProposition roleName, Game game, 
		int startClock, int playClock, long receptionTime) throws MetaGamingException {

		// create the match
		match = new Match(matchId, startClock, playClock, game);
		
		// create the state machine for move computation
		stateMachine = new ProverStateMachine();
		stateMachine.initialize(game.getRules());
		currentState = stateMachine.getInitialState();
		
		// set our player's role
		role = stateMachine.getRoleFromProp(roleName);
		return true;
	}

	@Override
	public GdlSentence play(String matchId, List<GdlSentence> moves,
			long receptionTime) throws MoveSelectionException,
			TransitionDefinitionException, MoveDefinitionException {
		
		// make sure we have the right match
		if (!match.getMatchId().equals(matchId)) {
			throw new RuntimeException("match ID is wrong!");
		}
		
		// add move history to match
		if (moves != null) {
			match.appendMoves(moves);
		}
		
		// update the current state
		List<GdlSentence> list = match.getMostRecentMoves();
		if (list != null) {
			// build a list of Moves
			List<Move> moveList = new ArrayList<Move>();
			for (GdlSentence gdl: match.getMostRecentMoves()) {
				moveList.add(stateMachine.getMoveFromSentence(gdl));
			}
			
			// update game state
			currentState = stateMachine.getNextState(currentState, moveList);
			match.appendState(currentState.getContents());
		}
		
		// compute legal moves
		List<Move> legalMoves = stateMachine.getLegalMoves(currentState, role);
		
		// return a random move
		int randIdx = (int)(Math.random() * legalMoves.size());
		return legalMoves.get(randIdx).getContents();
	}

	@Override
	public boolean stop(String matchId, List<GdlSentence> moves) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean ping() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean abort(String matchId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Match getMatch(String matchId) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
