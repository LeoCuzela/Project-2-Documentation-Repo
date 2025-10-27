package app;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;

/**
 * Controller class for handling login functionality for the POS system.
 * <p>
 * This class manages authentication for manager and cashier roles,
 * verifying credentials from the database and loading the appropriate dashboard view.
 * </p>
 * @author Leo
 */
public class LoginController {

    @FXML private PasswordField managerPasswordField;
    @FXML private PasswordField cashierPasswordField;
    @FXML private Label statusLabel;

    private final DatabaseConnector db = new DatabaseConnector();

    /**
     * Handles the login process for the manager role.
     * Validates the entered password and opens the manager dashboard if successful.
     */
    @FXML
    private void handleManagerLogin() {
        String password = managerPasswordField.getText();
        if (authenticate("Manager", password)) {
            loadView("ManagerView.fxml", "Manager Dashboard");
        } else {
            statusLabel.setText("Invalid manager password.");
        }
    }

    /**
     * Handles the login process for the cashier role.
     * Validates the entered password and opens the cashier dashboard if successful.
     */
    @FXML
    private void handleCashierLogin() {
        String password = cashierPasswordField.getText();
        if (authenticate("Cashier", password)) {
            loadView("CashierView.fxml", "Cashier Dashboard");
        } else {
            statusLabel.setText("Invalid cashier password.");
        }
    }

    /**
     * Authenticates the user based on their role and password.
     *
     * @param role the user role (e.g., "Manager" or "Cashier")
     * @param password the password entered by the user
     * @return true if authentication is successful, false otherwise
     */
    private boolean authenticate(String role, String password) {
        String sql = "SELECT employeePasscode FROM employee WHERE employeePosition = ?;";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (password.equals(rs.getString("employeePasscode"))) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Database error.");
        }
        return false;
    }

    /**
     * Loads the specified FXML view and updates the window title.
     *
     * @param fxml  the FXML file to load
     * @param title the window title to display
     */
    private void loadView(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/" + fxml));
            Scene scene = new Scene(loader.load(), 1000, 700);
            Stage stage = (Stage) managerPasswordField.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Error loading " + title + ".");
        }
    }
}