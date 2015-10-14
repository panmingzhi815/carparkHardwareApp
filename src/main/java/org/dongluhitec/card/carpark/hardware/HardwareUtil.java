package org.dongluhitec.card.carpark.hardware;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.session.IoSession;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.Uninterruptibles;
import org.dongluhitec.card.carpark.domain.ConnectionDirection;
import org.dongluhitec.card.carpark.domain.ConnectionUsage;
import org.dongluhitec.card.carpark.exception.EncryptException;
import org.dongluhitec.card.carpark.model.CarparkSetting;
import org.dongluhitec.card.carpark.model.Device;
import org.dongluhitec.card.carpark.ui.Config;
import org.dongluhitec.card.carpark.ui.LinkDevice;
import org.dongluhitec.card.carpark.util.RSAUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardwareUtil {

    private static final String MSGKEY = "message_prefix";

    public static String he_publicKey;

    private static String session_id;

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyddMM");
    private static SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyyddMMHHmmss");
    private static Logger LOGGER = LoggerFactory.getLogger(HardwareUtil.class);

    public static String checkSubpackage(IoSession session, Object message) {
        String msg = ((String) message).trim();
        if (session.getAttribute(MSGKEY) == null) {
            session.setAttribute(MSGKEY, "");
        }

        String oldValue = (String) session.getAttribute(MSGKEY);
        session.setAttribute(MSGKEY, oldValue + msg);

        if (msg.endsWith("</dongluCarpark>")) {
            String result = (String) session.getAttribute(MSGKEY);
            session.removeAttribute(MSGKEY);
            return result;
        }
        return null;
    }

    public static void responsePublicKey(IoSession session, String message) {
        try {
            Document dom = DocumentHelper.parseText(message);
            Element element = dom.getRootElement().element("publicKey");
            he_publicKey = element.getStringValue();
            element.setText(RSAUtils.getPublicKeyString());
            session.write(dom.getRootElement().asXML());
        } catch (Exception e) {
            throw new EncryptException("客户端响应公钥失败", e);
        }
    }

    public static void responsePublicKey_server(IoSession session, String message) {
        try {
            Document dom = DocumentHelper.parseText(message);
            Element element = dom.getRootElement().element("publicKey");
            he_publicKey = element.getStringValue();
        } catch (Exception e) {
            throw new EncryptException("服务端响应公钥失败", e);
        }
    }

    public static void sendCardNO(IoSession session, String cardNO, String readerID, String deviceName) {
        if(session == null || !session.isConnected()){
            LOGGER.error("会话不存在或己关闭，暂不能发送刷卡信息");
        }
        try {
            Document document = DocumentHelper.createDocument();
            Element root = document.addElement("dongluCarpark");
            root.addAttribute("session_id", session_id);

            Element deviceElement = root.addElement("device");
            deviceElement.addElement("deviceName").setText(deviceName);

            root.addElement("cardSerialNumber").setText(cardNO);
            root.addElement("CardReaderID").setText(readerID);

            WebMessage wm = new WebMessage(WebMessageType.发送卡号, document.getRootElement().asXML());
            HardwareUtil.writeMsg(session, wm.toString(),"发送刷卡记录");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String responseDeviceControl(IoSession session, Document dom) {
        if(session == null || !session.isConnected()){
            LOGGER.error("会话不存在或己关闭，返回响应");
        }
        try {
            String value = "<dongluCarpark><result>true</result></dongluCarpark>";
            WebMessage wm = new WebMessage(WebMessageType.成功, value);
            writeMsg(session, wm.toString(),"返回接收成功响应");
            return wm.toString();
        } catch (Exception e) {
            throw new EncryptException("响应设备控制失败", e);
        }
    }

    public static String formatDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    public static void sendDeviceInfo(IoSession session, Config cs) {
        try {
            Document dom = DocumentHelper.createDocument();
            Element rootElement = dom.addElement("dongluCarpark");
            Element station = rootElement.addElement("station");
            station.addElement("account").setText("donglu");
            station.addElement("password").setText("123456");
            station.addElement("stationName").setText(cs.getGangtingName());
            station.addElement("stationIP").setText(HardwareUtil.getLocalIP());
            station.addElement("stationTime").setText(HardwareUtil.formatDateTime(new Date()));

            List<LinkDevice> linkDeviceList = cs.getLinkDeviceList();
            cs.getLinkDeviceList().forEach(each->{
                Element monitor = rootElement.addElement("monitor");
                Element deviceElement = monitor.addElement("device");
                deviceElement.addElement("deviceName").setText(each.getDeviceName());
                deviceElement.addElement("deviceInOutType").setText(each.getDeviceType().equals("进口") == true ? "in" : "out");
                deviceElement.addElement("deviceDisplayAndVoiceInside").setText("true");
                deviceElement.addElement("deviceDisplayAndVoiceOutside").setText("true");
                deviceElement.addElement("deviceDisplaySupportChinese").setText("true");
//				deviceElement.addElement("deviceDisplayAndVoiceInside").setText(device.getSupportInsideVoice().equals("支持") == true ? "true" : "false");
//				deviceElement.addElement("deviceDisplayAndVoiceOutside").setText(device.getSupportOutsideVoice().equals("支持") == true ? "true" : "false");
//				deviceElement.addElement("deviceDisplaySupportChinese").setText(device.getSupportChinese().equals("支持") == true ? "true" : "false");
            });
            WebMessage wm = new WebMessage(WebMessageType.设备信息, dom.getRootElement().asXML());
            HardwareUtil.writeMsg(session, wm.toString(),"发送设备信息");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getLocalIP() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostAddress().toString();
        } catch (Exception e) {
            return null;
        }

    }

    public static void writeMsg(IoSession ioSession, String msg,String mark) {
        try {
            ioSession.write(msg);

            ConnectionUsage connectionUsage = new ConnectionUsage();
            connectionUsage.setDirection(ConnectionDirection.发送);
            connectionUsage.setShortContent(mark);
            connectionUsage.setLongContent(msg.getBytes());
            org.dongluhitec.card.carpark.hardware.HardwareService.hibernateDao.save(connectionUsage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String decode(String msg) {
        try {
            int indexOf = msg.indexOf(">") + 1;
            String subStr = msg.substring(indexOf, msg.length() - 16);

            String decrypt = RSAUtils.decrypt(subStr, RSAUtils.getPrivateKey());
            String replace = msg.replace(subStr, decrypt);
            return replace;
        } catch (Exception e) {
            throw new EncryptException("解密失败", e);
        }
    }

    public static String encode(String msg) {
        try {
            int indexOf = msg.indexOf(">") + 1;
            String subStr = msg.substring(indexOf, msg.length() - 16);
            String encrypt = RSAUtils.encrypt(subStr, RSAUtils.getPublicKey(he_publicKey));
            String replace = msg.replace(subStr, encrypt);
            return replace;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void changeDecretKey(IoSession ioSession) {
        try {
            Document document = DocumentHelper.createDocument();
            Element dongluCarpark = document.addElement("dongluCarpark");
            dongluCarpark.addAttribute("type", "publicKey");
            Element publicKey = dongluCarpark.addElement("publicKey");
            publicKey.setText(RSAUtils.getPublicKeyString());
            ioSession.write(document.getRootElement().asXML());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void controlSpeed(long start, long speed) {
        long used = Math.abs(start - System.currentTimeMillis());
        if (used < speed) {
            Uninterruptibles.sleepUninterruptibly(speed - used, TimeUnit.MILLISECONDS);
        }
    }

    public static void responseResult(IoSession session, Document dom) {
        try {
            Element rootElement = dom.getRootElement();
            Element element = rootElement.element("session_id");
            if (element.getText() != null) {
                session_id = element.getText();
            }
        } catch (Exception e) {

        }
    }

    public static void setPlateInfo(IoSession session, String deviceName, String ip, String plateNO, byte[] bigImage, byte[] smallImage) {
        try {
            String format = simpleDateFormat.format(new Date());
            String floder = ip + File.separator + format;
            Path path = Paths.get(floder);
            if(Files.notExists(path,LinkOption.NOFOLLOW_LINKS)){
                Files.createDirectories(path);
            }

            Path bigImagePath = Paths.get(floder, simpleDateFormat2.format(new Date())+"_"+plateNO+"_big.jpg");
            Files.write(bigImagePath, bigImage,StandardOpenOption.CREATE);

            Path smallImagePath = Paths.get(floder, simpleDateFormat2.format(new Date())+"_"+plateNO+"_small.jpg");
            Files.write(smallImagePath, smallImage,StandardOpenOption.CREATE);

            Document document = DocumentHelper.createDocument();
            Element root = document.addElement("dongluCarpark");

            Element deviceElement = root.addElement("device");
            deviceElement.addElement("deviceName").setText(deviceName);

            root.addElement("plateCode").setText(plateNO);
            root.addElement("plateBigImage").setText(bigImagePath.toFile().getAbsolutePath());
            root.addElement("plateSmallImage").setText(smallImagePath.toFile().getAbsolutePath());

            WebMessage wm = new WebMessage(WebMessageType.发送车牌, document.getRootElement().asXML());
            HardwareUtil.writeMsg(session, wm.toString(),"发送车牌记录");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
