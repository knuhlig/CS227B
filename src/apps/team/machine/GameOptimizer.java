package apps.team.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import util.gdl.grammar.Gdl;

import apps.team.game.GameDescription;
import apps.team.game.GameLoader;

public class GameOptimizer {

	public static void main(String[] args) {
		String[] gameNames = new String[] {
				"knightsTour"
		};
		
		GameLoader loader = new GameLoader();
		//List<String> gameNames = new ArrayList<String>(new TreeSet<String>(loader.getAvailableGames()));
		
		GameOptimizer opt = new GameOptimizer();
		for (String name: gameNames) {
			try {
				GameDescription game = loader.loadGame(name);
				System.out.println("======= " + game.getName() + " =========");
				opt.optimize(game);
			} catch (Exception e) {
				System.err.println("error with " + name + ": " + e.getMessage());
				System.err.println("rules:");
				for (Gdl rule: loader.loadGdlGame(name).getRules()) {
					System.err.println(rule);
				}
				e.printStackTrace();
			}
		}
		System.out.println("done!");
	}
	
	
	public void optimize(GameDescription game) {
		//game.printInfo();
		game.analyze();
	}
	
}
