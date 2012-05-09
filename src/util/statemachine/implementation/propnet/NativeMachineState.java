package util.statemachine.implementation.propnet;

import java.util.HashSet;
import java.util.Set;

import util.statemachine.MachineState;


public class NativeMachineState extends MachineState {

	private Set<Integer> state;
	
	public NativeMachineState() {
		state = new HashSet<Integer>();
	}
	
	public void add(Integer idx) {
		state.add(idx);
	}
	
	public Set<Integer> getState() {
		return state;
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return state.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof NativeMachineState)) return false;
		return state.equals((NativeMachineState)o);
	}
}
