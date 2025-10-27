package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the Manager view of the POS system.
 * <p>
 * This class initializes and launches the JavaFX application for the manager dashboard,
 * loading the ManagerView interface and setting up the application window.
 * </p>
 * @author Grant, Brenden, Leo, Ryan
 */
public class ManagerMain extends Application {

    /**
     * Starts the Manager dashboard application window.
     *
     * @param stage the primary stage used to display the manager interface
     * @throws Exception if loading the FXML file fails
     */
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/ManagerView.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        stage.setTitle("Manager Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Launches the ManagerMain JavaFX application.
     *
     * @param args command-line arguments passed to the program
     */
    public static void main(String[] args) {
        launch();
    }
}
