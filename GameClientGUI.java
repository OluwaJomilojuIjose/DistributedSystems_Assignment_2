import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;

public class GameClientGUI extends Application {
    private GameInterface server;
    private String playerName;
    private String gameId;
    private int boardSize;
    private String[][] board;
    private Button[][] buttons;
    private Label statusLabel;
    private TextArea chatArea;
    private TextField chatInput;
    private Stage primaryStage;
    private String currentTurn;
    private boolean gameOver = false;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Connect to the server
        try {
            server = (GameInterface) Naming.lookup("rmi://localhost/GameServer");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Show login and game settings
        showLoginAndSettings();
    }

    private void showLoginAndSettings() {
        TextField nameField = new TextField();
        Spinner<Integer> boardSizeSpinner = new Spinner<>(3, 10, 3);
        Spinner<Integer> winConditionSpinner = new Spinner<>(3, 10, 3);
        CheckBox aiCheckBox = new CheckBox("Play with AI");
        Button startButton = new Button("Start Game");

        VBox settingsLayout = new VBox(10);
        settingsLayout.getChildren().addAll(
                new Label("Enter your name:"),
                nameField,
                new Label("Select board size:"),
                boardSizeSpinner,
                new Label("Select win condition:"),
                winConditionSpinner,
                aiCheckBox,
                startButton
        );

        Scene settingsScene = new Scene(settingsLayout, 300, 300);
        primaryStage.setScene(settingsScene);
        primaryStage.setTitle("Tic-Tac-Toe Settings");
        primaryStage.show();

        startButton.setOnAction(event -> {
            playerName = nameField.getText();
            boardSize = boardSizeSpinner.getValue();
            int winCondition = winConditionSpinner.getValue();
            boolean playWithAI = aiCheckBox.isSelected();

            if (playerName.isEmpty()) {
                showAlert("Name cannot be empty.");
                return;
            }
            if (winCondition > boardSize) {
                showAlert("Win condition cannot be greater than board size.");
                return;
            }

            try {
                if (playWithAI) {
                    gameId = server.createGame(playerName, boardSize, winCondition, true);
                    System.out.println("Starting a game against AI.");
                } else {
                    gameId = server.findGame(playerName, boardSize, winCondition);
                    System.out.println("Joined game: " + gameId);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            board = new String[boardSize][boardSize];
            buttons = new Button[boardSize][boardSize];
            currentTurn = "";

            showGameBoard();
            startGameLoop();
        });
    }

    private void showGameBoard() {
        GridPane gridPane = new GridPane();

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                Button button = new Button();
                button.setPrefSize(50, 50);
                int r = row;
                int c = col;
                button.setOnAction(event -> {
                    if (gameOver || !button.getText().isEmpty()) return;
                    try {
                        String result = server.makeMove(gameId, playerName, r, c);
                        if (!result.equals("Move accepted!")) {
                            showAlert(result);
                        } else {
                            updateBoard();
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                });
                buttons[row][col] = button;
                gridPane.add(button, col, row);
            }
        }

        statusLabel = new Label("Game Status");
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatInput = new TextField();
        chatInput.setPromptText("Enter message");
        chatInput.setOnAction(event -> {
            String message = chatInput.getText();
            try {
                server.sendMessage(gameId, playerName, message);
                chatInput.clear();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        VBox chatBox = new VBox(5, chatArea, chatInput);
        chatBox.setPrefWidth(200);

        HBox mainLayout = new HBox(10, gridPane, chatBox);
        VBox rootLayout = new VBox(10, statusLabel, mainLayout);

        Scene gameScene = new Scene(rootLayout);
        primaryStage.setScene(gameScene);
        primaryStage.setTitle("Tic-Tac-Toe Game");
        primaryStage.show();
    }

    private void startGameLoop() {
        Thread gameThread = new Thread(() -> {
            while (!gameOver) {
                try {
                    Platform.runLater(() -> {
                        try {
                            updateBoard();
                            updateStatus();
                            receiveMessages();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    });
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        gameThread.setDaemon(true);
        gameThread.start();
    }

    private void updateBoard() throws RemoteException {
        int size = server.getBoardSize(gameId);
        String gameState = server.getGameState(gameId);
        String[] rows = gameState.trim().split("\n");
        for (int row = 0; row < size; row++) {
            String[] cells = rows[row].trim().split("\\s+");
            for (int col = 0; col < size; col++) {
                String symbol = cells[col].equals("-") ? "" : cells[col];
                buttons[row][col].setText(symbol);
            }
        }
    }

    private void updateStatus() throws RemoteException {
        String winnerMessage = server.getWinner(gameId);
        String turnMessage = server.getTurn(gameId);

        if (winnerMessage.contains("Winner") || winnerMessage.contains("draw")) {
            statusLabel.setText(winnerMessage);
            gameOver = true;
        } else {
            statusLabel.setText(turnMessage);
        }
    }

    private void receiveMessages() throws RemoteException {
        List<String> messages = server.receiveMessages(gameId, playerName);
        for (String msg : messages) {
            chatArea.appendText(msg + "\n");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
