package util.statemachine.implementation.propnet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.GdlSentence;
import util.statemachine.MachineState;
import util.statemachine.Move;

public abstract class NativePropNetStateMachine {

	protected Map<String, Integer> baseTranslation = new HashMap<String, Integer>();
	protected Map<String, Integer> inputTranslation = new HashMap<String, Integer>();
	
	protected boolean[] baseProps;
	protected boolean[] inputProps;
	
	public int[] fromState(MachineState state) {
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		int[] vals = new int[contents.size()];
		int idx = 0;
		for (GdlSentence item: contents) {
			vals[idx++] = baseTranslation.get(item.toString());
		}
		return vals;
	}
	
	
	public void setInputProps(int[] inputIdx) {
		Arrays.fill(inputProps, false);
		for (int idx: inputIdx) {
			inputProps[idx] = true;
		}
	}
	
	public void setBaseProps(int[] baseIdx) {
		Arrays.fill(baseProps, false);
		for (int idx: baseIdx) {
			baseProps[idx] = true;
		}
		
	}
}
