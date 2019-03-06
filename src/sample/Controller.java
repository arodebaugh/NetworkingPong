package sample;

import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    private static final int sceneHeight = 400;

    private ArrayList<Shape> nodes;
    private double mouseX;
    private double mouseY;

    private double ballX = 100;
    private double ballY = 100;
    private double ballVx = 0;
    private double ballVy = 0;
    private double ballSpeed = 3.0;
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

    private HBox hbox;
    private Button connect;
    private TextField ip;

    public int player = 1;

    private Scene scene;

    private SynchronizedQueue inQueue;
    private SynchronizedQueue outQueue;

    private int lastSent;

    private boolean serverMode;
    static boolean connected;

    public Controller(Scene s, Group root) {
        scene = s;

        nodes = new ArrayList<>();

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

        leftWall = new Rectangle(0,0,5,sceneHeight + 50);
        rightWall = new Rectangle(sceneWidth - 5,0,5,sceneHeight + 50);
        topWall = new Rectangle(0,0,sceneWidth + 50,5);
        bottomWall = new Rectangle(0,sceneHeight - 5,sceneWidth + 50,5);

        for (int x = 0; x <= 9; x++) { // 5 a row
            if (x < 5) {
                leftBreak[x] = new Rectangle(0, x * 82, 10, 80);
            } else {
                leftBreak[x] = new Rectangle(12, (x - 5) * 82, 10, 80);
            }
        }

        for (int x = 0; x <= 9; x++) { // 5 a row
            if (x < 5) {
                rightBreak[x] = new Rectangle(sceneWidth - 10, x * 82, 10, 80);
            } else {
                rightBreak[x] = new Rectangle(sceneWidth - 22, (x - 5) * 82, 10, 80);
            }
        }

        hbox = new HBox();
        connect = new Button("START");
        ip = new TextField("");

        leftWall.setFill(Color.WHITE);
        rightWall.setFill(Color.WHITE);
        topWall.setFill(Color.WHITE);
        bottomWall.setFill(Color.WHITE);

        nodes.add(ball);
        nodes.add(leftPaddle);
        nodes.add(rightPaddle);
        nodes.add(leftWall);
        nodes.add(rightWall);
        nodes.add(topWall);
        nodes.add(bottomWall);

        for (int x = 0; x <= 9; x++) {
            nodes.add(leftBreak[x]);
            nodes.add(rightBreak[x]);
        }

        hbox.getChildren().add(connect);

        root.getChildren().addAll(nodes);
        root.getChildren().addAll(hbox);
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

    void lobby() {
        connect.setOnAction(event -> {
            connect.setVisible(false);
            ip.setVisible(false);
            connected = true;

            if (serverMode) {
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
            } else {
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
        testBounds();
        if (serverMode) {
            setBallPosition();
        }
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

    private void testBounds() {
        Bounds leftPaddleBounds = leftPaddle.getBoundsInParent();
        Bounds rightPaddleBounds = rightPaddle.getBoundsInParent();
        Bounds leftWallBounds = leftWall.getBoundsInParent();
        Bounds rightWallBounds = rightWall.getBoundsInParent();
        Bounds topWallBounds = topWall.getBoundsInParent();
        Bounds bottomWallBounds = bottomWall.getBoundsInParent();
        Bounds ballBounds = ball.getBoundsInParent();

        if (ballBounds.intersects(leftPaddleBounds)) {
            if (ballSpeed < 10.0) {
                ballSpeed += .5;
            }

            relativeIntersectY = (ball.getLayoutY() + (5/2)) - ball.getLayoutX();
            normalizedRelativeIntersectionY = (relativeIntersectY / (5/2));
            bounceAngle = normalizedRelativeIntersectionY * 75;
        } else if (ballBounds.intersects(rightPaddleBounds)) {
            if (ballSpeed < 10.0) {
                ballSpeed += .5;
            }

            relativeIntersectY = (ball.getLayoutY() + (5/2)) - ball.getLayoutX();
            normalizedRelativeIntersectionY = (relativeIntersectY / (5/2));
            bounceAngle = normalizedRelativeIntersectionY * 75;
        } else if (ballBounds.intersects(rightWallBounds)) {
            relativeIntersectY = (ball.getLayoutY() + (5/2)) - ball.getLayoutX();
            normalizedRelativeIntersectionY = (relativeIntersectY / (5/2));
            bounceAngle = normalizedRelativeIntersectionY * 75;
        } else if (ballBounds.intersects(leftWallBounds)) {
            relativeIntersectY = (ball.getLayoutY() + (5/2)) - ball.getLayoutX();
            normalizedRelativeIntersectionY = (relativeIntersectY / (5/2));
            bounceAngle = normalizedRelativeIntersectionY * 75;
        } else if (ballBounds.intersects(bottomWallBounds)) {
            relativeIntersectY = (ball.getLayoutY() + (5/2)) - ball.getLayoutX();
            normalizedRelativeIntersectionY = (relativeIntersectY / (5/2));
            bounceAngle = normalizedRelativeIntersectionY * 75;
        } else if (ballBounds.intersects(topWallBounds)) {
            relativeIntersectY = (ball.getLayoutY() + (5/2)) - ball.getLayoutX();
            normalizedRelativeIntersectionY = (relativeIntersectY / (5/2));
            bounceAngle = normalizedRelativeIntersectionY * 75;
        } else {
            for (int x = 0; x <= 9; x++) {
                if (ballBounds.intersects(leftBreak[x].getBoundsInParent())) {
                    relativeIntersectY = (ball.getLayoutY() + (5/2)) - ball.getLayoutX();
                    normalizedRelativeIntersectionY = (relativeIntersectY / (5/2));
                    bounceAngle = normalizedRelativeIntersectionY * 75;
                    leftBreak[x].setVisible(false);
                } else if (ballBounds.intersects(rightBreak[x].getBoundsInParent())) {
                    relativeIntersectY = (ball.getLayoutY() + (5 / 2)) - ball.getLayoutX();
                    normalizedRelativeIntersectionY = (relativeIntersectY / (5 / 2));
                    bounceAngle = normalizedRelativeIntersectionY * 75;
                    rightBreak[x].setVisible(false);
                }
            }
        }

        ballVx = (ballSpeed * Math.cos(bounceAngle));
        ballVy = (ballSpeed * -Math.sin(bounceAngle));
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
