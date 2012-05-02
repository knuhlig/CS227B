package util.gdl.grammar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
public final class GdlRelation extends GdlSentence
{

	private final List<GdlTerm> body;
	private transient Boolean ground;
	private final GdlConstant name;

	GdlRelation(GdlConstant name, List<GdlTerm> body)
	{
		this.name = name;
		this.body = body;
		ground = null;
	}

	@Override
	public int arity()
	{
		return body.size();
	}
	
	@Override
	public void getDependencies(Set<String> types) {
		for (GdlTerm term: body) {
			term.getDependencies(types);
		}
		String name = this.name.getValue();
		if (!name.equals(T_TRUE)) {
			types.add(name);
		}
	}

	private boolean computeGround()
	{
		for (GdlTerm term : body)
		{
			if (!term.isGround())
			{
				return false;
			}
		}

		return true;
	}
	
	@Override
	public boolean isMoveIndependent() {
		return !name.getValue().equals(T_DOES);
	}
	
	@Override
	public void computeBindings(Map<String, Set<String>> bindings,
			Map<String, List<Set<String>>> typeValues) {
		if (name.getValue().equals(T_TRUE)) {
			GdlTerm term = get(0);
			if (term instanceof GdlFunction) {
				((GdlFunction) term).computeBindings(bindings, typeValues);
			}
		} else {
			for (int i = 0; i < arity(); i++) {
				GdlTerm term = get(i);
				if (term instanceof GdlVariable) {
					String varName = ((GdlVariable) term).getName();
					Set<String> values = typeValues.get(name.getValue()).get(i);
					if (!bindings.containsKey(varName)) {
						bindings.put(varName, new HashSet<String>(values));
					} else {
						bindings.get(varName).retainAll(values);
					}
				}
			}
		}
	}

	@Override
	public GdlTerm get(int index)
	{
		return body.get(index);
	}

	@Override
	public GdlConstant getName()
	{
		return name;
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

		sb.append("( " + name + " ");
		for (GdlTerm term : body)
		{
			sb.append(term + " ");
		}
		sb.append(")");

		return sb.toString();
	}

	@Override
	public GdlFunction toTerm()
	{
		return GdlPool.getFunction(name, body);
	}
	
	@Override
	public List<GdlTerm> getBody()
	{
		return body;
	}

	@Override
	public GdlRelation replace(Map<Gdl, Gdl> replacementMap) {
		
		Gdl newName = Gdl.applyReplacement(this.name, replacementMap);
		
		List<GdlTerm> newBody = new ArrayList<GdlTerm>();
		boolean hasChanged = newName != this.name;
		for (GdlTerm literal : this.body) {
			Gdl newLiteral = Gdl.applyReplacement(literal, replacementMap);
			if (newLiteral != literal) hasChanged = true;
			newBody.add((GdlTerm) newLiteral);
		}
		
		if (hasChanged) {
			return GdlPool.getRelation((GdlConstant) newName, newBody);
		}
		
		return this;
		
	}
	
	@Override
	public List<Gdl> getChildren() {
		List<Gdl> result = new ArrayList<Gdl>();
		
		result.add(this.name);
		result.addAll(this.body);
		
		return result;
	}

}
