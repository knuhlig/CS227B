package player.gamer;

import java.util.List;

import player.gamer.exception.MetaGamingException;
import player.gamer.exception.MoveSelectionException;
import util.game.Game;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlSentence;
import util.match.Match;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;
import apps.player.config.ConfigPanel;
import apps.player.config.EmptyConfigPanel;
import apps.player.detail.DetailPanel;
import apps.player.detail.EmptyDetailPanel;

/**
 * The Gamer class defines methods for both meta-gaming and move selection in a
 * pre-specified amount of time. The Gamer class is based on the <i>algorithm</i>
 * design pattern.
 */
public abstract class Gamer
{	

	public Gamer()
	{
	}

	/* The following values are recommendations to the implementations
	 * for the minimum length of time to leave between the stated timeout
	 * and when you actually return from metaGame and selectMove. They are
	 * stored here so they can be shared amongst all Gamers. */
    public static final long PREFERRED_METAGAME_BUFFER = 3900;
    public static final long PREFERRED_PLAY_BUFFER = 1900;    
	
	// ==== The Gaming Algorithms ====
	public abstract boolean start(String matchId, GdlProposition roleName, Game game, 
			int startClock, int playClock, long receptionTime) throws MetaGamingException;
	
	public abstract GdlSentence play(String matchId, List<GdlSentence> moves, long receptionTime)
			throws MoveSelectionException, TransitionDefinitionException, MoveDefinitionException;
	
	public abstract boolean stop(String matchId, List<GdlSentence> moves);
		
	/** 
	 * @return true if the Gamer is ready to receive a start message for a new game
	 */
	public abstract boolean ping();
	
	public abstract boolean abort(String matchId);
	
	// ==== Gamer Profile and Configuration ====
	public String getName() {
		return this.getClass().getName();
	}
	
	public ConfigPanel getConfigPanel() {
		return new EmptyConfigPanel();
	}
	
	public DetailPanel getDetailPanel() {
		return new EmptyDetailPanel();
	}

	public abstract Match getMatch(String matchId);

	public abstract void abortAll();
	
}