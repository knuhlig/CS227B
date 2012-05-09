package apps.player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import player.GamePlayer;
import player.gamer.Gamer;
import util.reflection.ProjectSearcher;

public class ConsolePlayer {

	private static Map<String, Class<Gamer>> gamers;

	private static List<String> getParameters (String[] args, String flag) { 
		List<String> result = new ArrayList<String> ();
		
		boolean found = false;
		for (int i = 0; i < args.length; i++) {
			if (!found && args[i].equals(flag)) {
				found = true;
			} else if (found && args[i].charAt(0) != '-') {
				result.add(args[i]);
			} else if (found) {
				break;
			}
		}
		
		return result;
	}
	
	enum Mode {
		PLAYER_MODE,
		LIST_MODE,
		HELP_MODE
	}
	
	/**
	 * @param args
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IOException {
		
		List<Class<?>> gamerClasses = ProjectSearcher.getAllClassesThatAre(Gamer.class);
		
		gamers = new HashMap<String, Class<Gamer>>();
		for (Class<?> c : gamerClasses) {
			if (c.getCanonicalName().contains("kiosk")) continue;
			if (c.getCanonicalName().contains("human")) continue;
			try {
				Gamer g = (Gamer)c.newInstance();
				gamers.put(g.getName(), (Class<Gamer>)c);
			} catch (InstantiationException e){
				System.out.println("Could not instantiate " + c);
			}
		}
		
		Mode mode = Mode.PLAYER_MODE;
		boolean containsGamerFlag = false;
		for (String s : args) {
			if (s.equals("-l")) mode = Mode.LIST_MODE;
			if (s.equals("-h")) mode = Mode.HELP_MODE;
			if (s.equals("-g")) containsGamerFlag = true;
		}
		
		if (!containsGamerFlag && mode == Mode.PLAYER_MODE) {
			mode = Mode.HELP_MODE;
		}
		
		switch (mode) {
		case PLAYER_MODE:
			playerMode(args);
			break;
		case LIST_MODE:
			for (String g : gamers.keySet()) {
				System.out.println(g);
			}
			break;
		case HELP_MODE:
			System.out.println("-l     lists the gamers available");
			System.out.println("-p     specify the port number");
			System.out.println("-g     specify the gamer");
			System.out.println("-r     specify the registration server");
			System.out.println();
			break;
		}
	}

	private static void playerMode(String[] args) throws InstantiationException, IllegalAccessException, IOException {
		// Get arguments
		List<String> portsRaw = getParameters(args, "-p");
		List<String> gamersRaw = getParameters(args, "-g");
		List<String> registrationRaw = getParameters(args, "-r");
		
		List<Integer> ports = new ArrayList<Integer>();
		for (String portString : portsRaw) {
			try {
				int port = Integer.parseInt(portString);
				ports.add(port);
			} catch (NumberFormatException e) {
				System.out.println("Invalid port " + portString);
				System.exit(1);
			}
		}
		
		List<Gamer> gamersObjects = new ArrayList<Gamer>();
		for (String gamerName : gamersRaw) {
			if (!gamers.containsKey(gamerName)) {
				System.out.println("Invalid gamer name " + gamerName);
				System.exit(1);
			} else {
				gamersObjects.add(gamers.get(gamerName).newInstance());
			}
		}
		
		if (gamersObjects.size() == 0) {
			System.out.println("No gamers listed after -g option");
		}
		
		// Parse registration server
	   String regHost = null;
       int regPort = -1;
		
		if (registrationRaw.size() > 0) {
			String addressRaw = registrationRaw.get(0);
			String[] splitAddress = addressRaw.split(":");
			if (splitAddress.length > 1) {
				regHost = splitAddress[0];
				regPort = Integer.parseInt(splitAddress[1]);
			}
		}
		
		// Construct players
		int nextPort = GamePlayer.DEFAULT_PLAYER_PORT;
		for (int i = 0; i < gamersObjects.size(); i++) {
			int curPort;
			if (i < ports.size()) {
				curPort = ports.get(i);
			} else {
				curPort = nextPort;
			}
			
			GamePlayer newPlayer = new GamePlayer (curPort, gamersObjects.get(i));
			newPlayer.setRegistrationServer(regHost, regPort);
			newPlayer.start();
			nextPort = curPort + 1;
		}
	}

}
