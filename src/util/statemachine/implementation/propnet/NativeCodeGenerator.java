package util.statemachine.implementation.propnet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.tools.javac.Main;

import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlTerm;
import util.propnet.architecture.Component;
import util.propnet.architecture.PropNet;
import util.propnet.architecture.components.And;
import util.propnet.architecture.components.Constant;
import util.propnet.architecture.components.Not;
import util.propnet.architecture.components.Or;
import util.propnet.architecture.components.Proposition;
import util.propnet.architecture.components.Transition;
import util.statemachine.MachineState;
import util.statemachine.Role;

public class NativeCodeGenerator {

	private PropNet propNet;
	private List<Component> order;
	private List<Role> roles;
	
	private String className;
	private String fileName;
	private BufferedWriter out;

	private Map<Component, Integer> translation = new HashMap<Component, Integer>();
	
	public NativeCodeGenerator(PropNet propNet, List<Role> roles, List<Component> order) {
		this.propNet = propNet;
		this.roles = roles;
		this.order = order;
		className = "StateMachine" + System.currentTimeMillis();
		fileName = "src/util/statemachine/implementation/propnet/gen/" + className + ".java";
	}
	
	public int getNativeIdx(Component c) {
		return translation.get(c);
	}
	
	private void generateTranslation() {
		// init
		translation.put(propNet.getInitProposition(), translation.size());
		
		// base
		for (Proposition p: propNet.getBasePropositions().values()) {
			translation.put(p, translation.size());
		}
		
		// input
		for (Proposition p: propNet.getInputPropositions().values()) {
			translation.put(p, translation.size());
		}
		
		// components
		for (Component c: order) {
			translation.put(c, translation.size());
		}
	}

	public NativePropNetStateMachine generateCode() {
		try {
			generateSourceFile();
			for (Component c: translation.keySet()) {
				System.out.println(translation.get(c) + ": " + c);
			}
			String[] args = new String[] {
				"-classpath", "bin",
				"-d", "bin",
				fileName
			};
			// compile
			int res = Main.compile(args);
			if (res != 0) {
				throw new RuntimeException("compilation failed with status " + res);
			}
			
			// load
			NativePropNetStateMachine machine = (NativePropNetStateMachine) Class.forName("util.statemachine.implementation.propnet.gen." + className).newInstance();
			return machine;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private void generateSourceFile() throws Exception {
		out = new BufferedWriter(new FileWriter(fileName));
		generateTranslation();
		writeLine(0, "package util.statemachine.implementation.propnet.gen;");
		writeLine(0, "");
		writeLine(0, "import util.statemachine.implementation.propnet.NativePropNetStateMachine;");
		writeLine(0, "import util.statemachine.implementation.propnet.NativeMachineState;");
		writeLine(0, "import java.util.*;");
		writeLine(0, "");
		writeLine(0, "public class " + className + " extends NativePropNetStateMachine {");
		writeConstructor();
		writeUpdateMethod();
		writeGoalMethod();
		writeMarkMethod();
		writeStateMethod();
		writeLegalMethod();
		writeLine(0, "}");
		out.close();
	}
	
	private void writeMarkMethod() {
		writeLine(1, "public void mark() {");
		for (Component c: order) {
			writeLine(2, getComponentVar(c) + " = " + getRValue(c) + ";");
		}
		writeLine(1, "}");
	}
	
	private String getComponentVar(Component c) {
		return "components[" + translation.get(c) + "]";
	}
	

	private void writeConstructor() {
		writeLine(1, "public " + className + "() {");
		writeLine(2, "super();");
		writeLine(2, "components = new boolean[" + translation.size() + "];");
		writeLine(2, "terminalIdx = " + translation.get(propNet.getTerminalProposition()) + ";");
		writeLine(2, "initIdx = " + translation.get(propNet.getInitProposition()) + ";");
		int count = 0;
		for (Role role: roles) {
			writeLine(2, "roleIndices.put(\""+role.toString()+"\", "+(count++)+");");
			writeLine(2, "addRole(\""+role.toString()+"\");");
		}
		for (Proposition p: propNet.getInputPropositions().values()) {
			writeLine(2, "moveMap.put(\""+p.getName().toString()+"\", "+translation.get(p)+");");
		}
		writeLine(1, "}");
	}
	
	private void writeUpdateMethod() {
		writeLine(1, "public void updateBase() {");
		for (Proposition p: propNet.getBasePropositions().values()) {
			writeLine(2, getComponentVar(p) + " = " + getComponentVar(p.getSingleInput()) + ";");
		}
		writeLine(1, "}");
	}
	
	private void writeGoalMethod() {
		writeLine(1, "public int getGoal(int roleIdx) {");
		writeLine(2, "switch (roleIdx) {");
		int count = 0;
		for (Role role: roles) {
			writeLine(2, "case " + count + ":");
			for (Proposition p: propNet.getGoalPropositions().get(role)) {
				writeLine(3, "if (" + getComponentVar(p) + ") return " + getGoalValue(p) + ";");
			}
			writeLine(3, "break;");
			count++;
		}
		writeLine(2, "}");
		writeLine(2, "throw new RuntimeException(\"bad goal\");");
		writeLine(1, "}");
	}
	
	private void writeLegalMethod() {
		writeLine(1, "public Set<Integer> getLegalMoves(int roleIdx) {");
		writeLine(2, "Set<Integer> moves = new HashSet<Integer>();");
		writeLine(2, "switch (roleIdx) {");
		int count = 0;
		for (Role role: roles) {
			writeLine(2, "case " + count + ":");
			for (Proposition p: propNet.getLegalPropositions().get(role)) {
				writeLine(3, "if ("+getComponentVar(p)+") moves.add("+translation.get(p)+");");
			}
			writeLine(3, "break;");
			count++;
		}
		writeLine(2, "}");
		writeLine(2, "return moves;");
		writeLine(1, "}");
	}
	
	private void writeStateMethod() {
		writeLine(1, "public NativeMachineState getStateFromBase() {");
		writeLine(2, "NativeMachineState state = new NativeMachineState();");
		for (Proposition bp: propNet.getBasePropositions().values()) {
			int idx = translation.get(bp);
			writeLine(2, "if (components["+idx+"]) state.add("+idx+");");
		}
		writeLine(2, "return state;");
		writeLine(1, "}");
	}
	
	
	private String getRValue(Component c) {
		StringBuilder b = new StringBuilder();
		if (c instanceof Proposition || c instanceof Transition) {
			b.append(getComponentVar(c.getSingleInput()));
		} else if (c instanceof And) {
			int count = 0;
			for (Component input: c.getInputs()) {
				if (count > 0) b.append(" && ");
				b.append(getComponentVar(input));
				count++;
			}
		} else if (c instanceof Or) {
			int count = 0;
			for (Component input: c.getInputs()) {
				if (count > 0) b.append(" || ");
				b.append(getComponentVar(input));
				count++;
			}
		} else if (c instanceof Not) {
			b.append("!" + getComponentVar(c.getSingleInput()));
		} else if (c instanceof Constant) {
			b.append(c.getValue() + "");
		}
		return b.toString();
	}
	
	private void writeLine(int indent, String line) {
		try {
			for (int i = 0; i < indent; i++) {
				out.write("\t");
			}
			out.write(line + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName().toSentence();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}
	
}
