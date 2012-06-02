package apps.pgggppg.optimizations;

import java.util.HashSet;
import java.util.Set;

import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;

public class PassthroughNodeRemover extends Optimization {
	
	public PassthroughNodeRemover(PropNet propnet) {
		super(propnet);
	}
	
	public boolean isPassthrough (Component c) {
		return !isSpecial(c) && (c instanceof Proposition || c instanceof And || c instanceof Or) && c.getInputs().size() == 1;
	}
	
	// Removes propositions that have only a single input (and which therefore can be eliminated;
	// just have their input pass straight to the output
	public void runPass() {
		Set<Component> components = new HashSet<Component>(propnet.getComponents());
		
		for (Component c : components) {
			if (isPassthrough(c)) {
				propnet.removeComponent(c);
				Component input = c.getSingleInput();
				for (Component output : c.getOutputs()) {
					output.addInput(input);
					input.addOutput(output);
				}
			}
		}
	}
}
