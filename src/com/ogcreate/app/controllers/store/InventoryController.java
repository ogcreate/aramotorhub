package com.ogcreate.app.controllers.store;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

import com.ogcreate.app.SettingsWindowHelper;
import com.ogcreate.app.database.DatabaseConnection;
import com.ogcreate.app.database.User;
import com.ogcreate.app.database.UserSession;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class InventoryController implements Initializable {

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private Label labelCategory1, labelCategory2, labelCategory3, labelCategory4;
    @FXML
    private Label labelCategory5, labelCategory6, labelCategory7, labelCategory8;
    @FXML
    private Label labelCategoryItem1, labelCategoryItem2, labelCategoryItem3, labelCategoryItem4;
    @FXML
    private Label labelCategoryItem5, labelCategoryItem6, labelCategoryItem7, labelCategoryItem8;

    @FXML
    private Label labelTotalAssetValue, labelTotalProduct, labelSoldStock;

    @FXML
    private Pane redPercent, greenPercent;

    private void loadTotalProductStats() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null)
            return;

        String query = """
                    SELECT
                        COALESCE(SUM(CASE WHEN status = 'AVAILABLE' THEN stock ELSE 0 END), 0) AS available_stock,
                        COALESCE(SUM(CASE WHEN status = 'OUT_OF_STOCK' THEN stock ELSE 0 END), 0) AS out_of_stock,
                        COALESCE(SUM(CASE WHEN status = 'AVAILABLE' THEN price * stock ELSE 0 END), 0) AS total_value
                    FROM product
                    WHERE seller_id = ?
                """;

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, currentUser.getUserId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int available = rs.getInt("available_stock");
                int outOfStock = rs.getInt("out_of_stock");
                int total = available + outOfStock;
                double totalValue = rs.getDouble("total_value");

                labelTotalProduct.setText(total + " items");
                labelSoldStock.setText(outOfStock + " sold");
                labelTotalAssetValue.setText("₱" + String.format("%,.2f", totalValue));

                updateStockPercentBar(outOfStock, total); // 👈 Visual bar update
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateStockPercentBar(int sold, int total) {
        if (total <= 0) {
            redPercent.setPrefWidth(0);
            greenPercent.setPrefWidth(0);
            return;
        }

        double redRatio = (double) sold / total;
        double greenRatio = 1.0 - redRatio;

        double totalWidth = 200.0; // Set to match HBox's width
        redPercent.setPrefWidth(totalWidth * redRatio);
        greenPercent.setPrefWidth(totalWidth * greenRatio);
    }

    @FXML
    void handleUploadProduct(ActionEvent event) {
        loadScene(event, "/resources/fxml/store/InventoryUpload.fxml");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCategoryComboBox();
        loadCategoryData();
        loadTotalProductStats();
    }

    private void setupCategoryComboBox() {
        String sql = """
                    SELECT DISTINCT c.name
                    FROM category c
                    JOIN product p ON c.category_id = p.category_id
                    ORDER BY c.name ASC
                """;

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categoryComboBox.getItems().add(rs.getString("name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        categoryComboBox.setOnAction(event -> {
            String selectedCategory = categoryComboBox.getValue();
            if (selectedCategory != null) {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                openCategoryView(selectedCategory, stage);
            }
        });
    }

    private void loadCategoryData() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null)
            return;

        String query = """
                    SELECT c.name AS category_name, COUNT(p.product_id) AS product_count
                    FROM category c
                    JOIN product p ON c.category_id = p.category_id
                    WHERE p.seller_id = ? AND p.status = 'AVAILABLE'
                    GROUP BY c.category_id, c.name
                    ORDER BY c.category_id ASC
                """;

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, currentUser.getUserId());
            ResultSet rs = stmt.executeQuery();

            int index = 1;
            while (rs.next() && index <= 8) {
                String category = rs.getString("category_name");
                int count = rs.getInt("product_count");

                switch (index) {
                    case 1 -> {
                        labelCategory1.setText(category);
                        labelCategoryItem1.setText(count + " items");
                    }
                    case 2 -> {
                        labelCategory2.setText(category);
                        labelCategoryItem2.setText(count + " items");
                    }
                    case 3 -> {
                        labelCategory3.setText(category);
                        labelCategoryItem3.setText(count + " items");
                    }
                    case 4 -> {
                        labelCategory4.setText(category);
                        labelCategoryItem4.setText(count + " items");
                    }
                    case 5 -> {
                        labelCategory5.setText(category);
                        labelCategoryItem5.setText(count + " items");
                    }
                    case 6 -> {
                        labelCategory6.setText(category);
                        labelCategoryItem6.setText(count + " items");
                    }
                    case 7 -> {
                        labelCategory7.setText(category);
                        labelCategoryItem7.setText(count + " items");
                    }
                    case 8 -> {
                        labelCategory8.setText(category);
                        labelCategoryItem8.setText(count + " items");
                    }
                }

                index++;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openCategoryView(String categoryName, Stage currentStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/store/InventorySelect.fxml"));
            Parent root = loader.load();

            InventorySelectController controller = loader.getController();

            User currentUser = UserSession.getCurrentUser();
            if (currentUser != null) {
                controller.setSellerId(currentUser.getUserId());
            }

            controller.loadProductsByCategory(categoryName);

            currentStage.setScene(new Scene(root));
            currentStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleCategoryBolts(ActionEvent event) {
        openCategoryView("Bolts", getStage(event));
    }

    @FXML
    void handleCategoryElectrical(ActionEvent event) {
        openCategoryView("Electrical", getStage(event));
    }

    @FXML
    void handleCategoryEngine(ActionEvent event) {
        openCategoryView("Engine", getStage(event));
    }

    @FXML
    void handleCategoryExterior(ActionEvent event) {
        openCategoryView("Exterior", getStage(event));
    }

    @FXML
    void handleCategoryOil(ActionEvent event) {
        openCategoryView("Oil", getStage(event));
    }

    @FXML
    void handleCategorySuspension(ActionEvent event) {
        openCategoryView("Suspension", getStage(event));
    }

    @FXML
    void handleCategoryTransmission(ActionEvent event) {
        openCategoryView("Transmission", getStage(event));
    }

    @FXML
    void handleCategoryWheels(ActionEvent event) {
        openCategoryView("Wheels", getStage(event));
    }

    @FXML
    void handleDashboardClick(ActionEvent event) {
        loadScene(event, "/resources/fxml/store/Dashboard.fxml");
    }

    @FXML
    void handleHomeButton(ActionEvent event) {
        loadScene(event, "/resources/fxml/store/Profile.fxml");
    }

    @FXML
    void handleProductsClick(ActionEvent event) {
        loadScene(event, "/resources/fxml/store/Products.fxml");
    }

    @FXML
    void handleShopsClick(ActionEvent event) {
        loadScene(event, "/resources/fxml/store/Shops.fxml");
    }

    @FXML
    void handleProfileClick(ActionEvent event) {
        loadScene(event, "/resources/fxml/store/Profile.fxml");
    }

    @FXML
    void handleLogOutButton(ActionEvent event) {
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        SettingsWindowHelper.logout(currentStage);
    }

    @FXML
    void handleOpenSettings(ActionEvent event) {
        SettingsWindowHelper.openSettings((Node) event.getSource());
    }

    private void loadScene(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(new Scene(root));
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Stage getStage(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }
}
