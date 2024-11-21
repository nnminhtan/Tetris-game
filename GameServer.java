import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 8888; // Server port
    private static ConcurrentHashMap<String, List<ClientHandler>> rooms = new ConcurrentHashMap<>();

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

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                this.out = out;

                // Initial handshake
                out.println("Welcome to the Tetris server!");
                out.println("Enter your name:");
                clientName = in.readLine();

                while (true) {
                    out.println("Enter a command: CREATE <room>, JOIN <room>, or LIST");
                    String command = in.readLine();
                    if (command.startsWith("CREATE")) {
                        String roomName = command.split(" ")[1];
                        createRoom(roomName);
                    } else if (command.startsWith("JOIN")) {
                        String roomName = command.split(" ")[1];
                        joinRoom(roomName);
                    } else if (command.equals("LIST")) {
                        listRooms();
                    } else {
                        out.println("Invalid command.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                leaveRoom();
                System.out.println(clientName + " disconnected.");
            }
        }

        private void createRoom(String roomName) {
            rooms.putIfAbsent(roomName, new ArrayList<>());
            out.println("Room " + roomName + " created.");
        }

        private void joinRoom(String roomName) {
            if (!rooms.containsKey(roomName)) {
                out.println("Room " + roomName + " does not exist.");
                return;
            }
            leaveRoom(); // Leave the current room if already in one
            currentRoom = roomName;
            rooms.get(roomName).add(this);
            out.println("Joined room " + roomName);
        }

        private void listRooms() {
            out.println("Available rooms:");
            for (String room : rooms.keySet()) {
                out.println(" - " + room + " (" + rooms.get(room).size() + " players)");
            }
        }

        private void leaveRoom() {
            if (currentRoom != null) {
                rooms.get(currentRoom).remove(this);
                if (rooms.get(currentRoom).isEmpty()) {
                    rooms.remove(currentRoom);
                }
                currentRoom = null;
            }
        }
    }
}
