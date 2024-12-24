import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 8888;
    private static ConcurrentHashMap<String, List<ClientHandler>> rooms = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, GameState> gameStates = new ConcurrentHashMap<>();

    static class GameState {
        long startTime;
        boolean isActive;
        Map<String, Integer> scores;
        
        GameState() {
            startTime = System.currentTimeMillis();
            isActive = false;
            scores = new ConcurrentHashMap<>();
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = in.readLine();
                    if (message == null) break;

                    if (message.startsWith("CREATE ")) {
                        handleCreateRoom(message.split(" ")[1]);
                    } else if (message.startsWith("JOIN ")) {
                        handleJoinRoom(message.split(" ")[1]);
                    } else if (message.startsWith("GAME_STATE:")) {
                        handleGameState(message);
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
            rooms.putIfAbsent(roomName, new ArrayList<>());
            rooms.get(roomName).add(this);
            currentRoom = roomName;
            out.println("ROOM_CREATED:" + roomName);
            checkRoomStatus(roomName);
        }

        private void handleJoinRoom(String roomName) {
            if (rooms.containsKey(roomName)) {
                rooms.get(roomName).add(this);
                currentRoom = roomName;
                out.println("JOINED_ROOM:" + roomName);
                checkRoomStatus(roomName);
            } else {
                out.println("ROOM_NOT_FOUND");
            }
        }

        private void checkRoomStatus(String roomName) {
            List<ClientHandler> clients = rooms.get(roomName);
            if (clients.size() >= 2) {
                for (ClientHandler client : clients) {
                    client.out.println("START_GAME");
                }
                gameStates.put(roomName, new GameState());
            }
        }

        private void handleGameState(String message) {
            if (currentRoom != null) {
                broadcastToRoom(currentRoom, message, true);
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
                List<ClientHandler> roomClients = rooms.get(currentRoom);
                if (roomClients != null) {
                    roomClients.remove(this);
                    if (roomClients.isEmpty()) {
                        rooms.remove(currentRoom);
                        gameStates.remove(currentRoom);
                    }
                }
            }
        }

        private void broadcastToRoom(String roomName, String message, boolean excludeSelf) {
            List<ClientHandler> clients = rooms.get(roomName);
            if (clients != null) {
                for (ClientHandler client : clients) {
                    if (!excludeSelf || !client.equals(this)) {
                        client.out.println(message);
                    }
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
