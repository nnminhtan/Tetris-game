import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 8888;
    private static ConcurrentHashMap<String, RoomData> rooms = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    static class RoomData {
        private String roomId;
        private PlayerData player1;
        private PlayerData player2;
        private boolean gameStarted;

        public RoomData(String roomId) {
            this.roomId = roomId;
            this.gameStarted = false;
        }

        public void addPlayer(String playerName) {
            if (player1 == null) {
                player1 = new PlayerData(playerName);
            } else if (player2 == null) {
                player2 = new PlayerData(playerName);
            }
        }

        public void updatePlayerStats(String playerName, int linesCleared, int level, int score) {
            PlayerData player = getPlayerData(playerName);
            if (player != null) {
                player.setLinesCleared(linesCleared);
                player.setLevel(level);
                player.setScore(score);
                printRoomStatus();
            }
        }

        public PlayerData getPlayerData(String playerName) {
            if (player1 != null && player1.getName().equals(playerName)) {
                return player1;
            } else if (player2 != null && player2.getName().equals(playerName)) {
                return player2;
            }
            return null;
        }

        public PlayerData getOpponentData(String playerName) {
            if (player1 != null && player1.getName().equals(playerName)) {
                return player2;
            } else if (player2 != null && player2.getName().equals(playerName)) {
                return player1;
            }
            return null;
        }

        public void printRoomStatus() {
            // System.out.println("\n" + roomId + " Status:");
            // System.out.println("------------------------");
            // if (player1 != null) {
            // System.out.println("Player: " + player1.getName());
            // System.out.println("- Lines cleared: " + player1.getLinesCleared());
            // System.out.println("- Score: " + (player1.getLinesCleared() * 100));
            // System.out.println("- Level: " + player1.getLevel());
            // } else {
            // System.out.println("Waiting for Player 1");
            // }

            // System.out.println("------------------------");

            // if (player2 != null) {
            // System.out.println("Player: " + player2.getName());
            // System.out.println("- Lines cleared: " + player2.getLinesCleared());
            // System.out.println("- Score: " + (player2.getLinesCleared() * 100));
            // System.out.println("- Level: " + player2.getLevel());
            // } else {
            // System.out.println("Waiting for Player 2");
            // }
            // System.out.println("------------------------\n");
        }

        public boolean isFull() {
            return player1 != null && player2 != null;
        }

        public void setGameStarted(boolean started) {
            this.gameStarted = started;
        }

        public String getOpponentName(String playerName) {
            if (player1 != null && player1.getName().equals(playerName)) {
                return player2 != null ? player2.getName() : "Waiting...";
            } else if (player2 != null && player2.getName().equals(playerName)) {
                return player1 != null ? player1.getName() : "Waiting...";
            }
            return "Unknown";
        }

        public int getOpponentScore(String playerName) {
            if (player1 != null && player1.getName().equals(playerName)) {
                return player2 != null ? player2.getScore() : 0;
            } else if (player2 != null && player2.getName().equals(playerName)) {
                return player1 != null ? player1.getScore() : 0;
            }
            return 0;
        }

        public Set<String> getPlayers() {
            Set<String> players = new HashSet<>();
            if (player1 != null)
                players.add(player1.getName());
            if (player2 != null)
                players.add(player2.getName());
            return players;
        }

        public void removePlayer(String playerName) {
            if (player1 != null && player1.getName().equals(playerName)) {
                player1 = null;
            } else if (player2 != null && player2.getName().equals(playerName)) {
                player2 = null;
            }
        }
    }

    public GameServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server is running on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());
            new ClientHandler(clientSocket).start();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private String clientName;
        private String currentRoom;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.clientName = in.readLine();
                clients.put(clientName, this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = in.readLine();
                    if (message == null)
                        break;

                    if (message.startsWith("CREATE ")) {
                        handleCreateRoom(message.split(" ")[1]);
                    } else if (message.startsWith("JOIN ")) {
                        handleJoinRoom(message.split(" ")[1]);
                    } else if (message.startsWith("GAME_STATE:")) {
                        String[] parts = message.split(":");
                        String playerName = parts[1];
                        String gameState = parts[2];
                        handleGameState(playerName, gameState);
                    } else if (message.startsWith("GARBAGE:")) {
                        handleGarbageLines(message);
                    } else if (message.startsWith("SCORE_UPDATE:")) {
                        handleMessage(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                handleDisconnect();
            }
        }

        private void handleCreateRoom(String roomName) {
            RoomData roomData = new RoomData(roomName);
            roomData.addPlayer(clientName);
            rooms.put(roomName, roomData);
            currentRoom = roomName;
            out.println("ROOM_CREATED:" + roomName);
            System.out.println("Room created: " + roomName + " by player: " + clientName);
            broadcastRoomInfo(roomName);
        }

        private void handleJoinRoom(String roomName) {
            RoomData roomData = rooms.get(roomName);
            if (roomData != null && !roomData.isFull()) {
                roomData.addPlayer(clientName);
                currentRoom = roomName;
                out.println("JOINED_ROOM:" + roomName);
                System.out.println("Player " + clientName + " joined room: " + roomName);
                broadcastRoomInfo(roomName);
                if (roomData.isFull()) {
                    roomData.setGameStarted(true);
                    System.out.println("Room " + roomName + " is full, starting game...");
                    System.out.println("Players in room: " + roomData.getPlayers());
                    broadcastToRoom(roomName, "START_GAME", false);
                }
            } else {
                out.println("ROOM_FULL");
                System.out.println("Failed to join room " + roomName + " - Room is full or doesn't exist");
            }
        }

        private void broadcastRoomInfo(String roomName) {
            RoomData roomData = rooms.get(roomName);
            if (roomData != null) {
                for (String playerName : roomData.getPlayers()) {
                    PlayerData opponent = roomData.getOpponentData(playerName);
                    if (opponent != null) {
                        String message = "ROOM_INFO:" + roomName + ":" +
                                opponent.getName() + ":" +
                                opponent.getScore() + ":" +
                                opponent.getLinesCleared() + ":" +
                                opponent.getLevel();
                        ClientHandler client = getClientByName(playerName);
                        if (client != null) {
                            client.out.println(message);
                        }
                    }
                }
            }
        }

        private void checkRoomStatus(String roomName) {
            RoomData roomData = rooms.get(roomName);
            if (roomData != null && roomData.isFull()) {
                broadcastToRoom(roomName, "START_GAME", false);
                roomData.setGameStarted(true);
            }
        }

        private void handleGameState(String playerName, String gameState) {
            RoomData roomData = rooms.get(currentRoom);
            if (roomData != null) {
                String[] parts = gameState.split(",");
                int linesCleared = Integer.parseInt(parts[parts.length - 3]);
                int level = Integer.parseInt(parts[parts.length - 2]);
                int score = linesCleared * 100;

                roomData.updatePlayerStats(playerName, linesCleared, level, score);

                // Gửi điểm số cho tất cả người chơi trong phòng
                String scoreUpdate = String.format("SCORE_UPDATE:%s:%d:%d:%d",
                        playerName, score, linesCleared, level);
                broadcastToRoom(currentRoom, scoreUpdate, false);

                System.out.println("Broadcasting score update in room " + currentRoom);
                System.out.println("From player: " + playerName);
                System.out.println("Score: " + score + ", Lines: " + linesCleared);
            }
        }

        private int calculateScore(int linesCleared) {
            switch (linesCleared) {
                case 1:
                    return 100;
                case 2:
                    return 300;
                case 3:
                    return 500;
                case 4:
                    return 800;
                default:
                    return linesCleared * 100;
            }
        }

        private void handleGarbageLines(String message) {
            if (currentRoom != null) {
                String[] parts = message.split(":");
                String sender = parts[1];
                int lines = Integer.parseInt(parts[2]);
                broadcastToRoom(currentRoom, "ADD_GARBAGE:" + sender + ":" + lines, true);
            }
        }

        private void handleDisconnect() {
            if (currentRoom != null) {
                RoomData roomData = rooms.get(currentRoom);
                if (roomData != null) {
                    roomData.removePlayer(clientName);
                    if (roomData.getPlayers().isEmpty()) {
                        rooms.remove(currentRoom);
                    }
                    broadcastToRoom(currentRoom, "PLAYER_DISCONNECTED:" + clientName, false);
                }
            }
            clients.remove(clientName);
        }

        private void broadcastToRoom(String roomName, String message, boolean excludeSelf) {
            RoomData roomData = rooms.get(roomName);
            if (roomData != null) {
                for (String playerName : roomData.getPlayers()) {
                    ClientHandler client = getClientByName(playerName);
                    if (client != null && (!excludeSelf || !client.equals(this))) {
                        client.out.println(message);
                    }
                }
            }
        }

        private ClientHandler getClientByName(String playerName) {
            return clients.get(playerName);
        }

        private void handleMessage(String message) {
            if (message.startsWith("SCORE_UPDATE:")) {
                String[] parts = message.split(":");
                String playerName = parts[1];
                int score = Integer.parseInt(parts[2]);
                int linesCleared = Integer.parseInt(parts[3]);
                int level = Integer.parseInt(parts[4]);

                RoomData roomData = rooms.get(currentRoom);
                if (roomData != null) {
                    roomData.updatePlayerStats(playerName, linesCleared, level, score);

                    broadcastToRoom(currentRoom, message, false);

                    System.out.println("Score updated - Player: " + playerName);
                    System.out.println("Lines: " + linesCleared + ", Score: " + score);
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            new GameServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
