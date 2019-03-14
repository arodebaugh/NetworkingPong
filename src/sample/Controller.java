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
import javafx.scene.shape.Shape;
import javafx.scene.Group;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Controller {
    private static final int sceneWidth = 500;
    private static final int sceneHeight = 430;

    private Pane nodes;
    private double mouseX;
    private double mouseY;

    double ballX = 100;
    double ballY = 100;
    private double ballVx = 0;
    private double ballVy = 0;
    private double ballSpeed = 4.0;
    private double relativeIntersectY;
    private double normalizedRelativeIntersectionY;
    private double bounceAngle;

    private double ms;
    private double lastTime = System.currentTimeMillis();

    private Circle ball;

    private Rectangle leftPaddle;
    private Rectangle rightPaddle;

    private Rectangle leftWall;
    private Rectangle rightWall;
    private Rectangle topWall;
    private Rectangle bottomWall;

    private Rectangle[] leftBreak = new Rectangle[10];
    private Rectangle[] rightBreak = new Rectangle[10];

    private HBox connectPane;
    private Button connect;
    private TextField ip;

    public int player = 1;

    private Scene scene;
    private BorderPane root;

    private SynchronizedQueue inQueue;
    private SynchronizedQueue outQueue;

    private int lastSent;

    private boolean serverMode;
    static boolean connected;

    public Controller(Scene s, BorderPane r) {
        scene = s;
        root = r;

        scene.setFill(Color.color(0, 0, 0));

        nodes = new Pane();

        inQueue = new SynchronizedQueue();
        outQueue = new SynchronizedQueue();
        connected = false;

        ball = new Circle(ballX, ballY, 15);

        if (player == 0) {
            leftPaddle = new Rectangle(50, mouseY, 10, 80);
            rightPaddle = new Rectangle(sceneWidth - 70, 200, 10, 80);
        } else if (player == 1) {
            leftPaddle = new Rectangle(50, 200, 10, 80);
            rightPaddle = new Rectangle(sceneWidth - 70, mouseY, 10, 80);
        }

        leftPaddle.setFill(Color.WHITE);
        rightPaddle.setFill(Color.WHITE);

        leftWall = new Rectangle(0,0,5,sceneHeight + 50);
        rightWall = new Rectangle(sceneWidth - 5,0,5,sceneHeight + 50);
        topWall = new Rectangle(0,0,sceneWidth + 50,5);
        bottomWall = new Rectangle(0,sceneHeight - 5,sceneWidth + 50,5);

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

        leftWall.setFill(Color.BLACK);
        rightWall.setFill(Color.BLACK);
        topWall.setFill(Color.BLACK);
        bottomWall.setFill(Color.BLACK);

        ball.setFill(Color.WHITE);

        nodes.getChildren().add(ball);
        nodes.getChildren().add(leftPaddle);
        nodes.getChildren().add(rightPaddle);
        nodes.getChildren().add(leftWall);
        nodes.getChildren().add(rightWall);
        nodes.getChildren().add(topWall);
        nodes.getChildren().add(bottomWall);

        for (int x = 0; x <= 9; x++) {
            nodes.getChildren().add(leftBreak[x]);
            nodes.getChildren().add(rightBreak[x]);
        }

        connectPane.getChildren().add(ip);
        connectPane.getChildren().add(connect);

        connectPane.setAlignment(Pos.CENTER);

        connectPane.setStyle("-fx-background-color: #000000;");
        nodes.setStyle("-fx-background-color: #000000;");

        root.setCenter(nodes);
        root.setTop(connectPane);
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
        lobby();
    }

    void reset() {
        System.out.println("RESET");
        ballX = 100;
        ballY = 100;
        ball.setLayoutX(100);
        ball.setLayoutY(100);
        ballSpeed = 3.0;
        ballVx = 0;
        ballVy = 0;

        Message message = new Message(player, 7777, ballX, ballY);
        boolean putSucceeded = outQueue.put(message);
        while (!putSucceeded) {
            Thread.currentThread().yield();
            putSucceeded = outQueue.put(message);
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
        if (serverMode) {
            connected = true;

            if (player == 0) {
                ConnectToNewClients connectToNewClients = new ConnectToNewClients(this, 8080, inQueue, outQueue, 1, rightPaddle, ball, ip);
                Thread connectThread = new Thread(connectToNewClients);
                connectThread.start();
            } else if (player == 1) {
                //   Thread 2: handles communication FROM server TO client
                ConnectToNewClients connectToNewClients = new ConnectToNewClients(this, 8080, inQueue, outQueue, 0, leftPaddle, ball, ip);
                Thread connectThread = new Thread(connectToNewClients);
                connectThread.start();
            }
        }

        connect.setOnAction(event -> {
            connect.setVisible(false);
            ip.setVisible(false);
            connected = true;

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
                        CommunicationIn communicationIn = new CommunicationIn(this, socketClientSide, new ObjectInputStream(socketClientSide.getInputStream()), inQueue, null, 1, rightPaddle, ball);
                        Thread communicationInThread = new Thread(communicationIn);
                        communicationInThread.start();
                    } else if (player == 1) {
                        //   Thread 2: handles communication FROM server TO client
                        CommunicationIn communicationIn = new CommunicationIn(this, socketClientSide, new ObjectInputStream(socketClientSide.getInputStream()), inQueue, null, 0, leftPaddle, ball);
                        Thread communicationInThread = new Thread(communicationIn);
                        communicationInThread.start();
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // We connected!
            }

            Message message = new Message(player, 9999, ballX, ballY);
            boolean putSucceeded = outQueue.put(message);
            while (!putSucceeded) {
                Thread.currentThread().yield();
                putSucceeded = outQueue.put(message);
            }
        });
    }

    void start() {
        setListeners();

        Thread draw = new Thread(() -> {
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
        if (serverMode) {
            setBallPosition();
            testBounds();
        }
        walls();
        sendToServer();
    }

    private void sendToServer() {
        Message message = new Message(player, (int) Math.round(mouseY), ballX, ballY);

        boolean putSucceeded = outQueue.put(message);
        while (!putSucceeded) {
            Thread.currentThread().yield();
            putSucceeded = outQueue.put(message);
        }
    }

    private void walls() {
        Bounds ballBounds = ball.getBoundsInParent();

        for (int x = 0; x <= 9; x++) {
            if (ballBounds.intersects(leftBreak[x].getBoundsInParent())) {
                relativeIntersectY = (ball.getLayoutY() + (5/2)) - ball.getLayoutX();
                normalizedRelativeIntersectionY = (relativeIntersectY / (5/2));
                bounceAngle = normalizedRelativeIntersectionY * 75;
                leftBreak[x].setWidth(0);
                leftBreak[x].setHeight(0);
            } else if (ballBounds.intersects(rightBreak[x].getBoundsInParent())) {
                relativeIntersectY = (ball.getLayoutY() + (5 / 2)) - ball.getLayoutX();
                normalizedRelativeIntersectionY = (relativeIntersectY / (5 / 2));
                bounceAngle = normalizedRelativeIntersectionY * 75;
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
            Bounds ballBounds = ball.getBoundsInParent();

            if (ballBounds.intersects(leftPaddleBounds)) {
                if (ballSpeed < 5.0) { // Todo: Speed is a problem... Gosh Darn It!
                    ballSpeed += .5;
                }

                relativeIntersectY = (ball.getLayoutY() + (5 / 2)) - ball.getLayoutX();
                normalizedRelativeIntersectionY = (relativeIntersectY / (5 / 2));
                bounceAngle = normalizedRelativeIntersectionY * 75;
            } else if (ballBounds.intersects(rightPaddleBounds)) {
                if (ballSpeed < 5.0) {
                    ballSpeed += .5;
                }

                relativeIntersectY = (ball.getLayoutY() + (5 / 2)) - ball.getLayoutX();
                normalizedRelativeIntersectionY = (relativeIntersectY / (5 / 2));
                bounceAngle = normalizedRelativeIntersectionY * 75;
            } else if (ballBounds.intersects(bottomWallBounds)) {
                relativeIntersectY = (ball.getLayoutY() + (5 / 2)) - ball.getLayoutX();
                normalizedRelativeIntersectionY = (relativeIntersectY / (5 / 2));
                bounceAngle = normalizedRelativeIntersectionY * 75;
            } else if (ballBounds.intersects(topWallBounds)) {
                relativeIntersectY = (ball.getLayoutY() + (5 / 2)) - ball.getLayoutX();
                normalizedRelativeIntersectionY = (relativeIntersectY / (5 / 2));
                bounceAngle = normalizedRelativeIntersectionY * 75;
            } else if (ball.getLayoutX() > (sceneWidth + 20)) { // player 0 scores
                reset();
                return;
            } else if (ball.getLayoutX() < -100) { // player 1 scores
                reset();
                return;
            }

            ballVx = (ballSpeed * Math.cos(bounceAngle));
            ballVy = (ballSpeed * -Math.sin(bounceAngle));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setBallPosition() {
        ballX += ballVx;
        ballY += ballVy;

        ball.setLayoutX(ballX);
        ball.setLayoutY(ballY);
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
