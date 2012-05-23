package apps.team.util;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class FileUtil {

	public static void writeToFile(String contents, String filename) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			out.write(contents + "\n");
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
