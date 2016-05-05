package org.dongluhitec.card.carpark.tcp;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.util.byteaccess.SimpleByteArrayFactory;
import org.dongluhitec.card.carpark.tcp.code.ByteArrayCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by xiaopan on 2016-05-04 0004.
 */
public class TcpHardwareService extends AbstractIdleService implements Service {

    private static Logger LOGGER = LoggerFactory.getLogger(TcpHardwareService.class);

    public final int PORT = 10002;
    private NioSocketAcceptor acceptor;
    private TcpRecordCallable callable;

    public TcpHardwareService(TcpRecordCallable callable) {
        this.callable = callable;
    }

    public void init() {
        this.acceptor = new NioSocketAcceptor();
        acceptor.getFilterChain().addLast("codec",new ProtocolCodecFilter(new ByteArrayCodecFactory()));
        acceptor.setHandler(new TcpHandler(callable));
        acceptor.getSessionConfig().setReadBufferSize(2048);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
        LOGGER.info("初始化TCP记录接收底层");
    }

    @Override
    protected void startUp() throws Exception {
        if (this.acceptor == null) {
            init();
        }
        try {
            acceptor.bind(new InetSocketAddress(PORT));
            LOGGER.info("绑定TCP记录接收底层端口:{}成功", PORT);
        } catch (IOException e) {
            LOGGER.info("绑定TCP记录接收底层端口:" + PORT + "时发生异常,请检查是否该端口号是否己被占用", e);
        }
    }

    @Override
    protected void shutDown() throws Exception {
        acceptor.unbind();
        LOGGER.info("解绑TCP记录接收底层端口:{}成功", PORT);
    }
}
