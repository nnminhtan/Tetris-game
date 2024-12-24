public class RoomData {
    private String roomId;
    private PlayerData player1;
    private PlayerData player2;
    private boolean isGameStarted;

    public RoomData(String roomId) {
        this.roomId = roomId;
        this.isGameStarted = false;
    }

    public void addPlayer(String playerName) {
        if (player1 == null) {
            player1 = new PlayerData(playerName);
        } else if (player2 == null) {
            player2 = new PlayerData(playerName);
        }
    }

    public String getOpponentName(String playerName) {
        if (player1 != null && player1.getName().equals(playerName)) {
            return player2 != null ? player2.getName() : "Waiting...";
        } else if (player2 != null && player2.getName().equals(playerName)) {
            return player1 != null ? player1.getName() : "Waiting...";
        }
        return "Unknown";
    }

    public void updateScore(String playerName, int score) {
        if (player1 != null && player1.getName().equals(playerName)) {
            player1.setScore(score);
        } else if (player2 != null && player2.getName().equals(playerName)) {
            player2.setScore(score);
        }
        printRoomStatus();
    }

    public int getOpponentScore(String playerName) {
        if (player1 != null && player1.getName().equals(playerName)) {
            return player2 != null ? player2.getScore() : 0;
        } else if (player2 != null && player2.getName().equals(playerName)) {
            return player1 != null ? player1.getScore() : 0;
        }
        return 0;
    }

    public PlayerData getPlayerData(String playerName) {
        if (player1 != null && player1.getName().equals(playerName)) {
            return player1;
        } else if (player2 != null && player2.getName().equals(playerName)) {
            return player2;
        }
        return null;
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

    public PlayerData getOpponentData(String playerName) {
        if (player1 != null && player1.getName().equals(playerName)) {
            return player2;
        } else if (player2 != null && player2.getName().equals(playerName)) {
            return player1;
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

    @Override
    public String toString() {
        return roomId + " Status:\n" +
                "Player 1: " + (player1 != null ? player1.getName() + " - score: " + player1.getScore() : "Waiting...")
                + "\n" +
                "Player 2: " + (player2 != null ? player2.getName() + " - score: " + player2.getScore() : "Waiting...");
    }

    // Getters and setters
    public String getRoomId() {
        return roomId;
    }

    public boolean isGameStarted() {
        return isGameStarted;
    }

    public void setGameStarted(boolean started) {
        this.isGameStarted = started;
    }

    public boolean isFull() {
        return player1 != null && player2 != null;
    }
}