package sample;

import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {
    private Scene scene;
    private Controller controller;

    private static final int sceneWidth = 500;
    private static final int sceneHeight = 430;

    @Override
    public void start(Stage primaryStage) throws Exception {
        BorderPane root = new BorderPane();
        scene = new Scene(root, sceneWidth, sceneHeight);

        controller = new Controller(scene, root);

        controller.player = 1;
        controller.setClientMode();
        controller.lobby();

        primaryStage.setTitle("Pong Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
