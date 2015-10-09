package org.dongluhitec.card.carpark.connect.body;

import org.dongluhitec.card.carpark.connect.MessageBody;
import org.dongluhitec.card.carpark.connect.util.ByteUtils;

public class SimpleBody implements MessageBody {

	public static int LENGTH = 1;

	protected byte simpleBody;

	public void setSimpleBody(byte simpleBody) {
		this.simpleBody = simpleBody;
	}

	public byte getSimpleBody() {
		return this.simpleBody;
	}

	@Override
	public void initContent(byte[] array) {
		assert array.length == SimpleBody.LENGTH;
		this.simpleBody = array[0];
	}

	@Override
	public byte[] toBytes() {
		return new byte[]{this.simpleBody};
	}

	@Override
	public String toString() {
		return "字符: " + ByteUtils.byteToHexString(simpleBody);
	}

}
