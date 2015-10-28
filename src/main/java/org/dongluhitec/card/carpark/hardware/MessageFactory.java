package org.dongluhitec.card.carpark.hardware;

import org.dongluhitec.card.carpark.connect.*;
import org.dongluhitec.card.carpark.connect.body.*;
import org.dongluhitec.card.carpark.connect.util.SerialDeviceAddress;
import org.dongluhitec.card.carpark.model.Device;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MessageFactory {
	
	private static Map<String,Message<MessageBody>> commandMap = new HashMap<>();
	
	public static Message<?> createOpenDoorMsg(Device device,OpenDoorEnum openDoorEnum) {
		String key = device.toString() + openDoorEnum + "createOpenDoorMsg";
		Message<MessageBody> message = commandMap.get(key);
		if(message != null){
			return message;
		}
		
		SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
		serialDeviceAddress.setAddress(device.getArea());
		MessageHeader mh = new MessageHeader(serialDeviceAddress, DirectonType.请求, MessageConstance.Message_OpenDoor, SimpleBody.LENGTH);
		SimpleBody sb = new SimpleBody();
		sb.setSimpleBody((byte)openDoorEnum.getI());
		
		Message<MessageBody> msg = new Message<>(mh, sb);
		commandMap.put(key, msg);
		return msg;
	}

	public static Message<?> createReadNowRecordMsg(Device device) {
		String key = device.toString() + "createReadNowRecordMsg";
		Message<MessageBody> message = commandMap.get(key);
		if(message != null){
			return message;
		}
		
		SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
		serialDeviceAddress.setAddress(device.getArea());
		MessageHeader mh = new MessageHeader(serialDeviceAddress,DirectonType.请求,MessageConstance.Message_ReadNowRecord, EmptyBody.LENGTH);
		EmptyBody sb = new EmptyBody();
		Message<MessageBody> msg = new Message<>(mh, sb);
		commandMap.put(key, msg);
		return msg;
	}
	
	public static Message<?> createScreenVoiceDoorMsg(Device device, int screenID, int voice, int font, int door, String text) {
		String key = device.toString() + screenID + voice + font + door + text + "createScreenVoiceDoorMsg";
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
		
		Message<MessageBody> msg = new Message<>(mh, sb);
		commandMap.put(key, msg);
		return msg;
	}
	
	public static Message<?> createSetDateTime(Device device, Date date) {
		SetDateTimeBody setDateTimeBody = new SetDateTimeBody();
		setDateTimeBody.setDate(date);

		SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
		serialDeviceAddress.setAddress(device.getArea());
		MessageHeader mh = new MessageHeader(serialDeviceAddress,DirectonType.请求,MessageConstance.Message_SetTime,SetDateTimeBody.LENGTH);

		return new Message<MessageBody>(mh, setDateTimeBody);
	}
	
	public static Message<?> createADScreenMsg(Device device, String adStr) {
		String key = device.toString() + adStr;
		Message<MessageBody> message = commandMap.get(key);
		if(message != null){
			return message;
		}
		ADScreenBody adScreenBody = new ADScreenBody();
		adScreenBody.setText(adStr);

		SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
		serialDeviceAddress.setAddress(device.getArea());
		MessageHeader mh = new MessageHeader(serialDeviceAddress,DirectonType.请求,MessageConstance.Message_AD,ADScreenBody.LENGTH);

		return new Message<MessageBody>(mh, adScreenBody);
	}

	public static Message<?> createVersionMsg(Device device) {
		String key = device.toString() + "createVersionMsg";
		Message<MessageBody> message = commandMap.get(key);
		if(message != null){
			return message;
		}
		EmptyBody eb = new EmptyBody();

		SerialDeviceAddress serialDeviceAddress = new SerialDeviceAddress();
		serialDeviceAddress.setAddress(device.getArea());
		MessageHeader mh = new MessageHeader(serialDeviceAddress,DirectonType.请求,MessageConstance.Message_ReadVersion,EmptyBody.LENGTH);

		return new Message<MessageBody>(mh, eb);
	}
}
