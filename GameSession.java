import java.io.Serializable;
import java.util.*;

public class GameSession implements Serializable {
    public String gameId;
    public String player1, player2;
    public String[][] board;
    public String currentTurn;
    public String winner;
    public boolean isAI;
    public List<String> spectators;
    public Map<String, List<String>> messageQueues;
    public int boardSize;
    public int winCondition;

    public GameSession(String gameId, String playerName, int boardSize, int winCondition) {
        this.gameId = gameId;
        this.player1 = playerName;
        this.boardSize = boardSize;
        this.winCondition = winCondition;
        this.board = new String[boardSize][boardSize];
        for (String[] row : board) {
            Arrays.fill(row, "");
        }
        this.currentTurn = null;
        this.winner = null;
        this.isAI = false;
        this.spectators = new ArrayList<>();
        this.messageQueues = new HashMap<>();
    }

    public boolean isBoardFull() {
        for (String[] row : board) {
            for (String cell : row) {
                if (cell.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
