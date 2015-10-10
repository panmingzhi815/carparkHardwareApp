package org.dongluhitec.card.carpark.ui;

import com.google.common.eventbus.Subscribe;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.dongluhitec.card.carpark.hardware.HardwareService;
import org.dongluhitec.card.carpark.util.EventBusUtil;
import org.dongluhitec.card.carpark.util.EventInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 停车场底层启动程序
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
public class DongluCarparkApp extends Application {

    private Logger LOGGER = LoggerFactory.getLogger(DongluCarparkApp.class);
    public static String softPrivilegeGroupName = "长春海吉星";
    public static HardwareService hardwareService;

    private final String TITLE = "停车场底层";
    private TrayIcon trayIcon;

    private BufferedImage run;
    private BufferedImage error;
    private BufferedImage warn;
    private Image set;

    public DongluCarparkApp() {
    }


    public static void main(String[] args) {
        if(args.length > 0 && args[0] != null){
            softPrivilegeGroupName = args[0];
        }
        DongluCarparkApp.launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            run = ImageIO.read(ClassLoader.getSystemResourceAsStream("image/run_16.png"));
            error = ImageIO.read(ClassLoader.getSystemResourceAsStream("image/error_16.png"));
            warn = ImageIO.read(ClassLoader.getSystemResourceAsStream("image/warn_16.png"));
            set = new Image(ClassLoader.getSystemResourceAsStream("image/set_64.png"));
        } catch (IOException e) {
            LOGGER.error("加载图片资源错误", e);
        }
        enableTray(primaryStage);

        VBox load = FXMLLoader.load(ClassLoader.getSystemResource("carparkApp.fxml"));
        Scene scene = new Scene(load);
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(set);
        primaryStage.setTitle(TITLE + "(" + softPrivilegeGroupName + ")");
//        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            primaryStage.hide();
        });

        startHardwareService(primaryStage);
    }

    private void startHardwareService(Stage stage) {
        EventBusUtil.register(this);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    hardwareService = HardwareService.getInstance();
                    hardwareService.start();
                    LOGGER.info("启动硬件服务成功");
                } catch (Exception e) {
                    LOGGER.info("启动硬件服务时发生错误",e);
                    Platform.runLater(()->{
                        ButtonType save = new ButtonType("保存错误信息到文件");
                        ButtonType cancel = new ButtonType("取消");

                        String headerText = "启动系统服务失败！！！\r\n你可将该错误保存到文件，以便开发商查找解决方案";
                        Optional<ButtonType> buttonType = Alerts.create(Alert.AlertType.ERROR).setButtons(save, cancel).setTitle("错误").setHeaderText(headerText).setContentText(e.getMessage()).showAndWait();
                        if (buttonType.isPresent() && buttonType.get() == save){
                            DirectoryChooser directoryChooser = new DirectoryChooser();
                            directoryChooser.setTitle("请选择在保存的路径");
                            File file = directoryChooser.showDialog(stage);
                            try {
                                Files.write(Paths.get(file.getCanonicalPath(),"log.txt"),e.getMessage().getBytes());
                                String headerText1 = "保存成功！\r\n你可将该错误日志文件发给开发商，快速查找解决方案";
                                Alerts.create(Alert.AlertType.INFORMATION).setTitle("提示").setHeaderText(headerText1).showAndWait();
                            } catch (IOException e1) {
                                LOGGER.error("保存系统错误日志时发生错误",e1);
                            }
                        }
                    });
                }
            }
        },3000);
    }

    private void enableTray(final Stage stage) {
        Platform.setImplicitExit(false);
        PopupMenu popupMenu = new PopupMenu();
        java.awt.MenuItem openItem = new java.awt.MenuItem("打开");
        java.awt.MenuItem quitItem = new java.awt.MenuItem("退出");

        ActionListener acl = e -> {
            MenuItem item = (MenuItem) e.getSource();
            if (item.getLabel().equals("退出")) {
                SystemTray.getSystemTray().remove(trayIcon);
                Platform.exit();
                System.exit(1);
            }
            if (item.getLabel().equals("打开")) {
                Platform.runLater(stage::show);
            }

        };

        openItem.addActionListener(acl);
        quitItem.addActionListener(acl);

        popupMenu.add(openItem);
        popupMenu.add(quitItem);

        try {
            SystemTray tray = SystemTray.getSystemTray();

            trayIcon = new TrayIcon(run, TITLE, popupMenu);
            trayIcon.setToolTip(TITLE);
            tray.add(trayIcon);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Platform.runLater(stage::show);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Subscribe
    public void listenTrayInfo(final EventInfo event) {
        SwingUtilities.invokeLater(()->{
            switch (event.getEventType()) {
                case 硬件通讯异常:
                    if(trayIcon.getImage() == error){
                        return;
                    }
                    trayIcon.setImage(error);
                    createTip((String)event.getObj());
                    break;
                case 外接服务通讯异常:
                    if(trayIcon.getImage() == warn){
                        return;
                    }
                    trayIcon.setImage(warn);
                    createTip((String)event.getObj());
                    break;
                case 外接服务通讯正常:
                    if(trayIcon.getImage() == run){
                        return;
                    }
                    trayIcon.setImage(run);
                    createTip((String)event.getObj());
                    break;
                case 硬件通讯正常:
                    if(trayIcon.getImage() == run){
                        return;
                    }
                    trayIcon.setImage(run);
                    createTip((String)event.getObj());
                    break;
                default:
                    break;
            }
        });
    }

    public void createTip(final String content) {
        SwingUtilities.invokeLater(() -> trayIcon.setToolTip(content));
    }

}
