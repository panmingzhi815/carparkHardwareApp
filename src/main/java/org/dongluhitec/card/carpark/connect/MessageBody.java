package org.dongluhitec.card.carpark.connect;

import org.dongluhitec.card.carpark.connect.exception.DongluInvalidMessageException;

public interface MessageBody extends Bytenize {

	public void initContent(byte[] bytes) throws DongluInvalidMessageException;

}
