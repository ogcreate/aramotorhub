package com.ogcreate.app.controllers.customer;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class CartProductContainerController {

    @FXML
    private Label labelPrice;

    @FXML
    private Label labelProductInitials;

    @FXML
    private Label labelProductName;

    @FXML
    private Label labelStoreAdress;

    @FXML
    private Label labelStoreName;

    @FXML
    private Spinner<Integer> spinnerProductAmount;

    @FXML
    private HBox rootHBox;

    private double pricePerItem;
    private Runnable onQuantityChangeCallback;

    private int productId;
    private int cartId;

    public int getProductId() {
        return productId;
    }

    public int getQuantity() {
        return spinnerProductAmount.getValue();
    }

    public double getPrice() {
        return Double.parseDouble(labelPrice.getText().replace(",", ""));
    }

    public void setProductDetails(String name, String initials, String price, String storeName, String storeAddress,
            int quantity, int productId, int cartId, Runnable onQuantityChangeCallback) {
        labelProductName.setText(name);
        labelProductInitials.setText(initials);
        labelPrice.setText(price);
        labelStoreName.setText(storeName);
        labelStoreAdress.setText(storeAddress);

        this.pricePerItem = Double.parseDouble(price);
        this.onQuantityChangeCallback = onQuantityChangeCallback;
        this.productId = productId;
        this.cartId = cartId;

        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000,
                quantity);
        spinnerProductAmount.setValueFactory(valueFactory);

        spinnerProductAmount.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateQuantityInDatabase(cartId, productId, newVal);
            if (onQuantityChangeCallback != null) {
                onQuantityChangeCallback.run();
            }
        });
    }

    public double getSubtotal() {
        return spinnerProductAmount.getValue() * pricePerItem;
    }

    @FXML
    private void handleDeleteButton() {
        deleteFromDatabase(cartId, productId);
        ((javafx.scene.layout.Pane) rootHBox.getParent()).getChildren().remove(rootHBox);
        if (onQuantityChangeCallback != null) {
            onQuantityChangeCallback.run();
        }
    }

    private void updateQuantityInDatabase(int cartId, int productId, int newQuantity) {
        String updateSql = "UPDATE cart_item SET quantity = ? WHERE cart_id = ? AND product_id = ?";
        try (var conn = com.ogcreate.app.database.DatabaseConnection.connect();
                var stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, newQuantity);
            stmt.setInt(2, cartId);
            stmt.setInt(3, productId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteFromDatabase(int cartId, int productId) {
        String deleteSql = "DELETE FROM cart_item WHERE cart_id = ? AND product_id = ?";
        try (var conn = com.ogcreate.app.database.DatabaseConnection.connect();
                var stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, cartId);
            stmt.setInt(2, productId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        @FXML
    private void handleProfileClick(ActionEvent event) {
        System.out.println("handleProfileClick triggered");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/fxml/customer/Profile.fxml"));
            Parent newRoot = loader.load();

            Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene newScene = new Scene(newRoot);
            currentStage.setScene(newScene);
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
