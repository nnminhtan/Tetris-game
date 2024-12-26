public class PlayerData {
    private String name;
    private int score;
    private int linesCleared;
    private int level;
    private int opponentScore = 0;

    public PlayerData(String name) {
        this.name = name;
        this.score = 0;
        this.linesCleared = 0;
        this.level = 0;
        // this.isGameOver = false;
        // this.wins = 0;

    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
        System.out.println("Updated score for " + name + ": " + score);
    }

    public void setLinesCleared(int lines) {
        this.linesCleared = lines;
    }

    public int getLinesCleared() {
        return linesCleared;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public void setOpponentScore(int score) {
        this.opponentScore = score;
    }

    public int getOpponentScore() {
        return opponentScore;
    }

    @Override
    public String toString() {
        return name + " - Score: " + score +
                ", Lines: " + linesCleared +
                ", Level: " + level;
    }
}