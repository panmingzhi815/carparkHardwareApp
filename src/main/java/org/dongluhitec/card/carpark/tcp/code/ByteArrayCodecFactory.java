package org.dongluhitec.card.carpark.tcp.code;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 * Created by xiaopan on 2016-05-05 0005.
 */
public class ByteArrayCodecFactory implements ProtocolCodecFactory {

    private ByteArrayDecoder decoder;
    private ByteArrayEncoder encoder;

    public ByteArrayCodecFactory() {
        encoder = new ByteArrayEncoder();
        decoder = new ByteArrayDecoder();
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return decoder;
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return encoder;
    }

}
