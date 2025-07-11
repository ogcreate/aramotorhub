package com.ogcreate.app.controllers.store;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

import com.ogcreate.app.SettingsWindowHelper;
import com.ogcreate.app.database.DatabaseConnection;
import com.ogcreate.app.database.User;
import com.ogcreate.app.database.UserSession;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashboardController implements Initializable {

    @FXML
    private Label labelTodayRevenue, labelTodayCompleteOrder, labelPendingOrder, labelTodayCustomer, labelStatus;

    @FXML
    private VBox dashboardContainer;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    void handleViewAllOrder(ActionEvent event) {
        loadOrdersForCurrentStore(null);
    }

    @FXML
    void handleViewCompletedOrder(ActionEvent event) {
        loadOrdersForCurrentStore("COMPLETED");
    }

    @FXML
    void handleViewPendingOrder(ActionEvent event) {
        loadOrdersForCurrentStore("PENDING");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dashboardContainer.setSpacing(7.5);
        setupCategoryComboBox();
        loadDashboardStats();
        loadOrdersForCurrentStore();

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> loadDashboardStats()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void loadOrdersForCurrentStore() {
        loadOrdersForCurrentStore(null);
    }

    public void loadOrdersForCurrentStore(String statusFilter) {
        dashboardContainer.getChildren().clear();
        int storeId = UserSession.getCurrentUser().getUserId();

        StringBuilder query = new StringBuilder("""
                    SELECT o.order_id, o.total_price, o.status, o.created_at, o.address,
                           CONCAT(u.first_name, ' ', u.last_name) AS customer_name,
                           SUM(oi.quantity) AS total_quantity,
                           SUM(oi.price_at_purchase * oi.quantity) AS total_price_at_purchase
                    FROM `order` o
                    JOIN user u ON o.customer_id = u.user_id
                    JOIN order_item oi ON o.order_id = oi.order_id
                    JOIN product p ON oi.product_id = p.product_id
                    WHERE p.seller_id = ?
                """);

        if (statusFilter != null && !statusFilter.isEmpty()) {
            query.append(" AND o.status = ? ");
        }

        query.append("GROUP BY o.order_id ORDER BY o.created_at DESC");

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement stmt = conn.prepareStatement(query.toString())) {

            stmt.setInt(1, storeId);
            if (statusFilter != null && !statusFilter.isEmpty()) {
                stmt.setString(2, statusFilter);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/store/DashboardRow.fxml"));
                Node orderRow = loader.load();

                DashboardRowController controller = loader.getController();
                controller.setData(
                        rs.getString("customer_name"),
                        rs.getString("address"),
                        rs.getString("created_at"),
                        String.valueOf(rs.getInt("total_quantity")),
                        String.valueOf(rs.getDouble("total_price_at_purchase")),
                        rs.getString("status"),
                        rs.getInt("order_id"));
                controller.setDashboardController(this);

                dashboardContainer.getChildren().add(orderRow);
            }

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    public void loadDashboardStats() {
        User currentUser = UserSession.getCurrentUser();
        if (currentUser == null) {
            System.err.println("No logged-in user.");
            return;
        }

        String totalRevenueQuery = "SELECT SUM(total_price) AS revenue FROM `order` WHERE status = 'COMPLETED'";
        String todayCompletedOrdersQuery = "SELECT COUNT(*) AS count FROM `order` WHERE status = 'COMPLETED' AND DATE(created_at) = CURDATE()";
        String pendingOrdersQuery = "SELECT COUNT(*) AS count FROM `order` WHERE status = 'PENDING'";
        String todayCustomersQuery = "SELECT COUNT(DISTINCT customer_id) AS count FROM `order` WHERE DATE(created_at) = CURDATE()";

        try (Connection conn = DatabaseConnection.connect()) {
            try (PreparedStatement stmt = conn.prepareStatement(totalRevenueQuery);
                    ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    labelTodayRevenue.setText(String.valueOf((int) rs.getDouble("revenue")));
            }

            try (PreparedStatement stmt = conn.prepareStatement(todayCompletedOrdersQuery);
                    ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    labelTodayCompleteOrder.setText(String.valueOf(rs.getInt("count")));
            }

            try (PreparedStatement stmt = conn.prepareStatement(pendingOrdersQuery);
                    ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    labelPendingOrder.setText(String.valueOf(rs.getInt("count")));
            }

            try (PreparedStatement stmt = conn.prepareStatement(todayCustomersQuery);
                    ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    labelTodayCustomer.setText(String.valueOf(rs.getInt("count")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupCategoryComboBox() {
        String sql = "SELECT name FROM category ORDER BY name ASC";

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
                openCategoryView(selectedCategory);
            }
        });
    }

    private void openCategoryView(String categoryName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/store/Categories.fxml"));
            Parent root = loader.load();
            CategoriesController controller = loader.getController();
            controller.setSelectedCategory(categoryName);

            Stage stage = (Stage) categoryComboBox.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleHomeButton(ActionEvent event) {
    }

    @FXML
    void handleInventoryClick(ActionEvent event) {
        loadScene(event, "/resources/fxml/store/Inventory.fxml");
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

    @FXML
    void handleProductsClick(ActionEvent event) {
        loadScene(event, "/resources/fxml/store/Products.fxml");
    }

    @FXML
    void handleProfileClick(ActionEvent event) {
        loadScene(event, "/resources/fxml/store/Profile.fxml");
    }

    @FXML
    void handleShopsClick(ActionEvent event) {
        loadScene(event, "/resources/fxml/store/Shops.fxml");
    }

    private void loadScene(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newRoot = loader.load();
            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            currentStage.setScene(new Scene(newRoot));
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
