package apps.team.game;

import java.util.ArrayList;
import java.util.List;

public class GameObject {

	public static GameObject constant(String value) {
		return new GameObject(value);
	}
	
	private String value;
	private List<GameObject> children;
	
	public GameObject(String value) {
		this.value = value;
	}
	
	public void addChild(GameObject child) {
		if (children == null) {
			children = new ArrayList<GameObject>();
		}
		children.add(child);
	}
	
	public GameObject getChild(int index) {
		if (children == null) {
			return null;
		}
		return children.get(index);
	}
	
	public String getValue() {
		return value;
	}
	
	public String getType() {
		return value;
	}
	
	public boolean isAtom() {
		return children == null;
	}
	
	public boolean isVariable() {
		return value.startsWith("?");
	}
	
	public boolean isGround() {
		if (isVariable()) return false;
		if (isAtom()) return true;
		for (GameObject child: children) {
			if (!child.isGround()) return false;
		}
		return true;
	}
	
	public int arity() {
		if (children == null) {
			return 0;
		}
		return children.size();
	}
	
	public GameObject substitute(String varName, GameObject replacement) {
		if (isAtom()) {
			if (value.equals(varName)) {
				return replacement;
			}
			return this;
		}
		GameObject sub = new GameObject(value);
		for (int i = 0; i < arity(); i++) {
			sub.addChild(getChild(i).substitute(varName, replacement));
		}
		return sub;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof GameObject)) return false;
		return toString().equals(((GameObject) obj).toString());
	}
	
	@Override
	public String toString() {
		if (isAtom()) return value;
		StringBuilder b = new StringBuilder();
		b.append("(" + value);
		for (int i = 0; i < children.size(); i++) {
			b.append(" ");
			b.append(getChild(i));
		}
		b.append(")");
		return b.toString();
	}
}
