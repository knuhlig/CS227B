package apps.team.game;

import java.util.ArrayList;
import java.util.List;

import apps.team.graph.DependencyGraph;

public class GameCondition implements GameConstants {
	
	private List<LiteralCondition> literals = new ArrayList<LiteralCondition>();
	private List<DistinctCondition> distinct = new ArrayList<DistinctCondition>();
	
	public GameCondition() {
		
	}
	
	public GameCondition(GameCondition copy) {
		literals = new ArrayList<GameCondition.LiteralCondition>(copy.literals);
		distinct = new ArrayList<GameCondition.DistinctCondition>(copy.distinct);
	}
	
	public GameCondition substitute(String varName, GameObject obj) {
		return this;
	}
	
	public void addDistinct(GameObject a, GameObject b, boolean isNegated) {
		DistinctCondition d = new DistinctCondition();
		d.a = a;
		d.b = b;
		d.isNegated = isNegated;
		distinct.add(d);
	}
	
	public void addCondition(GameObject obj, int type, boolean isNegated) {
		LiteralCondition cond = new LiteralCondition();
		cond.obj = obj;
		cond.isNegated = isNegated;
		cond.type = type;
		literals.add(cond);
	}
	
	private static class DistinctCondition {
		public GameObject a;
		public GameObject b;
		public boolean isNegated;
		
		@Override
		public String toString() {
			return (isNegated? "~" : "") + "distinct:" + a + ":" + b;
		}
	}
	
	private static class LiteralCondition {
		public GameObject obj;
		public boolean isNegated;
		public int type;
		
		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			if (isNegated) b.append("~");
			switch (type) {
			case COND_DOES:
				b.append("does:");
				break;
			case COND_LEGAL:
				b.append("legal:");
				break;
			case COND_ROLE:
				b.append("role:");
				break;
			case COND_TRUE:
				b.append("true:");
				break;
			case COND_GOAL:
				b.append("goal:");
				break;
			}
			b.append(obj);
			return b.toString();
		}
	}
	
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (DistinctCondition d: distinct) {
			b.append(d + " ");
		}
		for (LiteralCondition literal: literals) {
			b.append(literal + " ");
		}
		return b.toString();
	}
	
	public void addDependencies(DependencyGraph g, String dst) {
		for (LiteralCondition literal: literals) {
			String depName = null;
			String depType = null;
			switch (literal.type) {
				case COND_DOES: 
					depName = literal.obj.getChild(1).getType();
					depType = T_DOES; 
					break;
				case COND_LEGAL: 
					depName = literal.obj.getType();
					depType = T_LEGAL; 
					break;
				case COND_ROLE: 
					depName = depType = T_ROLE; 
					break;
				case COND_TRUE:
					depName = literal.obj.getType();
					depType = T_TRUE; 
					break;
				case COND_INTERNAL: 
					depName = literal.obj.getType(); 
					depType = T_FACT;
					break;
				case COND_GOAL:
					depName = T_GOAL;
					depType = T_GOAL;
					break;
				default: throw new RuntimeException("unknown type: " + literal.type);
			}
			String src = depType + ":" + depName;
			g.addEdge(src, dst, literal.isNegated ? true : null);
		}
	}
}
