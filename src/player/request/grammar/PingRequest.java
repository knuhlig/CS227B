package player.request.grammar;

import player.gamer.Gamer;

public final class PingRequest extends Request
{
	private final Gamer gamer;

	public PingRequest(Gamer gamer)
	{
		this.gamer = gamer;
	}
	
	@Override
	public String getMatchId() {
		return null;
	}
	
	static public String PING_SUCCESS = "available";
	static public String PING_FAILURE = "busy";

	@Override
	public String process(long receptionTime)
	{
	    return (gamer.ping()) ? PING_SUCCESS : PING_FAILURE;
	}

	@Override
	public String toString()
	{
		return "ping";
	}
}