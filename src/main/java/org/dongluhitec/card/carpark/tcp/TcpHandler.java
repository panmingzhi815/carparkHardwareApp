package org.dongluhitec.card.carpark.tcp;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dongluhitec.card.carpark.connect.MessageBody;
import org.dongluhitec.card.carpark.connect.body.MessageDateTimeBody;
import org.dongluhitec.card.carpark.connect.body.TcpRecordBody;
import org.dongluhitec.card.carpark.connect.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;

/**
 * Created by xiaopan on 2016-05-04 0004.
 */
public class TcpHandler extends IoHandlerAdapter implements IoHandler {

    private static Logger LOGGER = LoggerFactory.getLogger(TcpHandler.class);
    private final TcpRecordCallable callable;
    private final SimpleMessage returnMessage = new SimpleMessage((byte) 0x57,(byte) 0x00,(byte) 0x01,(byte) 0x00,(byte) 0x01,(byte)0x08);

    public TcpHandler(TcpRecordCallable callable) {
        this.callable = callable;
    }

    public MessageBody parse(byte[] bytes){
        if (bytes.length == TcpRecordBody.LENGTH) {
            TcpRecordBody tcpRecordBody = new TcpRecordBody();
            tcpRecordBody.initContent(Arrays.copyOfRange(bytes,8,22));
            return tcpRecordBody;
        }
        return new TcpRecordBody();
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        LOGGER.error("会话发生异常",cause);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        byte[] bytes = (byte[])message;
        LOGGER.debug("收到设备:{}消息:{}",session, ByteUtils.byteArrayToHexString(bytes));
        try {
            TcpRecordBody parse = (TcpRecordBody) parse(bytes);
            LOGGER.debug("解析消息成功");
            callable.call(session.toString(),parse.getCardIdentifier());
            returnMessage.setMessageBody(new MessageDateTimeBody(new Date()));
            session.write(returnMessage.toBytes());
            session.close(true);
        } catch (Exception e) {
            LOGGER.error("处理TCP刷卡记录时发生异常",e);
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        byte[] bytes = (byte[])message;
        LOGGER.debug("向设备:{}发送消息:{}",session,ByteUtils.byteArrayToHexString(bytes));
    }
}
