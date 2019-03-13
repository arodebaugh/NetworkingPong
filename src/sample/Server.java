package sample;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Server extends Application {
    private Scene scene;
    private Controller controller;

    private static final int sceneWidth = 500;
    private static final int sceneHeight = 400;

    static boolean multicastMode = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Group root = new Group();
        scene = new Scene(root, sceneWidth, sceneHeight);

        controller = new Controller(scene, root);

        controller.player = 0;
        controller.setServerMode();
        controller.lobby();

        primaryStage.setTitle("Pong Host");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
