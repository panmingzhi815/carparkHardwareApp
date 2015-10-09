package org.dongluhitec.card.carpark.hardware.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.dongluhitec.card.carpark.connect.Message;
import org.dongluhitec.card.carpark.connect.MessageTransport;
import org.dongluhitec.card.carpark.connect.body.CarparkNowRecordBody;
import org.dongluhitec.card.carpark.connect.body.OpenDoorEnum;
import org.dongluhitec.card.carpark.connect.body.ProductIDBody;
import org.dongluhitec.card.carpark.connect.body.SimpleBody;
import org.dongluhitec.card.carpark.hardware.MessageService;
import org.dongluhitec.card.carpark.hardware.MessageUtil;
import org.dongluhitec.card.carpark.model.CarparkNowRecord;
import org.dongluhitec.card.carpark.model.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class MessageServiceImpl implements MessageService {
	
	private Logger LOGGER = LoggerFactory.getLogger(MessageServiceImpl.class);
	private static Map<String,MessageTransport> transportMap = new HashMap<String,MessageTransport>();
	private static ListeningExecutorService listeningDecorator = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
	
	public MessageTransport getMessageTransport(Device device){
		String key = device.getAddress();
		MessageTransport messageTransport = transportMap.get(key);
		if(messageTransport != null){
			return messageTransport;
		}
		String type = device.getType();
		if(type.equals(MessageTransport.TransportType.COM.name())){
			messageTransport = new MessageTransport(device.getAddress(), MessageTransport.TransportType.COM);
		}
		if(type.equals(MessageTransport.TransportType.TCP.name())){
			messageTransport = new MessageTransport(device.getAddress(), MessageTransport.TransportType.TCP);
		}
		transportMap.put(key, messageTransport);
		return messageTransport;
	}
	
	@Override
	public ListenableFuture<Boolean> carparkOpenDoor(final Device device,OpenDoorEnum openDoorEnum){
		LOGGER.debug("carpark open door for :{}" , device);
		final Message<?> msg = MessageUtil.createOpenDoorMsg(device, openDoorEnum);
		
		ListenableFuture<Boolean> submit = listeningDecorator.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				MessageTransport messageTransport = getMessageTransport(device);
				Message<?> sendMessage = messageTransport.sendMessage(msg,1000);
				if(sendMessage == null){
					return null;
				}
				SimpleBody body = (SimpleBody)sendMessage.getBody();
				return body.getSimpleBody() == 'y';
			}
		});
		return submit;
	}
	
	@Override
	public ListenableFuture<CarparkNowRecord> carparkReadNowRecord(final Device device){
		LOGGER.debug("read carpark current record for :{}" , device);
		final Message<?> msg = MessageUtil.createReadNowRecordMsg(device);
		
		ListenableFuture<CarparkNowRecord> submit = listeningDecorator.submit(new Callable<CarparkNowRecord>() {
			@Override
			public CarparkNowRecord call() throws Exception {
				MessageTransport messageTransport = getMessageTransport(device);
				Message<?> sendMessage = messageTransport.sendMessage(msg);
				if(sendMessage == null){
					return null;
				}
				CarparkNowRecordBody body = (CarparkNowRecordBody)sendMessage.getBody();
				if(!body.isHasRecord()){
					return null;
				}
				String cardID = body.getCardID();
				int readerID = body.getReaderID();
				return new CarparkNowRecord(readerID,cardID);
			}
		});
		return submit;
	}
	
	@Override
	public ListenableFuture<Boolean> carparkScreenVoiceDoor(final Device device,final int screenID,final int voice,final int font,final int door,final String text){
		LOGGER.debug("carpark's screen and voice and door for :{}" , device);
		final Message<?> msg = MessageUtil.createScreenVoiceDoorMsg(device,screenID,voice,font,door,text);
		ListenableFuture<Boolean> submit = listeningDecorator.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				MessageTransport messageTransport = getMessageTransport(device);
				Message<?> sendMessage = messageTransport.sendMessage(msg,3000);
				if(sendMessage == null){
					return null;
				}
				SimpleBody body = (SimpleBody)sendMessage.getBody();
				return body.getSimpleBody() == 'y';
			}
		});
		return submit;
	}
	
	@Override
	public void setDateTime(final Device device,final Date date){
		LOGGER.debug("carpark's set date :{} for :{}" ,date, device);
		final Message<?> msg = MessageUtil.createSetDateTime(device, date);
		listeningDecorator.submit(new Runnable() {
			@Override
			public void run() {
				MessageTransport messageTransport = getMessageTransport(device);
				messageTransport.sendMessageNoReturn(msg);
			}
		});
	}

	@Override
	public ListenableFuture<Boolean> setAD(final Device device, String adStr) {
		LOGGER.debug("carpark's set ad:{} for:{}" , adStr, device);
		final Message<?> msg = MessageUtil.creatADScreenMsg(device, adStr);
		ListenableFuture<Boolean> submit = listeningDecorator.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				MessageTransport messageTransport = getMessageTransport(device);
				Message<?> sendMessage = messageTransport.sendMessage(msg);
				if(sendMessage == null){
					return null;
				}
				SimpleBody body = (SimpleBody)sendMessage.getBody();
				return body.getSimpleBody() == 'y';
			}
		});
		return submit;
	}

	@Override
	public ListenableFuture<String> readVersion(final Device device) {
		LOGGER.debug("carpark's read version for:{}" , device);
		final Message<?> msg = MessageUtil.creatVersionMsg(device);
		ListenableFuture<String> submit = listeningDecorator.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				MessageTransport messageTransport = getMessageTransport(device);
				Message<?> sendMessage = messageTransport.sendMessage(msg);
				if(sendMessage == null){
					return null;
				}
				ProductIDBody body = (ProductIDBody)sendMessage.getBody();
				return body.getProductinId();
			}
		});
		return submit;
	}
}
