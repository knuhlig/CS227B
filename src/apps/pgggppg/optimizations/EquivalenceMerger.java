package apps.pgggppg.optimizations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import apps.team.Pair;


// Merges equivalent nodes (i.e. those which are of the same type and have the
// same inputs

public class EquivalenceMerger extends Optimization {
	Map<Pair<Class, Set<Component>>, Set<Component>> componentsByInput;

	public EquivalenceMerger(PropNet propnet) {
		super(propnet);
	}

	Set<Component> getEquivalents(Component c) {
		Pair<Class, Set<Component>> pair = new Pair<Class, Set<Component>>(c.getClass(), c.getInputs());
		Set<Component> equivalenceClass = componentsByInput.get(pair);
		if (equivalenceClass == null) {
			equivalenceClass = new HashSet<Component>();
			componentsByInput.put(pair, equivalenceClass);
		}
		return equivalenceClass;
	}
	
	// Merges two components
	void mergeTwo(Component c1, Component c2) {
		for (Component c : c2.getOutputs()) {
			getEquivalents(c).remove(c);
			c1.addOutput(c);
			c.removeInput(c2);
			c.addInput(c1);
		}
		for (Component c : c2.getOutputs()) {
			getEquivalents(c).add(c);
		}
		
		propnet.removeComponent(c2);
	}

	// Merge the given input components (which should all have the same inputs) returns the merged result
	Component mergeEquivalents(Set<Component> components) {
		Iterator<Component> it = components.iterator();
		Component representative = it.next();
		while (it.hasNext()) {
			mergeTwo(representative, it.next());
		}
		return representative;
	}

	public void runPass() {
		// Set up the initial equivalence classes
		componentsByInput = new HashMap<Pair<Class, Set<Component>>, Set<Component>>();
		for (Component c : propnet.getComponents()) {
			// ignore special components altogether
			if (!isSpecial(c)) {
				getEquivalents(c).add(c);
			}
		}
		
		Set<Component> componentsToConsider = new HashSet<Component>();
		for (Component c : propnet.getComponents()) {
			if (!isSpecial(c)) {
				componentsToConsider.add(c);
			}
		}

		while (!componentsToConsider.isEmpty()) {
			// Find an equivalence set to merge
			Component c = componentsToConsider.iterator().next();
			componentsToConsider.remove(c);

			Set<Component> equivalentComponents = getEquivalents(c);
			componentsToConsider.removeAll(equivalentComponents);

			Component result = mergeEquivalents(equivalentComponents);
			for (Component output : result.getOutputs()) {
				if (!isSpecial(output)) {
					componentsToConsider.add(output);
				}
			}
		}
	}
}
