package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;

import java.util.ArrayList;

public class Main extends Application {
    private Controller controller;
    private Scene scene;

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

    private Data data = new Data(true);


    @Override
    public void start(Stage primaryStage) throws Exception {
        Group root = new Group();
        scene = new Scene(root, sceneWidth, sceneHeight);

        nodes = new ArrayList<>();

        ball = new Circle(ballX, ballY, 15);

        leftPaddle = new Rectangle(50, mouseY,10, 80);
        rightPaddle = new Rectangle(sceneWidth - 70, 200,10, 80);

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
        }

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

        root.getChildren().addAll(nodes);

        primaryStage.setTitle("Pong");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void tick() {
        testBounds();
        leftPaddle.setLayoutY(mouseY);
        setBallPosition();
    }

    /*private void testBreak() {
        for (int x = 0; x <= 9; x++) {
            Bounds ballBounds = ball.getBoundsInParent();

            if (leftBreak.)
        }

        testBounds();
    }*/

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
        }

        ballVx = (ballSpeed * Math.cos(bounceAngle));
        ballVy = (ballSpeed * -Math.sin(bounceAngle));
    }

    private void setBallPosition() {
        // ms = lastTime - System.currentTimeMillis();
        // lastTime = System.currentTimeMillis();

        System.out.println(ballX + ", " + ballY);

        ballX += ballVx;
        ballY += ballVy;

        ball.setLayoutX(ballX);
        ball.setLayoutY(ballY);
    }

    private void setListeners() {
        scene.setOnMouseMoved(event -> {
            mouseX = event.getSceneX();
            mouseY = event.getSceneY();

            data.put(mouseY);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
