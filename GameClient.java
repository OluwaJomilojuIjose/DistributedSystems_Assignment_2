import java.rmi.Naming;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class GameClient {
    public static void main(String[] args) {
        try {
            GameInterface server = (GameInterface) Naming.lookup("rmi://localhost/GameServer");
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter your name: ");
            String playerName = scanner.nextLine();

            System.out.println("Do you want to play against another player or AI? (Type 'player' or 'AI'):");
            String opponentChoice = scanner.nextLine();

            int boardSize = 3;
            int winCondition = 3;
            System.out.print("Enter board size (default 3): ");
            String boardSizeInput = scanner.nextLine();
            if (!boardSizeInput.isEmpty()) {
                boardSize = Integer.parseInt(boardSizeInput);
            }
            System.out.print("Enter win condition (default 3): ");
            String winConditionInput = scanner.nextLine();
            if (!winConditionInput.isEmpty()) {
                winCondition = Integer.parseInt(winConditionInput);
            }

            String gameId;
            if (opponentChoice.equalsIgnoreCase("AI")) {
                gameId = server.createGame(playerName, boardSize, winCondition, true);
                System.out.println("Starting a game against AI.");
            } else {
                gameId = server.findGame(playerName, boardSize, winCondition);
                System.out.println("Joined game: " + gameId);
            }

            String lastGameState = "";
            String lastTurn = "";
            boolean waitingMessageDisplayed = false;

            while (true) {
                // Receive and display messages
                List<String> messages = server.receiveMessages(gameId, playerName);
                for (String msg : messages) {
                    System.out.println(msg);
                }

                // Get current game state and turn
                String currentGameState = server.getGameState(gameId);
                String currentTurn = server.getTurn(gameId);

                // Check for winner
                String winnerMessage = server.getWinner(gameId);
                if (winnerMessage.contains("Winner") || winnerMessage.contains("draw")) {
                    if (!currentGameState.equals(lastGameState)) {
                        System.out.println("Game Board:\n" + currentGameState);
                        lastGameState = currentGameState;
                    }
                    System.out.println(winnerMessage);
                    break;
                }

                // If the game state or turn has changed, display the new state
                if (!currentGameState.equals(lastGameState) || !currentTurn.equals(lastTurn)) {
                    System.out.println("Game Board:\n" + currentGameState);
                    System.out.println(currentTurn);
                    lastGameState = currentGameState;
                    lastTurn = currentTurn;
                    waitingMessageDisplayed = false; // Reset waiting message on state change
                }

                if (!currentTurn.contains(playerName)) {
                    if (!waitingMessageDisplayed) {
                        System.out.println("Waiting for the other player...");
                        waitingMessageDisplayed = true;
                    }
                    Thread.sleep(1000); // Wait before checking again
                    continue;
                }

                // It's the player's turn
                // Reset waiting message flag
                waitingMessageDisplayed = false;


                System.out.println("Enter your move as row and column (e.g., 1 1), 'chat' to send a message, or 'leaderboard' to view rankings:");
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("chat")) {
                    System.out.print("Enter your message: ");
                    String message = scanner.nextLine();
                    server.sendMessage(gameId, playerName, message);
                    continue;
                }

                if (input.equalsIgnoreCase("leaderboard")) {
                    Map<String, int[]> leaderboard = server.getLeaderboard();
                    System.out.println("Leaderboard:");
                    for (Map.Entry<String, int[]> entry : leaderboard.entrySet()) {
                        String name = entry.getKey();
                        int wins = entry.getValue()[0];
                        int losses = entry.getValue()[1];
                        System.out.println(name + " - Wins: " + wins + ", Losses: " + losses);
                    }
                    continue;
                }

                String[] parts = input.trim().split("\\s+");
                if (parts.length != 2) {
                    System.out.println("Invalid input! Enter two numbers separated by a space.");
                    continue;
                }

                try {
                    int row = Integer.parseInt(parts[0]);
                    int col = Integer.parseInt(parts[1]);
                    String moveResult = server.makeMove(gameId, playerName, row, col);
                    System.out.println(moveResult);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid numbers! Please enter valid integers.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
