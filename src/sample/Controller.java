package sample;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.event.*;
import javafx.scene.input.*;
import javafx.scene.shape.Shape;
import java.util.ArrayList;

public class Controller {
    public Canvas canvas;
    public VBox box;
    private Stage primaryStage;

    private double mouseX;
    private double mouseY;

    private double ballX = 50;
    private double ballY = 50;
    private double ballSpeed = 3.0;

    private Thread draw;

    private ArrayList<Shape> nodes;

    public Controller() { }

    public void setStage(Stage p) {
        primaryStage = p;
    }

    public void initialize() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawShapes(gc);

        /*canvas.setOnMouseMoved(event -> {
            mouseX = event.getSceneX();
            mouseY = event.getSceneY();
        });

        Thread draw = new Thread(() -> {
            while (true) {
                drawShapes(gc);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException err) {
                    System.out.println(err);
                }
            }
        });

        draw.start();*/
    }

    private void drawShapes(GraphicsContext gc) {
        gc.clearRect(0, 0, 600, 400);
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.WHITE);

        ballX = ballX + ballSpeed * 1.0; // Direction
        ballY = ballY + ballSpeed * 1.0;

        nodes = new ArrayList<>();

        Circle ball = new Circle(ballX, ballY, 20);
        nodes.add(ball);

        Rectangle leftPaddle;

        // gc.fillOval(ballX , ballY, 20, 20);

        // gc.setLineWidth(8);
        if ((mouseY + 40) < 400) {
            leftPaddle = new Rectangle(10, mouseY + 40, 8, 8);
        } else {
            leftPaddle = new Rectangle(10, mouseY, 8, 8);
        }

        nodes.add(leftPaddle);


    }
}
