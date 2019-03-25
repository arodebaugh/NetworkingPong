package sample;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;

public class CommunicationIn implements Runnable {
    private Socket socket;
    private ObjectInputStream messageReader;
    private SynchronizedQueue inQueue;
    private SynchronizedQueue outQueue;
    private boolean serverMode;
    private int player;
    private Rectangle paddle;
    private Circle ball;
    private Controller controller;
    private Text score;

    // CommunicationIn reads from a Socket and puts data into the Program's inQueue

    CommunicationIn(Controller c, Socket s, ObjectInputStream in, SynchronizedQueue inQ, SynchronizedQueue outQ, int p, Rectangle pa, Circle b, Text sc) {
        controller = c;
        socket = s;
        messageReader = in;
        // CommunicationIn puts data read from the socket into the inQueue
        inQueue = inQ;
        // Only the server needs the outQueue from CommunicationIn
        outQueue = outQ;
        serverMode = (outQ != null);
        player = p;
        paddle = pa;
        ball = b;
        score = sc;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("CommunicationIn Thread");
        System.out.println("CommunicationIn thread running");

        try {

            // Read all incoming communication
            // dataReader reads objects from 1 socket
            while (Controller.connected && !Thread.interrupted()) {
                Message message = null;
                while (message == null) {
                    try {
                        message = (Message) messageReader.readObject();
                    } catch (EOFException ex) {
                        // EOFException means data has NOT been written yet; so yield and try reading again
                        Thread.currentThread().yield();
                    }
                }
                final Message finalMessage = message;

                if (finalMessage.sender() == player) {
                    if (finalMessage.data() == 9999) {
                        controller.paused = false;
                        controller.start();
                    }

                    if (finalMessage.data() == 7777) {
                        controller.resetWalls();
                    }

                    if (finalMessage.data() == 5555) {
                        controller.disconnect();
                    }

                    Platform.runLater(() -> {
                        System.out.println(finalMessage.data());
                        paddle.setY(finalMessage.data());

                        if (!serverMode) {
                            score.setText(finalMessage.player0Score() + " - " + finalMessage.player1Score());
                            controller.ball1X = finalMessage.ballX();
                            controller.ball1Y = finalMessage.ballY();
                            ball.setLayoutX(finalMessage.ballX());
                            ball.setLayoutY(finalMessage.ballY());
                        }
                    });
                }

                System.out.println("CommunicationIn: RECEIVING " + message);
                // Receiving incoming message!!!

                // ignore any messages sent by yourself: only put messages from others into your inQueue
                // Now put message on the InputQueue so that the GUI will see it
                boolean putSucceeded = inQueue.put(message);
                while (!putSucceeded) {
                    Thread.currentThread().yield();
                    putSucceeded = inQueue.put(message);
                }
                System.out.println("CommunicationIn PUT into InputQueue: " + message);

                // IF SERVER and MULTICAST: also put that incoming message on the OutputQueue so ALL clients see it
                if (serverMode && Server.multicastMode) {
                    putSucceeded = outQueue.put(message);
                    while (!putSucceeded) {
                        Thread.currentThread().yield();
                        putSucceeded = outQueue.put(message);
                    }
                    System.out.println("CommunicationIn MULTICAST into OutputQueue: " + message);

                }
            }


            // while loop ended!  close reader and socket
            socket.close();
            System.out.println("CommunicationIn thread DONE; reader and socket closed.");

        } catch (SocketException se) {
            // nothing to do
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            // CommunicationIn ending!
            socket.close();
            System.out.println("CommunicationIn thread DONE; reader and socket closed.");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
