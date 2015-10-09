package org.dongluhitec.card.carpark.ui;

import com.sun.javafx.stage.StageHelper;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Optional;

/**
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
public class Alerts {

    private Alert alert;

    private Alerts(Alert.AlertType alertType) {
        this.alert = new Alert(alertType);
    }

    public static Alerts create(Alert.AlertType alertType) {
        return new Alerts(alertType);
    }


    public Alerts setHeaderText(String headerText) {
        alert.setHeaderText(headerText);
        return this;
    }

    public Alerts setIcon(Image icon) {
        //TODO how set icon
        return this;
    }

    public Alerts setButtons(ButtonType... buttonTypes) {
        alert.getButtonTypes().setAll(buttonTypes);
        return this;
    }

    public Alerts setHeaderContent(Node node) {
        alert.getDialogPane().setHeader(node);
        return this;
    }

    public Optional<ButtonType> showAndWait() {
        return alert.showAndWait();
    }

    public void show() {
        alert.show();
    }


    public Alerts setTitle(String title) {
        alert.setTitle(title);
        return this;
    }


    public Alerts setContentText(String contentText) {
        alert.setContentText(contentText);
        return this;
    }
}
