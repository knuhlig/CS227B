package util.gdl.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public final class GdlConstant extends GdlTerm
{

	private final String value;

	GdlConstant(String value)
	{
		this.value = value.intern();
	}

	public String getValue()
	{
		return value;
	}
	
	@Override
	public void getDependencies(Set<String> types) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isGround()
	{
		return true;
	}

	@Override
	public GdlSentence toSentence()
	{
		return GdlPool.getProposition(this);
	}

	@Override
	public String toString()
	{
		return value;
	}

	@Override
	public GdlConstant replace(Map<Gdl, Gdl> replacementMap) {
		return this;
	}

	@Override
	public List<Gdl> getChildren() {
		return new ArrayList<Gdl>();
	}

}
