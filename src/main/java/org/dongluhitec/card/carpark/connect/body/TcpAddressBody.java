package org.dongluhitec.card.carpark.connect.body;

import org.dongluhitec.card.carpark.connect.MessageBody;
import org.dongluhitec.card.carpark.connect.exception.DongluInvalidMessageException;

/**
 * Created by xiaopan on 2016-05-04 0004.
 */
public class TcpAddressBody implements MessageBody{

    public static final int LENGTH = 4;
    private String address;

    @Override
    public void initContent(byte[] bytes) throws DongluInvalidMessageException {
        this.address = String.format("%d.%d.%d.%d", bytes[0], bytes[1], bytes[2], bytes[3]);
    }

    @Override
    public byte[] toBytes() {
        String[] split = address.split("\\.");
        byte[] result = {(byte)Integer.parseInt(split[3]),(byte)Integer.parseInt(split[2]),(byte)Integer.parseInt(split[1]),(byte)Integer.parseInt(split[0])};
        return result;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

}
