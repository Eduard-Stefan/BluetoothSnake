package snake;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.swing.*;
import java.io.*;

public class BluetoothServer implements Runnable {
    // UUID for the Serial Port Profile (SPP) service
    private static final String UUID_STRING = "0000110100001000800000805F9B34FB";
    // Delay between retry attempts when Bluetooth is unavailable
    private static final long RETRY_DELAY_MS = 1000;

    // Reference to the main application
    private final SnakeApp app;
    // Flag to control server execution
    private boolean serverShouldBeRunning = true;

    // Constructor for the Bluetooth server
    public BluetoothServer(SnakeApp app) {
        this.app = app;
    }

    // Main server thread execution method
    @Override
    public void run() {
        LocalDevice localDevice = null;

        // First loop: Attempt to initialize Bluetooth
        while (serverShouldBeRunning && localDevice == null) {
            try {
                // Get the local Bluetooth device
                localDevice = LocalDevice.getLocalDevice();
                try {
                    // Make the device discoverable
                    localDevice.setDiscoverable(DiscoveryAgent.GIAC);
                } catch (BluetoothStateException ignored) {
                    // Discovery mode setting may fail, but we can still proceed
                }
            } catch (BluetoothStateException bse) {
                // Update UI and retry after delay if Bluetooth is off
                SwingUtilities.invokeLater(() -> app.updateBluetoothStatus("Bluetooth: Off"));
                if (sleepInterruptibly(RETRY_DELAY_MS)) return;
            }
        }

        // Second loop: Main server operation
        while (serverShouldBeRunning) {
            StreamConnectionNotifier notifier = null;
            StreamConnection connection = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                // Create a UUID object from our string
                UUID uuid = new UUID(UUID_STRING, false);
                // Create the service URL
                String url = "btspp://localhost:" + uuid + ";name=SnakeControlService;authenticate=false;encrypt=false";

                // Open the connection notifier
                notifier = (StreamConnectionNotifier) Connector.open(url);
                SwingUtilities.invokeLater(() -> app.updateBluetoothStatus("Bluetooth: Waiting for connection..."));

                // Wait for and accept a client connection
                connection = notifier.acceptAndOpen();
                if (!serverShouldBeRunning) break;

                // Connection established - update UI
                SwingUtilities.invokeLater(() -> app.updateBluetoothStatus("Bluetooth: Connected"));

                // Get the input and output streams
                inputStream = connection.openInputStream();
                outputStream = connection.openOutputStream();

                // Create a reader for the input stream
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String command;

                // Main command processing loop
                while (serverShouldBeRunning) {
                    command = reader.readLine();
                    if (command != null) {
                        processCommand(command);
                    } else {
                        // Null command indicates connection was closed
                        break;
                    }
                }
            } catch (IOException e) {
                // Handle connection errors
                if (serverShouldBeRunning) {
                    if (e.getMessage() == null || !e.getMessage().contains("Notifier is closed")) {
                        if (connection == null) {
                            // No connection was established - retry after delay
                            SwingUtilities.invokeLater(() -> app.updateBluetoothStatus("Bluetooth: Off"));
                            if (sleepInterruptibly(1000)) return;
                        }
                    }
                }
            } finally {
                // Clean up resources
                closeQuietly(inputStream);
                closeQuietly(outputStream);
                try {
                    if (connection != null) connection.close();
                } catch (IOException ignored) {
                }
                try {
                    if (notifier != null) notifier.close();
                } catch (IOException ignored) {
                }

                // If server should keep running, wait before retrying
                if (serverShouldBeRunning) {
                    if (sleepInterruptibly(500)) {
                        return;
                    }
                }
            }
        }
    }

    // Safely closes a Closeable resource, ignoring any exceptions
    private void closeQuietly(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException ignored) {
            }
        }
    }

    // Sleeps for the specified time, but can be interrupted
    private boolean sleepInterruptibly(long millis) {
        try {
            Thread.sleep(millis);
            return false;
        } catch (InterruptedException ie) {
            // Restore the interrupted status and stop the server
            Thread.currentThread().interrupt();
            serverShouldBeRunning = false;
            return true;
        }
    }

    // Processes a received command and updates the game state
    private void processCommand(String command) {
        final SnakeGame currentGame = app.game;
        // Only process commands if game is in progress
        if (app.gameState == GameState.GAME && currentGame != null) {
            SwingUtilities.invokeLater(() -> {
                // Verify game state hasn't changed since we checked
                if (app.gameState == GameState.GAME && app.game == currentGame) {
                    // Update snake direction based on command
                    switch (command.toLowerCase()) {
                        case "up":
                            currentGame.setDirection(Direction.UP);
                            break;
                        case "down":
                            currentGame.setDirection(Direction.DOWN);
                            break;
                        case "left":
                            currentGame.setDirection(Direction.LEFT);
                            break;
                        case "right":
                            currentGame.setDirection(Direction.RIGHT);
                            break;
                    }
                }
            });
        }
    }
}