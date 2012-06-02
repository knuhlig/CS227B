package apps.pgggppg.compilation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	
	private static final int BLOCK_BITS = 32;

	private PropNet propNet;
	private List<Component> order;
	private List<Role> roles;
	
	private String className;
	private String srcPackage;
	private String fileName;
	private BufferedWriter out;
	
	
	private int numBlocks;
	private Map<Component, Integer> translation = new HashMap<Component, Integer>();
	
	public NativeCodeGenerator(PropNet propNet) {
		this.propNet = propNet;
		this.roles = propNet.getRoles();
		this.order = computeOrder();
		className = "MachineState" + System.currentTimeMillis();
		srcPackage = "apps.pgggppg.compilation.gen";
		fileName = "src/apps/pgggppg/compilation/gen/" + className + ".java";
		generateTranslation();
		numBlocks = (int) Math.ceil(1.0 * translation.size() / BLOCK_BITS);
	}
	
	private void generateTranslation() {
		// base
		for (Proposition p: propNet.getBasePropositions().values()) {
			translation.put(p, translation.size());
		}
		
		// init
		translation.put(propNet.getInitProposition(), translation.size());
		
		// input
		for (Proposition p: propNet.getInputPropositions().values()) {
			if (translation.containsKey(p)) continue;
			translation.put(p, translation.size());
		}
		
		for (Component c: order) {
			if (c instanceof Constant) continue;
			if (translation.containsKey(c)) continue;
			translation.put(c, translation.size());
		}
	}
	
	public void generateCode() {
		try {
			generateSourceFile();

			// compile
			System.out.println(">> compiling source file...");
			String[] args = new String[] {
				"-classpath", "bin",
				"-d", "bin",
				fileName
			};
			int res = Main.compile(args);
			if (res != 0) {
				throw new RuntimeException("compilation failed with status " + res);
			}
			System.out.println(">> compilation complete.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void addOutputs(Component c, Set<Component> set) {
		for (Component output: c.getOutputs()) {
			set.add(output);
		}
	}
	
	private List<Component> computeOrder() {
		// List to contain the topological ordering.
		List<Component> order = new ArrayList<Component>();

		Set<Component> marked = new HashSet<Component>();
		Set<Component> fringe = new HashSet<Component>();
		
		// constants
		for (Component term : propNet.getComponents()) {
			if (term instanceof Constant) {
				marked.add(term);
				addOutputs(term, fringe);
			}
		}

		// input
		Map<GdlTerm, Proposition> baseProps = propNet.getBasePropositions();
		for (GdlTerm term: baseProps.keySet()) {
			marked.add(baseProps.get(term));
			addOutputs(baseProps.get(term), fringe);
		}

		// moves
		baseProps = propNet.getInputPropositions();
		for (GdlTerm term: baseProps.keySet()) {
			marked.add(baseProps.get(term));
			addOutputs(baseProps.get(term), fringe);
		}
		// initial
		marked.add(propNet.getInitProposition());
		addOutputs(propNet.getInitProposition(), fringe);

		while (!fringe.isEmpty()) {
			Iterator<Component> it = fringe.iterator();
			Component c = it.next();
			it.remove();

			boolean ready = true;
			for (Component input: c.getInputs()) {
				if (!marked.contains(input)) {
					ready = false;
					break;
				}
			}

			if (ready) {
				marked.add(c);
				order.add(c);
				for (Component output: c.getOutputs()) {
					if (!marked.contains(output)) {
						fringe.add(output);
					}
				}
			}
		}
		return order;
	}
	
	
	public int getNativeIdx(Component c) {
		return translation.get(c);
	}
	
	public MachineState getInitialState() {
		try {
			NativeMachineState state = (NativeMachineState) Class.forName(srcPackage + "." + className).newInstance();
			return state.getInitialState();
		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new RuntimeException("unable to initialize native machine state");
	}
	
	private void generateSourceFile() throws Exception {
		/*for (Component c: translation.keySet()) {
			if (c instanceof Proposition) {
				int idx = translation.get(c);
				System.out.println(">> " + idx + " => " + ((Proposition) c).getName());
			}
		}*/
		System.out.println(">> writing source file...");
		out = new BufferedWriter(new FileWriter(fileName));
		
		// package
		writeLine(0, "package " + srcPackage + ";");
		writeLine(0, "");
		
		// imports
		writeLine(0, "import java.util.*;");
		writeLine(0, "import util.statemachine.MachineState;");
		writeLine(0, "import apps.pgggppg.compilation.NativeMachineState;");
		writeLine(0, "");
		
		// class def
		writeLine(0, "public class " + className + " extends NativeMachineState {");
		writeInstanceVars();
		writeConstructor();
		
		// public API
		writeTerminalMethod();
		writeGoalMethod();
		writeLegalMethod();
		writeNextStateMethod();
		writeInitialStateMethod();
		
		// internal methods
		writeCopyBaseMethod();
		writeMarkMethod();
		writeTransitionMethod();
		
		// hash methods
		writeHashCodeMethod();
		writeEqualsMethod();
		writeToStringMethod();
		
		writeLine(0, "}");
		out.close();
	}
	
	private void writeInstanceVars() {
		writeLine(1, "private int[] blocks = new int["+numBlocks+"];");
		writeLine(1, "");
	}
	
	private void writeConstructor() {
		writeLine(1, "public " + className + "() {");
		writeLine(2, "super();");
		writeLine(1, "}");
		writeLine(1, "");
	}
	
	private void writeTerminalMethod() {
		writeLine(1, "public boolean isTerminal() {");
		writeLine(2, "return " + getComponentBoolean(propNet.getTerminalProposition()) + ";");
		writeLine(1, "}");
		writeLine(1, "");
	}
	
	private void writeGoalMethod() {
		writeLine(1, "public int getGoal(int roleIdx) {");
		writeLine(2, "switch (roleIdx) {");
		for (int i = 0; i < roles.size(); i++) {
			Role role = roles.get(i);
			writeLine(2, "case " + i + ":");
			for (Proposition p: propNet.getGoalPropositions().get(role)) {
				writeLine(3, "if ("+getComponentBoolean(p)+") return " + getGoalValue(p) + ";");
			}
			writeLine(3, "break;");
		}
		writeLine(2, "}");
		writeLine(2, "throw new RuntimeException(\"bad goal: no values found\");");
		writeLine(1, "}");
		writeLine(1, "");
	}
	
	private void writeLegalMethod() {
		writeLine(1, "public List<Integer> getLegalMoves(int roleIdx) {");
		writeLine(2, "List<Integer> moves = new ArrayList<Integer>();");
		writeLine(2, "switch (roleIdx) {");
		for (int i = 0; i < roles.size(); i++) {
			Role role = roles.get(i);
			writeLine(2, "case " + i + ":");
			for (Proposition p: propNet.getLegalPropositions().get(role)) {
				writeLine(3, "if ("+getComponentBoolean(p)+") moves.add("+translation.get(p)+");");
			}
			writeLine(3, "break;");
		}
		writeLine(2, "}");
		writeLine(2, "return moves;");
		writeLine(1, "}");
		writeLine(1, "");
	}
	
	private void writeNextStateMethod() {
		writeLine(1, "public MachineState getNextState(List<Integer> moves) {");
		writeLine(2, className + " state = new " + className + "();");
		writeLine(2, "copyBase(state);");
		writeLine(2, "for (int move: moves) {");
		writeLine(3, "int blockIdx = move / " + BLOCK_BITS + ";");
		writeLine(3, "int offset = move % " + BLOCK_BITS + ";");
		writeLine(3, "state.blocks[blockIdx] |= 1 << offset;");
		writeLine(2, "}");
		writeLine(2, "state.mark();");
		writeLine(2, "return state.transition();");
		writeLine(1, "}");
		writeLine(1, "");
	}
	
	private void writeInitialStateMethod() {
		writeLine(1, "public MachineState getInitialState() {");
		writeLine(2, className + " state = new " + className + "();");
		
		int idx = translation.get(propNet.getInitProposition());
		int blockIdx = idx / BLOCK_BITS;
		int offset = idx % BLOCK_BITS;
		writeLine(2, "state." + getDataBlock(blockIdx) + " |= 1 << "+ offset + ";");
		
		writeLine(2, "state.mark();");
		writeLine(2, "return state.transition();");
		writeLine(1, "}");
		writeLine(1, "");
	}
	
	private void writeCopyBaseMethod() {
		writeLine(1, "private void copyBase(" + className + " state) {");
		int baseSize = propNet.getBasePropositions().size();
		int idx = 0;
		while (baseSize >= BLOCK_BITS) {
			writeLine(2, "state." + getDataBlock(idx) + " = " + getDataBlock(idx) + ";");
			baseSize -= BLOCK_BITS;
			idx++;
		}
		if (baseSize > 0) {
			int mask = -1 >>> (BLOCK_BITS - baseSize);
			String maskStr = "0x" + Integer.toHexString(mask);
			writeLine(2, "state." + getDataBlock(idx) + " = " + getDataBlock(idx) + " & " + maskStr + ";");
		}
		writeLine(1, "}");
		writeLine(1, "");
	}
	
	private void writeMarkMethod() {
		writeLine(1, "private void mark() {");
		for (Component c: order) {
			int idx = translation.get(c);
			int blockIdx = idx / BLOCK_BITS;
			int offset = idx % BLOCK_BITS;
			String maskStr = getBitMask(offset);
			writeLine(2, getDataBlock(blockIdx) + " |= "+getBitFunction(c, offset)+" & "+maskStr+";");
		}
		writeLine(1, "}");
	}
	
	private void writeTransitionMethod() {
		writeLine(1, "private MachineState transition() {");
		writeLine(2, className + " state = new " + className + "();");
		for (Proposition p: propNet.getBasePropositions().values()) {
			int idx = translation.get(p);
			int blockIdx = idx / BLOCK_BITS;
			int offset = idx % BLOCK_BITS;
			String maskStr = getBitMask(offset);
			writeLine(2, "state." + getDataBlock(blockIdx) + " |= "+getBitFunction(p, offset) + " & " + maskStr + ";");
		}
		writeLine(2, "state.mark();");
		writeLine(2, "return state;");
		writeLine(1, "}");
	}
	
	private void writeHashCodeMethod() {
		writeLine(1, "public int hashCode() {");
		writeLine(2, "int hash = 7;");
		int baseSize = propNet.getBasePropositions().size();
		int idx = 0;
		while (baseSize >= BLOCK_BITS) {
			writeLine(2, "hash += 31 * " + getDataBlock(idx) + ";");
			baseSize -= BLOCK_BITS;
			idx++;
		}
		if (baseSize > 0) {
			int mask = -1 >>> (BLOCK_BITS - baseSize);
			String maskStr = "0x" + Integer.toHexString(mask);
			writeLine(2, "hash += 31 * (" + getDataBlock(idx) + " & " + maskStr + ");");
		}
		writeLine(2, "return hash;");
		writeLine(1, "}");
	}
	
	private void writeEqualsMethod() {
		writeLine(1, "public boolean equals(Object obj) {");
		writeLine(2, "if (this == obj) return true;");
		writeLine(2, "if (!(obj instanceof " + className + ")) return false;");
		writeLine(2, className + " s = (" + className + ") obj;");

		int baseSize = propNet.getBasePropositions().size();
		int idx = 0;
		while (baseSize >= BLOCK_BITS) {
			writeLine(2, "if ("+getDataBlock(idx) + " != s."+getDataBlock(idx)+") return false;");
			baseSize -= BLOCK_BITS;
			idx++;
		}
		if (baseSize > 0) {
			int mask = -1 >>> (BLOCK_BITS - baseSize);
			String maskStr = "0x" + Integer.toHexString(mask);
			writeLine(2, "if ((" + getDataBlock(idx) + " & " + maskStr + ") != (s." + getDataBlock(idx) + " & " + maskStr + ")) return false;");
		}
		writeLine(2, "return true;");
		writeLine(1, "}");
	}
	
	private void writeToStringMethod() {
		writeLine(1, "public String toString() {");
		writeLine(2, "Set<Integer> props = new TreeSet<Integer>();");
		for (Proposition p: propNet.getBasePropositions().values()) {
			int idx = translation.get(p);
			writeLine(2, "if ("+getComponentBoolean(p)+") props.add("+idx+");");
		}
		writeLine(2, "return props.toString();");
		writeLine(1, "}");
	}
	
	
	
	private String getBitFunction(Component c, int finalPos) {
		StringBuilder b = new StringBuilder();
		b.append("(");
		if (c instanceof Proposition || c instanceof Transition) {
			// propositions and terminals have single inputs?
			b.append(getComponentBit(c.getSingleInput(), finalPos));
		} else if (c instanceof And) {
			int count = 0;
			for (Component input: c.getInputs()) {
				if (count > 0) b.append(" & ");
				b.append(getComponentBit(input, finalPos));
				count++;
			}
		} else if (c instanceof Or) {
			int count = 0;
			for (Component input: c.getInputs()) {
				if (count > 0) b.append(" | ");
				b.append(getComponentBit(input, finalPos));
				count++;
			}
		} else if (c instanceof Not) {
			b.append("~" + getComponentBit(c.getSingleInput(), finalPos));
		} else if (c instanceof Constant) {
			if (!c.getValue()) b.append("0");
			b.append(getBitMask(finalPos));
		}
		b.append(")");
		return b.toString();
	}
	
	private String getBitMask(int pos) {
		int mask = 1 << pos;
		return "0x" + Integer.toHexString(mask);
	}
	
	private String getComponentBit(Component c, int finalPos) {
		if (c instanceof Constant) {
			if (!c.getValue()) return "0";
			return getBitMask(finalPos);
		}
		if (!translation.containsKey(c)) {
			throw new RuntimeException("no definition for " + c.getClass().getName() + "");
		}
		int idx = translation.get(c);
		int blockIdx = idx / BLOCK_BITS;
		int shift = idx % BLOCK_BITS - finalPos;
		
		if (shift < 0) {
			shift = -shift;
			return "(" + getDataBlock(blockIdx) + " << " + shift + ")";
		}
		if (shift > 0) {
			return "(" + getDataBlock(blockIdx) + " >>> " + shift + ")";
		}
		return getDataBlock(blockIdx);
		
	}
	
	private String getComponentBoolean(Component c) {
		if (!translation.containsKey(c)) {
			throw new RuntimeException("no definition for " + c.getClass().getName() + "");
		}
		int idx = translation.get(c);
		int blockIdx = idx / BLOCK_BITS;
		int offset = idx % BLOCK_BITS;
		
		int mask = 1 << offset;
		String maskStr = "0x" + Integer.toHexString(mask);
		
		return "(" + getDataBlock(blockIdx) + " & " + maskStr + ") != 0";
	}
	
	
	private String getDataBlock(int num) {
		return "blocks[" + num + "]";
	}
	
	private int getGoalValue(Proposition goalProposition) {
		GdlRelation relation = (GdlRelation) goalProposition.getName().toSentence();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
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
    
	
	
	
}
