package apps.team;

import util.game.Game;
import util.game.GameRepository;

public class Test {
	
	public static void main(String[] args) {
		// name of the game to try out
		String gameName = "ticTacToe";
		
		GameRepository repository = GameRepository.getDefaultRepository(); 
		Game game = repository.getGame(gameName);
		
		
	}
}
