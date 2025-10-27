package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main class that serves as the entry point for the POS system.
 * <p>
 * This class starts the JavaFX application by loading the login screen
 * and initializing database connection settings.
 * </p>
 * @author Grant
 */
public class Main extends Application {

    /**
     * Starts the application by loading the LoginView.fxml file
     * and displaying it in a window.
     *
     * @param stage the main window for the application
     * @throws Exception if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Loads your password/login view as the first screen
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/LoginView.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);
        stage.setTitle("POS — Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        String[] creds = LineReader.getCredentials();
        DatabaseConnector.setDbUrl(creds[0]);
        DatabaseConnector.setDbUser(creds[1]);
        DatabaseConnector.setDbPassword(creds[2]);

        // creds[3] is date
        if (creds[3] != "") {
            CashierController.setCurrDate(creds[3]);
            ManagerController.setCurrDate(creds[3]);
        }
        launch(args);
    }
}