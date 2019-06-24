package dk.orda.overlaynetwork.net.broadcast;

import dk.orda.overlaynetwork.overlay.dht.Number160;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class NodeEventDecoder extends MessageToMessageDecoder<DatagramPacket> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        try {
            ByteBuf buf = msg.content();
            int idx = buf.indexOf(0, buf.readableBytes(), NodeEvent.SEPERATOR);
            String ipaddress = buf.readBytes(idx).toString(CharsetUtil.UTF_8);
            buf.readByte();
            int port = buf.readInt();
            byte[] nodeid = new byte[buf.readableBytes()];
            buf.readBytes(nodeid);

//            try {
//                ReferenceCountUtil.release(msg);
//            } catch (Exception e) {
//                // ignore
//            }

            NodeEvent event = new NodeEvent(ipaddress, port, nodeid);
            out.add(event);
        } catch (Exception ignore) {
            // if the message can't be parsed, throw it away
            log.debug("Received broadcast message which could not be parsed.", ignore);
        }
    }
}
