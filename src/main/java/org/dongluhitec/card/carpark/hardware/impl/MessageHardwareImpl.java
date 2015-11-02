package org.dongluhitec.card.carpark.hardware.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.dongluhitec.card.carpark.connect.Message;
import org.dongluhitec.card.carpark.connect.MessageTransport;
import org.dongluhitec.card.carpark.connect.body.CarparkNowRecordBody;
import org.dongluhitec.card.carpark.connect.body.ProductIDBody;
import org.dongluhitec.card.carpark.connect.body.SimpleBody;
import org.dongluhitec.card.carpark.hardware.MessageFactory;
import org.dongluhitec.card.carpark.hardware.MessageHardware;
import org.dongluhitec.card.carpark.model.CarparkNowRecord;
import org.dongluhitec.card.carpark.model.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class MessageHardwareImpl implements MessageHardware {
	
	private Logger LOGGER = LoggerFactory.getLogger(MessageHardwareImpl.class);
	private static Map<String,MessageTransport> transportMap = new HashMap<>();
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
	public ListenableFuture<CarparkNowRecord> carparkReadNowRecord(final Device device){
		LOGGER.debug("read carpark current record for :{}" , device);
		final Message<?> msg = MessageFactory.createReadNowRecordMsg(device);

		return listeningDecorator.submit(() -> {
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
        });
	}
	
	@Override
	public ListenableFuture<Boolean> carparkScreenVoiceDoor(final Device device,final int screenID,final int voice,final int font,final int door,final String text){
		LOGGER.debug("carpark's screen and voice and door for :{}" , device);
		final Message<?> msg = MessageFactory.createScreenVoiceDoorMsg(device, screenID, voice, font, door, text);
		ListenableFuture<Boolean> submit;
		submit = listeningDecorator.submit(() -> {
            MessageTransport messageTransport = getMessageTransport(device);
            Message<?> sendMessage = messageTransport.sendMessage(msg,3000);
            if(sendMessage == null){
                return null;
            }
            SimpleBody body = (SimpleBody)sendMessage.getBody();
            return body.getSimpleBody() == 'y';
        });
		return submit;
	}
	
	@Override
	public void setDateTime(final Device device,final Date date){
		LOGGER.debug("carpark's set date :{} for :{}" ,date, device);
		final Message<?> msg = MessageFactory.createSetDateTime(device, date);
		listeningDecorator.submit(() -> {
            MessageTransport messageTransport = getMessageTransport(device);
            messageTransport.sendMessageNoReturn(msg);
        });
	}

	@Override
	public ListenableFuture<Boolean> setAD(final Device device, String adStr) {
		LOGGER.debug("carpark's set ad:{} for:{}" , adStr, device);
		final Message<?> msg = MessageFactory.createADScreenMsg(device, adStr);
		return listeningDecorator.submit(() -> {
            MessageTransport messageTransport = getMessageTransport(device);
            Message<?> sendMessage = messageTransport.sendMessage(msg);
            if(sendMessage == null){
                return null;
            }
            SimpleBody body = (SimpleBody)sendMessage.getBody();
            return body.getSimpleBody() == 'y';
        });
	}

	@Override
	public ListenableFuture<String> readVersion(final Device device) {
		LOGGER.debug("carpark's read version for:{}" , device);
		final Message<?> msg = MessageFactory.createVersionMsg(device);
		return listeningDecorator.submit(() -> {
            MessageTransport messageTransport = getMessageTransport(device);
            Message<?> sendMessage = messageTransport.sendMessage(msg);
            if(sendMessage == null){
                return null;
            }
            ProductIDBody body = (ProductIDBody)sendMessage.getBody();
            return body.getProductinId();
        });
	}
}
