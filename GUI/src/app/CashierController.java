package app;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.sql.*;
import java.time.*;
import java.util.*;


/**
 * Controller class for the Cashier view of the POS system.
 *
 * <p>This class manages menu loading, order creation, customization dialogs,
 * and order submission to the database. It serves as the main user interface
 * for cashiers handling transactions and interacting with menu data.</p>
 *
 * @author Brenden Barber
 */
public class CashierController {

    // ========================= FXML Connections =========================

    /** Container displaying drink item cards. */
    @FXML FlowPane drinkPane;

    /** List of ordered drinks for the current transaction. */
    @FXML ListView<String> orderListView;

    /** Label showing the current total. */
    @FXML Label totalLabel;

    /** Back button used to return to the login screen. */
    @FXML Button btnBack;

    private double total = 0.0;
    private static LocalDateTime currDateTime = LocalDateTime.now();

    // ========================= CATEGORY BUTTON HANDLERS =========================

    /** Loads Ice-Blended drinks into the view. */
    @FXML
    void handleIceBlendedClick(ActionEvent e) { loadDrinks("Ice-Blended"); }

    /** Loads Fruity Beverage drinks into the view. */
    @FXML
    void handleFruityClick(ActionEvent e) { loadDrinks("Fruity Beverage"); }

    /** Loads Fresh Brew drinks into the view. */
    @FXML
    void handleFreshBrewClick(ActionEvent e) { loadDrinks("Fresh Brew"); }

    /** Loads Milky Series drinks into the view. */
    @FXML
    void handleMilkyClick(ActionEvent e) { loadDrinks("Milky Series"); }

    /** Loads New Matcha Series drinks into the view. */
    @FXML
    void handleMatchaClick(ActionEvent e) { loadDrinks("New Matcha Series"); }

    /** Loads Non-Caffeinated drinks into the view. */
    @FXML
    void handleNonCaffeinatedClick(ActionEvent e) { loadDrinks("Non-Caffeinated"); }

    // ========================= DATE MANAGEMENT =========================

    /**
     * Sets the current date for use in seasonal menu filtering and order timestamps.
     *
     * @param date the date string in YYYY-MM-DD format
     */
    public static void setCurrDate(String date) {
        LocalDate currDay = LocalDate.parse(date);
        currDateTime = currDay.atStartOfDay();
    }

    /** Randomizes the current timestamp between 9 AM and 5 PM. */
    private void randomizeTime() {
        Random rand = new Random();
        int hour = 9 + rand.nextInt(9);
        int minute = rand.nextInt(60);
        int second = rand.nextInt(60);
        currDateTime = currDateTime.withHour(hour).withMinute(minute).withSecond(second).withNano(0);
    }

    // ========================= IMAGE LOADING =========================

    /**
     * Loads a menu item image by ID or returns a default placeholder.
     *
     * @param imageID numeric ID of the image
     * @return loaded {@link Image}, or {@code null} if none found
     */
    private Image loadMenuImage(int imageID) {
        String basePath = System.getProperty("user.dir") + "/GUI/src/images/";
        File imageFile = new File(basePath + imageID + ".png");
        if (imageFile.exists()) return new Image(imageFile.toURI().toString());

        File defaultFile = new File(basePath + "default.png");
        if (defaultFile.exists()) {
            System.out.println("Missing image for ID " + imageID + ", using default.");
            return new Image(defaultFile.toURI().toString());
        }
        System.err.println("Default image missing! Please ensure default.png exists.");
        return null;
    }

    // ========================= MENU LOADING =========================

    /**
     * Loads drinks of the specified category from the database.
     *
     * @param category menu category to load
     */
    private void loadDrinks(String category) {
        drinkPane.getChildren().clear();
        try (Connection conn = DatabaseConnector.getConnection()) {
            if (conn == null) return;

            String query = """
                SELECT menuName, price, menuImage, SeasonalStart, SeasonalEnd
                FROM menu WHERE category = ?
            """;
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, category);
                ResultSet rs = stmt.executeQuery();
                boolean found = false;

                while (rs.next()) {
                    Timestamp startTs = rs.getTimestamp("SeasonalStart");
                    Timestamp endTs = rs.getTimestamp("SeasonalEnd");

                    boolean showItem = false;
                    if(startTs == null || endTs == null)
                    {
                        showItem = true;
                    }
                    else{
                        LocalDateTime start = startTs.toLocalDateTime();
                        LocalDateTime end = endTs.toLocalDateTime();

                        int startMonth = start.getMonthValue();
                        int startDay = start.getDayOfMonth();
                        int endMonth = end.getMonthValue();
                        int endDay = end.getDayOfMonth();

                        int currMonth = currDateTime.getMonthValue();
                        int currDay = currDateTime.getDayOfMonth();

                        // Compare by month/day only
                        boolean afterStart = (currMonth > startMonth) || (currMonth == startMonth && currDay >= startDay);
                        boolean beforeEnd = (currMonth < endMonth) || (currMonth == endMonth && currDay <= endDay);

                        // Handle wrap-around seasons (e.g., starts in Nov, ends in Feb)
                        if (endMonth < startMonth) {
                            showItem = afterStart || beforeEnd;
                        } else {
                            showItem = afterStart && beforeEnd;
                        }
                    }
                    if(showItem)
                    {
                        found = true;
                        String name = rs.getString("menuName");
                        double price = rs.getDouble("price");
                        int imageID = rs.getInt("menuImage");
                        drinkPane.getChildren().add(createDrinkCard(name, price, imageID));
                    }
                }
                if (!found) {
                    Label noItems = new Label("No items found for: " + category);
                    noItems.setStyle("-fx-text-fill: gray;");
                    drinkPane.getChildren().add(noItems);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    /** Returns whether an item is currently within its seasonal range. */
    private boolean isItemInSeason(Timestamp startTs, Timestamp endTs) {
        if (startTs == null || endTs == null) return true;
        LocalDateTime start = startTs.toLocalDateTime();
        LocalDateTime end = endTs.toLocalDateTime();
        LocalDateTime now = currDateTime;

        int sM = start.getMonthValue(), sD = start.getDayOfMonth();
        int eM = end.getMonthValue(), eD = end.getDayOfMonth();
        int cM = now.getMonthValue(), cD = now.getDayOfMonth();

        boolean afterStart = (cM > sM) || (cM == sM && cD >= sD);
        boolean beforeEnd = (cM < eM) || (cM == eM && cD <= eD);
        return (eM < sM) ? (afterStart || beforeEnd) : (afterStart && beforeEnd);
    }

    // ========================= DRINK CARD CREATION =========================

    /** Creates a visual card for a menu item. */
    private VBox createDrinkCard(String name, double basePrice, int imageID) {
        VBox card = new VBox(10);
        card.setPrefWidth(160);
        card.setAlignment(Pos.CENTER);
        card.setStyle("""
            -fx-border-color: #ccc;
            -fx-background-color: #f9f9f9;
            -fx-border-radius: 8;
            -fx-padding: 12;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);
        """);

        ImageView imageView = new ImageView(loadMenuImage(imageID));
        imageView.setFitWidth(100);
        imageView.setFitHeight(100);
        imageView.setPreserveRatio(true);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label priceLabel = new Label(String.format("$%.2f", basePrice));
        priceLabel.setStyle("-fx-text-fill: #444;");

        Button selectButton = new Button("Select");
        selectButton.setStyle("-fx-background-color: #a6b1b7; -fx-text-fill: white;");
        selectButton.setOnAction(e -> showDrinkOptions(name, basePrice));

        card.getChildren().addAll(imageView, nameLabel, priceLabel, selectButton);
        return card;
    }

    // ========================= DRINK CUSTOMIZATION =========================

    /** Opens a dialog to customize the selected drink. */
    private void showDrinkOptions(String drinkName, double basePrice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Customize " + drinkName);
        dialog.setHeaderText("Select ingredients / options");

        VBox content = new VBox(8);
        content.setPadding(new Insets(10));

        List<String> baseIngredients = List.of("Milk", "Sugar", "Boba", "Ice");
        List<String> extras = List.of("Aloe", "Pudding", "Jelly", "Extra Boba");

        Label baseLabel = new Label("Remove Ingredients:");
        content.getChildren().add(baseLabel);

        List<CheckBox> baseChecks = new ArrayList<>();
        for (String ingr : baseIngredients) {
            CheckBox cb = new CheckBox(ingr);
            cb.setSelected(true);
            baseChecks.add(cb);
            content.getChildren().add(cb);
        }

        Label extraLabel = new Label("\nAdd Extras (+$0.50 each):");
        content.getChildren().add(extraLabel);
        List<CheckBox> extraChecks = new ArrayList<>();
        for (String ingr : extras) {
            CheckBox cb = new CheckBox(ingr);
            extraChecks.add(cb);
            content.getChildren().add(cb);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                double itemPrice = basePrice;
                StringBuilder desc = new StringBuilder(drinkName + " [");
                for (CheckBox cb : baseChecks)
                    if (!cb.isSelected()) desc.append("-").append(cb.getText()).append(" ");
                for (CheckBox cb : extraChecks)
                    if (cb.isSelected()) { desc.append("+").append(cb.getText()).append(" "); itemPrice += 0.50; }
                desc.append("]");
                addToOrder(desc.toString().trim(), itemPrice);
            }
        });
    }

    // ========================= ORDER HANDLING =========================

    /** Adds a drink to the current order list. */
    private void addToOrder(String itemName, double price) {
        orderListView.getItems().add(String.format("%s - $%.2f", itemName, price));
        total += price;
        totalLabel.setText(String.format("Total: $%.2f", total));
    }

    /** Removes the selected drink from the order list. */
    @FXML
    void handleRemoveItemClick(ActionEvent event) {
        String selectedItem = orderListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            new Alert(Alert.AlertType.WARNING, "Please select an item to remove.").showAndWait();
            return;
        }
        String priceStr = selectedItem.substring(selectedItem.lastIndexOf('$') + 1);
        try {
            double price = Double.parseDouble(priceStr);
            total -= price;
            totalLabel.setText(String.format("Total: $%.2f", total));
        } catch (NumberFormatException e) { System.err.println("Error parsing price: " + e.getMessage()); }
        orderListView.getItems().remove(selectedItem);
    }

    /** Submits the current order to the database. */
    @FXML
    void handleSubmitOrderClick(ActionEvent event) {
        if (orderListView.getItems().isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Cannot submit an empty order!").showAndWait();
            return;
        }
        boolean success = saveOrderToDatabase();
        Alert.AlertType type = success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR;
        String msg = success ? "Your order has been submitted successfully!"
                             : "There was a problem saving your order to the database.";
        new Alert(type, msg).showAndWait();

        if (success) {
            orderListView.getItems().clear();
            total = 0.0;
            totalLabel.setText("Total: $0.00");
        }
    }

    /** Saves the current order to the database. */
    private boolean saveOrderToDatabase() {
        try (Connection conn = DatabaseConnector.getConnection()) {
            if (conn == null) return false;

            String insertOrder = """
                INSERT INTO ordertest (orderID, employeeID, orderLocation, orderDate, orderTotal)
                VALUES ((SELECT COALESCE(MAX(orderID), 0) + 1 FROM ordertest), ?, ?, ?, ?)
            """;
            try (PreparedStatement stmt = conn.prepareStatement(insertOrder)) {
                int employeeID = 2; // default fallback

                String lookup = """
                    SELECT employeeID FROM employee
                    WHERE employeePosition = 'Cashier' AND employeeID IN (2, 3)
                    ORDER BY employeeID
                    LIMIT 1 OFFSET (
                        (SELECT COUNT(*) FROM ordertest)
                        % (SELECT COUNT(*) FROM employee WHERE employeePosition = 'Cashier' AND employeeID IN (2, 3))
                    )
                """;

                try (Statement empStmt = conn.createStatement();
                     ResultSet rs = empStmt.executeQuery(lookup)) {
                    if (rs.next()) {
                        employeeID = rs.getInt("employeeID");
                    }
                }

                stmt.setInt(1, employeeID);

                stmt.setString(2, "College Station");
                randomizeTime();
                stmt.setTimestamp(3, Timestamp.valueOf(currDateTime));
                stmt.setDouble(4, total);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // ========================= NAVIGATION =========================

    /** Returns to the login view. */
    @FXML
    void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/LoginView.fxml"));
            Scene scene = new Scene(loader.load(), 800, 600);
            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.setTitle("POS — Login");
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error returning to login.").showAndWait();
        }
    }
}



