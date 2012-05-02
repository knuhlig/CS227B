package util.gdl.grammar;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public abstract class Gdl implements Serializable
{
	public static final String T_DOES = "does";
	public static final String T_TRUE = "true";

	public abstract boolean isGround();

	@Override
	public abstract String toString();
	
	public abstract Gdl replace(Map<Gdl, Gdl> replacementMap);
	
	public abstract List<Gdl> getChildren();
	
	public abstract void getDependencies(Set<String> types);
	
	protected static Gdl applyReplacement(Gdl gdl, Map<Gdl, Gdl> replacementMap) {
		if (replacementMap.containsKey(gdl)) {
			return replacementMap.get(gdl);
		} else {
			Gdl cloneCandidate = gdl.replace(replacementMap);
			if (cloneCandidate == gdl) return gdl;
			else return cloneCandidate;
		}
	}
	
}
