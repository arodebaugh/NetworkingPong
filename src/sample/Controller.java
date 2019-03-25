package sample;

import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Controller {
    private static final int sceneWidth = 500;
    private static final int sceneHeight = 430;

    private Pane nodes;
    private double mouseX;
    private double mouseY;

    double ball1X = 100;
    double ball1Y = 100;
    private double ball1Vx = 0;
    private double ball1Vy = 0;
    private double ball1Speed = 4.0;
    private double relativeIntersectY1;
    private double normalizedRelativeIntersectionY1;
    private double bounceAngle1;

    private double ms;
    private double lastTime = System.currentTimeMillis();

    private Circle ball1;

    private Rectangle leftPaddle;
    private Rectangle rightPaddle;

    private Rectangle topWall;
    private Rectangle bottomWall;

    private Rectangle[] leftBreak = new Rectangle[10];
    private Rectangle[] rightBreak = new Rectangle[10];

    private HBox connectPane;
    private HBox scorePane;

    private Button connect;
    private TextField ip;
    private Button reset;
    // private Button disconnect;

    private Text score;

    int player = 1;
    int player0Score = 0;
    int player1Score = 0;

    private Scene scene;
    private BorderPane root;

    private SynchronizedQueue inQueue;
    private SynchronizedQueue outQueue;

    private int lastSent;

    private boolean serverMode;
    static boolean connected;
    private boolean stopTick;
    boolean paused = false;

    Controller(Scene s, BorderPane r) {
        scene = s;
        root = r;
        stopTick = false;

        scene.setFill(Color.color(0, 0, 0));

        nodes = new Pane();

        inQueue = new SynchronizedQueue();
        outQueue = new SynchronizedQueue();
        connected = false;

        ball1 = new Circle(ball1X, ball1Y, 15);

        if (player == 0) {
            leftPaddle = new Rectangle(50, mouseY, 10, 80);
            rightPaddle = new Rectangle(sceneWidth - 70, 200, 10, 80);
        } else if (player == 1) {
            leftPaddle = new Rectangle(50, 200, 10, 80);
            rightPaddle = new Rectangle(sceneWidth - 70, mouseY, 10, 80);
        }

        leftPaddle.setFill(Color.WHITE);
        rightPaddle.setFill(Color.WHITE);

        topWall = new Rectangle(0,-10,sceneWidth + 10,15);
        bottomWall = new Rectangle(0,sceneWidth - 125,sceneWidth + 10,15);

        for (int x = 0; x <= 9; x++) { // 5 a row
            if (x < 5) {
                leftBreak[x] = new Rectangle(0, x * 82, 10, 80);
                rightBreak[x] = new Rectangle(sceneWidth - 10, x * 82, 10, 80);
            } else {
                leftBreak[x] = new Rectangle(12, (x - 5) * 82, 10, 80);
                rightBreak[x] = new Rectangle(sceneWidth - 22, (x - 5) * 82, 10, 80);
            }

            leftBreak[x].setFill(Color.WHITE);
            rightBreak[x].setFill(Color.WHITE);
        }

        connectPane = new HBox();
        connect = new Button("START");
        ip = new TextField("");

        scorePane = new HBox();
        score = new Text("0 - 0");
        reset = new Button("NEW GAME");
        // disconnect = new Button("DISCONNECT");

        topWall.setFill(Color.WHITE);
        bottomWall.setFill(Color.WHITE);

        score.setFill(Color.WHITE);

        score.setStyle("-fx-font-size: 20;");

        ball1.setFill(Color.WHITE);

        nodes.getChildren().add(ball1);
        nodes.getChildren().add(leftPaddle);
        nodes.getChildren().add(rightPaddle);
        nodes.getChildren().add(topWall);
        nodes.getChildren().add(bottomWall);

        for (int x = 0; x <= 9; x++) {
            nodes.getChildren().add(leftBreak[x]);
            nodes.getChildren().add(rightBreak[x]);
        }

        connectPane.getChildren().add(ip);
        connectPane.getChildren().add(connect);
        connectPane.getChildren().add(reset);
        // connectPane.getChildren().add(disconnect);

        reset.setVisible(false);
        // disconnect.setVisible(false);

        scorePane.getChildren().add(score);

        connectPane.setAlignment(Pos.CENTER);
        scorePane.setAlignment(Pos.CENTER);

        connectPane.setStyle("-fx-background-color: #000000;");
        scorePane.setStyle("-fx-background-color: #000000;");
        nodes.setStyle("-fx-background-color: #000000;");

        root.setCenter(nodes);
        root.setTop(connectPane);
        root.setBottom(scorePane);
    }

    void setServerMode() {
        serverMode = true;
    }

    void setClientMode() {
        serverMode = false;
    }

    void disconnect() {
        connected = false;
        connect.setVisible(true);

        // disconnect.setVisible(false);
        reset.setVisible(false);

        paused = true;

        lobby();
    }

    void reset(boolean resetScore, boolean sendReset) {
        System.out.println("RESET");
        ball1X = 100;
        ball1Y = 100;
        ball1.setLayoutX(100);
        ball1.setLayoutY(100);
        ball1Speed = 3.0;
        ball1Vx = 0;
        ball1Vy = 0;

        if (resetScore) {
            player0Score = 0;
            player1Score = 0;
        }

        if (sendReset) {
            Message message = new Message(player, 7777, ball1X, ball1Y, player0Score, player1Score);
            boolean putSucceeded = outQueue.put(message);
            while (!putSucceeded) {
                Thread.currentThread().yield();
                putSucceeded = outQueue.put(message);
            }
        }

        resetWalls();
    }

    void resetWalls() {
        for (int x = 0; x <= 9; x++) {
            leftBreak[x].setWidth(10);
            leftBreak[x].setHeight(80);

            rightBreak[x].setWidth(10);
            rightBreak[x].setHeight(80);
        }
    }

    void lobby() {
        connect.setVisible(true);
        ip.setVisible(true);

        if (serverMode) {
            connected = true;

            if (player == 0) {
                ConnectToNewClients connectToNewClients = new ConnectToNewClients(this, 8080, inQueue, outQueue, 1, rightPaddle, ball1, ip, score);
                Thread connectThread = new Thread(connectToNewClients);
                connectThread.start();
            } else if (player == 1) {
                //   Thread 2: handles communication FROM server TO client
                ConnectToNewClients connectToNewClients = new ConnectToNewClients(this, 8080, inQueue, outQueue, 0, leftPaddle, ball1, ip, score);
                Thread connectThread = new Thread(connectToNewClients);
                connectThread.start();
            }
        }

        connect.setOnAction(event -> {
            connect.setVisible(false);
            ip.setVisible(false);
            if (serverMode) {
                reset.setVisible(true);
                // disconnect.setVisible(true);
            }

            connected = true;

            reset(true, true);

            if (!serverMode) {
                // We're a client: connect to a server
                try {
                    Socket socketClientSide = new Socket(ip.getText(), 8080);

                    // The socketClientSide provides 2 separate streams for 2-way communication
                    //   the InputStream is for communication FROM server TO client
                    //   the OutputStream is for communication TO server FROM client
                    // Create data reader and writer from those stream (NOTE: ObjectOutputStream MUST be created FIRST)

                    // Every client prepares for communication with its server by creating 2 new threads:
                    //   Thread 1: handles communication TO server FROM client
                    CommunicationOut communicationOut = new CommunicationOut(socketClientSide, new ObjectOutputStream(socketClientSide.getOutputStream()), outQueue);
                    Thread communicationOutThread = new Thread(communicationOut);
                    communicationOutThread.start();

                    if (player == 0) {
                        //   Thread 2: handles communication FROM server TO client
                        CommunicationIn communicationIn = new CommunicationIn(this, socketClientSide, new ObjectInputStream(socketClientSide.getInputStream()), inQueue, null, 1, rightPaddle, ball1, score);
                        Thread communicationInThread = new Thread(communicationIn);
                        communicationInThread.start();
                    } else if (player == 1) {
                        //   Thread 2: handles communication FROM server TO client
                        CommunicationIn communicationIn = new CommunicationIn(this, socketClientSide, new ObjectInputStream(socketClientSide.getInputStream()), inQueue, null, 0, leftPaddle, ball1, score);
                        Thread communicationInThread = new Thread(communicationIn);
                        communicationInThread.start();
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // We connected!
            }

            Message message = new Message(player, 9999, ball1X, ball1Y, player0Score, player1Score);
            boolean putSucceeded = outQueue.put(message);
            while (!putSucceeded) {
                Thread.currentThread().yield();
                putSucceeded = outQueue.put(message);
            }
        });
    }

    void start() {
        Thread draw;

        setListeners();

        draw = new Thread(() -> {
            while (true) {
                tick();
                try {
                    Thread.sleep(25);
                } catch (InterruptedException err) {
                    System.out.println(err);
                }
            }
        });
        draw.start();
    }

    private void tick() {
        if (stopTick || !connected || paused) {
            return;
        }

        try {
            walls();
            if (serverMode) {
                testBounds();
                setBallPosition();
            }
            sendToServer();
        } catch (Exception ex) {
            stopTick = true;
            ex.printStackTrace();
        }

        reset.setOnAction(event -> {
            reset(true, true);
        });

        /*disconnect.setOnAction(event -> {
            Message message = new Message(player, 5555, ball1X, ball1Y, player0Score, player1Score);

            boolean putSucceeded = outQueue.put(message);
            while (!putSucceeded) {
                Thread.currentThread().yield();
                putSucceeded = outQueue.put(message);
            }

            disconnect();
        });*/
    }

    private void sendToServer() {
        Message message = new Message(player, (int) Math.round(mouseY), ball1X, ball1Y, player0Score, player1Score);

        boolean putSucceeded = outQueue.put(message);
        while (!putSucceeded) {
            Thread.currentThread().yield();
            putSucceeded = outQueue.put(message);
        }
    }

    private void walls() {
        Bounds ballBounds = ball1.getBoundsInParent();

        for (int x = 0; x <= 9; x++) {
            if (ballBounds.intersects(leftBreak[x].getBoundsInParent())) {
                relativeIntersectY1 = (ball1.getLayoutY() + (80/2)) - ball1.getLayoutX();
                normalizedRelativeIntersectionY1 = (relativeIntersectY1 / (80/2));
                bounceAngle1 = normalizedRelativeIntersectionY1 * 75;
                leftBreak[x].setWidth(0);
                leftBreak[x].setHeight(0);
            } else if (ballBounds.intersects(rightBreak[x].getBoundsInParent())) {
                relativeIntersectY1 = (ball1.getLayoutY() + (80 / 2)) - ball1.getLayoutX();
                normalizedRelativeIntersectionY1 = (relativeIntersectY1 / (80 / 2));
                bounceAngle1 = normalizedRelativeIntersectionY1 * 75;
                rightBreak[x].setWidth(0);
                rightBreak[x].setHeight(0);
            }
        }
    }

    private void testBounds() {
        try {
            Bounds leftPaddleBounds = leftPaddle.getBoundsInParent();
            Bounds rightPaddleBounds = rightPaddle.getBoundsInParent();
            Bounds topWallBounds = topWall.getBoundsInParent();
            Bounds bottomWallBounds = bottomWall.getBoundsInParent();
            Bounds ballBounds = ball1.getBoundsInParent();

            if (ballBounds.intersects(leftPaddleBounds)) {
                if (ball1Speed < 7.0) { // Todo: Speed is a problem... Gosh Darn It!
                    ball1Speed += .5;
                }

                relativeIntersectY1 = (ball1.getLayoutY() + (80 / 2)) - ball1.getLayoutX();
                normalizedRelativeIntersectionY1 = (relativeIntersectY1 / (80 / 2));
                bounceAngle1 = normalizedRelativeIntersectionY1 * 75;
            } else if (ballBounds.intersects(rightPaddleBounds)) {
                if (ball1Speed < 7.0) {
                    ball1Speed += .5;
                }

                relativeIntersectY1 = (ball1.getLayoutY() + (80 / 2)) - ball1.getLayoutX();
                normalizedRelativeIntersectionY1 = (relativeIntersectY1/ (80 / 2));
                bounceAngle1 = normalizedRelativeIntersectionY1 * 75;
            } else if (ballBounds.intersects(bottomWallBounds)) {
                relativeIntersectY1 = (ball1.getLayoutY() + (40 / 2)) - ball1.getLayoutX();
                normalizedRelativeIntersectionY1 = (relativeIntersectY1 / (80 / 2));
                bounceAngle1 = normalizedRelativeIntersectionY1 * 75;
            } else if (ballBounds.intersects(topWallBounds)) {
                relativeIntersectY1 = (ball1.getLayoutY() + (40 / 2)) - ball1.getLayoutX();
                normalizedRelativeIntersectionY1 = (relativeIntersectY1 / (80 / 2));
                bounceAngle1 = normalizedRelativeIntersectionY1 * 75;
            } else if (ball1.getLayoutX() > (sceneWidth + 20)) { // player 1 scores
                player0Score += 1;
                reset(false, true);
                return;
            } else if (ball1.getLayoutX() < -100) { // player 0 scores
                player1Score += 1;
                reset(false, true);
                return;
            }

            /*if (player0Score > 10 || player1Score > 10) {
                Message message = new Message(player, 5555, ball1X, ball1Y, player0Score, player1Score);

                boolean putSucceeded = outQueue.put(message);
                while (!putSucceeded) {
                    Thread.currentThread().yield();
                    putSucceeded = outQueue.put(message);
                }

                disconnect();
            }*/

            score.setText(player0Score + " - " + player1Score);

            System.out.println("0: " + player0Score + " 1: " + player1Score);

            ball1Vx = (ball1Speed * Math.cos(bounceAngle1));
            ball1Vy = (ball1Speed * -Math.sin(bounceAngle1));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setBallPosition() throws Exception {
        ball1X += ball1Vx;
        ball1Y += ball1Vy;

        ball1.setLayoutX(ball1X);
        ball1.setLayoutY(ball1Y);
    }

    private void setListeners() {
        scene.setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();

            if (player == 0) {
                leftPaddle.setY(mouseY);
            } else if (player == 1) {
                rightPaddle.setY(mouseY);
            }
        });
    }
}
