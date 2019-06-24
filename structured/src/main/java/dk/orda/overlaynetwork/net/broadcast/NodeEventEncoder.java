package dk.orda.overlaynetwork.net.broadcast;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

public class NodeEventEncoder extends MessageToMessageEncoder<NodeEvent> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final InetSocketAddress remoteAddress;

    public NodeEventEncoder(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, NodeEvent msg, List<Object> out) throws Exception {
        byte[] ipaddress = msg.getIpAddress().getBytes(CharsetUtil.UTF_8);
        byte[] nodeId = msg.getNodeIdBytes();
        byte[] port = msg.getPortBytes();
//        ByteBuf buf = ctx.alloc().buffer(ipaddress.length + Integer.BYTES + nodeId.length + 1);
//        buf.writeBytes(ipaddress);
//        buf.writeByte(NodeEvent.SEPERATOR);
//        buf.writeInt(msg.getPort());
//        buf.writeBytes(nodeId);
//        out.add(new DatagramPacket(buf, remoteAddress));

        byte[] arr = new byte[ipaddress.length + port.length + nodeId.length + 1];
        System.arraycopy(ipaddress, 0, arr, 0, ipaddress.length);
        arr[ipaddress.length] = NodeEvent.SEPERATOR;
        System.arraycopy(port, 0, arr, ipaddress.length + 1, port.length);
        System.arraycopy(nodeId, 0, arr, ipaddress.length + 1 + port.length, nodeId.length);
        ByteBuf buf = Unpooled.copiedBuffer(arr);
        out.add(new DatagramPacket(buf, remoteAddress));
    }


}
