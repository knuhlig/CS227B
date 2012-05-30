package apps.team;

public class Score implements Comparable<Score> {

	
	private int score;
	private int depth;
	
	public Score() {
		
	}
	
	public Score(int score, int depth) {
		this.score = score;
		this.depth = depth;
	}
	
	public void setScore(int score) {
		this.score = score;
	}
	
	public int getScore() {
		return score;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	public int getDepth() {
		return depth;
	}
	
	@Override
	public int compareTo(Score s) {
		int res = score - s.score;
		return res;
		/*if (res != 0) {
			return res;
		}
		return s.depth - depth;*/
	}
	
	@Override
	public String toString() {
		return score + " @ " + depth;
	}
}
