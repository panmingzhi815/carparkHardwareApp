package org.dongluhitec.card.carpark.hardware;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dongluhitec.card.carpark.connect.body.OpenDoorEnum;
import org.dongluhitec.card.carpark.connect.exception.DongluHWException;
import org.dongluhitec.card.carpark.dao.HibernateDao;
import org.dongluhitec.card.carpark.domain.CardUsage;
import org.dongluhitec.card.carpark.hardware.impl.MessageServiceImpl;
import org.dongluhitec.card.carpark.model.CarparkNowRecord;
import org.dongluhitec.card.carpark.model.Device;
import org.dongluhitec.card.carpark.plate.XinlutongCallback;
import org.dongluhitec.card.carpark.plate.XinlutongJNAImpl;
import org.dongluhitec.card.carpark.ui.Config;
import org.dongluhitec.card.carpark.ui.DongluCarparkApp;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HardwareService {
	private Logger LOGGER = LoggerFactory.getLogger(HardwareService.class);
	private final int PORT = 9124;

	public static HardwareService service = null;
	public static MessageService messageService = null;
	private static boolean isPlayVoice = false;
	public static HibernateDao hibernateDao;
	public static boolean hasSend = false;
	public static boolean hasReadId = false;

	private ConnectFuture cf = null;
	private NioSocketConnector connector;
	private ExecutorService newSingleThreadExecutor;

	private XinlutongJNAImpl xinlutongJNAImpl;
	private IoSession session;
	private Map<String, XinlutongCallback.XinlutongResult> plateDeviceMap = new HashMap<>();

	private HardwareService(){};
	
	public static HardwareService getInstance(){
		if(service == null){
			hibernateDao = new HibernateDao();
			service = new HardwareService();
			messageService = new MessageServiceImpl();
		}
		return service;
	}
	
	public void start(){
		if(newSingleThreadExecutor == null){
			newSingleThreadExecutor = Executors.newSingleThreadExecutor();
		}
		startWebConnector();
		checkWebConnector();
		checkDateTime();
		startLogging();
		startListne();
		startPlateMonitor();
	}
	
	private void startPlateMonitor() {
		Config cs = DongluCarparkAppController.config;
		if(cs == null){
			return;
		}
		List<LinkDevice> linkDeviceList = cs.getLinkDeviceList();
		for (LinkDevice linkDevice : linkDeviceList) {
			if(xinlutongJNAImpl == null){
				xinlutongJNAImpl = new XinlutongJNAImpl();
			}
			XinlutongCallback.XinlutongResult xlr = (ip, channel, plateNO, bigImage, smallImage) -> HardwareUtil.setPlateInfo(session,linkDevice.getDeviceName(),ip,plateNO,bigImage,smallImage);
			xinlutongJNAImpl.openEx(linkDevice.getPlateIp(), xlr);
			plateDeviceMap.put(linkDevice.getPlateIp(),xlr);
		}
	}

	private void startListne(){
		try {
			NioSocketAcceptor acceptor = new NioSocketAcceptor();

			acceptor.getFilterChain().addLast("logger", new LoggingFilter());
			//指定编码过滤器 
			TextLineCodecFactory lineCodec=new TextLineCodecFactory(Charset.forName("UTF-8"));
			lineCodec.setDecoderMaxLineLength(1024*1024); //1M  
			lineCodec.setEncoderMaxLineLength(1024*1024); //1M  
			acceptor.getFilterChain().addLast("codec",new ProtocolCodecFilter(lineCodec));  //行文本解析
			acceptor.setHandler(new listenHandler());
			
			acceptor.bind(new InetSocketAddress(PORT));
			LOGGER.info("监听服务开始，端口：{}",PORT);
		} catch (Exception e) {
//			throw new DongluHWException("开始监听服务器失败!",e);
		}
	}
	
	private void startLogging(){
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

							if(!Strings.isNullOrEmpty(config.getAd()) && !hasSend){
								ListenableFuture<Boolean> booleanListenableFuture = messageService.setAD(device, config.getAd());
								booleanListenableFuture.get(5,TimeUnit.SECONDS);
								hasSend = true;
							}

							if(!Strings.isNullOrEmpty(config.getAd()) && !hasReadId){
								ListenableFuture<String> stringListenableFuture = messageService.readVersion(device);
								String deviceVersion = stringListenableFuture.get(5, TimeUnit.SECONDS);
								linkDevice.setDeviceVersion(deviceVersion);
								FileUtil.writeObjectToFile(config,DongluCarparkAppController.CONFIG_FILEPATH);
								hasReadId = true;
							}

							ListenableFuture<CarparkNowRecord> carparkReadNowRecord = messageService.carparkReadNowRecord(device);
							CarparkNowRecord carparkNowRecord = carparkReadNowRecord.get(5000,TimeUnit.MILLISECONDS);
							if(carparkNowRecord != null){
								CardUsage cardUsage = new CardUsage();
								cardUsage.setDeviceName(linkDevice.getDeviceName());
								cardUsage.setDatabaseTime(new Date());
								cardUsage.setIdentifier(carparkNowRecord.getCardID());
								hibernateDao.save(cardUsage);

								HardwareUtil.sendCardNO(session, carparkNowRecord.getCardID(),carparkNowRecord.getReaderID()+"", device.getName());
								HardwareUtil.controlSpeed(start, 3000);
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
		Timer timer = new Timer("check date time");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try{
					if(config == null){
						return;
					}
					List<LinkDevice> linkDeviceList = new ArrayList<>();
					linkDeviceList.addAll(config.getLinkDeviceList());
					for (LinkDevice linkDevice : linkDeviceList) {
						Device device = toDevice(linkDevice);
						Date date = new Date();
						LOGGER.debug("开始设置设备{}时间:{}",device.getName(),date);
						
						long start = System.currentTimeMillis();
						try{
							if(isPlayVoice){
								HardwareUtil.controlSpeed(start, 10000);
							}
							messageService.setDateTime(device, date);
							EventBusUtil.post(new EventInfo(EventInfo.EventType.硬件通讯正常, "硬件通讯恢复正常"));
						}catch(Exception e){
							EventBusUtil.post(new EventInfo(EventInfo.EventType.硬件通讯异常, "当前主机与停车场硬件设备通讯时发生异常,请检查"));
						}finally{
							HardwareUtil.controlSpeed(start, 400);
						}
					}
				}catch(Exception ignored){}
			}
		},3000,(config == null ? 30 : config.getValidateTimeLength()) * 60000);
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
			connector.setHandler(new listenHandler());
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
	
	class listenHandler extends IoHandlerAdapter{

		@Override
		public void messageReceived(final IoSession session, Object message) throws Exception {
			String checkSubpackage = HardwareUtil.checkSubpackage(session, message);
			if(checkSubpackage == null){
				return;
			}
			
			WebMessage wm = new WebMessage(checkSubpackage);
			
			final Document dom = DocumentHelper.parseText(wm.getContent());
			final Element rootElement = dom.getRootElement();
			
			if(wm.getType() == WebMessageType.成功){
				HardwareUtil.responseResult(session,dom);
				return;
			}
			
			if(wm.getType() == WebMessageType.广告){
				newSingleThreadExecutor.submit(() -> {
                    try{
                        String deviceName = rootElement.element("device").element("deviceName").getTextTrim();
                        String ad = rootElement.element("ad").getTextTrim();

                        Config config = DongluCarparkAppController.config;
                        List<LinkDevice> collect = config.getLinkDeviceList().stream().filter(filter -> filter.getDeviceName().equals(deviceName)).collect(Collectors.toList());
                        if(!collect.isEmpty()){
                            LinkDevice linkDevice = collect.get(0);
                            Device device = toDevice(linkDevice);

                            ListenableFuture<Boolean> setAD = messageService.setAD(device, ad);
                            setAD.get();
                            HardwareUtil.responseDeviceControl(session,dom);
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                });
				
			}
			
			if(wm.getType() == WebMessageType.设备控制){
				newSingleThreadExecutor.submit(() -> {
					try {
						isPlayVoice = true;
						Element controlElement = rootElement.element("control");
						Element element = rootElement.element("device");

						String deviceName = element.element("deviceName").getTextTrim();
						String gate = controlElement.element("gate").getTextTrim();
						String Insidevoice, Outsidevoice, InsideScreen, OutsideScreen, InsideScreenAndVoiceData, OutsideScreenAndVoiceData;
						if (controlElement.element("insideVoice") == null) {
							Insidevoice = controlElement.element("InsideVoice").getTextTrim();
							Outsidevoice = controlElement.element("OutsideVoice").getTextTrim();
							InsideScreen = controlElement.element("InsideScreen").getTextTrim();
							OutsideScreen = controlElement.element("OutsideScreen").getTextTrim();
							InsideScreenAndVoiceData = controlElement.element("InsideScreenAndVoiceData").getTextTrim();
							OutsideScreenAndVoiceData = controlElement.element("OutsideScreenAndVoiceData").getTextTrim();
						} else {
							Insidevoice = controlElement.element("insideVoice").getTextTrim();
							Outsidevoice = controlElement.element("outsideVoice").getTextTrim();
							InsideScreen = controlElement.element("insideScreen").getTextTrim();
							OutsideScreen = controlElement.element("outsideScreen").getTextTrim();
							InsideScreenAndVoiceData = controlElement.element("insideScreenAndVoiceData").getTextTrim();
							OutsideScreenAndVoiceData = controlElement.element("outsideScreenAndVoiceData").getTextTrim();
						}
						Config config = DongluCarparkAppController.config;
						List<LinkDevice> collect = config.getLinkDeviceList().stream().filter(filter -> filter.getDeviceName().equals(deviceName)).collect(Collectors.toList());
						if (collect.isEmpty()) {
							LOGGER.warn("未配置设备:{} 信息，暂不能接收请求", deviceName);
							return;
						}
						Device device = toDevice(collect.get(0));
						if (device == null) {
							return;
						}
						if (InsideScreen.equals("true")) {
							int voice = Insidevoice.equals("false") ? 1 : 9;
							ListenableFuture<Boolean> carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 1, voice, 0, OpenDoorEnum.parse(gate), InsideScreenAndVoiceData);
							Boolean boolean1 = carparkScreenVoiceDoor.get();
							if (boolean1 == null) {
								carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 1, voice, 0, OpenDoorEnum.parse(gate), InsideScreenAndVoiceData);
								carparkScreenVoiceDoor.get();
							}
						}
						if (OutsideScreen.equals("true")) {
							int voice = Outsidevoice.equals("false") ? 1 : 9;
							ListenableFuture<Boolean> carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 2, voice, 0, OpenDoorEnum.parse(gate), OutsideScreenAndVoiceData);
							Boolean boolean1 = carparkScreenVoiceDoor.get();
							if (boolean1 == null) {
								carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 2, voice, 0, OpenDoorEnum.parse(gate), OutsideScreenAndVoiceData);
								carparkScreenVoiceDoor.get();
							}
						}
						HardwareUtil.responseDeviceControl(session, dom);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}
		}

		@Override
		public void messageSent(IoSession session, Object message) throws Exception {
			// TODO Auto-generated method stub
			super.messageSent(session, message);
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


}
