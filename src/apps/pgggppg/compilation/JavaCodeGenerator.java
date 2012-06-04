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

public class JavaCodeGenerator {
	// something
	private static final int BLOCK_BITS = 32;
	private int methodLengthLimit = 50000;

	private PropNet propNet;
	private List<Component> order;
	private List<Role> roles;
	
	private String className;
	private String srcPackage;
	private String fileName;
	private BufferedWriter out;
	
	
	private int numBlocks;
	private Map<Component, Integer> translation = new HashMap<Component, Integer>();
	
	public JavaCodeGenerator(PropNet propNet) {
		this.propNet = propNet;
		this.roles = propNet.getRoles();
		this.order = computeOrder();
		className = "MachineState" + System.currentTimeMillis() + "_" + ((int)(Math.random() * 100000));
		srcPackage = "apps.pgggppg.compilation.gen";
		fileName = "src/apps/pgggppg/compilation/gen/" + className + ".java";
		generateTranslation();
		numBlocks = (int) Math.ceil(1.0 * translation.size() / BLOCK_BITS);
	}
	
	private void generateTranslation() {
		int bitsSaved = 0;
		
		// base
		for (Proposition p: propNet.getBasePropositions().values()) {
			translation.put(p, translation.size());
		}
		
		// init
		translation.put(propNet.getInitProposition(), translation.size());
		
		// terminal
		translation.put(propNet.getTerminalProposition(), translation.size());
		
		// goals
		for (Set<Proposition> set: propNet.getGoalPropositions().values()) {
			for (Proposition p: set) {
				translation.put(p, translation.size());
			}
		}
		
		// goals
		for (Set<Proposition> set: propNet.getLegalPropositions().values()) {
			for (Proposition p: set) {
				translation.put(p, translation.size());
			}
		}
		
		// input
		for (Proposition p: propNet.getInputPropositions().values()) {
			if (translation.containsKey(p)) continue;
			translation.put(p, translation.size());
		}
		
		for (Component c: order) {
			if (translation.containsKey(c)) continue;
			
			// don't give bits to constants
			if (c instanceof Constant) continue;
			
			// don't give a bit if only one output
			if (c.getOutputs().size() <= 1) {
				bitsSaved++;
				continue;
			}
			
			translation.put(c, translation.size());
		}
		
		System.out.println(">> bit vector length: " + translation.size());
		System.out.println(">> eliminated " + bitsSaved + " unnecessary bits");
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
		if (!translation.containsKey(c)) {
			return -1;
		}
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
		writeGetAllBitsMethod();
		
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
		writeLine(1, "private int numBits = " + translation.size() + ";");
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
	
	private void writeGetAllBitsMethod() {
		writeLine(1, "public Set<Integer> getAllBits(boolean bitType) {");
		writeLine(2, "Set<Integer> bits = new HashSet<Integer>();");
		writeLine(2, "for (int i = 0; i < blocks.length; i++) {");
		writeLine(3, "int block = blocks[i];");
		writeLine(3, "for (int j = 0; j < " + BLOCK_BITS + "; j++) {");
		writeLine(4, "if ("+BLOCK_BITS + " * i + j >= numBits) break;");
		writeLine(4, "boolean on = (block & (1 << j)) != 0;");
		writeLine(4, "if (on == bitType) bits.add("+BLOCK_BITS+" * i + j);");
		writeLine(3, "}");
		writeLine(2, "}");
		writeLine(2, "return bits;");
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
		StringBuilder b = new StringBuilder();
		int methodNum = 0;
		
		writeLine(b, 1, "private void mark" + methodNum + "() {");
		
		for (Component c: order) {
			if (!translation.containsKey(c)) {
				continue;
			}
			
			// get code line
			int idx = translation.get(c);
			int blockIdx = idx / BLOCK_BITS;
			int offset = idx % BLOCK_BITS;
			String maskStr = getBitMask(offset);
			String line = getDataBlock(blockIdx) + " |= "+getBitFunction(c, offset)+" & "+maskStr+";";
			
			int nextLength = b.length() + line.length() + 2;
			if (nextLength >= methodLengthLimit) {
				writeLine(b, 1, "}");
				writeLine(0, b.toString());
				b = new StringBuilder();
				methodNum++;
				writeLine(b, 1, "private void mark" + methodNum + "() {");
			}
			
			writeLine(b, 2, line);
		}
		
		writeLine(b, 1, "}");
		writeLine(0, b.toString());
		
		writeLine(1, "public void mark() {");
		for (int i = 0; i <= methodNum; i++) {
			writeLine(2, "mark" + i + "();");
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
		// compute transient bits on the fly
		if (!translation.containsKey(c)) {
			return getBitFunction(c, finalPos);
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
		// compute bit function for unstored bits
		if (!translation.containsKey(c)) {
			return getBitFunction(c, 0) + " != 0";
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
	
	private void writeLine(StringBuilder b, int indent, String line) {
		try {
			for (int i = 0; i < indent; i++) {
				b.append("\t");
			}
			b.append(line + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
	
	
	
}
