package org.dongluhitec.card.carpark.connect.body;


import org.dongluhitec.card.carpark.connect.MessageBody;

public class EmptyBody implements MessageBody {

	public static int LENGTH = 0;

	@Override
	public void initContent(byte[] array) {
	}

	@Override
	public byte[] toBytes() {
		return new byte[EmptyBody.LENGTH];
	}

	@Override
	public String toString() {
		return "空消息体";
	}

}
