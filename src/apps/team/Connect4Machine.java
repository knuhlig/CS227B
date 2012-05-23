package apps.team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import util.gdl.grammar.Gdl;
import util.gdl.grammar.GdlConstant;
import util.gdl.grammar.GdlFunction;
import util.gdl.grammar.GdlPool;
import util.gdl.grammar.GdlProposition;
import util.gdl.grammar.GdlRelation;
import util.gdl.grammar.GdlSentence;
import util.gdl.grammar.GdlTerm;
import util.statemachine.MachineState;
import util.statemachine.Move;
import util.statemachine.Role;
import util.statemachine.StateMachine;
import util.statemachine.exceptions.GoalDefinitionException;
import util.statemachine.exceptions.MoveDefinitionException;
import util.statemachine.exceptions.TransitionDefinitionException;

public class Connect4Machine extends StateMachine {
	
	public static void main(String[] args) {
		GState state = GState.initialState();
		int n = 200;
		int[] moves = new int[n];
		for (int i = 0; i < n; i++) {
			moves[i] = (int) (Math.random() * 8);
		}
		for (int move: moves) {
			if (!state.isLegal(move)) continue;
			System.out.println("drop: " + move);
			state = new GState(state, move);
			System.out.println(state);
			if (state.isTerminal()) {
				System.out.println("terminal! " + state.getGoal('r') + ", " + state.getGoal('b'));
				break;
			}
		}
	}

	private GdlConstant redName = GdlPool.getConstant("red");
	private GdlConstant blackName = GdlPool.getConstant("black");
	private GdlConstant drop = GdlPool.getConstant("drop");
	private GdlConstant noop = GdlPool.getConstant("noop");
	
	private Role red = new Role(GdlPool.getProposition(redName));
	private Role black = new Role(GdlPool.getProposition(blackName));
	private List<Role> roles = new ArrayList<Role>();
	private List<Move> redNoop, blackNoop;
	private List<Move> redDrops = new ArrayList<Move>();
	private List<Move> blackDrops = new ArrayList<Move>();
	
	@Override
	public void initialize(List<Gdl> description) {
		roles.add(red);
		roles.add(black);

		redNoop = new ArrayList<Move>();
		redNoop.add(new Move(GdlPool.getProposition(noop)));
		
		blackNoop = new ArrayList<Move>();
		blackNoop.add(new Move(GdlPool.getProposition(noop)));
		
		for (int i = 0; i < GState.NUM_COLS; i++) {
			redDrops.add(new Move(GdlPool.getRelation(drop, new GdlTerm[]{GdlPool.getConstant(i+"")})));
			blackDrops.add(new Move(GdlPool.getRelation(drop, new GdlTerm[]{GdlPool.getConstant(i+"")})));
		}
	}

	@Override
	public int getGoal(MachineState state, Role role)
			throws GoalDefinitionException {
		if (role.equals(red)) {
			return ((GState) state).getGoal('r');
		}
		return ((GState) state).getGoal('b');
	}

	@Override
	public boolean isTerminal(MachineState state) {
		return ((GState) state).isTerminal();
	}
	

	@Override
	public List<Role> getRoles() {
		return roles;
	}

	@Override
	public MachineState getInitialState() {
		return GState.initialState();
	}

	@Override
	public List<Move> getLegalMoves(MachineState state, Role role)
			throws MoveDefinitionException {
		GState s = (GState) state;
		List<Move> moves = new ArrayList<Move>();
		char control = s.getControl();
		if (role.equals(red)) {
			if (control == 'b') return redNoop;
			for (int i = 0; i < GState.NUM_COLS; i++) {
				if (s.isLegal(i)) moves.add(redDrops.get(i));
			}
			return moves;
		}
		if (control == 'r') return blackNoop;
		for (int i = 0; i < GState.NUM_COLS; i++) {
			if (s.isLegal(i)) moves.add(blackDrops.get(i));
		}
		return moves;
	}

	@Override
	public MachineState getNextState(MachineState state, List<Move> moves)
			throws TransitionDefinitionException {
		GState s = (GState) state;
		int col = 0;
		for (Move move: moves) {
			GdlSentence action = move.getContents();
			if (!action.getName().equals(noop)) {
				col = Integer.parseInt(action.getChildren().get(1).toString());
				break;
			}
		}
		return new GState(s, col);
	}

	private static class GState extends MachineState {
		private static final int NUM_COLS = 8;
		private static final int NUM_ROWS = 6;
		
		public static GState initialState() {
			GState state = new GState();
			Arrays.fill(state.remaining, NUM_ROWS);
			for (int y = 0; y < NUM_COLS; y++) {
				Arrays.fill(state.board[y], '-');
			}
			state.computeLegalMoves();
			return state;
		}
		
		private int[] remaining = new int[NUM_COLS];
		private char[][] board = new char[NUM_COLS][NUM_ROWS];
		private char control = 'r';
		
		private int legalMoves = 0;
		private char winner = 'x';
		
		public GState() {
			
		}
		
		public boolean isLegal(int drop) {
			return (legalMoves & (1 << drop)) != 0;
		}
		
		public GState(GState state, int col) {
			copyState(state);
			board[col][NUM_ROWS-remaining[col]] = control;
			remaining[col]--;
			control = control == 'r' ? 'b' : 'r';
			computeTerminal();
			if (!isTerminal()) {
				computeLegalMoves();
			}
		}
		
		public char getControl() {
			return control;
		}
		
		private void copyState(GState state) {
			for (int i = 0; i < NUM_COLS; i++) {
				System.arraycopy(state.board[i], 0, board[i], 0, NUM_ROWS);
				remaining[i] = state.remaining[i];
			}
			control = state.control;
		}
		
		private void computeLegalMoves() {
			for (int i = 0; i < NUM_COLS; i++) {
				if (remaining[i] > 0) {
					legalMoves |= (1 << i);
				}
			}
		}
		
		private void computeColWinner() {
			for (int i = 0; i < NUM_COLS; i++) {
				int red = 0;
				int black = 0;
				for (int j = 0; j < NUM_ROWS - remaining[i]; j++) {
					if (board[i][j] == 'r') {
						red++;
						black = 0;
						if (red == 4) {
							winner = 'r';
							return;
						}
					} else if (board[i][j] == 'b') {
						black++;
						red = 0;
						if (black == 4) {
							winner = 'b';
							return;
						}
					} else {
						red = black = 0;
					}
				}
			}
		}
		
		private void computeRowWinner() {
			for (int j = 0; j < NUM_ROWS; j++) {
				int red = 0;
				int black = 0;
				for (int i = 0; i < NUM_COLS; i++) {
					if (board[i][j] == 'r') {
						red++;
						black = 0;
						if (red == 4) {
							winner = 'r';
							return;
						}
					} else if (board[i][j] == 'b') {
						black++;
						red = 0;
						if (black == 4) {
							winner = 'b';
							return;
						}
					} else {
						red = black = 0;
					}
				}
			}
		}
		
		private void computeDiagWinner() {
			for (int i = 0; i < NUM_COLS - 3; i++) {
				for (int j = 0; j < NUM_ROWS - 3; j++) {
					if (board[i][j] == '-') {
						continue;
					}
					char color = board[i][j];
					boolean line = true;
					for (int d = 1; d <= 3; d++) {
						if (board[i+d][j+d] != color) {
							line = false;
							break;
						}
					}
					if (line) {
						winner = color;
						return;
					}
				}
			}
			for (int i = 0; i < NUM_COLS - 3; i++) {
				for (int j = 3; j < NUM_ROWS; j++) {
					if (board[i][j] == '-') {
						continue;
					}
					char color = board[i][j];
					boolean line = true;
					for (int d = 1; d <= 3; d++) {
						if (board[i+d][j-d] != color) {
							line = false;
							break;
						}
					}
					if (line) {
						winner = color;
						return;
					}
				}
			}
		}
		
		private void computeTerminal() {
			winner = 'x';
			computeColWinner();
			if (winner != 'x') return;
			computeRowWinner();
			if (winner != 'x') return;
			computeDiagWinner();
			if (winner != 'x') return;
			for (int i = 0; i < NUM_COLS; i++) {
				if (remaining[i] > 0) return;
			}
			winner = 't';
		}
		
		public int getGoal(char role) {
			if (winner == 't') return 50;
			return winner == role ? 100 : 0;
		}
		
		public boolean isTerminal() {
			return winner != 'x';
		}
		
		@Override
		public int hashCode() {
			int hash = control;
			for (int i = 0; i < board.length; i++) {
				for (int j = 0; j < board[i].length; j++) {
					hash = 7 * hash + 31 * board[i][j];
				}
			}
			return hash;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof GState)) return false;
			GState tmp = (GState) obj;
			if (control != tmp.control) return false;
			for (int i = 0; i < NUM_COLS; i++) {
				for (int j = 0; j < NUM_ROWS; j++) {
					if (board[i][j] != tmp.board[i][j]) return false;
				}
			}
			return true;
		}
		
		public String toString() {
			StringBuilder b = new StringBuilder();
			b.append("+- - - - - - - -+\n");
			for (int j = NUM_ROWS - 1; j >= 0; j--) {
				b.append("|");
				for (int i = 0; i < NUM_COLS; i++) {
					if (i > 0) b.append(" ");
					if (board[i][j] != '-') {
						b.append(board[i][j] == 'r' ? '.' : 'O');
					}
					else b.append(" ");
				}
				b.append("|\n");
			}
			b.append("+- - - - - - - -+");
			return b.toString();
		}
	}

}
