package org.dongluhitec.card.carpark.tcp;

import org.dongluhitec.card.carpark.connect.MessageBody;

/**
 * Created by xiaopan on 2016-05-04 0004.
 */
public class SimpleMessage {

    private static final int MESSAGE_START = 0;
    private static final int DATA_START = 8;

    private byte direction;
    private byte address1;
    private byte address2;
    private byte address3;
    private byte address4;
    private byte functionCode;
    private MessageBody messageBody;

    public SimpleMessage(byte direction, byte address1, byte address2, byte address3, byte address4, byte functionCode) {
        this.direction = direction;
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.functionCode = functionCode;
    }

    public byte[] toBytes() {
        byte[] prefix_bytes = {0x01, direction, address1, address2, address3, address4, functionCode, 0x02};
        byte[] bodyBytes = messageBody.toBytes();
        byte[] end_bytes = {0x03, 0x00};

        byte[] result = new byte[prefix_bytes.length + bodyBytes.length + end_bytes.length];
        System.arraycopy(prefix_bytes, 0, result, MESSAGE_START, prefix_bytes.length);
        System.arraycopy(bodyBytes, 0, result, DATA_START, bodyBytes.length);
        System.arraycopy(end_bytes, 0, result, DATA_START + bodyBytes.length, end_bytes.length);
        return result;
    }

    public byte getDirection() {
        return direction;
    }

    public void setDirection(byte direction) {
        this.direction = direction;
    }

    public byte getAddress1() {
        return address1;
    }

    public void setAddress1(byte address1) {
        this.address1 = address1;
    }

    public byte getAddress2() {
        return address2;
    }

    public void setAddress2(byte address2) {
        this.address2 = address2;
    }

    public byte getAddress3() {
        return address3;
    }

    public void setAddress3(byte address3) {
        this.address3 = address3;
    }

    public byte getAddress4() {
        return address4;
    }

    public void setAddress4(byte address4) {
        this.address4 = address4;
    }

    public byte getFunctionCode() {
        return functionCode;
    }

    public void setFunctionCode(byte functionCode) {
        this.functionCode = functionCode;
    }

    public MessageBody getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(MessageBody messageBody) {
        this.messageBody = messageBody;
    }
}
