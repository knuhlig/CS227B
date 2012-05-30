package apps.pgggppg;

public class Outcome {

	public double score;
	public int depth;
	
	public Outcome(double score, int depth) {
		this.score = score;
		this.depth = depth;
	}
	
	public boolean isBetterThan(Outcome o) {
		if (score != o.score) {
			return score > o.score;
		}
		return depth < o.depth;
	}
	
	public boolean isWorseThan(Outcome o) {
		if (score != o.score) {
			return score < o.score;
		}
		return depth < o.depth;
	}
	
	@Override
	public String toString() {
		return "(" + score + " at depth " + depth + ")";
	}
}
