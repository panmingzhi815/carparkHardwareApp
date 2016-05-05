package org.dongluhitec.card.carpark.tcp.code;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * Created by xiaopan on 2016-05-05 0005.
 */
public class ByteArrayEncoder extends ProtocolEncoderAdapter {

    @Override
    public void encode(IoSession session, Object message,
                       ProtocolEncoderOutput out) throws Exception {
        // TODO Auto-generated method stub
        byte[] bytes = (byte[])message;

        IoBuffer buffer = IoBuffer.allocate(256);
        buffer.setAutoExpand(true);

        buffer.put(bytes);
        buffer.flip();

        out.write(buffer);
        out.flush();

        buffer.free();
    }
}
