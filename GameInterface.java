import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface GameInterface extends Remote {
    // Game methods
    String createGame(String playerName, int boardSize, int winCondition, boolean playWithAI) throws RemoteException;
    String findGame(String playerName, int boardSize, int winCondition) throws RemoteException;
    String makeMove(String gameId, String playerName, int row, int col) throws RemoteException;
    String getGameState(String gameId) throws RemoteException;
    String getWinner(String gameId) throws RemoteException;
    String getTurn(String gameId) throws RemoteException;
    int getBoardSize(String gameId) throws RemoteException;

    // Chat methods
    void sendMessage(String gameId, String playerName, String message) throws RemoteException;
    List<String> receiveMessages(String gameId, String playerName) throws RemoteException;

    // Leaderboard methods
    void recordGameResult(String playerName, boolean won) throws RemoteException;
    Map<String, int[]> getLeaderboard() throws RemoteException;

    // Spectator methods (Optional)
    String joinAsSpectator(String gameId) throws RemoteException;
    String getSpectatorGameState(String gameId) throws RemoteException;
}
