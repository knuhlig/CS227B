package util.gdl.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public final class GdlRule extends Gdl
{

	private final List<GdlLiteral> body;
	private transient Boolean ground;
	private final GdlSentence head;

	public GdlRule(GdlSentence head, List<GdlLiteral> body)
	{
		this.head = head;
		this.body = body;
		ground = null;
	}

	public int arity()
	{
		return body.size();
	}
	
	@Override
	public void getDependencies(Set<String> types) {
		for (GdlLiteral literal: body) {
			literal.getDependencies(types);
		}
	}

	private Boolean computeGround()
	{
		for (GdlLiteral literal : body)
		{
			if (!literal.isGround())
			{
				return false;
			}
		}

		return true;
	}

	public GdlLiteral get(int index)
	{
		return body.get(index);
	}

	public GdlSentence getHead()
	{
		return head;
	}
	
	public List<GdlLiteral> getBody()
	{
		return body;
	}

	@Override
	public boolean isGround()
	{
		if (ground == null)
		{
			ground = computeGround();
		}

		return ground;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("( <= " + head + " ");
		for (GdlLiteral literal : body)
		{
			sb.append(literal + " ");
		}
		sb.append(")");

		return sb.toString();
	}

	@Override
	public GdlRule replace(Map<Gdl, Gdl> replacementMap) {
		Gdl newHead = Gdl.applyReplacement(this.head, replacementMap);
		
		List<GdlLiteral> newBody = new ArrayList<GdlLiteral>();
		boolean hasChanged = newHead != this.head;
		for (GdlLiteral literal : this.body) {
			GdlLiteral newLiteral = (GdlLiteral) Gdl.applyReplacement(literal, replacementMap);
			if (newLiteral != literal) hasChanged = true;
			newBody.add(newLiteral);
		}
		
		if (hasChanged) {
			return GdlPool.getRule((GdlSentence) newHead, newBody);
		}
		
		return this;
	}
	
	@Override
	public List<Gdl> getChildren() {
		List<Gdl> result = new ArrayList<Gdl>();
		
		result.add(this.head);
		result.addAll(this.body);
		
		return result;
	}
}
