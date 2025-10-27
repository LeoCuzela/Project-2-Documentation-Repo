package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the Cashier view of the POS system.
 * <p>
 * This class initializes and launches the JavaFX application for the cashier interface,
 * loading the CashierView layout and displaying the cashier dashboard window.
 * </p>
 * @author Grant and Brenden 
 */
public class CashierMain extends Application {

    /**
     * Starts the Cashier dashboard application window.
     *
     * @param stage the primary stage used to display the cashier interface
     * @throws Exception if loading the FXML file fails
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Load your CashierView.fxml
        Parent root = FXMLLoader.load(getClass().getResource("/app/CashierView.fxml"));
        Scene scene = new Scene(root, 1200, 700);

        stage.setTitle("Boba POS System - Cashier View");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Launches the CashierMain JavaFX application.
     *
     * @param args command-line arguments passed to the program
     */
    public static void main(String[] args) {
        launch(args);
    }
}
