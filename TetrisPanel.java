import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.*;

public class TetrisPanel extends Panel implements KeyListener {
	private static final long serialVersionUID = -8444879183679955468L;

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
		setFocusable(true);
		requestFocusInWindow();

		// Initialize player's game
		playerGame = new Tetris(0, 0, this, 0, playerNames[0]);
		playerGame.setPrintWriter(pw);
		screens[0] = playerGame;

		// Initialize opponent's game
		opponentGame = new Tetris(400, 0, this, 1, playerNames[1]); // Sử dụng tên thc của đối thủ
		screens[1] = opponentGame;

		this.roomData = new RoomData(roomName); // Sử dụng tên phòng được truyền vào
		this.roomData.addPlayer(playerNames[0]);
		this.roomData.addPlayer(playerNames[1]);
	}

	public void sendGameState() {
		if (screens[0] != null) {
			PlayerData playerData = screens[0].getPlayerData();
			int linesCleared = screens[0].getLinesCleared();
			int score = linesCleared * 100;

			// Gửi điểm số qua mạng cho đối thủ
			String scoreMessage = String.format("SCORE_UPDATE:%s:%d:%d:%d",
					playerNames[0], // tên người gửi
					score, // điểm số
					linesCleared, // số dòng đã xóa
					playerData.getLevel() // level
			);
			pw.println(scoreMessage);
			System.out.println("Sending score to opponent: " + score);

			// Gửi trạng thái grid
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
		bi = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_RGB);
		gi = bi.getGraphics();
		update(g);
	}

	public void update(Graphics g) {
		gi.setColor(background);
		gi.fillRect(0, 0, dim.width, dim.height);

		// Chỉ vẽ màn hình người chơi bên trái
		screens[0].displayGrid(gi);
		screens[0].displayPieces(gi);
		screens[0].displayUI(gi);

		// Bỏ qua việc vẽ màn hình đối thủ
		// screens[1].displayGrid(gi);
		// screens[1].displayPieces(gi);
		// screens[1].displayUI(gi);

		// Bỏ qua việc vẽ đường phân cách
		// gi.setColor(Color.WHITE);
		// gi.drawLine(400, 0, 400, dim.height);

		updatePlayerDisplay(gi);

		g.drawImage(bi, 0, 0, this);
	}

	private void updatePlayerDisplay(Graphics gi) {
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
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
		for (int i = 0; i < numOfPlayers; i++) {
			for (int j = 0; j < 6; j++) {
				if (e.getKeyCode() == key[i][j]) {
					if (screens[i].curr == null)
						break;
					if (j == 3)
						screens[i].delay = (screens[i].level >= 20 ? Tetris.GLOBAL_DELAY[19]
								: Tetris.GLOBAL_DELAY[screens[i].level]);
				}
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// user input
		// three cases that handle when the user adjusts the game states (ACTIVE,
		// PAUSED, CLOSEd)
		if (e.getKeyCode() == KeyEvent.VK_P) {
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
		if (screens[0].isPaused || screens[0].isGameOver)
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
						case 3:
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
						case 5:
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
	}

	protected void sendGarbage(int id, int send) {
		if (numOfPlayers == 1)
			return;
		int rand = (int) (Math.random() * (numOfPlayers - 1));
		if (rand >= id)
			rand++;
		screens[rand].addGarbage(send);
		// System.out.println("SENT " + send);
	}

	public void updateOpponentState(String gameState) {
		String[] parts = gameState.split(",");
		int index = 0;

		// Update grid
		for (int i = 0; i < 22; i++) {
			for (int j = 0; j < 10; j++) {
				screens[1].setGridValue(i, j, Integer.parseInt(parts[index++]));
			}
		}

		// Update current piece
		String pieceState = parts[index++];
		if (pieceState.equals("P")) {
			Piece.Point[] newPos = new Piece.Point[4];
			for (int i = 0; i < 4; i++) {
				int r = Integer.parseInt(parts[index++]);
				int c = Integer.parseInt(parts[index++]);
				newPos[i] = new Piece.Point(r, c);
			}
			int pieceId = Integer.parseInt(parts[index++]);
			screens[1].curr = screens[1].p.getActive(pieceId - 1);
			screens[1].curr.pos = newPos;
		}

		// Update score and level
		screens[1].setLinesCleared(Integer.parseInt(parts[index++]));
		screens[1].setLevel(Integer.parseInt(parts[index++]));
		screens[1].holdId = Integer.parseInt(parts[index]);

		repaint();
	}

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

	public void setOpponentGrid(int[][] grid) {
		this.opponentGrid = grid;
	}

	public void displayOpponentGrid(Graphics gi) {
		for (int i = 2; i < 22; i++) {
			for (int j = 0; j < 10; j++) {
				gi.setColor(c[opponentGrid[i][j]]);
				gi.fillRect(400 + j * 25 + 10, i * 25, 24, 24); // Vẽ lưới đối thủ bên phải
			}
		}
	}

	public void updateOpponentGrid(int[][] opponentGrid) {
		// Logic to update the UI with the opponent's grid
		// This could involve repainting the panel or updating specific components
		this.opponentGrid = opponentGrid; // Assuming you have a variable to store the opponent's grid
		repaint(); // Call repaint to refresh the display
	}

	public void checkGameOver() {
		if (screens[0].isGameOver) {
			String winner = playerNames[1]; // Opponent wins
			pw.println("GAME_OVER:" + winner);
			displayGameOverMessage(winner);
		}
	}

	private void displayGameOverMessage(String winner) {
		Graphics g = getGraphics();
		g.setColor(Color.WHITE);
		g.setFont(new Font("Arial", Font.BOLD, 24));
		String message = winner + " wins!";
		g.drawString(message, getWidth()/2 - 50, getHeight()/2);
	}
}
