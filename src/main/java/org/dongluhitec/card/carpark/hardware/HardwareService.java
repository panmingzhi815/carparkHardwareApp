package org.dongluhitec.card.carpark.hardware;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.google.common.io.Closer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
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
import org.dongluhitec.card.carpark.hardware.impl.MessageServiceImpl;
import org.dongluhitec.card.carpark.model.CarparkNowRecord;
import org.dongluhitec.card.carpark.model.CarparkSetting;
import org.dongluhitec.card.carpark.model.Device;
import org.dongluhitec.card.carpark.plate.XinlutongCallback;
import org.dongluhitec.card.carpark.plate.XinlutongJNAImpl;
import org.dongluhitec.card.carpark.util.EventBusUtil;
import org.dongluhitec.card.carpark.util.EventInfo;
import org.dongluhitec.card.carpark.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class HardwareService {
	private Logger LOGGER = LoggerFactory.getLogger(HardwareService.class);
	private static final String dataFilePath = "donglu.data";

	public static HardwareService service = null;
	private static MessageService messageService = null;
	private ConnectFuture cf = null;
	private NioSocketConnector connector;
	private static CarparkSetting cs;
	private final long checkConnectorSecond = 3;
	private NioSocketAcceptor acceptor;
	private ExecutorService newSingleThreadExecutor;
	private final int PORT = 9124;
	private XinlutongCallback.XinlutongResult xlr;
	
	private static boolean isPlayVoice = false;
	private XinlutongJNAImpl xinlutongJNAImpl;
	private Map<String,String> deviceVersionMap = Maps.newHashMap();
	private IoSession session;

	private HardwareService(){};
	
	public static HardwareService getInstance(){
		if(service == null){
			service = new HardwareService();
			messageService = new MessageServiceImpl();
			try {
				cs = (CarparkSetting)FileUtil.readObjectFromFile(dataFilePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return service;
	}
	
	public void start(){
		if(cs == null){
			return;
		}
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
		final String plateDeviceip = cs.getPlateDeviceip();
		final String name = cs.getDeviceList().get(0).getName();
		if(plateDeviceip != null && !plateDeviceip.trim().isEmpty()){
			if(xlr == null){
    			xlr = new XinlutongCallback.XinlutongResult() {
    				@Override
    				public void invok(String ip, int channel, String plateNO, byte[] bigImage, byte[] smallImage) {
    					HardwareUtil.setPlateInfo(session,name,ip,plateNO,bigImage,smallImage);
    				}
    			};
			}
			xinlutongJNAImpl = new XinlutongJNAImpl();
			xinlutongJNAImpl.openEx(plateDeviceip, xlr);
		}
	}

	private void startListne(){
		try {
			acceptor = new NioSocketAcceptor();

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
			throw new DongluHWException("开始监听服务器失败!",e);
		}
	}
	
	private void startLogging(){
		Timer timer = new Timer("check web connector");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try{
					List<Device> deviceList = cs.getDeviceList();
					for (Device device : deviceList) {
						LOGGER.debug("开始轮询设备:{}",device.getName());
						long start = System.currentTimeMillis();
						try{
							if(isPlayVoice == true){
								HardwareUtil.controlSpeed(start, 300);
								isPlayVoice = false;
							}

							if(deviceVersionMap.get(device.getName()) == null){
								ListenableFuture<String> versionFuture = messageService.readVersion(device);
								String version = versionFuture.get(3000, TimeUnit.MILLISECONDS);
								device.setVersion(version);
								deviceVersionMap.put(device.getName(),version);
								device.setVersion(version);
								FileUtil.writeObjectToFile(cs,dataFilePath);
							}

							ListenableFuture<CarparkNowRecord> carparkReadNowRecord = messageService.carparkReadNowRecord(device);
							CarparkNowRecord carparkNowRecord = carparkReadNowRecord.get(5000,TimeUnit.MILLISECONDS);
							if(carparkNowRecord != null){
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
		Timer timer = new Timer("check date time");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try{
					List<Device> deviceList = cs.getDeviceList();
					for (Device device : deviceList) {
						Date date = new Date();
						LOGGER.debug("开始设置设备{}时间:{}",device.getName(),date);
						
						long start = System.currentTimeMillis();
						try{
							if(isPlayVoice == true){
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
				}catch(Exception e){}
			}
		},3000,1000*60*30);
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
			cf = connector.connect(new InetSocketAddress(cs.getIp(), Integer.parseInt(cs.getPort())));
			boolean b = cf.awaitUninterruptibly(5, TimeUnit.SECONDS);
			if(b == false || cf.getException() != null){
				EventBusUtil.post(new EventInfo(EventInfo.EventType.外接服务通讯异常, "当前主机与对接服务通讯失败,3秒后会自动重联"));
			}
			this.session = cf.getSession();
		} catch (Exception e) {
			throw new DongluHWException("对接连接检查发生异常",e);
		}
	}
	
	private boolean isSendDevice = false;
	private void checkWebConnector(){
		Timer timer = new Timer("check web connector");
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				try{
					String ip = cs.getIp();
					String port = cs.getPort();
					LOGGER.debug("正在检查外接服务,ip:{} port:{}",ip,port);
					if(session == null || !session.isConnected()){
						LOGGER.debug("检查到会话不存在或己关闭，准备重新建立会话");

						EventBusUtil.post(new EventInfo(EventInfo.EventType.外接服务通讯异常, "当前主机与对接服务通讯失败,3秒后会自动重联"));

						ConnectFuture connect = connector.connect(new InetSocketAddress(cs.getIp(), Integer.parseInt(cs.getPort())));
						boolean awaitUninterruptibly = connect.awaitUninterruptibly(10,TimeUnit.SECONDS);
						if(awaitUninterruptibly && connect.getException() == null){
							cf = connect;
							session = cf.getSession();
							isSendDevice = false;
						}
					}else{
						EventBusUtil.post(new EventInfo(EventInfo.EventType.外接服务通讯正常, "外接服务通讯恢复正常"));
						if(isSendDevice == false){
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
				newSingleThreadExecutor.submit(new Runnable() {
					
					@Override
					public void run() {
						try{
							String deviceName = rootElement.element("device").element("deviceName").getTextTrim();
							String ad = rootElement.element("ad").getTextTrim();
							
							Device device = cs.getDeviceByName(deviceName);
							ListenableFuture<Boolean> setAD = messageService.setAD(device, ad);
							setAD.get();
							
							HardwareUtil.responseDeviceControl(session,dom);	
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				});
				
			}
			
			if(wm.getType() == WebMessageType.设备控制){
				newSingleThreadExecutor.submit(new Runnable() {
					
					@Override
					public void run() {
						try{
							isPlayVoice = true;
							Element controlElement = rootElement.element("control");
							Element element = rootElement.element("device");
							
							String deviceName = element.element("deviceName").getTextTrim();
							String gate = controlElement.element("gate").getTextTrim();
							String Insidevoice,Outsidevoice,InsideScreen,OutsideScreen,InsideScreenAndVoiceData,OutsideScreenAndVoiceData;
							if(controlElement.element("insideVoice") == null){
								Insidevoice = controlElement.element("InsideVoice").getTextTrim();
								Outsidevoice = controlElement.element("OutsideVoice").getTextTrim();
								InsideScreen = controlElement.element("InsideScreen").getTextTrim();
								OutsideScreen = controlElement.element("OutsideScreen").getTextTrim();
								InsideScreenAndVoiceData = controlElement.element("InsideScreenAndVoiceData").getTextTrim();
								OutsideScreenAndVoiceData = controlElement.element("OutsideScreenAndVoiceData").getTextTrim();
							}else{
								Insidevoice = controlElement.element("insideVoice").getTextTrim();
								Outsidevoice = controlElement.element("outsideVoice").getTextTrim();
								InsideScreen = controlElement.element("insideScreen").getTextTrim();
								OutsideScreen = controlElement.element("outsideScreen").getTextTrim();
								InsideScreenAndVoiceData = controlElement.element("insideScreenAndVoiceData").getTextTrim();
								OutsideScreenAndVoiceData = controlElement.element("outsideScreenAndVoiceData").getTextTrim();
							}
							
							Device device = cs.getDeviceByName(deviceName);
							if(device == null){
								return;
							}
							if(InsideScreen.equals("true")){
								int voice = Insidevoice.equals("false")==true ? 1 : 9;
								ListenableFuture<Boolean> carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 1, voice, 0, OpenDoorEnum.parse(gate), InsideScreenAndVoiceData);
								Boolean boolean1 = carparkScreenVoiceDoor.get();
								if(boolean1 == null){
									carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 1, voice, 0, OpenDoorEnum.parse(gate), InsideScreenAndVoiceData);
									carparkScreenVoiceDoor.get();
								}
							}
							if(OutsideScreen.equals("true")){
								int voice = Outsidevoice.equals("false")==true ? 1 : 9;
								ListenableFuture<Boolean> carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 2, voice, 0, OpenDoorEnum.parse(gate), OutsideScreenAndVoiceData);
								Boolean boolean1 = carparkScreenVoiceDoor.get();
								if(boolean1 == null){
									carparkScreenVoiceDoor = messageService.carparkScreenVoiceDoor(device, 2, voice, 0, OpenDoorEnum.parse(gate), OutsideScreenAndVoiceData);
									carparkScreenVoiceDoor.get();
								}
							}
							HardwareUtil.responseDeviceControl(session,dom);		
						}catch(Exception e){
							e.printStackTrace();
						}
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


}
