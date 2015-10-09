package org.dongluhitec.card.carpark.connect.body;


import org.dongluhitec.card.carpark.connect.MessageBody;
import org.dongluhitec.card.carpark.connect.exception.DongluInvalidMessageException;
import org.dongluhitec.card.carpark.connect.util.BCDDateTimeAdaptor;

import java.util.Date;

public class SetDateTimeBody implements MessageBody {

	public static final int LENGTH = 6;

	private Date date;

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getDate() {
		return this.date;
	}

	@Override
	public void initContent(byte[] array) throws DongluInvalidMessageException {
		this.date = new BCDDateTimeAdaptor(array, 0, true).getDate();

	}

	@Override
	public byte[] toBytes() {
		return new BCDDateTimeAdaptor(this.date).getBytes(true);
	}

	@Override
	public String toString() {
		return "时间: " + this.date.toString();
	}

}
