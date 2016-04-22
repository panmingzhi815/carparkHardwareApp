package org.dongluhitec.card.carpark.hardware;

import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.mina.core.session.IoSession;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dongluhitec.card.carpark.domain.ConnectionDirection;
import org.dongluhitec.card.carpark.domain.ConnectionUsage;
import org.dongluhitec.card.carpark.exception.DongluServiceException;
import org.dongluhitec.card.carpark.ui.Config;
import org.dongluhitec.card.carpark.util.EventBusUtil;
import org.dongluhitec.card.carpark.util.EventInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HardwareUtil {

    private static AtomicLong plateSize = new AtomicLong(0);
    private static final String MSG_PRE = "message_prefix";
    private static String session_id;

    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
    public static SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyyMMddHHmmss");
    private static Logger LOGGER = LoggerFactory.getLogger(HardwareUtil.class);
    public static Logger sendPlateLog = LoggerFactory.getLogger("sendPlateLog");

    public static String checkSubpackage(IoSession session, Object message) {
        String msg = ((String) message).trim();
        if (session.getAttribute(MSG_PRE) == null) {
            session.setAttribute(MSG_PRE, "");
        }

        String oldValue = (String) session.getAttribute(MSG_PRE);
        session.setAttribute(MSG_PRE, oldValue + msg);

        if (msg.endsWith("</dongluCarpark>")) {
            String result = (String) session.getAttribute(MSG_PRE);
            session.removeAttribute(MSG_PRE);
            return result;
        }
        return null;
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

    public static String responseDeviceControl(IoSession session) {
        if(session == null || !session.isConnected()){
            LOGGER.error("会话不存在或己关闭，返回响应");
        }
        try {
            String value = "<dongluCarpark><result>true</result></dongluCarpark>";
            WebMessage wm = new WebMessage(WebMessageType.成功, value);
            writeMsg(session, wm.toString(), "返回接收成功响应");
            return wm.toString();
        } catch (Exception e) {
            throw new DongluServiceException("响应设备控制失败", e);
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

            cs.getLinkDeviceList().forEach(each->{
                Element monitor = rootElement.addElement("monitor");
                Element deviceElement = monitor.addElement("device");
                deviceElement.addElement("deviceName").setText(each.getDeviceName());
                deviceElement.addElement("deviceInOutType").setText(each.getDeviceType().equals("进口") ? "in" : "out");
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
            InetAddress address = InetAddress.getLocalHost();
            return address.getHostAddress();
        } catch (Exception e) {
            return null;
        }

    }

    public static void writeMsg(IoSession ioSession, String msg,String mark) {
        try {
            ioSession.write(msg);
            saveConnectionUsage(ConnectionDirection.发送,mark,msg);
        } catch (Exception e) {
            EventBusUtil.post(new EventInfo(EventInfo.EventType.外接服务通讯异常, "发消息给外接服务时发生异常，请检查对接软件是否正常"));
        }
    }

    public static void controlSpeed(long start, long speed) {
        long used = Math.abs(start - System.currentTimeMillis());
        if (used < speed) {
            Uninterruptibles.sleepUninterruptibly(speed - used, TimeUnit.MILLISECONDS);
        }
    }

    public static void responseResult(Document dom) {
        try {
            Element rootElement = dom.getRootElement();
            Element element = rootElement.element("session_id");
            if (element.getText() != null) {
                session_id = element.getText();
            }
        } catch (Exception ignored) {

        }
    }

    public static void setPlateInfo(IoSession session, String deviceName, String ip, String plateNO, byte[] bigImage, byte[] smallImage) {
        try {
            String format = simpleDateFormat.format(new Date());
            String folder ="车牌图片" + File.separator + ip + File.separator + format;
            Path path = Paths.get(folder);
            if(Files.notExists(path,LinkOption.NOFOLLOW_LINKS)){
                Files.createDirectories(path);
            }

            String formatDateTime = simpleDateFormat2.format(new Date());
            Path bigImagePath = Paths.get(folder, formatDateTime +"_"+plateNO+"_big.jpg");
            Files.write(bigImagePath, bigImage,StandardOpenOption.CREATE);

            Path smallImagePath = Paths.get(folder, formatDateTime +"_"+plateNO+"_small.jpg");
            Files.write(smallImagePath, smallImage,StandardOpenOption.CREATE);

            Document document = DocumentHelper.createDocument();
            Element root = document.addElement("dongluCarpark");

            Element deviceElement = root.addElement("device");
            deviceElement.addElement("deviceName").setText(deviceName);

            root.addElement("plateCode").setText(plateNO);
            root.addElement("plateBigImage").setText(bigImagePath.toFile().getAbsolutePath());
            root.addElement("plateSmallImage").setText(smallImagePath.toFile().getAbsolutePath());

            WebMessage wm = new WebMessage(WebMessageType.发送车牌, document.getRootElement().asXML());
            HardwareUtil.writeMsg(session, wm.toString(), "发送车牌记录");
            sendPlateLog.info("{} :发送车牌到对接服务器，车牌号：{} 图片地址:{}",plateSize.getAndAdd(1),plateNO,bigImagePath.toFile().getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveConnectionUsage(ConnectionDirection connectionDirection,String shortContent,String longContent){
        ConnectionUsage connectionUsage = new ConnectionUsage();
        connectionUsage.setDirection(connectionDirection);
        connectionUsage.setShortContent(shortContent);
        connectionUsage.setLongContent(longContent.getBytes());
        org.dongluhitec.card.carpark.hardware.HardwareService.databaseDao.saveConnectionUsage(connectionUsage);
    }
}
