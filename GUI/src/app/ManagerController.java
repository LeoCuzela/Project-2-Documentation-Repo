package app;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.*;
import java.sql.*;
import java.time.*;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.geometry.Pos;

/**
 * Controller class that manages the manager view of the POS system.
 * <p>
 * Handles the user interface and interactions for inventory, employees, and
 * order history management. Communicates with the database through
 * {@link DatabaseConnector} and dynamically updates the display within
 * {@code mainDisplayPane}.
 * </p>
 * @author Grant, Ryan, and Brenden
 */
public class ManagerController {

    /** The main display area for loading manager interface content dynamically. */
    @FXML private AnchorPane mainDisplayPane;

    /** The current date used across reports and database queries. */
    private static LocalDate currDate = LocalDate.now();

    // ===== SET DATABASE DATE =====
    // Format is YYYY-MM-DD

    /**
     * Sets the current date used by the manager dashboard.
     * Primarily applied when generating daily or range-based reports.
     *
     * @param date the current date in {@code YYYY-MM-DD} format.
     */
    public static void setCurrDate(String date){
        currDate = LocalDate.parse(date);
    }

    // === BUTTON HANDLERS ===

    /**
     * Loads and displays the current inventory table from the database.
     * <p>
     * Each record shows the ingredient name, available quantity, and its
     * unit of measurement. The data is dynamically fetched using SQL.
     * </p>
     */
    @FXML private void handleInventoryButton() {
        loadTableFromQuery(
            "SELECT inventoryName AS \"Ingredient\", quantityAvailable AS \"Quantity\", unit AS \"Unit\" FROM inventory",
            "Inventory"
        );
    }

    /**
     * Loads and displays the most recent orders in the system.
     * <p>
     * The query retrieves up to 40 of the latest orders and presents
     * details including order ID, employee ID, order location, date, and total value.
     * </p>
     */
    @FXML private void handleOrdersButton() {
        loadTableFromQuery("""
            SELECT orderID AS "Order ID", employeeID AS "Employee ID",
                   orderLocation AS "Location", orderDate AS "Date",
                   orderTotal AS "Total ($)"
            FROM ordertest
            ORDER BY orderDate DESC
            LIMIT 40
        """, "Order History");
    }

    /**
     * Displays the employee management table.
     * <p>
     * Fetches employee information including ID, name, and position.
     * Provides UI controls for adding or removing employees through
     * {@link #showAddEmployeeForm()} and {@link #removeSelectedEmployee(TableView)}.
     * </p>
     */
    @FXML
    private void handleEmployeeButton() {
        mainDisplayPane.getChildren().clear();

        Label header = new Label("Employees");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10;");

        final TableView<ObservableList<String>>[] tableRef = new TableView[1];
        String query = "SELECT employeeID AS \"ID\", employeeName AS \"Name\", employeePosition AS \"Position\" FROM employee";

        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            tableRef[0] = buildTableFromResultSet(rs);

        } catch (SQLException e) {
            e.printStackTrace();
            mainDisplayPane.getChildren().add(new Label("Error loading employees."));
            return;
        }

        Button addBtn = new Button("Add Employee");
        Button removeBtn = new Button("Remove Employee");

        addBtn.setOnAction(e -> showAddEmployeeForm());
        removeBtn.setOnAction(e -> removeSelectedEmployee(tableRef[0]));

        HBox buttons = new HBox(10, addBtn, removeBtn);
        buttons.setStyle("-fx-padding: 10;");

        VBox layout = new VBox(10, header, tableRef[0], buttons);
        AnchorPane.setTopAnchor(layout, 0.0);
        AnchorPane.setBottomAnchor(layout, 0.0);
        AnchorPane.setLeftAnchor(layout, 0.0);
        AnchorPane.setRightAnchor(layout, 0.0);

        mainDisplayPane.getChildren().add(layout);
    }

        /**
     * Handles the "Restock" button action from the Manager interface.
     * <p>
     * When clicked, this triggers {@link #loadRestockTable()} to display
     * the interactive inventory restock table, where managers can update
     * item quantities and view restock statuses.
     * </p>
     */
    @FXML private void handleRestockButton() {
        loadRestockTable();
    }

    // === UNIVERSAL TABLE LOADER ===

    /**
     * Loads a generic table from a given SQL query and displays it inside the main pane.
     * <p>
     * This universal table loader is reused across multiple dashboard sections
     * (e.g., inventory, orders, employees) to dynamically populate tabular data
     * without hardcoding the schema.
     * </p>
     *
     * @param query SQL query used to fetch data from the database
     * @param title The display title shown above the generated table
     */
    private void loadTableFromQuery(String query, String title) {
        mainDisplayPane.getChildren().clear();

        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {

            TableView<ObservableList<String>> table = buildTableFromResultSet(rs);
            Label header = new Label(title);
            header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10;");

            VBox layout = new VBox(10, header, table);
            layout.setPrefSize(mainDisplayPane.getWidth(), mainDisplayPane.getHeight());
            AnchorPane.setTopAnchor(layout, 0.0);
            AnchorPane.setBottomAnchor(layout, 0.0);
            AnchorPane.setLeftAnchor(layout, 0.0);
            AnchorPane.setRightAnchor(layout, 0.0);
            mainDisplayPane.getChildren().add(layout);

        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("Error loading data: " + e.getMessage());
            mainDisplayPane.getChildren().add(errorLabel);
        }
    }

    // === RESTOCK TABLE (INTERACTIVE) ===

    /**
     * Loads the interactive restock management table into the main display pane.
     * <p>
     * This method dynamically builds a table of all inventory items, their current quantities,
     * and restock thresholds. Users can manually add quantities to restock individual items.
     * <br><br>
     * Rows that meet or fall below their restock minimum threshold are visually highlighted
     * with a light yellow background for quick identification.
     * </p>
     */
    private void loadRestockTable() {
        mainDisplayPane.getChildren().clear();

        TableView<InventoryItem> table = new TableView<>();
        ObservableList<InventoryItem> data = FXCollections.observableArrayList();

        // Columns
        TableColumn<InventoryItem, String> nameCol = new TableColumn<>("Ingredient");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("ingredientName"));

        TableColumn<InventoryItem, Double> qtyCol = new TableColumn<>("Quantity Available");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<InventoryItem, Integer> minCol = new TableColumn<>("Restock Min");
        minCol.setCellValueFactory(new PropertyValueFactory<>("restockMin"));

        // New Restock Status column
        TableColumn<InventoryItem, String> statusCol = new TableColumn<>("Restock: Y/N");
        statusCol.setCellValueFactory(param ->
            new javafx.beans.property.SimpleStringProperty(param.getValue().needsRestock() ? "Y" : "N")
        );

        TableColumn<InventoryItem, Integer> addCol = new TableColumn<>("Add (+)");
        addCol.setCellFactory(col -> new TableCell<>() {
            private final TextField input = new TextField();
            {
                input.setPromptText("0");
                input.setPrefWidth(60);
            }
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else setGraphic(input);
            }
        });

        TableColumn<InventoryItem, Void> saveCol = new TableColumn<>("Save");
        saveCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Update");
            {
                btn.setOnAction(e -> {
                    InventoryItem item = getTableView().getItems().get(getIndex());
                    TableRow<?> row = getTableRow();
                    if (row != null) {
                        TextField inputField = (TextField) row.lookup(".text-field");
                        if (inputField != null && !inputField.getText().isEmpty()) {
                            try {
                                int addAmount = Integer.parseInt(inputField.getText());
                                updateInventory(item.getIngredientName(), addAmount);
                                item.setQuantity(item.getQuantity() + addAmount);
                                table.refresh();
                            } catch (NumberFormatException ex) {
                                System.err.println("Invalid number input.");
                            }
                        }
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // Add all columns to the table
        table.getColumns().addAll(nameCol, qtyCol, minCol, statusCol, addCol, saveCol);

        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT inventoryName, quantityAvailable, restockMin FROM inventory");
            ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                data.add(new InventoryItem(
                    rs.getString("inventoryName"),
                    rs.getDouble("quantityAvailable"),
                    rs.getInt("restockMin")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        table.setItems(data);

        // ✨ Highlight rows that are at or below restock minimum
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(InventoryItem item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.needsRestock()) {
                    setStyle("-fx-background-color: #fff3b0;"); // soft yellow highlight
                } else {
                    setStyle("");
                }
            }
        });

        Label header = new Label("Restock Inventory");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10;");

        VBox layout = new VBox(10, header, table);
        AnchorPane.setTopAnchor(layout, 0.0);
        AnchorPane.setBottomAnchor(layout, 0.0);
        AnchorPane.setLeftAnchor(layout, 0.0);
        AnchorPane.setRightAnchor(layout, 0.0);
        mainDisplayPane.getChildren().add(layout);
    }


    // === RESTOCK UPDATER ===

    /**
     * Updates the quantity of a specific inventory item in the database.
     * <p>
     * This method increases the {@code quantityAvailable} field of the specified ingredient
     * by the given {@code addAmount}. It also provides visual confirmation to the user through
     * a JavaFX alert upon successful update.
     * </p>
     *
     * @param ingredient the name of the ingredient to restock
     * @param addAmount  the amount to add to the current quantity
     */
    private void updateInventory(String ingredient, int addAmount) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String query = "UPDATE inventory SET quantityAvailable = quantityAvailable + ? WHERE inventoryName = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, addAmount);
                stmt.setString(2, ingredient);
                int rows = stmt.executeUpdate();

                if (rows > 0) {
                    System.out.println("Restocked " + ingredient + " by " + addAmount);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Restock Successful");
                    alert.setHeaderText(null);
                    alert.setContentText(ingredient + " restocked by " + addAmount + " units.");
                    alert.showAndWait();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to update inventory: " + e.getMessage());
            alert.showAndWait();
        }
    }

    // === HELPER: Build generic table from ResultSet ===

    /**
     * Builds a JavaFX {@link TableView} dynamically from a SQL {@link ResultSet}.
     * <p>
     * This method is used by multiple sections (e.g., inventory, orders, menu)
     * to construct generic tables without needing predefined column names.
     * Each column header is set according to the {@link ResultSetMetaData}.
     * </p>
     *
     * @param rs the {@link ResultSet} containing database query results
     * @return a fully constructed {@link TableView} with populated data
     * @throws SQLException if an SQL or data extraction error occurs
     */
    private TableView<ObservableList<String>> buildTableFromResultSet(ResultSet rs) throws SQLException {
        TableView<ObservableList<String>> table = new TableView<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        for (int i = 1; i <= columnCount; i++) {
            final int colIndex = i - 1;
            TableColumn<ObservableList<String>, String> col =
                    new TableColumn<>(meta.getColumnLabel(i));
            col.setCellValueFactory(param ->
                    new javafx.beans.property.SimpleStringProperty(param.getValue().get(colIndex)));
            table.getColumns().add(col);
        }

        while (rs.next()) {
            ObservableList<String> row = FXCollections.observableArrayList();
            for (int i = 1; i <= columnCount; i++) {
                row.add(rs.getString(i));
            }
            table.getItems().add(row);
        }

        return table;
    }

    // === DATA MODEL FOR RESTOCK TABLE ===

    /**
     * Represents an inventory item within the restock management table.
     * <p>
     * Each instance contains the ingredient’s name, available quantity,
     * and minimum quantity threshold required before restocking.
     * This inner class is used as a JavaFX model for {@link TableView} binding.
     * </p>
     */
    public static class InventoryItem {
        private final javafx.beans.property.SimpleStringProperty ingredientName;
        private final javafx.beans.property.SimpleDoubleProperty quantity;
        private final javafx.beans.property.SimpleIntegerProperty restockMin;

        /**
         * Constructs a new {@code InventoryItem}.
         *
         * @param ingredientName the name of the inventory item
         * @param quantity       the current available quantity
         * @param restockMin     the minimum quantity before restocking is required
         */
        public InventoryItem(String ingredientName, double quantity, int restockMin) {
            this.ingredientName = new javafx.beans.property.SimpleStringProperty(ingredientName);
            this.quantity = new javafx.beans.property.SimpleDoubleProperty(quantity);
            this.restockMin = new javafx.beans.property.SimpleIntegerProperty(restockMin);
        }

        /** @return the name of the ingredient */
        public String getIngredientName() { return ingredientName.get(); }

        /** @param value the new name of the ingredient */
        public void setIngredientName(String value) { ingredientName.set(value); }

        /** @return the available quantity */
        public double getQuantity() { return quantity.get(); }

        /** @param value the new quantity value */
        public void setQuantity(double value) { quantity.set(value); }

        /** @return the restock threshold */
        public int getRestockMin() { return restockMin.get(); }

        /** @param value the new restock threshold */
        public void setRestockMin(int value) { restockMin.set(value); }

        /**
         * Determines whether the item needs restocking.
         *
         * @return {@code true} if the available quantity is less than or equal to
         *         the restock minimum; {@code false} otherwise
         */
        public boolean needsRestock() {
            return getQuantity() <= getRestockMin();
        }
    }



    // === EMPLOYEE MANAGEMENT ===

    /**
     * Displays a dialog form to add a new employee to the database.
     * <p>
     * This method opens a JavaFX {@link Dialog} window where the manager can enter
     * the employee’s ID, name, position, and passcode. Upon submission,
     * the form validates that the ID is numeric and calls
     * {@link #addEmployeeToDatabase(int, String, String, String)} to save the entry.
     * </p>
     * <p>
     * If the user cancels or provides invalid data, appropriate alerts are displayed.
     * </p>
     */
    private void showAddEmployeeForm() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Employee");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField idField = new TextField();
        TextField nameField = new TextField();
        TextField positionField = new TextField();
        PasswordField passcodeField = new PasswordField();

        idField.setPromptText("Enter unique ID");
        nameField.setPromptText("Employee name");
        positionField.setPromptText("Position");
        passcodeField.setPromptText("Passcode");

        grid.add(new Label("Employee ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Position:"), 0, 2);
        grid.add(positionField, 1, 2);
        grid.add(new Label("Passcode:"), 0, 3);
        grid.add(passcodeField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    int employeeID = Integer.parseInt(idField.getText());
                    addEmployeeToDatabase(employeeID, nameField.getText(), positionField.getText(), passcodeField.getText());
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.ERROR, "Employee ID must be a number.").showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    /**
     * Inserts a new employee record into the database.
     * <p>
     * This method executes an {@code INSERT} SQL statement to add a new employee
     * using the provided ID, name, position, and passcode. Upon success,
     * a confirmation alert is displayed and the employee table view is refreshed
     * by calling {@link #handleEmployeeButton()}.
     * </p>
     * <p>
     * If a SQL error occurs (e.g., duplicate ID or connection failure),
     * an error alert is displayed with diagnostic details.
     * </p>
     *
     * @param id        the unique employee ID
     * @param name      the employee's full name
     * @param position  the role or position of the employee (e.g., Cashier, Manager)
     * @param passcode  the password/passcode assigned to the employee
     */
    private void addEmployeeToDatabase(int id, String name, String position, String passcode) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "INSERT INTO employee (employeeID, employeeName, employeePosition, employeePasscode) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setString(3, position);
            stmt.setString(4, passcode);
            stmt.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION, "Employee added successfully!").showAndWait();
            handleEmployeeButton();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Error adding employee: " + e.getMessage()).showAndWait();
        }
    }

    /**
     * Removes the currently selected employee from the database.
     * <p>
     * This method retrieves the selected row from the {@link TableView}
     * and deletes the corresponding record from the database after confirmation.
     * A warning alert is shown if no employee is selected, and a confirmation dialog
     * ensures the user intends to proceed with deletion.
     * </p>
     * <p>
     * After a successful deletion, the employee table view is refreshed.
     * </p>
     *
     * @param table the employee {@link TableView} from which the selected record is identified
     */
    private void removeSelectedEmployee(TableView<ObservableList<String>> table) {
        ObservableList<String> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select an employee to remove.").showAndWait();
            return;
        }

        int employeeID = Integer.parseInt(selected.get(0));
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to remove employee ID " + employeeID + "?",
                ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try (Connection conn = DatabaseConnector.getConnection()) {
                    String sql = "DELETE FROM employee WHERE employeeID = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, employeeID);
                    stmt.executeUpdate();

                    new Alert(Alert.AlertType.INFORMATION, "Employee removed successfully!").showAndWait();
                    handleEmployeeButton();
                } catch (SQLException e) {
                    new Alert(Alert.AlertType.ERROR, "Error removing employee: " + e.getMessage()).showAndWait();
                }
            }
        });
    }

        // === MENU MANAGEMENT ===

    /**
     * Handles the "Menu" button click on the Manager interface.
     * <p>
     * This method displays the menu management table by retrieving all menu items from
     * the database. The view allows managers to add, edit, or remove menu items using
     * the corresponding buttons. The table is dynamically populated with data from
     * the {@code menu} table in the database.
     * </p>
     */
    @FXML
    private void handleMenuButton() {
        mainDisplayPane.getChildren().clear();

        Label header = new Label("Menu Items");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10;");

        final TableView<ObservableList<String>>[] tableRef = new TableView[1];
        String query = "SELECT menuID AS \"ID\", menuName AS \"Name\", category AS \"Category\", price AS \"Price ($)\", menuDescription AS \"Description\" FROM menu ORDER BY menuID";

        try (Connection conn = DatabaseConnector.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery()) {
            tableRef[0] = buildTableFromResultSet(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            mainDisplayPane.getChildren().add(new Label("Error loading menu."));
            return;
        }

        Button addBtn = new Button("Add Item");
        Button editBtn = new Button("Edit Selected");
        Button removeBtn = new Button("Remove Selected");

        addBtn.setOnAction(e -> showAddMenuForm());
        editBtn.setOnAction(e -> showEditMenuForm(tableRef[0]));
        removeBtn.setOnAction(e -> removeSelectedMenuItem(tableRef[0]));

        HBox buttons = new HBox(10, addBtn, editBtn, removeBtn);
        buttons.setStyle("-fx-padding: 10;");

        VBox layout = new VBox(10, header, tableRef[0], buttons);
        AnchorPane.setTopAnchor(layout, 0.0);
        AnchorPane.setBottomAnchor(layout, 0.0);
        AnchorPane.setLeftAnchor(layout, 0.0);
        AnchorPane.setRightAnchor(layout, 0.0);

        mainDisplayPane.getChildren().add(layout);
    }

    /**
     * Displays a dialog for adding a new menu item to the database.
     * <p>
     * The dialog allows the manager to input a new item’s ID, name, category, price,
     * and description. Upon clicking OK, the data is validated to ensure numeric fields
     * are correctly formatted before being inserted into the database via
     * {@link #addMenuItemToDatabase(int, String, String, double, String)}.
     * </p>
     * <p>
     * If the input data is invalid (e.g., non-numeric ID or price), an error alert is displayed.
     * </p>
     */
    private void showAddMenuForm() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Menu Item");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField idField = new TextField();
        TextField nameField = new TextField();
        TextField categoryField = new TextField();
        TextField priceField = new TextField();
        TextField descField = new TextField();
        TextField startDateField = new TextField();
        TextField endDateField = new TextField();

        grid.add(new Label("Menu ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryField, 1, 2);
        grid.add(new Label("Price ($):"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("Description:"), 0, 4);
        grid.add(descField, 1, 4);
        grid.add(new Label("Start Date:"), 0, 5);
        grid.add(startDateField, 1, 5);
        grid.add(new Label("End Date:"), 0, 6);
        grid.add(endDateField, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    int id = Integer.parseInt(idField.getText());
                    double price = Double.parseDouble(priceField.getText());
                    LocalDate defaultStart = LocalDate.of(LocalDate.now().getYear(), 1, 1);
                    LocalDate defaultEnd = LocalDate.of(LocalDate.now().getYear(), 12, 31);

                    Timestamp startDate = Timestamp.valueOf(defaultStart.atStartOfDay());
                    Timestamp endDate = Timestamp.valueOf(defaultEnd.atStartOfDay());

                    if(!startDateField.getText().isBlank())
                    {
                        startDate = Timestamp.valueOf(LocalDate.parse(startDateField.getText()).atStartOfDay());
                    }
                    if(!endDateField.getText().isBlank())
                    {
                        endDate = Timestamp.valueOf(LocalDate.parse(endDateField.getText()).atStartOfDay());
                    }
                    addMenuItemToDatabase(id, nameField.getText(), categoryField.getText(), price, descField.getText(), startDate, endDate);
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.ERROR, "Menu ID and Price must be numbers.").showAndWait();
                }
                catch (Exception e) {
                    new Alert(Alert.AlertType.ERROR, "Invalid date format. Please use YYYY-MM-DD.").showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    /**
     * Inserts a new menu item into the database.
     * <p>
     * Executes an {@code INSERT} SQL statement to add a new record to the {@code menu} table
     * with the provided details (ID, name, category, price, description, seasonalStart, seasonalEnd).
     * A confirmation alert is shown upon success, and the menu view is refreshed.
     * </p>
     * <p>
     * If a SQL error occurs (e.g., duplicate ID or invalid connection),
     * an error alert is displayed with the failure message.
     * </p>
     *
     * @param id            the unique identifier for the menu item
     * @param name          the name of the menu item
     * @param category      the menu category (e.g., "Milky Series", "Fruity Beverage")
     * @param price         the item’s price
     * @param desc          the item’s textual description
     * @param seasonalStart the item's availability start date
     * @param seasonalEnd   the item's availability end date
     */
    private void addMenuItemToDatabase(int id, String name, String category, double price, String desc, Timestamp seasonalStart, Timestamp seasonalEnd) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "INSERT INTO menu (menuID, menuName, category, price, menuDescription, seasonalStart, seasonalEnd) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.setString(2, name);
            stmt.setString(3, category);
            stmt.setDouble(4, price);
            stmt.setString(5, desc);
            stmt.setTimestamp(6, seasonalStart);
            stmt.setTimestamp(7, seasonalEnd);
            stmt.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION, "Menu item added successfully!").showAndWait();
            handleMenuButton();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Error adding menu item: " + e.getMessage()).showAndWait();
        }
    }

    /**
     * Displays a dialog for editing a selected menu item.
     * <p>
     * This method retrieves the selected row from the menu table and populates
     * editable text fields with the existing values. The user can modify the name,
     * category, or price of the item. Upon confirmation, the data is validated and
     * passed to {@link #updateMenuItem(int, String, String, double)} for database update.
     * </p>
     * <p>
     * If no item is selected or an invalid price is entered, an alert is displayed.
     * </p>
     *
     * @param table the {@link TableView} containing the list of menu items
     */
    private void showEditMenuForm(TableView<ObservableList<String>> table) {
        ObservableList<String> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select a menu item to edit.").showAndWait();
            return;
        }

        int menuID = Integer.parseInt(selected.get(0));
        String currentName = selected.get(1);
        String currentCategory = selected.get(2);
        String currentPrice = selected.get(3);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Menu Item");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(currentName);
        TextField categoryField = new TextField(currentCategory);
        TextField priceField = new TextField(currentPrice);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Category:"), 0, 1);
        grid.add(categoryField, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    double price = Double.parseDouble(priceField.getText());
                    updateMenuItem(menuID, nameField.getText(), categoryField.getText(), price);
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.ERROR, "Price must be a valid number.").showAndWait();
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

        /**
     * Updates an existing menu item in the database.
     * <p>
     * Executes an {@code UPDATE} SQL statement to modify an existing menu record based on the provided ID.
     * After successfully updating the record, a confirmation alert is shown and the menu table view is refreshed.
     * </p>
     * <p>
     * If a database error occurs, an error alert is displayed with details of the failure.
     * </p>
     *
     * @param id        the unique ID of the menu item to update
     * @param name      the new menu name
     * @param category  the new category (e.g., "Fruity Beverage", "Milky Series")
     * @param price     the new price for the item
     */
    private void updateMenuItem(int id, String name, String category, double price) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "UPDATE menu SET menuName = ?, category = ?, price = ? WHERE menuID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, category);
            stmt.setDouble(3, price);
            stmt.setInt(4, id);
            stmt.executeUpdate();

            new Alert(Alert.AlertType.INFORMATION, "Menu item updated successfully!").showAndWait();
            handleMenuButton();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Error updating menu item: " + e.getMessage()).showAndWait();
        }
    }

    /**
     * Removes a selected menu item from the database.
     * <p>
     * This method checks the selected table row, confirms with the user via an alert dialog,
     * and then executes a {@code DELETE} SQL statement to remove the item from the database.
     * After successful removal, the menu list view is refreshed.
     * </p>
     * <p>
     * If no item is selected or an SQL error occurs, the method displays an appropriate alert message.
     * </p>
     *
     * @param table the {@link TableView} containing the list of menu items
     */
    private void removeSelectedMenuItem(TableView<ObservableList<String>> table) {
        ObservableList<String> selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Select a menu item to remove.").showAndWait();
            return;
        }

        int menuID = Integer.parseInt(selected.get(0));

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete menu item ID " + menuID + "?",
                ButtonType.YES, ButtonType.NO);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try (Connection conn = DatabaseConnector.getConnection()) {
                    String sql = "DELETE FROM menu WHERE menuID = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setInt(1, menuID);
                    stmt.executeUpdate();

                    new Alert(Alert.AlertType.INFORMATION, "Menu item removed successfully!").showAndWait();
                    handleMenuButton();
                } catch (SQLException e) {
                    new Alert(Alert.AlertType.ERROR, "Error removing menu item: " + e.getMessage()).showAndWait();
                }
            }
        });
    }

    // === BACK TO LOGIN ===

    /**
     * Returns the user to the login screen.
     * <p>
     * Loads the {@code LoginView.fxml} file and sets it as the current scene.
     * This allows managers to safely log out and return to the main login view.
     * </p>
     * <p>
     * If the FXML file cannot be loaded, an error alert is displayed.
     * </p>
     */
    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/LoginView.fxml"));
            Scene scene = new Scene(loader.load(), 800, 600);
            Stage stage = (Stage) mainDisplayPane.getScene().getWindow();
            stage.setTitle("POS — Login");
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error returning to login.").showAndWait();
        }
    }

    // === REPORTS SECTION ===

    /**
     * Displays the Sales Reports interface with options for various report types.
     * <p>
     * The manager can generate:
     * <ul>
     *     <li>X-Report (summary of current sales)</li>
     *     <li>Z-Report (end-of-day report)</li>
     *     <li>Range Report (custom date range totals)</li>
     *     <li>Product Usage Chart</li>
     * </ul>
     * This interface dynamically updates the {@code mainDisplayPane} with labels, totals,
     * and tables populated from the database queries.
     * </p>
     */
    @FXML
    private void handleReportsButton() {
        mainDisplayPane.getChildren().clear();

        Label header = new Label("Sales Reports");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10;");

        HBox buttonRow = new HBox(10);
        Button xReportBtn = new Button("X-Report");
        Button zReportBtn = new Button("Z-Report");
        Button rangeBtn = new Button("Range Report");
        Button productUsageChartBtn = new Button("Product Usage Chart");
        buttonRow.getChildren().addAll(xReportBtn, zReportBtn, rangeBtn, productUsageChartBtn);
        buttonRow.setStyle("-fx-padding: 10;");

        Label totalRevenueLabel = new Label("$0.00");
        Label totalOrdersLabel = new Label("0");

        HBox totals = new HBox(40);
        totals.setStyle("-fx-padding: 10;");
        VBox revenueBox = new VBox(5, new Label("Total Revenue:"), totalRevenueLabel);
        VBox ordersBox = new VBox(5, new Label("Total Orders:"), totalOrdersLabel);
        totals.getChildren().addAll(revenueBox, ordersBox);

        Label topLabel = new Label("Top 5 Grossing Items");
        topLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        TableView<TopItem> table = new TableView<>();
        TableColumn<TopItem, String> itemCol = new TableColumn<>("Item Name");
        itemCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        itemCol.setPrefWidth(250);
        TableColumn<TopItem, String> revCol = new TableColumn<>("Revenue");
        revCol.setCellValueFactory(new PropertyValueFactory<>("revenueFormatted"));
        revCol.setPrefWidth(150);
        table.getColumns().addAll(itemCol, revCol);

        VBox layout = new VBox(10, header, buttonRow, totals, topLabel, table);
        AnchorPane.setTopAnchor(layout, 0.0);
        AnchorPane.setBottomAnchor(layout, 0.0);
        AnchorPane.setLeftAnchor(layout, 0.0);
        AnchorPane.setRightAnchor(layout, 0.0);
        mainDisplayPane.getChildren().add(layout);

        // === RANGE REPORT BUTTON HANDLER ===
        rangeBtn.setOnAction(e -> {
            mainDisplayPane.getChildren().clear();
            Label rangeHeader = new Label("Sales Report by Date Range");
            rangeHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10;");

            DatePicker startPicker = new DatePicker();
            DatePicker endPicker = new DatePicker();
            Button generateBtn = new Button("Generate");
            HBox dateInputs = new HBox(10, new Label("Start:"), startPicker, new Label("End:"), endPicker, generateBtn);
            dateInputs.setStyle("-fx-padding: 10;");
            dateInputs.setAlignment(Pos.CENTER);

            Label totalRev = new Label("$0.00");
            Label totalOrders = new Label("0");
            TableView<TopItem> rangeTable = new TableView<>();
            TableColumn<TopItem, String> rangeItemCol = new TableColumn<>("Item Name");
            rangeItemCol.setCellValueFactory(new PropertyValueFactory<>("itemName"));
            rangeItemCol.setPrefWidth(250);
            TableColumn<TopItem, String> rangeRevCol = new TableColumn<>("Revenue");
            rangeRevCol.setCellValueFactory(new PropertyValueFactory<>("revenueFormatted"));
            rangeRevCol.setPrefWidth(150);
            rangeTable.getColumns().addAll(rangeItemCol, rangeRevCol);

            VBox rangeLayout = new VBox(10, rangeHeader, dateInputs, rangeTable);
            AnchorPane.setTopAnchor(rangeLayout, 0.0);
            AnchorPane.setBottomAnchor(rangeLayout, 0.0);
            AnchorPane.setLeftAnchor(rangeLayout, 0.0);
            AnchorPane.setRightAnchor(rangeLayout, 0.0);
            mainDisplayPane.getChildren().add(rangeLayout);

            generateBtn.setOnAction(ev -> {
                LocalDate start = startPicker.getValue();
                LocalDate end = endPicker.getValue();
                if (start == null || end == null) {
                    new Alert(Alert.AlertType.WARNING, "Please select both start and end dates.").showAndWait();
                    return;
                }

                try (Connection conn = DatabaseConnector.getConnection()) {
                    // === TOTALS QUERY ===
                    String totalsQuery = """
                        SELECT COALESCE(SUM(orderTotal), 0) AS totalRevenue, COUNT(*) AS totalOrders
                        FROM ordertest
                        WHERE orderDate >= ? AND orderDate < ?;
                    """;
                    try (PreparedStatement ps = conn.prepareStatement(totalsQuery)) {
                        ps.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
                        ps.setTimestamp(2, Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            totalRev.setText(String.format("$%,.2f", rs.getDouble("totalRevenue")));
                            totalOrders.setText(String.valueOf(rs.getInt("totalOrders")));
                        }
                    }

                    // === TOP 5 ITEMS QUERY ===
                    String topQuery = """
                        SELECT m.menuName AS itemName,
                               SUM(oi.quantityPurchased * oi.priceAtPurchase) AS totalRevenue
                        FROM orderItem oi
                        JOIN menu m ON m.menuID = oi.menuID
                        JOIN ordertest o ON o.orderID = oi.orderID
                        WHERE o.orderDate >= ? AND o.orderDate < ?
                        GROUP BY m.menuName
                        ORDER BY totalRevenue DESC
                        LIMIT 5;
                    """;

                    ObservableList<TopItem> list = FXCollections.observableArrayList();
                    try (PreparedStatement ps = conn.prepareStatement(topQuery)) {
                        ps.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
                        ps.setTimestamp(2, Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) {
                            list.add(new TopItem(rs.getString("itemName"), rs.getDouble("totalRevenue")));
                        }
                    }

                    rangeTable.setItems(list);

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Error generating Range Report: " + ex.getMessage()).showAndWait();
                }
            });
        });

        /**
         * Handles generation of the X-Report (Hourly Sales Report).
         * <p>
         * This report visualizes sales by hour for the current day using a {@link LineChart}.
         * It also displays total sales, returns, discards, and voids.
         * </p>
         * <ul>
         *     <li>Hourly sales are grouped by hour via {@code EXTRACT(HOUR FROM orderDate)}</li>
         *     <li>Returns are identified as orders with negative totals</li>
         *     <li>Voids are orders with zero total</li>
         *     <li>Discards are computed from positive {@code restockOrdered} values in inventory</li>
         * </ul>
         * <p>
         * Data are aggregated from the {@code ordertest} and {@code inventory} tables,
         * displayed as both numeric totals and an hourly line graph.
         * </p>
         */
        xReportBtn.setOnAction(e -> {
            mainDisplayPane.getChildren().clear();

            Label xReportHeader = new Label("Hourly Sales Report — (X-Report)");
            xReportHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10;");

            Label totalSalesLabel = new Label("Total Sales: $0.00");
            Label totalReturnsLabel = new Label("Returns: $0.00");
            Label totalDiscardsLabel = new Label("Discards: $0.00");
            Label totalVoidsLabel = new Label("Voids: 0");

            HBox totalsBox = new HBox(30, totalSalesLabel, totalReturnsLabel, totalDiscardsLabel, totalVoidsLabel);
            totalsBox.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-background-radius: 8;");

            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Hour of Day");
            yAxis.setLabel("Sales ($)");

            LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
            chart.setTitle("Hourly Sales for " + currDate);
            chart.setLegendVisible(false);
            chart.setPrefHeight(400);

            VBox xReportlayout = new VBox(10, xReportHeader, chart, totalsBox);
            AnchorPane.setTopAnchor(xReportlayout, 0.0);
            AnchorPane.setBottomAnchor(xReportlayout, 0.0);
            AnchorPane.setLeftAnchor(xReportlayout, 0.0);
            AnchorPane.setRightAnchor(xReportlayout, 0.0);
            mainDisplayPane.getChildren().add(xReportlayout);

            try (Connection conn = DatabaseConnector.getConnection()) {
                if (conn == null) {
                    new Alert(Alert.AlertType.ERROR, "Database connection failed.").showAndWait();
                    return;
                }

                // === HOURLY SALES ===
                String hourlySql = """
                    SELECT EXTRACT(HOUR FROM orderDate) AS hour,
                        SUM(orderTotal) AS totalSales
                    FROM ordertest
                    WHERE DATE(orderDate) = ?
                    GROUP BY hour
                    ORDER BY hour;
                """;
                PreparedStatement ps = conn.prepareStatement(hourlySql);
                ps.setDate(1, java.sql.Date.valueOf(currDate));
                ResultSet rs = ps.executeQuery();

                XYChart.Series<String, Number> series = new XYChart.Series<>();
                double totalSales = 0;

                while (rs.next()) {
                    String hour = String.format("%02d:00", rs.getInt("hour"));
                    double sales = rs.getDouble("totalSales");
                    totalSales += sales;
                    series.getData().add(new XYChart.Data<>(hour, sales));
                }
                chart.getData().add(series);
                totalSalesLabel.setText(String.format("Total Sales: $%,.2f", totalSales));

                // === RETURNS ===
                String returnsSql = """
                    SELECT COALESCE(SUM(orderTotal),0) AS totalReturns
                    FROM ordertest
                    WHERE orderTotal < 0 AND DATE(orderDate) = ?;
                """;
                PreparedStatement psReturns = conn.prepareStatement(returnsSql);
                psReturns.setDate(1, java.sql.Date.valueOf(currDate));
                ResultSet rsReturns = psReturns.executeQuery();
                if (rsReturns.next()) {
                    double returns = Math.abs(rsReturns.getDouble("totalReturns"));
                    totalReturnsLabel.setText(String.format("Returns: $%,.2f", returns));
                }

                // === VOIDS ===
                String voidsSql = """
                    SELECT COUNT(*) AS voidCount
                    FROM ordertest
                    WHERE orderTotal = 0 AND DATE(orderDate) = ?;
                """;
                PreparedStatement psVoids = conn.prepareStatement(voidsSql);
                psVoids.setDate(1, java.sql.Date.valueOf(currDate));
                ResultSet rsVoids = psVoids.executeQuery();
                if (rsVoids.next()) {
                    int voidCount = rsVoids.getInt("voidCount");
                    totalVoidsLabel.setText("Voids: " + voidCount);
                }

                // === DISCARDS ===
                String discardsSql = """
                    SELECT COALESCE(SUM(restockOrdered),0) AS totalDiscards
                    FROM inventory
                    WHERE restockOrdered > 0;
                """;
                ResultSet rsDiscards = conn.prepareStatement(discardsSql).executeQuery();
                if (rsDiscards.next()) {
                    double discards = rsDiscards.getDouble("totalDiscards");
                    totalDiscardsLabel.setText(String.format("Discards: %.0f items", discards));
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error generating X-Report: " + ex.getMessage()).showAndWait();
            }
        });

        /**
         * Handles generation of the Z-Report (End-of-Day Summary).
         * <p>
         * The Z-Report aggregates all daily transactions, showing:
         * total sales, order count, time of first and last order,
         * and placeholders for adjustments like discounts or service charges.
         * </p>
         * <p>
         * It provides an end-of-day snapshot for managerial review.
         * </p>
         */
        zReportBtn.setOnAction(e -> {
            mainDisplayPane.getChildren().clear();
            Label zReportHeader = new Label("Z-Report — End-of-Day Summary");
            zReportHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10;");
            VBox zReportLayout = new VBox(10, zReportHeader);
            zReportLayout.setStyle("-fx-padding: 15;");
            AnchorPane.setTopAnchor(zReportLayout, 0.0);
            AnchorPane.setBottomAnchor(zReportLayout, 0.0);
            AnchorPane.setLeftAnchor(zReportLayout, 0.0);
            AnchorPane.setRightAnchor(zReportLayout, 0.0);
            mainDisplayPane.getChildren().add(zReportLayout);

            try (Connection conn = DatabaseConnector.getConnection()) {
                String sql = """
                    SELECT 
                        COALESCE(SUM(orderTotal), 0) AS totalSales,
                        COUNT(orderID) AS totalOrders,
                        MIN(orderDate) AS firstOrder,
                        MAX(orderDate) AS lastOrder
                    FROM ordertest
                    WHERE DATE(orderDate) = ?;
                """;

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setDate(1, java.sql.Date.valueOf(currDate));
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    double totalSales = rs.getDouble("totalSales");
                    int totalOrders = rs.getInt("totalOrders");
                    Timestamp firstOrder = rs.getTimestamp("firstOrder");
                    Timestamp lastOrder = rs.getTimestamp("lastOrder");

                    Label salesLabel = new Label(String.format("Total Sales: $%,.2f", totalSales));
                    Label orderCountLabel = new Label("Orders Processed: " + totalOrders);
                    Label startLabel = new Label("First Order: " + (firstOrder != null ? firstOrder.toString() : "None"));
                    Label endLabel = new Label("Last Order: " + (lastOrder != null ? lastOrder.toString() : "None"));

                    VBox totalsBox = new VBox(5, salesLabel, orderCountLabel, startLabel, endLabel);
                    totalsBox.setStyle("-fx-padding: 10; -fx-background-color: #f4f4f4; -fx-background-radius: 8;");
                    zReportLayout.getChildren().add(totalsBox);
                }

                // === Adjustments Section ===
                Label adjHeader = new Label("Adjustments and Charges");
                adjHeader.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 5;");
                Label discLabel = new Label("Discounts: $0.00");
                Label voidLabel = new Label("Voids: $0.00");
                Label serviceLabel = new Label("Service Charges: $0.00");

                VBox adjBox = new VBox(5, adjHeader, discLabel, voidLabel, serviceLabel);
                adjBox.setStyle("-fx-padding: 10; -fx-background-color: #f9f9f9; -fx-background-radius: 8;");

                VBox finalizeBox = new VBox(10, adjBox);
                finalizeBox.setStyle("-fx-padding: 10;");
                zReportLayout.getChildren().add(finalizeBox);

            } catch (SQLException ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Error generating Z-Report: " + ex.getMessage()).showAndWait();
            }
        });

        /**
         * Displays the Product Usage Chart interface.
         * <p>
         * Allows managers to select a start and end date, then queries ingredient usage
         * over that period based on menu sales and recipe quantities.
         * </p>
         * <p>
         * Results are shown as a {@link BarChart} plotting ingredients vs. total quantity used,
         * computed from joins across {@code ordertest}, {@code orderItem}, {@code menuInfo}, and {@code inventory}.
         * </p>
         */
        productUsageChartBtn.setOnAction(e -> {
            mainDisplayPane.getChildren().clear();

            Label productHeader = new Label("Product Usage Chart — Select Time Window");
            productHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10;");

            Label startLabel = new Label("Start Date:");
            DatePicker startDatePicker = new DatePicker(LocalDate.now().minusDays(7));
            Label endLabel = new Label("End Date:");
            DatePicker endDatePicker = new DatePicker(LocalDate.now());
            Button generateBtn = new Button("Generate Chart");
            generateBtn.setStyle("-fx-background-color: #2c7; -fx-text-fill: white; -fx-font-weight: bold;");

            HBox dateBox = new HBox(10, startLabel, startDatePicker, endLabel, endDatePicker, generateBtn);
            dateBox.setStyle("-fx-padding: 10;");

            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Ingredient");
            yAxis.setLabel("Quantity Used");

            BarChart<String, Number> usageChart = new BarChart<>(xAxis, yAxis);
            usageChart.setTitle("Inventory Used Over Time");
            usageChart.setLegendVisible(false);
            usageChart.setPrefHeight(400);

            VBox productLayout = new VBox(10, productHeader, dateBox, usageChart);
            productLayout.setStyle("-fx-padding: 15;");
            AnchorPane.setTopAnchor(productLayout, 0.0);
            AnchorPane.setBottomAnchor(productLayout, 0.0);
            AnchorPane.setLeftAnchor(productLayout, 0.0);
            AnchorPane.setRightAnchor(productLayout, 0.0);
            mainDisplayPane.getChildren().add(productLayout);

            // === Generate button logic ===
            generateBtn.setOnAction(ev -> {
                LocalDate start = startDatePicker.getValue();
                LocalDate end = endDatePicker.getValue();

                if (start == null || end == null || end.isBefore(start)) {
                    new Alert(Alert.AlertType.WARNING, "Please select a valid date range.").showAndWait();
                    return;
                }

                usageChart.getData().clear();

                try (Connection conn = DatabaseConnector.getConnection()) {
                    if (conn == null) {
                        new Alert(Alert.AlertType.ERROR, "Database connection failed.").showAndWait();
                        return;
                    }

                    String sql = """
                        SELECT 
                            i.inventoryName AS ingredient,
                            i.unit AS unit,
                            ROUND(SUM(mi.menuInfoQuantity * oi.quantityPurchased), 2) AS totalUsed
                        FROM ordertest o
                        JOIN orderItem oi ON o.orderID = oi.orderID
                        JOIN menuInfo mi ON oi.menuID = mi.menuID
                        JOIN inventory i ON mi.inventoryID = i.inventoryID
                        WHERE o.orderDate BETWEEN ? AND ?
                        GROUP BY i.inventoryName, i.unit
                        ORDER BY totalUsed DESC;
                    """;

                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
                    ps.setTimestamp(2, Timestamp.valueOf(end.plusDays(1).atStartOfDay()));
                    ResultSet rs = ps.executeQuery();

                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    while (rs.next()) {
                        String ingredient = rs.getString("ingredient") + " (" + rs.getString("unit") + ")";
                        double used = rs.getDouble("totalUsed");
                        series.getData().add(new XYChart.Data<>(ingredient, used));
                    }

                    if (series.getData().isEmpty()) {
                        new Alert(Alert.AlertType.INFORMATION, "No ingredient usage found for this period.").showAndWait();
                    } else {
                        usageChart.getData().add(series);
                    }

                } catch (SQLException ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Error generating Product Usage Chart: " + ex.getMessage()).showAndWait();
                }
            });
        });

    }   
        // === Product Usage Chart ===

    /**
     * Handles the "Product Usage Chart" button click on the Manager interface.
     * <p>
     * This method prepares the layout and chart components for displaying ingredient
     * usage across a selected time range. It initializes a {@link BarChart} with labeled
     * axes for ingredient names and quantities used, and embeds it into the
     * {@code mainDisplayPane}.
     * </p>
     * <p>
     * The data population logic is handled separately via the report generation
     * function once user inputs are selected.
     * </p>
     */
    @FXML
    private void handleProductUsageChartButton() {
        mainDisplayPane.getChildren().clear();

        Label header = new Label("Product Usage Chart");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10;");

        // Chart axes
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Ingredient");
        yAxis.setLabel("Quantity Used");

        BarChart<String, Number> usageChart = new BarChart<>(xAxis, yAxis);
        usageChart.setTitle("Inventory Used Over Time");
        usageChart.setLegendVisible(false);
        usageChart.setPrefHeight(400);

        VBox layout = new VBox(10, header, usageChart);
        AnchorPane.setTopAnchor(layout, 0.0);
        AnchorPane.setBottomAnchor(layout, 0.0);
        AnchorPane.setLeftAnchor(layout, 0.0);
        AnchorPane.setRightAnchor(layout, 0.0);
        mainDisplayPane.getChildren().add(layout);
    }

    /**
     * Generates a sales report within a specified time window.
     * <p>
     * This method executes two SQL queries:
     * <ul>
     *     <li>A totals query that retrieves total revenue and number of orders within the date range.</li>
     *     <li>A top-selling items query that retrieves the top 5 grossing products and their revenue.</li>
     * </ul>
     * <p>
     * Results are displayed by updating provided UI components: total revenue and order count
     * labels, and a {@link TableView} showing {@link TopItem} objects.
     * </p>
     * <p>
     * This helper is reusable for multiple report types (daily, weekly, range-based, etc.)
     * that rely on consistent report aggregation logic.
     * </p>
     *
     * @param start               the start of the reporting period (inclusive)
     * @param end                 the end of the reporting period (exclusive)
     * @param totalRevenueLabel   label to update with total revenue
     * @param totalOrdersLabel    label to update with total order count
     * @param table               {@link TableView} to populate with top-selling items
     */
    private void generateReport(LocalDateTime start, LocalDateTime end,
                                Label totalRevenueLabel, Label totalOrdersLabel, TableView<TopItem> table) {
        try (Connection conn = DatabaseConnector.getConnection()) {
            String totalsQuery = """
                SELECT COALESCE(SUM(orderTotal), 0) AS totalRevenue, COUNT(*) AS totalOrders
                FROM ordertest
                WHERE orderDate >= ? AND orderDate < ?;
            """;
            try (PreparedStatement ps = conn.prepareStatement(totalsQuery)) {
                ps.setTimestamp(1, Timestamp.valueOf(start));
                ps.setTimestamp(2, Timestamp.valueOf(end));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    totalRevenueLabel.setText(String.format("$%,.2f", rs.getDouble("totalRevenue")));
                    totalOrdersLabel.setText(String.valueOf(rs.getInt("totalOrders")));
                }
            }

            String topQuery = """
                SELECT m.menuName AS itemName,
                       SUM(oi.quantityPurchased * oi.priceAtPurchase) AS totalRevenue
                FROM orderItem oi
                JOIN menu m ON m.menuID = oi.menuID
                JOIN ordertest o ON o.orderID = oi.orderID
                WHERE o.orderDate >= ? AND o.orderDate < ?
                GROUP BY m.menuName
                ORDER BY totalRevenue DESC
                LIMIT 5;
            """;
            ObservableList<TopItem> list = FXCollections.observableArrayList();
            try (PreparedStatement ps = conn.prepareStatement(topQuery)) {
                ps.setTimestamp(1, Timestamp.valueOf(start));
                ps.setTimestamp(2, Timestamp.valueOf(end));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(new TopItem(rs.getString("itemName"), rs.getDouble("totalRevenue")));
                }
            }
            table.setItems(list);
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error generating report: " + e.getMessage()).showAndWait();
        }
    }

    /**
     * Represents a top-selling menu item used in reports.
     * <p>
     * Each {@code TopItem} object contains the product name and total revenue generated
     * within a selected time frame. It provides both raw and formatted getters for revenue.
     * </p>
     */
    public static class TopItem {
        private final String itemName;
        private final double revenue;

        /**
         * Constructs a new {@code TopItem}.
         *
         * @param itemName the name of the menu item
         * @param revenue  the total revenue generated by this item
         */
        public TopItem(String itemName, double revenue) {
            this.itemName = itemName;
            this.revenue = revenue;
        }

        /**
         * @return the menu item’s name
         */
        public String getItemName() { return itemName; }

        /**
         * @return the total revenue for this item (unformatted)
         */
        public double getRevenue() { return revenue; }

        /**
         * @return a formatted version of the revenue (e.g., "$1,234.56")
         */
        public String getRevenueFormatted() { return String.format("$%,.2f", revenue); }
    }
}
