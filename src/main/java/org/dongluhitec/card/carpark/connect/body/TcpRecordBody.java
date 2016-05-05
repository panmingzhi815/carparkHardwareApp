package org.dongluhitec.card.carpark.connect.body;

import org.dongluhitec.card.carpark.connect.MessageBody;
import org.dongluhitec.card.carpark.connect.exception.DongluInvalidMessageException;
import org.dongluhitec.card.carpark.connect.util.BCDDateTimeAdaptor;

import java.util.Arrays;
import java.util.Date;

/**
 * Created by xiaopan on 2016-05-04 0004.
 */
public class TcpRecordBody implements MessageBody {

    public static final int LENGTH = 18;
    private String cardIdentifier;

    @Override
    public void initContent(byte[] bytes) throws DongluInvalidMessageException {
        byte[] cardIdentifierBytes = Arrays.copyOfRange(bytes, 0, 8);

        CardIDBody cardIDBody = new CardIDBody();
        cardIDBody.initContent(cardIdentifierBytes);
        this.cardIdentifier = cardIDBody.getCardID();
    }

    @Override
    public byte[] toBytes() {
        return new byte[0];
    }

    public String getCardIdentifier() {
        return cardIdentifier;
    }

    public void setCardIdentifier(String cardIdentifier) {
        this.cardIdentifier = cardIdentifier;
    }
}
