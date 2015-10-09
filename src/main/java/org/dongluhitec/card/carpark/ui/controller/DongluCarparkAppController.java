package org.dongluhitec.card.carpark.ui.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.dongluhitec.card.carpark.ui.Alerts;

import java.util.Arrays;
import java.util.Optional;

/**
 * 主设置窗口控制器
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
public class DongluCarparkAppController {

    public ComboBox combo_ip;
    public ComboBox combo_port;
    public ComboBox combo_gangting;
    public ComboBox combo_ad;
    public ComboBox combo_validateTime;
    public TableView deviceTable;
    public TableView cardUsageTable;
    public TableView connectionUsageTable;
    public Button addDevice;
    public Button modifyDevice;
    public Button deleteDevice;

    public void addDevice_on_action(ActionEvent actionEvent) {
        VBox deviceDialog = createDeviceDialog("COM", "COM1", "1.1", "192.168.1.1");
        ButtonType addButton = new ButtonType("添加");
        ButtonType cancelButton = new ButtonType("取消");
        Optional<ButtonType> buttonType = Alerts.create(Alert.AlertType.CONFIRMATION)
                .setTitle("添加新的监听设备")
                .setButtons(addButton, cancelButton)
                .setHeaderContent(deviceDialog)
                .setIcon(new Image(ClassLoader.getSystemResourceAsStream("image/set_64.png")))
                .showAndWait();
        if(!buttonType.isPresent()){
            return;
        }
        if(buttonType.get() == addButton){
            Alerts.create(Alert.AlertType.INFORMATION).setTitle("提示").setHeaderText("你点确定了").show();
        }
    }

    public void modifyDevice_on_action(ActionEvent actionEvent) {
    }

    public VBox createDeviceDialog(String linkTypeString,String linkAddressString,String deviceAddressString,String plateIpString){
        ComboBox<String> linkType = new ComboBox<>(FXCollections.observableArrayList("COM","TCP"));
        linkType.setEditable(false);
        linkType.setValue(linkTypeString);
        TextField linkAddress = new TextField(linkAddressString);
        TextField deviceAddress = new TextField(deviceAddressString);
        TextField plateIp = new TextField(plateIpString);

        VBox vBox = new VBox(0);
        HBox linkType_hbox = new HBox(10);
        HBox linkAddress_hbox = new HBox(10);
        HBox deviceAddress_hbox = new HBox(10);
        HBox plateIp_hbox = new HBox(10);

        Label label = new Label("通讯类型");
        linkType_hbox.getChildren().addAll(label, linkType);
        Label label1 = new Label("通讯地址");
        linkAddress_hbox.getChildren().addAll(label1, linkAddress);
        Label label2 = new Label("设备地址");
        deviceAddress_hbox.getChildren().addAll(label2, deviceAddress);
        Label label3 = new Label("车牌识别ip");
        plateIp_hbox.getChildren().addAll(label3, plateIp);
        vBox.getChildren().addAll(linkType_hbox,linkAddress_hbox,deviceAddress_hbox,plateIp_hbox);

        Arrays.asList(label,label1,label2,label3).forEach(each->{
            each.setPrefWidth(80);
            each.setAlignment(Pos.CENTER_RIGHT);
        });
        Arrays.asList(linkType_hbox,linkAddress_hbox,deviceAddress_hbox,plateIp_hbox).forEach(each->{
            each.setAlignment(Pos.CENTER);
            VBox.setMargin(each, new Insets(15, 0, 0, 0));
        });
        Arrays.asList(linkAddress,linkType,deviceAddress,plateIp).forEach(each->each.setPrefWidth(150));

        return vBox;
    }

    public void deleteDevice_on_action(ActionEvent actionEvent) {
    }

    public void refresh_cardUsage_on_action(ActionEvent actionEvent) {
    }

    public void refresh_connectionUsage_on_action(ActionEvent actionEvent) {
    }

    public void exit_on_action(ActionEvent actionEvent) {
        Platform.exit();
        System.exit(1);
    }

    public void menu_about_on_action(ActionEvent actionEvent) {

    }

    public void menu_startFlowSystem_on_action(ActionEvent actionEvent) {

    }

    public void rightnowUse_on_action(ActionEvent actionEvent) {

    }
}
