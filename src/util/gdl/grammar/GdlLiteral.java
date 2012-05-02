package util.gdl.grammar;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public abstract class GdlLiteral extends Gdl
{
	@Override
	public abstract boolean isGround();

	@Override
	public abstract String toString();
	
	public abstract boolean isMoveIndependent();

	@Override
	public abstract GdlLiteral replace(Map<Gdl, Gdl> replacementMap);
	
	public abstract void computeBindings(Map<String, Set<String>> bindings, Map<String, List<Set<String>>> typeValues);
	
}
