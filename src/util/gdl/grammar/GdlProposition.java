package util.gdl.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public final class GdlProposition extends GdlSentence
{

	private final GdlConstant name;

	public GdlProposition(GdlConstant name)
	{
		this.name = name;
	}

	@Override
	public int arity()
	{
		return 0;
	}

	@Override
	public GdlTerm get(int index)
	{
		throw new RuntimeException("GdlPropositions have no body!");
	}
	
	@Override
	public void getDependencies(Set<String> types) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public GdlConstant getName()
	{
		return name;
	}

	@Override
	public boolean isGround()
	{
		return name.isGround();
	}

	@Override
	public boolean isMoveIndependent() {
		return true;
	}
	
	@Override
	public void computeBindings(Map<String, Set<String>> bindings,
			Map<String, List<Set<String>>> typeValues) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String toString()
	{
		return name.toString();
	}

	@Override
	public GdlConstant toTerm()
	{
		return name;
	}

	@Override
	public List<GdlTerm> getBody() {
		throw new RuntimeException("GdlPropositions have no body!");
	}

	@Override
	public GdlProposition replace(Map<Gdl, Gdl> replacementMap) {
		Gdl newName = Gdl.applyReplacement(this.name, replacementMap);
		
		if (newName != this.name) {
			return GdlPool.getProposition((GdlConstant) newName);
		}
		
		return this;
	}
	
	@Override
	public List<Gdl> getChildren() {
		List<Gdl> result = new ArrayList<Gdl>();
		result.add(this.name);
		return result;
	}

}
