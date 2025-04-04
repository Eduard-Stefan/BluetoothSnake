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
    private static final String UUID_STRING = "0000110100001000800000805F9B34FB";
    private static final long RETRY_DELAY_MS = 1000;
    private final SnakeApp app;
    private boolean serverShouldBeRunning = true;

    public BluetoothServer(SnakeApp app) {
        this.app = app;
    }

    @Override
    public void run() {
        LocalDevice localDevice = null;
        while (serverShouldBeRunning && localDevice == null) {
            try {
                localDevice = LocalDevice.getLocalDevice();
                try {
                    localDevice.setDiscoverable(DiscoveryAgent.GIAC);
                } catch (BluetoothStateException ignored) {
                }
            } catch (BluetoothStateException bse) {
                SwingUtilities.invokeLater(() -> app.updateBluetoothStatus("Bluetooth: Off"));
                if (sleepInterruptibly(RETRY_DELAY_MS)) return;
            }
        }
        while (serverShouldBeRunning) {
            StreamConnectionNotifier notifier = null;
            StreamConnection connection = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                UUID uuid = new UUID(UUID_STRING, false);
                String url = "btspp://localhost:" + uuid + ";name=SnakeControlService;authenticate=false;encrypt=false";
                notifier = (StreamConnectionNotifier) Connector.open(url);
                SwingUtilities.invokeLater(() -> app.updateBluetoothStatus("Bluetooth: Waiting for connection..."));
                connection = notifier.acceptAndOpen();
                if (!serverShouldBeRunning) break;
                SwingUtilities.invokeLater(() -> app.updateBluetoothStatus("Bluetooth: Connected"));
                inputStream = connection.openInputStream();
                outputStream = connection.openOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String command;
                while (serverShouldBeRunning) {
                    command = reader.readLine();
                    if (command != null) {
                        processCommand(command);
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                if (serverShouldBeRunning) {
                    if (e.getMessage() == null || !e.getMessage().contains("Notifier is closed")) {
                        if (connection == null) {
                            SwingUtilities.invokeLater(() -> app.updateBluetoothStatus("Bluetooth: Off"));
                            if (sleepInterruptibly(1000)) return;
                        }
                    }
                }
            } finally {
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
                if (serverShouldBeRunning) {
                    if (sleepInterruptibly(500)) {
                        return;
                    }
                }
            }
        }
    }

    private void closeQuietly(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException ignored) {
            }
        }
    }

    private boolean sleepInterruptibly(long millis) {
        try {
            Thread.sleep(millis);
            return false;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            serverShouldBeRunning = false;
            return true;
        }
    }

    private void processCommand(String command) {
        final SnakeGame currentGame = app.game;
        if (app.gameState == GameState.GAME && currentGame != null) {
            SwingUtilities.invokeLater(() -> {
                if (app.gameState == GameState.GAME && app.game == currentGame) {
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