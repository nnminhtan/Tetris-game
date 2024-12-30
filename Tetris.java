import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyAdapter;
import java.util.*;
import java.util.Queue;
import java.io.PrintWriter;

import javax.swing.SwingUtilities;

// Core Tetris game logic and mechanics
public class Tetris {
	// grid of color ids that stores what kind of block is where
	private int[][] grid = new int[22][10];

	// dimensions of the frame
	private final int panelR, panelC;
	// the delay values for levels: the array index corresponds to the level. After
	// level 20 the delay remains consistent
	protected static final int[] GLOBAL_DELAY = { 800, 720, 630, 550, 470, 380, 300, 220, 130, 100, 80, 80, 80, 70, 70,
			70, 30, 30, 30, 20 };

	// the global delay lock value
	private final int GLOBAL_LOCK = 1000;

	/*
	 * Colors representing the different type of blocks
	 * light gray = empty square
	 * yellow = O
	 * cyan = I
	 * blue = L
	 * orange = J
	 * green = S
	 * red = Z
	 * Magenta = T
	 */
	private static final Color[] c = { Color.LIGHT_GRAY, Color.YELLOW, Color.CYAN, Color.BLUE, Color.ORANGE,
			Color.GREEN, Color.RED, Color.MAGENTA, Color.DARK_GRAY };
	private static final Color ghostColor = Color.DARK_GRAY;
	private static final Color UIColor = Color.LIGHT_GRAY;

	// Kick cases for J L S T Z blocks
	private static final int[][] movec1 = { { 0, -1, -1, 0, -1 },
			{ 0, +1, +1, 0, +1 },
			{ 0, +1, +1, 0, +1 },
			{ 0, +1, +1, 0, +1 },
			{ 0, +1, +1, 0, +1 },
			{ 0, -1, -1, 0, -1 },
			{ 0, -1, -1, 0, -1 },
			{ 0, -1, -1, 0, -1 } };
	private static final int[][] mover1 = { { 0, 0, +1, 0, -2 },
			{ 0, 0, +1, 0, -2 },
			{ 0, 0, -1, 0, +2 },
			{ 0, 0, -1, 0, +2 },
			{ 0, 0, +1, 0, -2 },
			{ 0, 0, +1, 0, -2 },
			{ 0, 0, -1, 0, +2 },
			{ 0, 0, -1, 0, +2 } };

	// Kick cases for I block
	private static final int[][] movec2 = { { 0, -2, +1, -2, +1 },
			{ 0, -1, +2, -1, +2 },
			{ 0, -1, +2, -1, +2 },
			{ 0, +2, -1, +2, -1 },
			{ 0, +2, -1, +2, -1 },
			{ 0, +1, -2, +1, -2 },
			{ 0, +1, -2, +1, -2 },
			{ 0, -2, +1, -2, +1 } };
	private static final int[][] mover2 = { { 0, 0, 0, -1, +2 },
			{ 0, 0, 0, +2, -1 },
			{ 0, 0, 0, +2, -1 },
			{ 0, 0, 0, +1, -2 },
			{ 0, 0, 0, +1, -2 },
			{ 0, 0, 0, -2, +1 },
			{ 0, 0, 0, -2, +1 },
			{ 0, 0, 0, -1, +2 } };

	// Handles the queue for pieces
	private Queue<Integer> bag = new ArrayDeque<Integer>();
	// Generates the pieces
	protected Piece p = new Piece();
	// Represents the current active piece
	protected Piece.Active curr = null;
	// Represents the ID of the current screen
	private int id;

	// Variables to manage the hold mechanism
	protected int holdId = 0;
	protected boolean isHolding = false;

	// Timing and level variables
	protected int time = 0;
	protected int delay = GLOBAL_DELAY[0];
	protected int level = 0;
	protected int lockTime = 0;
	protected int linesCleared = 0;

	// constants for UI
	private final int[] dy = { 50, 100, 150, 200, 300 };

	// Game state variables
	protected boolean isPaused = false;
	protected boolean isGameOver = false;

	private int combo = 0;

	// Thread that manages the gravity of the pieces
	private Timer t = new Timer();
	private TimerTask move = new TimerTask() {
		@Override
		public void run() {
			// checking for game states
			if (isPaused || isGameOver)
				return;

			// refill the queue if it is close to empty
			synchronized (bag) {
				if (bag.size() < 4)
					for (int id : p.getPermutation())
						bag.offer(id);
			}
			if (time >= delay) {
				// getting a new piece
				if (curr == null)
					curr = p.getActive(bag.poll());

				// attempting to move the piece
				if (movePiece(1, 0)) {
					lockTime = 0;
					time = 0;
				} else if (lockTime >= GLOBAL_LOCK) {
					// the piece cannot be moved down any further and the lock delay has expired
					// then place the piece and check for gameover
					isGameOver = true;
					for (int i = 0; i < 4; i++) {
						if (curr.pos[i].r >= 0)
							grid[curr.pos[i].r][curr.pos[i].c] = curr.id;
						if (curr.pos[i].r >= 2)
							isGameOver = false;
					}
					if (isGameOver) {
						System.out.println("GAMEOVER -- FINAL SCORE " + linesCleared);
						panel.setGameOver();
					}
					// set the piece down and allow the user to hold a piece. The lock time is also
					// reset
					synchronized (curr) {
						curr = null;
						isHolding = false;
						lockTime = 0;
					}

					// clear the lines and adjust the level
					int cleared = clearLines();
					if (cleared > 0)
						combo++;
					else
						combo = 0;
					int send = cleared > 0 ? ((1 << (cleared - 1)) / 2 + (combo / 2)) : 0;
					panel.sendGarbage(id, send);
					adjustLevel();

					// immediately get another piece
					time = delay;
				}
				panel.repaint();
			}
			time++;
			lockTime++;
		}
	};
	private TetrisPanel panel;
	private PrintWriter pw;
	private String playerName;

	// Add this line to declare the opponentGrid variable
	private int[][] opponentGrid; // Declare the opponentGrid variable

	private PlayerData playerData;

	private int numLinesInCurrentCombo = 0;  // Add this field to track lines in current combo

	// Initializes game instance with player data
	public Tetris(int x, int y, TetrisPanel panel, int id, String playerName) {
		this.panel = panel;
		this.playerName = playerName;
		this.panelC = x;
		this.panelR = y;
		this.id = id;
		this.playerData = new PlayerData(playerName);
		
		// Start timer only for player's game (id == 0)
		if (id == 0) {
			t.scheduleAtFixedRate(move, 1000, 1);
		}
		System.out.println(playerName);
		// Add KeyListener to handle key events
		panel.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_Q) {
					// Navigate back to RoomManager
					panel.setVisible(false); // Hide the Tetris panel
					new RoomManager("Room Name", new ArrayList<>(), playerName); // Replace with actual room name and player list
				}
			}
		});
		panel.setFocusable(true); // Ensure the panel can receive key events
		panel.requestFocusInWindow(); // Request focus for key events
	}

	// Add method to set PrintWriter
	public void setPrintWriter(PrintWriter pw) {
		this.pw = pw;
	}

	// adjust the level based on the number of lines cleared
	private void adjustLevel() {
		level = linesCleared / 4;
		if (level >= 20)
			delay = GLOBAL_DELAY[19];
		else
			delay = GLOBAL_DELAY[level];
	}

	// paints the grid based on the color id values in the 2D Array
	public void displayGrid(Graphics gi) {
		for (int i = 2; i < 22; i++) {
			for (int j = 0; j < 10; j++) {
				gi.setColor(c[grid[i][j]]); //Set the color of the block
				gi.fillRect(panelC + j * 25 + 10, panelR + i * 25, 24, 24); //Fill the grid with the color of the block
			}
		}
	}

	// paints the current piece
	public void displayPieces(Graphics gi) {
		if (curr == null)
			return;
		synchronized (curr) {
			int d = -1;
			// displaying the ghost piece
			boolean isValid = true;
			while (isValid) {
				d++;
				for (Piece.Point block : curr.pos)
					if (block.r + d >= 0 && (block.r + d >= 22 || grid[block.r + d][block.c] != 0))
						isValid = false;
			}
			d--;
			// painting the ghost piece and the active piece
			gi.setColor(ghostColor); //Set the color of the ghost piece
			for (Piece.Point block : curr.pos)
				if (block.r + d >= 2)
					gi.fillRect(panelC + block.c * 25 + 10, panelR + (block.r + d) * 25, 24, 24);

			gi.setColor(c[curr.id]); //Set the color of the active piece
			for (Piece.Point block : curr.pos)
				if (block.r >= 2)
					gi.fillRect(panelC + block.c * 25 + 10, panelR + block.r * 25, 24, 24);
		}
	}

	// paints the user interface
	public void displayUI(Graphics gi) {
		gi.setColor(UIColor);
		gi.drawString("LINES CLEARED: " + linesCleared, panelC + 10, panelR + 10);
		gi.drawString("CURRENT LEVEL: " + level, panelC + 10, panelR + 20);
		if (playerData != null) {
			gi.drawString("SCORE: " + playerData.getScore(), panelC + 10, panelR + 30);
		} else {
			gi.drawString("SCORE: " + 0, panelC + 10, panelR + 30);
		}

		// Display opponent score
		if (playerData != null) {
			int opponentScore = playerData.getOpponentScore();
			gi.drawString("OPPONENT SCORE: " + opponentScore, panelC + 10, panelR + 40);
		}

		if (isPaused)
			gi.drawString("PAUSED", panelC + 10, 50);
		if (isGameOver)
			gi.drawString("GAMEOVER -- Q FOR QUIT; R FOR RESTART", panelC + 10, panelR + 60);
		gi.drawString("HOLD", panelC + 300, panelR + 300);
		gi.drawString("NEXT", panelC + 300, panelR + 50);
		for (int k = 0; k < 5; k++) {
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < 4; j++) {
					gi.fillRect(panelC + j * 20 + 300, panelR + i * 20 + dy[k], 19, 19);
				}
			}
		}
		// paints the hold piece
		if (holdId != 0) {
			Piece.Active holdPiece = p.getActive(holdId - 1);
			gi.setColor(c[holdPiece.id]);
			for (Piece.Point block : holdPiece.pos) {
				gi.fillRect(panelC + (block.c - 3) * 20 + 300, panelR + block.r * 20 + dy[4], 19, 19);
			}
		}
		// paints the queue of blocks
		synchronized (bag) {
			int i = 0;
			for (int id : bag) {
				Piece.Active nextPiece = p.getActive(id);
				gi.setColor(c[nextPiece.id]);
				for (Piece.Point block : nextPiece.pos) {
					gi.fillRect(panelC + (block.c - 3) * 20 + 300, panelR + block.r * 20 + dy[i], 19, 19);
				}
				i++;
				if (i >= 4)
					break;
			}
		}
	}

	// Post condition: any full lines are cleared and the respective variable is
	// incremented
	private int clearLines() {
		int numCleared = 0;
		boolean hasCleared = false;
		
		while (true) {
			int index = -1;
			for (int j = 0; j < 22; j++) {
				int cnt = 0;
				for (int i = 0; i < 10; i++) {
					cnt += grid[j][i] != 0 ? 1 : 0;
				}
				if (cnt == 10) {
					index = j;
					break;
				}
			}
			if (index == -1) break;

			// Clear the line
			int[][] temp = new int[22][10];
			for (int i = 0; i < 22; i++)
				for (int j = 0; j < 10; j++)
					temp[i][j] = grid[i][j];
			for (int i = 0; i < index + 1; i++) {
				for (int j = 0; j < 10; j++) {
					if (i == 0)
						grid[i][j] = 0;
					else
						grid[i][j] = temp[i - 1][j];
				}
			}
			linesCleared++;
			numCleared++;
			hasCleared = true;
		}
		
		if (hasCleared) {
			// Calculate score for this combo
			int scoreIncrease = calculateScore(numCleared);
			
			if (playerData != null) {
				int currentScore = playerData.getScore();
				int newScore = currentScore + scoreIncrease;  // Accumulate the score
				
				System.out.println("Calculating new score: current=" + currentScore + 
								 ", increase=" + scoreIncrease + ", new=" + newScore);
								 
				playerData.setScore(newScore);  // Set the accumulated score
				playerData.setLinesCleared(linesCleared);
				if (pw != null) {
					pw.println("SCORE_UPDATE:" + playerName + ":" + newScore + ":" + 
							  linesCleared + ":" + level);
				}
			}
		}
		
		return numCleared;
	}

	private int calculateScore(int numCleared) {
		switch (numCleared) {
			case 1: return 100;
			case 2: return 300;
			case 3: return 500;
			case 4: return 800;
			default: return numCleared * 100;
		}
	}

	// // Add this method to handle combo reset
	// private void handleLineClears(int numCleared) {
	// 	if (numCleared > 0) {
	// 		combo++;
	// 		// Calculate garbage lines to send
	// 		int garbageLines = calculateGarbageLines(numCleared);
	// 		if (garbageLines > 0) {
	// 			sendGarbage(garbageLines);
	// 		}
	// 	} else {
	// 		combo = 0;
	// 		numLinesInCurrentCombo = 0;  // Reset lines count when combo breaks
	// 	}
	// }

	// private int calculateGarbageLines(int numCleared) {
		// Battle mechanics:
		// Single line = 0 garbage
		// Double line = 1 garbage
		// Triple line = 2 garbage
		// Tetris (4 lines) = 4 garbage
		// Additional garbage for combos
	// 	int baseGarbage = 0;
	// 	switch (numCleared) {
	// 		case 2:
	// 			baseGarbage = 1;
	// 			break;
	// 		case 3:
	// 			baseGarbage = 2;
	// 			break;
	// 		case 4:
	// 			baseGarbage = 4;
	// 			break;
	// 	}

	// 	// Add combo bonus (every 2 combos = 1 extra line)
	// 	int comboBonus = combo / 2;

	// 	return baseGarbage + comboBonus;
	// }

	public void restart() {
		curr = null;
		grid = new int[22][10];
		bag.clear();
		level = 0;
		linesCleared = 0;
		holdId = 0;
		isHolding = false;
		isGameOver = false;
	}

	// attempt to rotate the piece counterclockwise
	// Post condition: the current piece will be rotated counterclockwise if there
	// is one case (out of five) that work
	protected void rotateLeft() {
		if (curr.id == 1)
			return;
		Piece.Point[] np = new Piece.Point[4];
		for (int i = 0; i < 4; i++) {
			int nr = curr.pos[i].c - curr.loc + curr.lor;
			int nc = curr.pos[i].r - curr.lor + curr.loc;
			np[i] = new Piece.Point(nr, nc);
		}
		int lor = curr.lor;
		int hir = curr.hir;
		for (int i = 0; i < 4; i++) {
			np[i].r = hir - (np[i].r - lor);
		}
		kick(np, curr.state * 2 + 1);
		panel.repaint();
	}

	// attempt to rotate the piece clockwise
	// Post condition: the current piece will be rotated clockwise if there is one
	// case (out of five) that work
	protected void rotateRight() {
		if (curr.id == 1)
			return;
		Piece.Point[] np = new Piece.Point[4];
		for (int i = 0; i < 4; i++) {
			int nr = curr.pos[i].c - curr.loc + curr.lor;
			int nc = curr.pos[i].r - curr.lor + curr.loc;
			np[i] = new Piece.Point(nr, nc);
		}
		int loc = curr.loc;
		int hic = curr.hic;
		for (int i = 0; i < 4; i++) {
			np[i].c = hic - (np[i].c - loc);
		}
		kick(np, curr.state * 2);
		panel.repaint();

	}

	// handles the kick cases
	// Post condition: rotates the piece according to the state of the rotation
	// this method performs the actual rotation and copies the positions of the
	// blocks into the active block
	private void kick(Piece.Point[] pos, int id) {
		for (int i = 0; i < 5; i++) {
			boolean valid = true;
			int dr = curr.id == 2 ? mover2[id][i] : mover1[id][i];
			int dc = curr.id == 2 ? movec2[id][i] : movec1[id][i];
			for (Piece.Point block : pos) {
				if (block.r + dr < 0 || block.r + dr >= 22)
					valid = false;
				else if (block.c + dc < 0 || block.c + dc >= 10)
					valid = false;
				else if (grid[block.r + dr][block.c + dc] != 0)
					valid = false;
			}
			if (valid) {
				for (int j = 0; j < 4; j++) {
					curr.pos[j].r = pos[j].r + dr;
					curr.pos[j].c = pos[j].c + dc;
				}
				curr.hic += dc;
				curr.loc += dc;
				curr.hir += dr;
				curr.lor += dr;
				if (id % 2 == 1) //Rotate the piece counterclockwise
					curr.state = (curr.state + 3) % 4;
				else //Rotate the piece clockwise
					curr.state = (curr.state + 1) % 4;
				return;
			}
		}
	}

	// attempts to move the active piece
	// Post-condition: will return false if it cannot move and true if it can move
	protected boolean movePiece(int dr, int dc) {
		if (curr == null)
			return false;
		for (Piece.Point block : curr.pos) {
			if (block.r + dr < 0 || block.r + dr >= 22)
				return false;
			if (block.c + dc < 0 || block.c + dc >= 10)
				return false;
			if (grid[block.r + dr][block.c + dc] != 0)
				return false;
		}
		for (int i = 0; i < 4; i++) {
			curr.pos[i].r += dr;
			curr.pos[i].c += dc;
		}
		curr.loc += dc;
		curr.hic += dc;
		curr.lor += dr;
		curr.hir += dr;
		return true;
	}

	public int getGridValue(int row, int col) {
		return grid[row][col];
	}

	public void setGridValue(int row, int col, int value) {
		grid[row][col] = value;
	}

	public int getLinesCleared() {
		return linesCleared;
	}

	public void setLinesCleared(int value) {
		linesCleared = value;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int value) {
		this.level = value;
		if (playerData != null) {
			playerData.setLevel(value);
		}
	}

	// protected void sendGarbage(int lines) {
	// 	if (lines > 0 && pw != null) {
	// 		pw.println("GARBAGE:" + playerName + ":" + lines);
	// 	}
	// }

	// public void addGarbage(int lines) {
	// 	// Existing garbage line addition code...
	// 	// After adding garbage lines
	// 	if (panel != null) {
	// 		panel.sendGameState(); // Call sendGameState through panel reference
	// 	}
	// }

	// Modify the addGarbageLines method to handle incoming garbage
	public void addGarbageLines(int lines) {
		if (lines <= 0) return;
		
		// Save current piece position
		Piece.Active savedPiece = curr;
		
		// Shift existing blocks up
		for (int i = lines; i < 22; i++) {
			for (int j = 0; j < 10; j++) {
				grid[i - lines][j] = grid[i][j];
			}
		}
		
		// Add garbage lines
		Random rand = new Random();
		for (int i = 22 - lines; i < 22; i++) {
			int hole = rand.nextInt(10);
			for (int j = 0; j < 10; j++) {
				grid[i][j] = (j == hole) ? 0 : 8;
			}
		}
		
		// Restore piece if it exists
		if (savedPiece != null) {
			curr = savedPiece;
			// Check if piece position is valid
			boolean needsAdjustment = false;
			for (Piece.Point block : curr.pos) {
				if (block.r >= 0 && grid[block.r][block.c] != 0) {
					needsAdjustment = true;
					break;
				}
			}
			
			// Try to move piece up if needed
			if (needsAdjustment) {
				for (int i = 0; i < lines; i++) {
					for (Piece.Point block : curr.pos) {
						block.r--;
					}
					curr.lor--;
					curr.hir--;
				}
			}
		}
		
		panel.repaint();
	}

	public void sendGridState() {
		if (pw != null) {
			StringBuilder state = new StringBuilder("GRID_STATE:");
			for (int i = 0; i < 22; i++) {
				for (int j = 0; j < 10; j++) {
					state.append(grid[i][j]).append(",");
				}
			}
			pw.println(state.toString());
		}
	}

	public void setOpponentGrid(int[][] opponentGrid) {
		this.opponentGrid = opponentGrid;
		// Update the UI to reflect the opponent's grid
		panel.updateOpponentGrid(opponentGrid); // Assuming you have a method in TetrisPanel to handle this
		// Additional logic to update the game state can be added here if necessary
	}

	public void setPlayerName(String name) {
		this.playerName = name;
		if (playerData == null) {
			playerData = new PlayerData(name);
		}
	}

	public String getPlayerName() {
		return this.playerName;
	}

	public PlayerData getPlayerData() {
		return playerData;
	}

	public int getScore() {
		return playerData != null ? playerData.getScore() : 0;
	}
}
