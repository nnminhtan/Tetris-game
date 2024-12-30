import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.swing.JOptionPane;

public class TetrisPanel extends Panel implements KeyListener {

	// variables for double buffered display
	private BufferedImage bi;
	private Graphics gi;

	// dimensions of the frame
	private Dimension dim;

	// constants for panel
	private final Color background = Color.BLACK;

	// Variable representing the number of players
	private int numOfPlayers;

	// the left and right portions of the panel
	Tetris[] screens;

	private BufferedReader br;
	private int[][] key;
	private String[] playerNames;
	private Tetris playerGame;
	private Tetris opponentGame;

	private PrintWriter pw;

	private int[][] opponentGrid = new int[22][10];

	// Color array for Tetris pieces
	private Color[] c = {
			Color.BLACK, // 0 - empty
			Color.BLUE, // 1 - some block color
			Color.RED, // 2 - some block color
			Color.GREEN, // 3 - some block color
			Color.YELLOW, // 4 - some block color
			Color.CYAN, // 5 - some block color
			Color.MAGENTA // 6 - some block color
			// Add more colors as needed for your Tetris pieces
	};

	private RoomData roomData;

	private boolean gameOverAnnounced = false;

	TetrisPanel(int numOfPlayers, String[] playerNames, PrintWriter pw, String roomName) {
		this.pw = pw;
		this.numOfPlayers = numOfPlayers;
		this.playerNames = playerNames;
		key = new int[1][6]; // Only need controls for the player's game
		screens = new Tetris[2]; // Two screens: player and opponent

		// Default key mappings
		int[][] defaultKeys = {
				{ KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_SHIFT,
						KeyEvent.VK_SPACE }
		};

		try {
			br = new BufferedReader(new FileReader("INPUT"));
			for (int j = 0; j < 6; j++)
				key[0][j] = defaultKeys[0][j];
		} catch (IOException ie) {
			System.out.println("Using default key mappings");
			System.arraycopy(defaultKeys[0], 0, key[0], 0, 6);
		}

		addKeyListener(this);
		setFocusable(true); //Components must be focusable to receive keyboard events
		requestFocusInWindow(); //Request focus for the window

		// Initialize player's game
		playerGame = new Tetris(0, 0, this, 0, playerNames[0]);
		playerGame.setPrintWriter(pw); //Set the print writer for the player's game
		screens[0] = playerGame;

		// Initialize opponent's game
		opponentGame = new Tetris(400, 0, this, 1, playerNames[1]); // Sử dụng tên thc của đối thủ
		screens[1] = opponentGame;

		this.roomData = new RoomData(roomName); // Sử dụng tên phòng được truyền vào
		this.roomData.addPlayer(playerNames[0]); //Add the player's name to the room data
		this.roomData.addPlayer(playerNames[1]);
	}

	public void sendGameState() {
		if (screens[0] != null) {
			PlayerData playerData = screens[0].getPlayerData(); //Get the player's data
			int linesCleared = screens[0].getLinesCleared(); //Get the lines cleared
			int score = screens[0].getScore(); // Get actual score instead of calculating

			// Send score update to opponent
			String scoreMessage = String.format("SCORE_UPDATE:%s:%d:%d:%d",
					playerNames[0],
					score,
					linesCleared,
					playerData.getLevel()
			);
			pw.println(scoreMessage); //Send the score update to the opponent

			// Send grid state
			StringBuilder gridState = new StringBuilder("GRID_STATE:");
			for (int i = 0; i < 22; i++) {
				for (int j = 0; j < 10; j++) {
					gridState.append(screens[0].getGridValue(i, j)).append(",");
				}
			}
			pw.println(gridState.toString());
		}
	}

	public void paint(Graphics g) {
		dim = getSize();
		bi = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_RGB); //Create a buffered image with the panel's dimensions and RGB color type
		gi = bi.getGraphics(); //Get the graphics object for the buffered image
		update(g);
	}

	public void update(Graphics g) {
		gi.setColor(background);
		gi.fillRect(0, 0, dim.width, dim.height); //Fill the buffered image with the background color

		// Chỉ vẽ màn hình người chơi bên trái
		screens[0].displayGrid(gi); //Display the player's grid
		screens[0].displayPieces(gi); //Display the player's pieces
		screens[0].displayUI(gi); //Display the player's UI

		// Bỏ qua việc vẽ màn hình đối thủ
		// screens[1].displayGrid(gi);
		// screens[1].displayPieces(gi);
		// screens[1].displayUI(gi);

		// Bỏ qua việc vẽ đường phân cách
		// gi.setColor(Color.WHITE);
		// gi.drawLine(400, 0, 400, dim.height);

		// updatePlayerDisplay(gi); //Update the player's display

		g.drawImage(bi, 0, 0, this); //Draw the buffered image onto the panel
	}

	// private void updatePlayerDisplay(Graphics gi) {
		// Xóa hết các thông tin hiển thị cũ
		// String currentPlayer = playerNames[0];
		// gi.setColor(Color.WHITE);
		// gi.drawString("Player: " + currentPlayer, 10, 20);
		// gi.drawString("Score: " + screens[0].getPlayerData().getScore(), 10, 40);
		// gi.drawString("Level: " + screens[0].getPlayerData().getLevel(), 10, 60);
		// gi.drawString("Lines: " + screens[0].getPlayerData().getLinesCleared(), 10,
		// 80);

		// Bỏ qua việc hiển thị thông tin đối thủ
		// gi.drawString("Opponent: " + opponentName, 410, 20);
		// gi.drawString("Score: " + opponentScore, 410, 40);
		// gi.drawString("Level: " + roomData.getPlayerData(opponentName).getLevel(),
		// 410, 60);
		// gi.drawString("Lines: " +
		// roomData.getPlayerData(opponentName).getLinesCleared(), 410, 80);
	// }

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// Handle key release events
		for (int i = 0; i < numOfPlayers; i++) {
			for (int j = 0; j < 6; j++) {
				if (e.getKeyCode() == key[i][j]) {
					if (screens[i].curr == null)
						break;
					if (j == 3)
						screens[i].delay = (screens[i].level >= 20 ? Tetris.GLOBAL_DELAY[19]
								: Tetris.GLOBAL_DELAY[screens[i].level]); //Set the delay for the player's game
				}
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// user input
		// three cases that handle when the user adjusts the game states (ACTIVE, PAUSED, CLOSED)
		if (e.getKeyCode() == KeyEvent.VK_P) {
			// Toggle pause state for all players
			boolean currentState = screens[0].isPaused;
			for (int i = 0; i < numOfPlayers; i++)
				screens[i].isPaused = !currentState;
			repaint();
		} else if (e.getKeyCode() == KeyEvent.VK_Q) {
			System.exit(0);
		} else if (e.getKeyCode() == KeyEvent.VK_R) {
			for (int i = 0; i < numOfPlayers; i++)
				screens[i].restart();
			repaint();
			return;
		}
		if (screens[0].isPaused || screens[0].isGameOver) //Check if the game is paused or game over
			return;
		int keyCode = e.getKeyCode();
		for (int i = 0; i < numOfPlayers; i++) {
			for (int j = 0; j < 6; j++) {
				if (keyCode == key[i][j]) {
					if (screens[i].curr == null)
						break;
					switch (j) {
						case 0:
							screens[i].movePiece(0, -1);
							repaint();
							break;
						case 1:
							screens[i].movePiece(0, 1);
							repaint();
							break;
						case 2:
							screens[i].rotateRight();
							break;
						case 3: // faster drop
							screens[i].delay = (screens[i].level >= 20 ? Tetris.GLOBAL_DELAY[19]
									: Tetris.GLOBAL_DELAY[screens[i].level]) / 8;
							break;

						case 4:
							if (screens[i].isHolding)
								break;
							if (screens[i].holdId == 0) {
								screens[i].holdId = screens[i].curr.id;
								screens[i].curr = null;
							} else {
								int temp = screens[i].holdId;
								screens[i].holdId = screens[i].curr.id;
								screens[i].curr = screens[i].p.getActive(temp - 1);
							}
							screens[i].isHolding = true;
							screens[i].time = 1 << 30;
							break;
						case 5: //instant drop
							screens[i].time = 1 << 30;
							screens[i].lockTime = 1 << 30;
							while (screens[i].movePiece(1, 0))
								;
							break;
					}
				}
			}
		}
		sendGameState();
		repaint();
	}

	protected void setGameOver() {
		for (int i = 0; i < numOfPlayers; i++)
			screens[i].isGameOver = true;
		
		// Notify server about game over
		if (pw != null) {
			pw.println("GAME_OVER:" + playerNames[0]);
		}
	}

	protected void sendGarbage(int id, int send) {
		if (numOfPlayers == 1 || send <= 0) return;
		
		if (pw != null) {
			String garbageMessage = String.format("GARBAGE:%s:%d", playerNames[id], send);
			pw.println(garbageMessage);
		}
	}

	// public void handleGarbageLines(String message) {
	// 	String[] parts = message.split(":");
	// 	String sender = parts[1];
	// 	int lines = Integer.parseInt(parts[2]);
		
	// 	if (!sender.equals(playerNames[0])) {
	// 		screens[0].addGarbageLines(lines);
	// 	}
	// }

	// public void updateOpponentState(String gameState) {
	// 	String[] parts = gameState.split(",");
	// 	int index = 0;

	// 	// Update grid
	// 	for (int i = 0; i < 22; i++) {
	// 		for (int j = 0; j < 10; j++) {
	// 			screens[1].setGridValue(i, j, Integer.parseInt(parts[index++]));
	// 		}
	// 	}

	// 	// Update current piece
	// 	String pieceState = parts[index++];
	// 	if (pieceState.equals("P")) {
	// 		Piece.Point[] newPos = new Piece.Point[4];
	// 		for (int i = 0; i < 4; i++) {
	// 			int r = Integer.parseInt(parts[index++]);
	// 			int c = Integer.parseInt(parts[index++]);
	// 			newPos[i] = new Piece.Point(r, c);
	// 		}
	// 		int pieceId = Integer.parseInt(parts[index++]);
	// 		screens[1].curr = screens[1].p.getActive(pieceId - 1);
	// 		screens[1].curr.pos = newPos;
	// 	}

	// 	// Update score and level
	// 	screens[1].setLinesCleared(Integer.parseInt(parts[index++]));
	// 	screens[1].setLevel(Integer.parseInt(parts[index++]));
	// 	screens[1].holdId = Integer.parseInt(parts[index]);

	// 	repaint();
	// }

	public void updateOpponentInfo(String opponentName, int score, int lines, int level) {
		if (screens[0] != null) {
			// Cập nhật điểm của đối thủ cho người chơi hiện tại
			screens[0].getPlayerData().setOpponentScore(score);
		}

		// Cập nhật thông tin cho màn hình đối thủ
		if (screens[1] != null) {
			screens[1].setLinesCleared(lines);
			screens[1].setLevel(level);
			screens[1].setPlayerName(opponentName);
			screens[1].getPlayerData().setScore(score);
		}

		repaint(); // Vẽ lại giao diện
	}

	// public void setOpponentGrid(int[][] grid) {
	// 	this.opponentGrid = grid;
	// }

	// public void displayOpponentGrid(Graphics gi) {
	// 	for (int i = 2; i < 22; i++) {
	// 		for (int j = 0; j < 10; j++) {
	// 			gi.setColor(c[opponentGrid[i][j]]);
	// 			gi.fillRect(400 + j * 25 + 10, i * 25, 24, 24); // Vẽ lưới đối thủ bên phải
	// 		}
	// 	}
	// }

	public void updateOpponentGrid(int[][] opponentGrid) {
		// Logic to update the UI with the opponent's grid
		// This could involve repainting the panel or updating specific components
		this.opponentGrid = opponentGrid; // Assuming you have a variable to store the opponent's grid
		repaint(); // Call repaint to refresh the display
	}

	// public void checkGameOver() {
	// 	if (screens[0].isGameOver && !gameOverAnnounced) {
	// 		gameOverAnnounced = true;
	// 		String winner = playerNames[1]; // Opponent wins
	// 		pw.println("GAME_OVER:" + winner);
	// 		displayGameOverMessage(winner);
	// 	}
	// }

	// private void displayGameOverMessage(String winner) {
	// 	Graphics g = getGraphics();
	// 	g.setColor(Color.WHITE);
	// 	g.setFont(new Font("Arial", Font.BOLD, 24));
	// 	String message = winner + " wins!";
	// 	g.drawString(message, getWidth()/2 - 50, getHeight()/2);
	// }

	public void announceWinner(String winner) {
		JOptionPane.showMessageDialog(this, 
			winner.equals(playerNames[0]) ? "You Won!" : winner + " Won!", 
			"Game Over", 
			JOptionPane.INFORMATION_MESSAGE);
	}

	public void handleGameOver(String winner) {
		if (!gameOverAnnounced) {
			gameOverAnnounced = true;
			setGameOver();
			announceWinner(winner);
		}
	}
}
