package org.dongluhitec.card.carpark.connect.filterChain;


import org.dongluhitec.card.carpark.connect.MessageBodyInfo;
import org.dongluhitec.card.carpark.connect.MessageConstance;
import org.dongluhitec.card.carpark.connect.body.*;

import java.util.Map;

public class CarparkMessageRegisterImpl extends AbstractMessageRegister {

	@Override
	public void registerRequestBody(Map<Byte, MessageBodyInfo> requestMap) {
		requestMap.put(MessageConstance.Message_ReadNowRecord, new MessageBodyInfo(EmptyBody.LENGTH,EmptyBody.class));
		requestMap.put(MessageConstance.Message_OpenDoor, new MessageBodyInfo(SimpleBody.LENGTH,SimpleBody.class));
		requestMap.put(MessageConstance.Message_ScreenVoiceDoor, new MessageBodyInfo(ScreenVoiceDoorBody.LENGTH,ScreenVoiceDoorBody.class));
		requestMap.put(MessageConstance.Message_SetTime, new MessageBodyInfo(SetDateTimeBody.LENGTH,SetDateTimeBody.class));
		requestMap.put(MessageConstance.Message_ReadVersion, new MessageBodyInfo(EmptyBody.LENGTH,EmptyBody.class));
		requestMap.put(MessageConstance.Message_AD, new MessageBodyInfo(ADScreenBody.LENGTH,ADScreenBody.class));

	}

	@Override
	public void registerResponseBody(Map<Byte, MessageBodyInfo> responseMap) {
		responseMap.put(MessageConstance.Message_ReadNowRecord, new MessageBodyInfo(CarparkNowRecordBody.LENGTH,CarparkNowRecordBody.class));
		responseMap.put(MessageConstance.Message_OpenDoor, new MessageBodyInfo(SimpleBody.LENGTH,SimpleBody.class));
		responseMap.put(MessageConstance.Message_ScreenVoiceDoor, new MessageBodyInfo(SimpleBody.LENGTH,SimpleBody.class));
		responseMap.put(MessageConstance.Message_AD, new MessageBodyInfo(SimpleBody.LENGTH,SimpleBody.class));
		responseMap.put(MessageConstance.Message_ReadVersion, new MessageBodyInfo(ProductIDBody.LENGTH,ProductIDBody.class));
	}

}
