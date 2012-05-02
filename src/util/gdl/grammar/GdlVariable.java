package util.gdl.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public final class GdlVariable extends GdlTerm
{
	private final String name;

	GdlVariable(String name)
	{
		this.name = name.intern();
	}

	public String getName()
	{
		return name;
	}

	@Override
	public boolean isGround()
	{
		return false;
	}
	
	@Override
	public void getDependencies(Set<String> types) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public GdlSentence toSentence()
	{
		throw new RuntimeException("Unable to convert a GdlVariable to a GdlSentence!");
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public GdlVariable replace(Map<Gdl, Gdl> replacementMap) {
		return this;
	}

	@Override
	public List<Gdl> getChildren() {
		List<Gdl> result = new ArrayList<Gdl>();
		return result;
	}
}
