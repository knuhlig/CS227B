package apps.pgggppg.optimizations;

import java.util.HashSet;
import java.util.Set;

import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Not;
import util.propnet.architecture.components.Or;


// Converts expressions like !x && !y && !z into !(x || y || z) and those like
// !x || !y || !z into !(x && y && z)
public class NotSquasher extends Optimization {

	public NotSquasher(PropNet propnet) {
		super(propnet);
	}

	// Given either an and or an or, squashes the nots
	void squashNots(Component junction, boolean isAnd) {
		Set<Component> notInputs = new HashSet<Component>();
		for (Component input : junction.getInputs()) {
			if (input instanceof Not) {
				notInputs.add(input);
			}
		}
		if (notInputs.size() <= 1) {
			return;
		}
		
		Not not = new Not();
		Component otherJunction;
		if (isAnd) {
			otherJunction = new Or();
		} else {
			otherJunction = new And();
		}
		
		// First we disconnect all the nots from the component
		for (Component notInput : notInputs) {
			notInput.removeOutput(junction);
			junction.removeInput(notInput);
		}
		
		// Add the input to the nots to the other junction
		for (Component notInput : notInputs) {
			notInput.getSingleInput().addOutput(otherJunction);
			otherJunction.addInput(notInput.getSingleInput());
		}
		
		// Connect the otherJunction to the not
		otherJunction.addOutput(not);
		not.addInput(otherJunction);
		
		// Now connect the not to the junction
		junction.addInput(not);
		not.addOutput(junction);
		
		propnet.addComponent(otherJunction);
		propnet.addComponent(not);
	}

	@Override
	public void runPass() {
		// We are going to modify the hashset, so make a copy first.
		// Note that we don't need to keep track of additional not squashing we
		// can do as a result of notsquashing, since that should only be
		// possible if there's a not of a not, which an earlier pass should take
		// care of
		Set<Component> componentsToTry = new HashSet<Component>(propnet.getComponents());
		for (Component c : componentsToTry) {
			if (c instanceof And || c instanceof Or) {
				squashNots(c, c instanceof And);
			}
		}
	}

}
