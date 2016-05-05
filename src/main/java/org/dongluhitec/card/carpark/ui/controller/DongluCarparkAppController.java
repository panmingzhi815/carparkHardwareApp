package org.dongluhitec.card.carpark.ui.controller;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import com.sun.javafx.stage.StageHelper;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.dongluhitec.card.carpark.dao.HibernateDao;
import org.dongluhitec.card.carpark.domain.AbstractDomain;
import org.dongluhitec.card.carpark.domain.CardUsage;
import org.dongluhitec.card.carpark.domain.ConnectionUsage;
import org.dongluhitec.card.carpark.hardware.HardwareService;
import org.dongluhitec.card.carpark.model.Device;
import org.dongluhitec.card.carpark.ui.Alerts;
import org.dongluhitec.card.carpark.ui.Config;
import org.dongluhitec.card.carpark.ui.DongluCarparkApp;
import org.dongluhitec.card.carpark.ui.LinkDevice;
import org.dongluhitec.card.carpark.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 主设置窗口控制器
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
public class DongluCarparkAppController implements Initializable {
    private Logger LOGGER = LoggerFactory.getLogger(DongluCarparkAppController.class);

    public static final String CONFIG_FILEPATH = "config.data";
    public static Config config;

    public ComboBox<String> combo_ip;
    public ComboBox<String> combo_port;
    public ComboBox<String> combo_gangting;
    public ComboBox<String> combo_ad;
    public ComboBox<String> combo_validateTime;

    public TableView<LinkDevice> deviceTable;
    public TableView<CardUsage> cardUsageTable;
    public TableView<ConnectionUsage> connectionUsageTable;
    public Button addDevice;
    public Button modifyDevice;
    public Button deleteDevice;
    public SimpleStringProperty linkType = new SimpleStringProperty("COM");
    public SimpleStringProperty linkAddress = new SimpleStringProperty("COM1");
    public SimpleStringProperty deviceType = new SimpleStringProperty("进口");
    public SimpleStringProperty deviceName = new SimpleStringProperty("12345");
    public SimpleStringProperty deviceAddress = new SimpleStringProperty("1.1");
    public SimpleStringProperty plateIP = new SimpleStringProperty("192.168.1.1");
    private static ScheduledExecutorService scheduledExecutorService;
    private static ScheduledExecutorService scheduledExecutorService1;

    private HibernateDao hibernateDao = new HibernateDao();

    public void addDevice_on_action() {
        linkType.setValue("COM");
        linkAddress.setValue("COM1");
        deviceType.setValue("进口");
        deviceName.setValue("12345");
        deviceAddress.setValue("1.1");
        plateIP.setValue("192.168.1.1");

        Boolean aBoolean = addDiaglog();
        //点击取消
        if (aBoolean == null) {
            return;
        }
        //不符合条件，重新输入
        if (aBoolean) {

            LinkDevice linkDevice = new LinkDevice(linkType.get(), linkAddress.get(), deviceType.get(), deviceName.get(), deviceAddress.get(), plateIP.get());
            ObservableList<LinkDevice> items = deviceTable.getItems();

            items.add(linkDevice);
            deviceTable.setItems(items);
        } else {
            addDevice_on_action();
        }
    }

    public void modifyDevice_on_action() {
        TableView.TableViewSelectionModel<LinkDevice> linkDeviceTableViewSelectionModel = deviceTable.selectionModelProperty().get();
        LinkDevice selectedItem = linkDeviceTableViewSelectionModel.getSelectedItem();
        if (selectedItem == null) {
            Alerts.create(Alert.AlertType.INFORMATION).setTitle("提示").setHeaderText("请先选择一个设备，再开始编辑").showAndWait();
            return;
        }

        linkType.setValue(selectedItem.getLinkType());
        linkAddress.setValue(selectedItem.getLinkAddress());
        deviceType.setValue(selectedItem.getDeviceType());
        deviceName.setValue(selectedItem.getDeviceName());
        deviceAddress.setValue(selectedItem.getDeviceAddress());
        plateIP.setValue(selectedItem.getPlateIp());

        Boolean aBoolean = addDiaglog();

        //点击取消
        if (aBoolean == null) {
            return;
        }
        //不符合条件，重新输入
        if (!aBoolean) {
            addDevice_on_action();
            return;
        }
        selectedItem.setLinkType(linkType.get());
        selectedItem.setLinkAddress(linkAddress.get());
        selectedItem.setDeviceType(deviceType.get());
        selectedItem.setDeviceName(deviceName.get());
        selectedItem.setDeviceAddress(deviceAddress.get());
        selectedItem.setPlateIp(plateIP.get());

        ObservableList<LinkDevice> linkDevices = FXCollections.observableArrayList(deviceTable.getItems());
        ListProperty<LinkDevice> linkDeviceListProperty = new SimpleListProperty<>(linkDevices);
        deviceTable.getItems().clear();
        deviceTable.itemsProperty().set(linkDeviceListProperty);
    }

    public Boolean addDiaglog() {
        VBox deviceDialog = createDeviceDialog(linkType, linkAddress, deviceType, deviceName, deviceAddress, plateIP);
        ButtonType addButton = new ButtonType("保存");
        ButtonType cancelButton = new ButtonType("取消");
        Optional<ButtonType> buttonType = Alerts.create(Alert.AlertType.CONFIRMATION)
                .setTitle("编辑基本的监听设备信息")
                .setButtons(addButton, cancelButton)
                .setHeaderContent(deviceDialog)
                .setIcon()
                .showAndWait();
        if (buttonType.isPresent() && buttonType.get() == addButton) {
            if (Strings.isNullOrEmpty(linkType.get())) {
                Alerts.create(Alert.AlertType.ERROR).setTitle("错误").setHeaderText("连接类型未设置").showAndWait();
                return false;
            }
            if (Strings.isNullOrEmpty(linkAddress.get())) {
                Alerts.create(Alert.AlertType.ERROR).setTitle("错误").setHeaderText("连接地址未设置").showAndWait();
                return false;
            }
            if (Strings.isNullOrEmpty(deviceType.get())) {
                Alerts.create(Alert.AlertType.ERROR).setTitle("错误").setHeaderText("进出类型未设置").showAndWait();
                return false;
            }
            if (Strings.isNullOrEmpty(deviceAddress.get())) {
                Alerts.create(Alert.AlertType.ERROR).setTitle("错误").setHeaderText("设备地址未设置").showAndWait();
                return false;
            }
            return true;
        }
        return null;
    }

    public VBox createDeviceDialog(SimpleStringProperty linkTypeString, SimpleStringProperty linkAddressString, SimpleStringProperty deviceTypeString, SimpleStringProperty deviceNameString, SimpleStringProperty deviceAddressString, SimpleStringProperty plateIpString) {
        ComboBox<String> linkType = new ComboBox<>(FXCollections.observableArrayList("COM", "TCP"));
        linkType.setEditable(false);
        linkType.setValue(linkTypeString.getValue());
        linkType.valueProperty().bindBidirectional(linkTypeString);

        TextField linkAddress = new TextField("");
        linkAddress.textProperty().bindBidirectional(linkAddressString);

        ComboBox<String> deviceType = new ComboBox<>(FXCollections.observableArrayList("进口", "出口"));
        deviceType.setEditable(false);
        deviceType.setValue(deviceType.getValue());
        deviceType.valueProperty().bindBidirectional(deviceTypeString);

        TextField deviceName = new TextField("");
        deviceName.textProperty().bindBidirectional(deviceNameString);

        TextField deviceAddress = new TextField("");
        deviceAddress.textProperty().bindBidirectional(deviceAddressString);

        TextField plateIp = new TextField("");
        plateIp.textProperty().bindBidirectional(plateIpString);

        VBox vBox = new VBox(0);
        HBox linkType_hbox = new HBox(10);
        HBox linkAddress_hbox = new HBox(10);
        HBox deviceType_hbox = new HBox(10);
        HBox deviceName_hbox = new HBox(10);
        HBox deviceAddress_hbox = new HBox(10);
        HBox plateIp_hbox = new HBox(10);

        Label label = new Label("通讯类型");
        linkType_hbox.getChildren().addAll(label, linkType);

        Label label1 = new Label("通讯地址");
        linkAddress_hbox.getChildren().addAll(label1, linkAddress);

        Label lable_deviceType = new Label("进出类型");
        deviceType_hbox.getChildren().addAll(lable_deviceType, deviceType);

        Label lable_deviceName = new Label("设备名称");
        deviceName_hbox.getChildren().addAll(lable_deviceName, deviceName);

        Label label2 = new Label("设备地址");
        deviceAddress_hbox.getChildren().addAll(label2, deviceAddress);

        Label label3 = new Label("车牌识别ip");
        plateIp_hbox.getChildren().addAll(label3, plateIp);

        vBox.getChildren().addAll(linkType_hbox, linkAddress_hbox, deviceType_hbox, deviceName_hbox, deviceAddress_hbox, plateIp_hbox);

        Arrays.asList(label, label1, label2, label3, lable_deviceName, lable_deviceType).forEach(each -> {
            each.setPrefWidth(80);
            each.setAlignment(Pos.CENTER_RIGHT);
        });
        Arrays.asList(linkType_hbox, linkAddress_hbox, deviceAddress_hbox, plateIp_hbox, deviceName_hbox, deviceType_hbox).forEach(each -> {
            each.setAlignment(Pos.CENTER);
            VBox.setMargin(each, new Insets(15, 0, 0, 0));
        });
        Arrays.asList(linkAddress, linkType, deviceType, deviceName, deviceAddress, plateIp).forEach(each -> each.setPrefWidth(150));

        return vBox;
    }

    public void deleteDevice_on_action() {
        TableView.TableViewSelectionModel<LinkDevice> linkDeviceTableViewSelectionModel = deviceTable.selectionModelProperty().get();
        LinkDevice selectedItem = linkDeviceTableViewSelectionModel.getSelectedItem();
        if (selectedItem == null) {
            Alerts.create(Alert.AlertType.INFORMATION).setTitle("错误").setHeaderText("请先选择一个设备").showAndWait();
            return;
        }
        deviceTable.getItems().remove(selectedItem);
    }

    private void constructTableCellValueFactory(String[] columns, TableView<? extends AbstractDomain> tableView) {
        List<String> columnList = Arrays.asList(columns);
        for (int i = 0; i < columnList.size(); i++) {
            tableView.getColumns().get(i).setCellValueFactory(new PropertyValueFactory<>(columnList.get(i)));
        }
    }

    public void refresh_connectionUsage_on_action() {
        if (scheduledExecutorService != null) {
            return;
        }
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            ObservableList<Stage> stages = StageHelper.getStages();
            if (stages.size() == 0 || !stages.get(0).isShowing()) {
                return;
            }
            List list = hibernateDao.list(ConnectionUsage.class, 0, Integer.MAX_VALUE);
            ObservableList cardUsages = FXCollections.observableArrayList(list);
            connectionUsageTable.setItems(cardUsages);
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    public void refresh_cardUsage_on_action() {
        if (scheduledExecutorService1 != null) {
            return;
        }
        scheduledExecutorService1 = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService1.scheduleWithFixedDelay(() -> {
            ObservableList<Stage> stages = StageHelper.getStages();
            if (stages.size() == 0 || !stages.get(0).isShowing()) {
                return;
            }
            List list = hibernateDao.list(CardUsage.class, 0, Integer.MAX_VALUE);
            ObservableList cardUsages = FXCollections.observableArrayList(list);
            cardUsageTable.setItems(cardUsages);
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    public void exit_on_action() {
        Platform.exit();
        System.exit(1);
    }

    public void menu_about_on_action() {
        VBox vBox = new VBox(10);
        vBox.setPadding(new Insets(30, 30, 30, 30));

        ArrayList<String> labels = new ArrayList<>();
        labels.add("公司名称：深圳市东陆高新实业有限公司");
        labels.add("软件名称：停车场对接底层");
        labels.add("软件版本：1.0.0.3");
        labels.add("授权组织：" + DongluCarparkApp.softPrivilegeGroupName);
        labels.add("技术支持：26992770");

        labels.forEach(each -> {
            HBox hBox = new HBox(10);
            Label value = new Label(each);
            value.setFont(new Font(15));
            hBox.getChildren().add(value);

            vBox.getChildren().add(hBox);
        });

        Alerts.create(Alert.AlertType.INFORMATION).setTitle("关于").setHeaderContent(vBox).showAndWait();

    }

    public void menu_startFlowSystem_on_action() {

    }

    private void validate(String value, String tip) {
        if (Strings.isNullOrEmpty(value)) {
            Alerts.create(Alert.AlertType.ERROR).setTitle("错误").setHeaderText(tip).showAndWait();
            throw new RuntimeException(tip);
        }
    }

    public void rightNowUse_on_action() {
        validate(combo_ip.getValue(), "数据接收ip未设置");
        validate(combo_port.getValue(), "接收端口未设置");
        validate(combo_gangting.getValue(), "岗亭名称未设置");
        validate(combo_ad.getValue(), "广告语未设置");
        validate(combo_validateTime.getValue(), "自动校时设置");

        try {
            String ip = combo_ip.getValue();
            Integer port = Integer.valueOf(combo_port.getValue());
            String gangting = combo_gangting.getValue();
            String ad = combo_ad.getValue();
            Integer validateTime = Integer.valueOf(combo_validateTime.getValue());

            Config config = new Config(ip, port, gangting, ad, validateTime);
            config.setLinkDeviceList(deviceTable.getItems());

            FileUtil.writeObjectToFile(config, CONFIG_FILEPATH);
            DongluCarparkAppController.config = config;
            HardwareService.isAlreadySendAd = false;
            Alerts.create(Alert.AlertType.INFORMATION).setTitle("提示").setHeaderText("保存配置成功").showAndWait();
        } catch (NumberFormatException e) {
            Alerts.create(Alert.AlertType.ERROR).setTitle("错误").setHeaderText("数据格式不正确").setContentText(e.getMessage()).showAndWait();
            return;
        } catch (IOException e) {
            Alerts.create(Alert.AlertType.ERROR).setTitle("错误").setHeaderText("保存配置文件出错").setContentText(e.getMessage()).showAndWait();
        }

        setTcpDeviceIp();
    }

    private void loadMaxId() {
        hibernateDao.deleteAll(CardUsage.class);
        hibernateDao.deleteAll(ConnectionUsage.class);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        constructTableCellValueFactory(new String[]{"linkType", "linkAddress", "deviceType", "deviceName", "deviceAddress", "plateIp", "deviceVersion"}, deviceTable);
        constructTableCellValueFactory(new String[]{"table_id", "identifier", "deviceName", "databaseTime"}, cardUsageTable);
        constructTableCellValueFactory(new String[]{"table_id", "direction", "shortContent"}, connectionUsageTable);

        loadMaxId();

        try {
            Object o = FileUtil.readObjectFromFile(CONFIG_FILEPATH);
            if (o == null) {
                return;
            }
            config = (Config) o;

            combo_ip.setValue(Strings.nullToEmpty(config.getReceiveIp()));
            combo_port.setValue(String.valueOf(config.getReceivePort()));
            combo_gangting.setValue(config.getGangtingName());
            combo_ad.setValue(config.getAd());
            combo_validateTime.setValue(String.valueOf(config.getValidateTimeLength()));

            deviceTable.setItems(FXCollections.observableArrayList(config.getLinkDeviceList()));
        } catch (IOException e) {
            LOGGER.error("读取配置文件出错", e);
            Optional<ButtonType> buttonType = Alerts.create(Alert.AlertType.CONFIRMATION).setTitle("错误").setHeaderText("读取配置文件出错,是否现在删除旧的配置文件?").setContentText(e.getMessage()).showAndWait();
            if (buttonType.isPresent() && buttonType.get() == ButtonType.OK) {
                new File(CONFIG_FILEPATH).delete();
                Alerts.create(Alert.AlertType.INFORMATION).setTitle("提示").setHeaderText("删除成功,请重新启动本软件进行重新配置").showAndWait();
            }
        }

    }

    public static List<LinkDevice> getLinkDeviceList() {
        if (config == null) {
            return new ArrayList<>();
        }else {
            return config.getLinkDeviceList();
        }
    }

    public void setTcpDeviceIp(){
        String hostAddress = "";
        try {
            hostAddress = Inet4Address.getLocalHost().getHostAddress();
            LOGGER.info("本机ip地址为:{}",hostAddress);
        } catch (UnknownHostException e) {
            Alerts.create(Alert.AlertType.ERROR).setTitle("错误").setHeaderText("获取本机ip错误").setContentText(e.getMessage()).showAndWait();
        }

        List<LinkDevice> linkDeviceList = getLinkDeviceList();
        List<LinkDevice> tcpDeviceList = linkDeviceList.stream().filter(filter -> filter.getLinkType().equalsIgnoreCase("tcp")).collect(Collectors.toList());
        for (LinkDevice linkDevice : tcpDeviceList) {
            ListenableFuture<Boolean> setIpFuture = HardwareService.messageHardware.setIp(linkDevice.toDevice(), hostAddress);
            try {
                setIpFuture.get(2, TimeUnit.SECONDS);
                Alerts.create(Alert.AlertType.INFORMATION).setTitle("提示").setHeaderText("设置设备:" + linkDevice.getLinkAddress() + "服务器地址成功 !").showAndWait();
            } catch (Exception e) {
                String headText = "设置设备 " + linkDevice.getLinkAddress() + "的服务器ip为本机地址时发生错误";
                Alerts.create(Alert.AlertType.ERROR).setTitle("错误").setHeaderText(headText).setException(e).showAndWait();
            }
        }
    }
}
