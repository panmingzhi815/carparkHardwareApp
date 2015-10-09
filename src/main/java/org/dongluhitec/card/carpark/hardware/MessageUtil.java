package org.dongluhitec.card.carpark.hardware;

import org.dongluhitec.card.carpark.connect.*;
import org.dongluhitec.card.carpark.connect.body.*;
import org.dongluhitec.card.carpark.connect.util.SerialDeviceAddress;
import org.dongluhitec.card.carpark.model.Device;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessageUtil {
	
	private static Map<String,Message<MessageBody>> commandMap = new HashMap<String,Message<MessageBody>>();
	
	public static Message<?> createOpenDoorMsg(Device device,OpenDoorEnum openDoorEnum) {
		String key = new StringBuffer().append(device.toString()).append(openDoorEnum).append("createOpenDoorMsg").toString();
		Message<MessageBody> message = commandMap.get(key);
		if(message != null){
			return message;
		}
		
		SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
		serialDeviceAddress.setAddress(device.getArea());
		MessageHeader mh = new MessageHeader(serialDeviceAddress, DirectonType.请求, MessageConstance.Message_OpenDoor, SimpleBody.LENGTH);
		SimpleBody sb = new SimpleBody();
		sb.setSimpleBody((byte)openDoorEnum.getI());
		
		Message<MessageBody> msg = new Message<MessageBody>(mh, sb);
		commandMap.put(key, msg);
		return msg;
	}

	public static Message<?> createReadNowRecordMsg(Device device) {
		String key = new StringBuffer().append(device.toString()).append("createReadNowRecordMsg").toString();
		Message<MessageBody> message = commandMap.get(key);
		if(message != null){
			return message;
		}
		
		SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
		serialDeviceAddress.setAddress(device.getArea());
		MessageHeader mh = new MessageHeader(serialDeviceAddress,DirectonType.请求,MessageConstance.Message_ReadNowRecord, EmptyBody.LENGTH);
		EmptyBody sb = new EmptyBody();
		Message<MessageBody> msg = new Message<MessageBody>(mh, sb);
		commandMap.put(key, msg);
		return msg;
	}
	
	public static Message<?> createScreenVoiceDoorMsg(Device device, int screenID, int voice, int font, int door, String text) {
		String key = new StringBuffer().append(device.toString()).append(screenID).append(voice).append(font).append(door).append(text).append("createScreenVoiceDoorMsg").toString();
		Message<MessageBody> message = commandMap.get(key);
		if(message != null){
			return message;
		}
		
		SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
		serialDeviceAddress.setAddress(device.getArea());
		MessageHeader mh = new MessageHeader(serialDeviceAddress,DirectonType.请求,MessageConstance.Message_ScreenVoiceDoor, ScreenVoiceDoorBody.LENGTH);
		ScreenVoiceDoorBody sb = new ScreenVoiceDoorBody();
		sb.setDoor(door);
		sb.setFont(font);
		sb.setScreenID(screenID);
		sb.setVoice(voice);
		sb.setText(text);
		
		Message<MessageBody> msg = new Message<MessageBody>(mh, sb);
		commandMap.put(key, msg);
		return msg;
	}
	
	public static Message<?> createSetDateTime(Device device, Date date) {
		SetDateTimeBody setDateTimeBody = new SetDateTimeBody();
		setDateTimeBody.setDate(date);

		SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
		serialDeviceAddress.setAddress(device.getArea());
		MessageHeader mh = new MessageHeader(serialDeviceAddress,DirectonType.请求,MessageConstance.Message_SetTime,SetDateTimeBody.LENGTH);
		
		Message<MessageBody> msg = new Message<MessageBody>(mh, setDateTimeBody);
		return msg;
	}
	
	public static Message<?> creatADScreenMsg(Device device, String adStr) {
		String key = new StringBuffer().append(device.toString()).append(adStr).toString();
		Message<MessageBody> message = commandMap.get(key);
		if(message != null){
			return message;
		}
		ADScreenBody adsb = new ADScreenBody();
		adsb.setText(adStr);

		SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
		serialDeviceAddress.setAddress(device.getArea());
		MessageHeader mh = new MessageHeader(serialDeviceAddress,DirectonType.请求,MessageConstance.Message_AD,ADScreenBody.LENGTH);
		
		Message<MessageBody> msg = new Message<MessageBody>(mh, adsb);
		return msg;
	}

	public static Message<?> creatVersionMsg(Device device) {
		String key = new StringBuffer().append(device.toString()).append("creatVersionMsg").toString();
		Message<MessageBody> message = commandMap.get(key);
		if(message != null){
			return message;
		}
		EmptyBody eb = new EmptyBody();

		SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
		serialDeviceAddress.setAddress(device.getArea());
		MessageHeader mh = new MessageHeader(serialDeviceAddress,DirectonType.请求,MessageConstance.Message_ReadVersion,EmptyBody.LENGTH);

		Message<MessageBody> msg = new Message<MessageBody>(mh, eb);
		return msg;
	}
}
