package org.dongluhitec.card.carpark.hardware;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
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
import org.dongluhitec.card.carpark.ui.Config;
import org.dongluhitec.card.carpark.ui.LinkDevice;
import org.dongluhitec.card.carpark.ui.controller.DongluCarparkAppController;
import org.dongluhitec.card.carpark.util.EventBusUtil;
import org.dongluhitec.card.carpark.util.EventInfo;
import org.dongluhitec.card.carpark.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HardwareService {
    private Logger LOGGER = LoggerFactory.getLogger(HardwareService.class);
    private final int PORT = 9124;

    public static HardwareService singleInstance;

    public static MessageHardware messageHardware;
    public static HibernateDao databaseDao;
    private static ListenHandler listenHandler;
    private static boolean isPlayVoice = false;

    public static boolean isAlreadySendAd = false;
    public static boolean isAlreadyReadProductId = false;

    private ConnectFuture cf = null;
    private NioSocketConnector connector;

    private XinlutongJNAImpl xinlutongJNAImpl;
    private IoSession session;

    private HardwareService(){
        databaseDao = new HibernateDao();
        messageHardware = new MessageHardwareImpl();
        listenHandler = new ListenHandler(messageHardware);
    };

    public static HardwareService getInstance(){
        if(singleInstance == null){
            singleInstance = new HardwareService();
        }
        return singleInstance;
    }

    public void start(){
        startWebConnector();
        checkWebConnector();
        checkDateTime();
        loggingDeviceRecord();
        listenLocalPort();
        callbackPlateDeviceRecord();
    }

    private void callbackPlateDeviceRecord() {
        if(DongluCarparkAppController.config == null){
            return;
        }

        xinlutongJNAImpl.closeAllEx();

        List<LinkDevice> linkDeviceList = DongluCarparkAppController.config.getLinkDeviceList();
        for (LinkDevice linkDevice : linkDeviceList) {
            if(xinlutongJNAImpl == null){
                xinlutongJNAImpl = new XinlutongJNAImpl();
            }
            XinlutongCallback.XinlutongResult xlr = (ip, channel, plateNO, bigImage, smallImage) -> HardwareUtil.setPlateInfo(session,linkDevice.getDeviceName(),ip,plateNO,bigImage,smallImage);
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
        Timer timer = new Timer("check web connector");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try{
                    Config config = DongluCarparkAppController.config;
                    if(config == null){
                        return;
                    }
                    List<LinkDevice> LinkDevice = config.getLinkDeviceList();
                    for (LinkDevice linkDevice : LinkDevice) {
                        Device device = toDevice(linkDevice);
                        LOGGER.debug("开始轮询设备:{}",device.getName());
                        long start = System.currentTimeMillis();
                        try{
                            if(isPlayVoice){
                                HardwareUtil.controlSpeed(start, 300);
                                isPlayVoice = false;
                            }

                            if(!Strings.isNullOrEmpty(config.getAd()) && !isAlreadySendAd){
                                ListenableFuture<Boolean> booleanListenableFuture = messageHardware.setAD(device, config.getAd());
                                booleanListenableFuture.get(5,TimeUnit.SECONDS);
                                isAlreadySendAd = true;
                            }

                            if(!Strings.isNullOrEmpty(config.getAd()) && !isAlreadyReadProductId){
                                ListenableFuture<String> stringListenableFuture = messageHardware.readVersion(device);
                                String deviceVersion = stringListenableFuture.get(5, TimeUnit.SECONDS);
                                linkDevice.setDeviceVersion(deviceVersion);
                                FileUtil.writeObjectToFile(config,DongluCarparkAppController.CONFIG_FILEPATH);
                                isAlreadyReadProductId = true;
                            }

                            ListenableFuture<CarparkNowRecord> carparkReadNowRecord = messageHardware.carparkReadNowRecord(device);
                            CarparkNowRecord carparkNowRecord = carparkReadNowRecord.get(5000,TimeUnit.MILLISECONDS);
                            if(carparkNowRecord != null){
                                CardUsage cardUsage = new CardUsage();
                                cardUsage.setDeviceName(linkDevice.getDeviceName());
                                cardUsage.setDatabaseTime(new Date());
                                cardUsage.setIdentifier(carparkNowRecord.getCardID());
                                databaseDao.save(cardUsage);

                                HardwareUtil.sendCardNO(session, carparkNowRecord.getCardID(),carparkNowRecord.getReaderID()+"", device.getName());
                                HardwareUtil.controlSpeed(start, 1000);
                            }
                            EventBusUtil.post(new EventInfo(EventInfo.EventType.硬件通讯正常, "硬件通讯恢复正常"));
                        }catch(Exception e){
                            EventBusUtil.post(new EventInfo(EventInfo.EventType.硬件通讯异常, "当前主机与停车场硬件设备通讯时发生异常,请检查"));
                        }finally{
                            HardwareUtil.controlSpeed(start, 400);
                        }
                    }
                }catch(Exception e){}
            }
        },5000,100);
    }

    private void checkDateTime(){
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
                        if (isPlayVoice) {
                            HardwareUtil.controlSpeed(start, 10000);
                        }
                        messageHardware.setDateTime(device, date);
                        EventBusUtil.post(new EventInfo(EventInfo.EventType.硬件通讯正常, "硬件通讯恢复正常"));
                    } catch (Exception e) {
                        EventBusUtil.post(new EventInfo(EventInfo.EventType.硬件通讯异常, "当前主机与停车场硬件设备通讯时发生异常,请检查"));
                    } finally {
                        HardwareUtil.controlSpeed(start, 400);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("定时校验设备时间时发生错误", e);
            }
        }, 3000, (config == null ? 30 : config.getValidateTimeLength()), TimeUnit.MINUTES);
    }

    private void startWebConnector(){
        try {
            connector = new NioSocketConnector();

            connector.getFilterChain().addLast("logger", new LoggingFilter());
            //指定编码过滤器
            TextLineCodecFactory lineCodec=new TextLineCodecFactory(Charset.forName("UTF-8"));
            lineCodec.setDecoderMaxLineLength(1024*1024); //1M
            lineCodec.setEncoderMaxLineLength(1024*1024); //1M
            connector.getFilterChain().addLast("codec",new ProtocolCodecFilter(lineCodec));  //行文本解析
            connector.setHandler(listenHandler);
            // Set connect timeout.
            connector.setConnectTimeoutCheckInterval(30);
            // 连结到服务器:
            Config config = DongluCarparkAppController.config;
            cf = connector.connect(new InetSocketAddress(config.getReceiveIp(), config.getReceivePort()));
            boolean b = cf.awaitUninterruptibly(5, TimeUnit.SECONDS);
            if(!b || cf.getException() != null){
                EventBusUtil.post(new EventInfo(EventInfo.EventType.外接服务通讯异常, "当前主机与对接服务通讯失败,3秒后会自动重联"));
            }
            this.session = cf.getSession();
        } catch (Exception e) {
//			throw new DongluHWException("对接连接检查发生异常",e);
        }
    }

    private boolean isSendDevice = false;
    private void checkWebConnector(){
        Timer timer = new Timer("check web connector");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try{
                    Config cs = DongluCarparkAppController.config;
                    if(cs == null){
                        return;
                    }
                    String ip = cs.getReceiveIp();
                    String port = String.valueOf(cs.getReceivePort());
                    LOGGER.debug("正在检查外接服务,ip:{} port:{}",ip,port);
                    if(session == null || !session.isConnected()){
                        LOGGER.debug("检查到会话不存在或己关闭，准备重新建立会话");

                        EventBusUtil.post(new EventInfo(EventInfo.EventType.外接服务通讯异常, "当前主机与对接服务通讯失败,3秒后会自动重联"));

                        ConnectFuture connect = connector.connect(new InetSocketAddress(cs.getReceiveIp(),cs.getReceivePort()));
                        boolean awaitUninterruptibly = connect.awaitUninterruptibly(10,TimeUnit.SECONDS);
                        if(awaitUninterruptibly && connect.getException() == null){
                            cf = connect;
                            session = cf.getSession();
                            isSendDevice = false;
                        }
                    }else{
                        EventBusUtil.post(new EventInfo(EventInfo.EventType.外接服务通讯正常, "外接服务通讯恢复正常"));
                        if(!isSendDevice){
                            HardwareUtil.sendDeviceInfo(session, cs);
                            isSendDevice = true;
                        }
                    }
                }catch(Exception e){
                    LOGGER.error("检查外接服务发生错误",e);
                }
            }
        },5000,2000);
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


}