package org.dongluhitec.card.carpark.hardware;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.dongluhitec.card.carpark.dao.HibernateDao;
import org.dongluhitec.card.carpark.domain.CardUsage;
import org.dongluhitec.card.carpark.hardware.impl.MessageHardwareImpl;
import org.dongluhitec.card.carpark.model.CarparkNowRecord;
import org.dongluhitec.card.carpark.model.Device;
import org.dongluhitec.card.carpark.plate.XinlutongCallback;
import org.dongluhitec.card.carpark.plate.XinlutongJNAImpl;
import org.dongluhitec.card.carpark.tcp.TcpHardwareService;
import org.dongluhitec.card.carpark.tcp.TcpRecordCallable;
import org.dongluhitec.card.carpark.ui.Config;
import org.dongluhitec.card.carpark.ui.LinkDevice;
import org.dongluhitec.card.carpark.ui.controller.DongluCarparkAppController;
import org.dongluhitec.card.carpark.util.EventBusUtil;
import org.dongluhitec.card.carpark.util.EventInfo;
import org.dongluhitec.card.carpark.util.FileUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HardwareService implements TcpRecordCallable {
    public final static Logger LOGGER = LoggerFactory.getLogger(HardwareService.class);
    public final int PORT = 9124;

    private ScheduledExecutorService delayCheckWebServiceRight;
    private ScheduledExecutorService delayRemoveOldPicture;
    private ScheduledExecutorService delayLoggerDeviceRecord;

    public static HardwareService singleInstance;
    public static MessageHardware messageHardware;
    public static HibernateDao databaseDao;
    private static ListenHandler listenHandler;

    public static   boolean isAlreadySendAd = false;
    public static   boolean isAlreadyReadProductId = false;
    private boolean needReplaySendDeviceInfo = false;

    private ConnectFuture cf = null;
    private NioSocketConnector connector;

    private XinlutongJNAImpl xinlutongJNAImpl;
    private TcpHardwareService tcpHardwareService;

    private HardwareService(){
        databaseDao = new HibernateDao();
        messageHardware = new MessageHardwareImpl();
        listenHandler = new ListenHandler(messageHardware);
    }

    public static HardwareService getInstance(){
        if(singleInstance == null){
            singleInstance = new HardwareService();
        }
        return singleInstance;
    }

    public void start(){
        listenWebServiceMessage();
        listenLocalPort();
        listenTcpHardwareRecord();
        delayDownloadDeviceDateTime();
        loggingDeviceRecord();
        callbackPlateDeviceRecord();
        delayRemoveOldPicture();
    }

    private void listenTcpHardwareRecord() {
        if (this.tcpHardwareService != null) {
            return;
        }
        this.tcpHardwareService = new TcpHardwareService(this);
        this.tcpHardwareService.startAsync();
    }

    private void callbackPlateDeviceRecord() {
        if(DongluCarparkAppController.config == null){
            return;
        }

        if(xinlutongJNAImpl == null){
            xinlutongJNAImpl = new XinlutongJNAImpl();
        }

        xinlutongJNAImpl.closeAllEx();

        List<LinkDevice> linkDeviceList = DongluCarparkAppController.config.getLinkDeviceList();
        for (LinkDevice linkDevice : linkDeviceList) {
            XinlutongCallback.XinlutongResult xlr = (ip, channel, plateNO, bigImage, smallImage) -> HardwareUtil.setPlateInfo(cf, linkDevice.getDeviceName(), ip, plateNO, bigImage, smallImage);
            xinlutongJNAImpl.openEx(linkDevice.getPlateIp(), xlr);
        }
    }

    private void listenLocalPort(){
        try {
            NioSocketAcceptor acceptor = new NioSocketAcceptor();
            acceptor.getFilterChain().addLast("logger", new LoggingFilter());
            TextLineCodecFactory lineCodec=new TextLineCodecFactory(Charset.forName("UTF-8"));
            lineCodec.setDecoderMaxLineLength(1024*1024);
            lineCodec.setEncoderMaxLineLength(1024*1024);
            acceptor.getFilterChain().addLast("codec",new ProtocolCodecFilter(lineCodec));
            acceptor.setHandler(listenHandler);
            acceptor.bind(new InetSocketAddress(PORT));
            LOGGER.info("开始监听本地端口 {}",PORT);
        }catch (Exception e){
            LOGGER.error("监听本地端口 {} 失败",PORT);
        }
    }

    private void loggingDeviceRecord(){
        if(delayLoggerDeviceRecord != null){
            return;
        }
        long count = DongluCarparkAppController.getLinkDeviceList().stream().filter(filter -> filter.getLinkType().equalsIgnoreCase("com")).count();
        if (count <= 0) {
            return;
        }
        delayLoggerDeviceRecord = Executors.newSingleThreadScheduledExecutor();
        delayLoggerDeviceRecord.scheduleWithFixedDelay(() -> {
            try {
                Config config = DongluCarparkAppController.config;
                if (config == null) {
                    return;
                }
                List<LinkDevice> LinkDevice = config.getLinkDeviceList();
                for (LinkDevice linkDevice : LinkDevice) {
                    Device device = toDevice(linkDevice);
                    LOGGER.debug("开始轮询设备:{}", device.getName());
                    long start = System.currentTimeMillis();
                    try {
                        if (!Strings.isNullOrEmpty(config.getAd()) && !isAlreadySendAd) {
                            LOGGER.info("开始下发广告:{}", config.getAd());
                            ListenableFuture<Boolean> booleanListenableFuture = messageHardware.setAD(device, config.getAd());
                            booleanListenableFuture.get(5, TimeUnit.SECONDS);
                            isAlreadySendAd = true;
                        }

                        if (!isAlreadyReadProductId) {
                            LOGGER.info("开始读取 {} 产品ID",linkDevice.getDeviceName());
                            ListenableFuture<String> stringListenableFuture = messageHardware.readVersion(device);
                            String deviceVersion = stringListenableFuture.get(5, TimeUnit.SECONDS);
                            linkDevice.setDeviceVersion(deviceVersion);
                            FileUtil.writeObjectToFile(config, DongluCarparkAppController.CONFIG_FILEPATH);
                            isAlreadyReadProductId = true;
                        }

                        ListenableFuture<CarparkNowRecord> carparkReadNowRecord = messageHardware.carparkReadNowRecord(device);
                        CarparkNowRecord carparkNowRecord = carparkReadNowRecord.get(5000, TimeUnit.MILLISECONDS);
                        if (carparkNowRecord != null) {
                            CardUsage cardUsage = new CardUsage();
                            cardUsage.setDeviceName(linkDevice.getDeviceName());
                            cardUsage.setDatabaseTime(new Date());
                            cardUsage.setIdentifier(carparkNowRecord.getCardID());
                            databaseDao.saveCardUsage(cardUsage);

                            HardwareUtil.sendCardNO(cf.getSession(), carparkNowRecord.getCardID(), carparkNowRecord.getReaderID() + "", device.getName());
                            HardwareUtil.controlSpeed(start, 1000);

                            device.setArea("255.1");
                            ListenableFuture<CarparkNowRecord> carparkNowRecordListenableFuture = messageHardware.carparkReadNowRecord(device);
                            carparkNowRecordListenableFuture.get(5000, TimeUnit.MILLISECONDS);
                        }else{}

                        EventBusUtil.post(new EventInfo(EventInfo.EventType.硬件通讯正常, "硬件通讯恢复正常"));
                    } catch (Exception e) {
                        LOGGER.error("轮询设备时发生异常", e);
                        EventBusUtil.post(new EventInfo(EventInfo.EventType.硬件通讯异常, "当前主机与停车场硬件设备通讯时发生异常,请检查"));
                    } finally {
                        HardwareUtil.controlSpeed(start, 400);
                    }
                }
            } catch (Exception ignored) {
                LOGGER.error("读取记录时发生错误", ignored);
            }
        }, 3000, 300, TimeUnit.MILLISECONDS);
    }

    private void delayDownloadDeviceDateTime(){
        Config config = DongluCarparkAppController.config;
        ScheduledExecutorService checkDeviceDateTimeService = Executors.newSingleThreadScheduledExecutor();
        checkDeviceDateTimeService.scheduleWithFixedDelay(() -> {
            if (DongluCarparkAppController.config == null) {
                return;
            }
            try {
                List<LinkDevice> linkDeviceList = new ArrayList<>();
                linkDeviceList.addAll(DongluCarparkAppController.config.getLinkDeviceList());
                for (LinkDevice linkDevice : linkDeviceList) {
                    Device device = toDevice(linkDevice);
                    Date date = new Date();
                    LOGGER.debug("开始设置设备{}时间:{}", device.getName(), date);

                    long start = System.currentTimeMillis();
                    try {
                        messageHardware.setDateTime(device, date);
                    } catch (Exception e) {
                        EventBusUtil.post(new EventInfo(EventInfo.EventType.硬件通讯异常, "当前主机与停车场硬件设备通讯时发生异常,请检查"));
                    } finally {
                        HardwareUtil.controlSpeed(start, 400);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("定时校验设备时间时发生错误", e);
            }
        }, 1, (config == null ? 30 : config.getValidateTimeLength()), TimeUnit.MINUTES);
    }

    private void listenWebServiceMessage(){
        try {
            connector = new NioSocketConnector();

            connector.getFilterChain().addLast("logger", new LoggingFilter());
            //指定编码过滤器
            TextLineCodecFactory lineCodec=new TextLineCodecFactory(Charset.forName("UTF-8"));
            lineCodec.setDecoderMaxLineLength(1024 * 1024); //1M
            lineCodec.setEncoderMaxLineLength(1024 * 1024); //1M
            connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(lineCodec));  //行文本解析
            connector.setHandler(listenHandler);
            // Set connect timeout.
            connector.setConnectTimeoutCheckInterval(30);
            // 连结到服务器:
            Config config = DongluCarparkAppController.config;
            cf = connector.connect(new InetSocketAddress(config.getReceiveIp(), config.getReceivePort()));
        } catch (Exception e) {
            LOGGER.error("打开初始化外接服务监听失败");
        }

        delayCheckWebServiceRight();
    }


    private void delayCheckWebServiceRight(){
        if(delayCheckWebServiceRight != null){
            return;
        }
        delayCheckWebServiceRight = Executors.newSingleThreadScheduledExecutor();
        delayCheckWebServiceRight.scheduleWithFixedDelay(() -> {
            try {
                Config cs = DongluCarparkAppController.config;
                if (cs == null) {
                    return;
                }
                String ip = cs.getReceiveIp();
                String port = String.valueOf(cs.getReceivePort());
                LOGGER.debug("正在检查外接服务,ip:{} port:{}", ip, port);
                if (cf != null && cf.isConnected() && cf.getSession().isConnected()) {
                    if (!needReplaySendDeviceInfo) {
                        LOGGER.debug("外接服务状态正常");
                        EventBusUtil.post(new EventInfo(EventInfo.EventType.外接服务通讯正常, "外接服务通讯恢复正常"));
                        HardwareUtil.sendDeviceInfo(cf.getSession(), cs);
                        needReplaySendDeviceInfo = true;
                    }
                } else {
                    LOGGER.debug("检查到会话不存在或己关闭，准备重新建立会话");
                    EventBusUtil.post(new EventInfo(EventInfo.EventType.外接服务通讯异常, "当前主机与对接服务通讯失败,3秒后会自动重联"));
                    replayConnectWebService();
                }
            } catch (Exception e) {
                LOGGER.error("检查外接服务发生错误", e);
            }
        }, 5000, 3000, TimeUnit.MILLISECONDS);
    }

    private void replayConnectWebService(){
        Config config = DongluCarparkAppController.config;
        ConnectFuture connect = connector.connect(new InetSocketAddress(config.getReceiveIp(), DongluCarparkAppController.config.getReceivePort()));
        boolean awaitUninterruptibly = connect.awaitUninterruptibly(10, TimeUnit.SECONDS);
        if (awaitUninterruptibly) {
            if (cf != null) {
                cf.cancel();
            }
            cf = connect;
            needReplaySendDeviceInfo = false;
        }
    }

    public Device toDevice(LinkDevice linkDevice){
        Device device = new Device();
        device.setAddress(linkDevice.getLinkAddress());
        device.setType(linkDevice.getLinkType());
        device.setArea(linkDevice.getDeviceAddress());
        device.setInoutType(linkDevice.getDeviceType());
        device.setName(linkDevice.getDeviceName());
        device.setSupportChinese("支持");
        device.setSupportInsideVoice("支持");
        device.setSupportOutsideVoice("支持");
        return device;
    }


    public void delayRemoveOldPicture(){
        if(delayRemoveOldPicture != null){
            return;
        }
        delayRemoveOldPicture = Executors.newSingleThreadScheduledExecutor();
        delayRemoveOldPicture.scheduleWithFixedDelay(() -> {
            Path path = Paths.get("车牌图片");
            if (!Files.exists(path)) {
                return;
            }
            if (!Files.isDirectory(path)) {
                return;
            }
            try {
                Files.list(path).forEach(ip->{
                    try{
                        String deleteImageDay = System.getProperty("deleteImageDay", "3");
                        Date deleteDay = new DateTime().minusDays(Integer.parseInt(deleteImageDay)).toDate();
                        final String format = HardwareUtil.simpleDateFormat.format(deleteDay);
                        Files.list(ip).forEach(date->{
                            if (date.getFileName().toString().compareTo(format) < 0) {
                                try {
                                    Files.list(date).parallel().forEach(f -> {
                                        try {
                                            Files.delete(f);
                                        } catch (Exception e) {
                                            LOGGER.error("删除文件:" + f.getFileName() + " 失败", e);
                                        }
                                    });
                                } catch (Exception e) {
                                    LOGGER.error("读取目录:" + date.getFileName() + " 失败", e);
                                }
                            }
                        });
                    }catch (Exception e){
                        LOGGER.error("读取目录:" + ip.getFileName() + " 失败", e);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("读取目录:" + path.getFileName() + " 失败", e);
            }
        }, 5, 5400, TimeUnit.SECONDS);
    }

    @Override
    public void call(String deviceName, String cardIdentifier) {
        LOGGER.info("开始处理TCP设备:{} 上传的卡片:{}记录",deviceName,cardIdentifier);
        List<LinkDevice> linkDeviceList = DongluCarparkAppController.config.getLinkDeviceList();
        for (LinkDevice linkDevice : linkDeviceList) {
            if (deviceName.contains(linkDevice.getLinkAddress().split(":")[0])) {
                databaseDao.saveCardUsage(linkDevice.getDeviceName(),cardIdentifier,new Date());
                HardwareUtil.sendCardNO(cf.getSession(), cardIdentifier, "1", linkDevice.getDeviceName());
            }
        }
    }

}