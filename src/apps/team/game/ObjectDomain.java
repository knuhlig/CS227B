package apps.team.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class ObjectDomain {

	private List<Set<String>> args = new ArrayList<Set<String>>();
	
	public void add(int index, String value) {
		while (index >= args.size()) {
			args.add(new HashSet<String>());
		}
		Set<String> argDomain = args.get(index);
		if (!argDomain.contains(value)) {
			argDomain.add(value);
		}
	}
	
	public int arity() {
		return args.size();
	}

	public Set<String> getDomain(int idx) {
		if (idx >= args.size()) {
			return new HashSet<String>();
		}
		return args.get(idx);
	}
	
	public void addAll(int index, Collection<String> values) {
		for (String value: values) {
			add(index, value);
		}
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < args.size(); i++) {
			if (i > 0) {
				b.append(" ");
			}
			b.append("{");
			int count = 0;
			for (String value: args.get(i)) {
				if (count > 0) {
					b.append(", ");
				}
				b.append(value);
				count++;
			}
			b.append("}");
		}
		return b.toString();
	}

}
