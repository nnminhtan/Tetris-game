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

        public void updatePlayerStats(String playerName, int linesCleared, int level) {
            PlayerData player = getPlayerData(playerName);
            if (player != null) {
                player.setLinesCleared(linesCleared);
                player.setLevel(level);
                player.setScore(linesCleared * 100);
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

        public void printRoomStatus() {
            System.out.println(roomId + ": " +
                    (player1 != null ? "Player 1 (" + player1.getName() + ") - score: " + player1.getScore()
                            : "Waiting for Player 1")
                    +
                    "\n" + roomId + ": " +
                    (player2 != null ? "Player 2 (" + player2.getName() + ") - score: " + player2.getScore()
                            : "Waiting for Player 2"));
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
            broadcastRoomInfo(roomName);
        }

        private void handleJoinRoom(String roomName) {
            RoomData roomData = rooms.get(roomName);
            if (roomData != null && !roomData.isFull()) {
                roomData.addPlayer(clientName);
                currentRoom = roomName;
                out.println("JOINED_ROOM:" + roomName);
                broadcastRoomInfo(roomName);
                if (roomData.isFull()) {
                    roomData.setGameStarted(true);
                    broadcastToRoom(roomName, "START_GAME", false);
                }
            } else {
                out.println("ROOM_FULL");
            }
        }

        private void broadcastRoomInfo(String roomName) {
            RoomData roomData = rooms.get(roomName);
            String message = "ROOM_INFO:" + roomName + ":" +
                    roomData.getOpponentName(clientName) + ":" +
                    roomData.getOpponentScore(clientName);
            broadcastToRoom(roomName, message, false);
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
                roomData.updatePlayerStats(playerName, linesCleared, level);
                broadcastToRoom(currentRoom, "GAME_STATE:" + playerName + ":" + gameState, false);
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
    }

    public static void main(String[] args) {
        try {
            new GameServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
