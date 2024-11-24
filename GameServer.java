import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class GameServer extends UnicastRemoteObject implements GameInterface {

    private Map<String, GameSession> gameSessions;

    private Map<String, int[]> playerStats;

    public GameServer() throws RemoteException {
        super();
        gameSessions = new HashMap<>();
        playerStats = new HashMap<>();
    }

    // Game methods

    @Override
    public synchronized String createGame(String playerName, int boardSize, int winCondition, boolean playWithAI) throws RemoteException {
        String gameId = (playWithAI ? "AIGame" : "Game") + (gameSessions.size() + 1);
        GameSession newSession = new GameSession(gameId, playerName, boardSize, winCondition);
        if (playWithAI) {
            newSession.player2 = "AI";
            newSession.isAI = true;
            newSession.currentTurn = newSession.player1;
        }
        gameSessions.put(gameId, newSession);
        return gameId;
    }

    @Override
    public synchronized String findGame(String playerName, int boardSize, int winCondition) throws RemoteException {
        // Find a game waiting for a second player with matching settings
        for (GameSession session : gameSessions.values()) {
            if (session.player2 == null && !session.isAI && session.boardSize == boardSize && session.winCondition == winCondition) {
                session.player2 = playerName;
                session.currentTurn = session.player1; // Start the game
                return session.gameId;
            }
        }
        // Create a new game
        return createGame(playerName, boardSize, winCondition, false);
    }

    @Override
    public synchronized String makeMove(String gameId, String playerName, int row, int col) throws RemoteException {
        GameSession session = gameSessions.get(gameId);
        if (session == null) {
            return "Invalid game ID!";
        }
        if (session.winner != null) {
            return "The game has already ended!";
        }
        if (!playerName.equals(session.currentTurn)) {
            return "It's not your turn!";
        }
        if (row < 0 || row >= session.boardSize || col < 0 || col >= session.boardSize || !session.board[row][col].isEmpty()) {
            return "Invalid move!";
        }
        session.board[row][col] = playerName.equals(session.player1) ? "X" : "O";
        if (checkWinner(session, row, col)) {
            session.winner = session.currentTurn;
            // Record the game result
            recordGameResult(session.winner, true);
            String loser = session.winner.equals(session.player1) ? session.player2 : session.player1;
            if (loser != null && !loser.equals("AI")) {
                recordGameResult(loser, false);
            }
        } else {
            if (session.isBoardFull()) {
                session.winner = "Draw";
            } else {
                session.currentTurn = session.currentTurn.equals(session.player1) ? session.player2 : session.player1;
            }
        }

        // If playing against AI and it's AI's turn
        if (session.isAI && session.currentTurn.equals("AI") && session.winner == null) {
            aiMakeMove(session);
        }
        return "Move accepted!";
    }

    @Override
    public synchronized String getGameState(String gameId) throws RemoteException {
        GameSession session = gameSessions.get(gameId);
        if (session == null) {
            return "Invalid game ID!";
        }
        StringBuilder state = new StringBuilder();
        for (int row = 0; row < session.boardSize; row++) {
            for (int col = 0; col < session.boardSize; col++) {
                String cell = session.board[row][col];
                state.append(cell.isEmpty() ? "-" : cell).append(" ");
            }
            state.append("\n");
        }
        return state.toString();
    }

    @Override
    public synchronized String getWinner(String gameId) throws RemoteException {
        GameSession session = gameSessions.get(gameId);
        if (session == null) {
            return "Invalid game ID!";
        }
        if (session.winner == null) {
            return "No winner yet!";
        } else if (session.winner.equals("Draw")) {
            return "The game is a draw!";
        } else {
            return "Winner: " + session.winner;
        }
    }

    @Override
    public synchronized String getTurn(String gameId) throws RemoteException {
        GameSession session = gameSessions.get(gameId);
        if (session == null) {
            return "Invalid game ID!";
        }
        if (session.winner != null) {
            return "The game has ended.";
        }
        return session.currentTurn == null ? "Game hasn't started yet!" : "It's " + session.currentTurn + "'s turn.";
    }

    @Override
    public synchronized int getBoardSize(String gameId) throws RemoteException {
        GameSession session = gameSessions.get(gameId);
        return session != null ? session.boardSize : -1;
    }

    // Chat methods

    @Override
    public synchronized void sendMessage(String gameId, String playerName, String message) throws RemoteException {
        GameSession session = gameSessions.get(gameId);
        if (session == null) return;

        String opponent = playerName.equals(session.player1) ? session.player2 : session.player1;
        if (opponent != null && !opponent.equals("AI")) {
            session.messageQueues.computeIfAbsent(opponent, k -> new ArrayList<>()).add(playerName + ": " + message);
        }
    }

    @Override
    public synchronized List<String> receiveMessages(String gameId, String playerName) throws RemoteException {
        GameSession session = gameSessions.get(gameId);
        if (session == null) return new ArrayList<>();

        List<String> messages = session.messageQueues.getOrDefault(playerName, new ArrayList<>());
        session.messageQueues.put(playerName, new ArrayList<>()); // Clear messages after retrieval
        return messages;
    }

    // Leaderboard methods

    @Override
    public synchronized void recordGameResult(String playerName, boolean won) throws RemoteException {
        if (playerName.equals("AI")) return;
        int[] stats = playerStats.getOrDefault(playerName, new int[]{0, 0});
        if (won) {
            stats[0]++; // Increment wins
        } else {
            stats[1]++; // Increment losses
        }
        playerStats.put(playerName, stats);
    }

    @Override
    public synchronized Map<String, int[]> getLeaderboard() throws RemoteException {
        return playerStats;
    }


    @Override
    public synchronized String joinAsSpectator(String gameId) throws RemoteException {

        return null;
    }

    @Override
    public synchronized String getSpectatorGameState(String gameId) throws RemoteException {

        return null;
    }

    // AI logic

    private void aiMakeMove(GameSession session) {
        Random rand = new Random();
        int row, col;
        do {
            row = rand.nextInt(session.boardSize);
            col = rand.nextInt(session.boardSize);
        } while (!session.board[row][col].isEmpty());
        session.board[row][col] = "O"; // AI uses 'O'

        // Check if AI wins
        if (checkWinner(session, row, col)) {
            session.winner = "AI";
            try {
                recordGameResult(session.player1, false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else if (session.isBoardFull()) {
            session.winner = "Draw";
        } else {
            session.currentTurn = session.player1;
        }
    }

    // Helper methods

    private boolean checkWinner(GameSession session, int lastRow, int lastCol) {
        String[][] board = session.board;
        String symbol = board[lastRow][lastCol];
        int boardSize = session.boardSize;
        int winCondition = session.winCondition;

        // Check row
        int count = 0;
        for (int col = 0; col < boardSize; col++) {
            count = board[lastRow][col].equals(symbol) ? count + 1 : 0;
            if (count == winCondition) return true;
        }

        // Check column
        count = 0;
        for (int row = 0; row < boardSize; row++) {
            count = board[row][lastCol].equals(symbol) ? count + 1 : 0;
            if (count == winCondition) return true;
        }

        // Check diagonal (\)
        count = 0;
        int startRow = lastRow - Math.min(lastRow, lastCol);
        int startCol = lastCol - Math.min(lastRow, lastCol);
        while (startRow < boardSize && startCol < boardSize) {
            count = board[startRow][startCol].equals(symbol) ? count + 1 : 0;
            if (count == winCondition) return true;
            startRow++;
            startCol++;
        }

        // Check anti-diagonal (/)
        count = 0;
        startRow = lastRow + Math.min(boardSize - 1 - lastRow, lastCol);
        startCol = lastCol - Math.min(boardSize - 1 - lastRow, lastCol);
        while (startRow >= 0 && startCol < boardSize) {
            count = board[startRow][startCol].equals(symbol) ? count + 1 : 0;
            if (count == winCondition) return true;
            startRow--;
            startCol++;
        }

        return false;
    }

    public static void main(String[] args) {
        try {
            Naming.rebind("GameServer", new GameServer());
            System.out.println("Game Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
