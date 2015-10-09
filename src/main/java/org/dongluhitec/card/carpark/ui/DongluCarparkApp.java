package org.dongluhitec.card.carpark.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * 停车场底层启动程序
 * Created by panmingzhi815 on 2015/10/8 0008.
 */
public class DongluCarparkApp extends Application {

    private Logger LOGGER = LoggerFactory.getLogger(DongluCarparkApp.class);

    private final String TITLE = "停车场底层";
    private TrayIcon trayIcon;

    private BufferedImage run;
    private BufferedImage error;
    private BufferedImage warn;
    private Image set;


    public static void main(String[] args) {
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
        primaryStage.setTitle(TITLE);

        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            primaryStage.hide();
        });
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
                Platform.runLater(()->{
                    stage.show();
                });
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

}
