package org.dongluhitec.card.carpark.connect;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.serial.SerialAddress;
import org.apache.mina.transport.serial.SerialAddress.DataBits;
import org.apache.mina.transport.serial.SerialAddress.FlowControl;
import org.apache.mina.transport.serial.SerialAddress.Parity;
import org.apache.mina.transport.serial.SerialAddress.StopBits;
import org.apache.mina.transport.serial.SerialConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.dongluhitec.card.carpark.connect.body.ScreenVoiceDoorBody;
import org.dongluhitec.card.carpark.connect.exception.DongluHWException;
import org.dongluhitec.card.carpark.connect.filterChain.MessageFactory;
import org.dongluhitec.card.carpark.connect.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class MessageTransport {
	private static  final Logger LOGGER = LoggerFactory.getLogger(MessageTransport.class);

	public enum TransportType {
		TCP, COM
	}

	private final String address;
	private final TransportType transportType;

	private IoConnector ioConnector;
	private ConnectFuture currentConnect;
	private SocketAddress socketAddress;
	private IoSession session;
	private IoHandler ioHandler;
	private ProtocolCodecFilter protocolCodecFilter;

	public MessageTransport(String address, TransportType transportType) {
		this.address = address;
		this.transportType = transportType;
		this.ioHandler = new MessageHandler();
		this.protocolCodecFilter = new ProtocolCodecFilter(new MessageFactory(MessageTypeEnum.停车场));

		switch (this.transportType) {
			case TCP:
				String[] split = address.split(":");
				this.socketAddress = new InetSocketAddress(split[0], Integer.parseInt(split[1]));
				break;
			case COM:
				this.socketAddress = new SerialAddress(address, 9600, DataBits.DATABITS_8, StopBits.BITS_1, Parity.NONE, FlowControl.NONE);
				break;
			default:
				throw new DongluHWException("不支持的通讯类型:" + transportType);
		}
	}

	public void open() {
		if (ioConnector == null) {
			IoConnector ioConnector;
			if (transportType == TransportType.COM) {
				ioConnector = new SerialConnector();
			} else {
				ioConnector = new NioSocketConnector();
			}
			ioConnector.getFilterChain().addLast("codec",this.protocolCodecFilter);
			ioConnector.setHandler(new MessageHandler());
			ioConnector.setConnectTimeoutMillis(1000);
			this.ioConnector = ioConnector;
		}
		
		ConnectFuture connect = ioConnector.connect(this.socketAddress);
        boolean connected = connect.awaitUninterruptibly(200);

        if (!connected) {
            throw new DongluHWException("连接超时:"+address);
        }
		this.currentConnect = connect;
        this.session = connect.getSession();
	}

	public void close() {
		if (this.session != null) {
            CloseFuture close = this.session.close(true);
            close.awaitUninterruptibly();
            this.session = null;
        }
	}

	public synchronized Message<?> sendMessage(Message<?> message,long waitTime) {
		try {
			byte[] sendBytes = message.toBytes();
			open();

			WriteFuture write = session.write(message);
			boolean awaitUninterruptibly2 = write.awaitUninterruptibly(200);
			if(!awaitUninterruptibly2){
				throw new DongluHWException("发送消息超时",write.getException());
			}
			LOGGER.info("发送消息:{}", ByteUtils.byteArrayToHexString(sendBytes));
			session.getConfig().setUseReadOperation(true);
			ReadFuture read = session.read();
			boolean awaitUninterruptibly = read.awaitUninterruptibly(waitTime);
			if(!awaitUninterruptibly){
				throw new DongluHWException("等待消息超时",read.getException());
			}
			Message<?> readMsg = (Message<?>) read.getMessage();
			if(readMsg.getBody() instanceof ScreenVoiceDoorBody){				
				LOGGER.info("收到消息:{}", ByteUtils.byteArrayToHexString(readMsg.toBytes()));
			}
			session.getConfig().setUseReadOperation(false);
			
			return readMsg;
		}catch (Exception e){
			this.ioConnector.dispose(true);
			this.ioConnector = null;
			throw e;
		}finally {
			close();
		}
	}
	
	public Message<?> sendMessage(Message<?> message) {
		return sendMessage(message,300);
	}

	public synchronized void sendMessageNoReturn(Message<?> message) {
		try {
			open();
			WriteFuture write = session.write(message);
			write.awaitUninterruptibly(100);
		}finally {
			close();
		}

	}

}
