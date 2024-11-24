import java.rmi.Naming;
import java.util.Scanner;

public class SpectatorClient {
    public static void main(String[] args) {
        try {
            GameInterface server = (GameInterface) Naming.lookup("rmi://localhost/GameServer");
            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter the game ID to spectate: ");
            String gameId = scanner.nextLine();

            String spectatorId = server.joinAsSpectator(gameId);
            System.out.println(spectatorId);

            String lastGameState = "";

            while (true) {
                String currentGameState = server.getSpectatorGameState(gameId);

                if (!currentGameState.equals(lastGameState)) {
                    System.out.println(currentGameState);
                    lastGameState = currentGameState;
                }

                Thread.sleep(1000); // Wait before checking again
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
