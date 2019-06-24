package dk.orda.overlaynetwork.net.broadcast;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeEventHandler extends SimpleChannelInboundHandler<NodeEvent> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final List<NodeEventListener> list = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, NodeEvent msg) throws Exception {
//        log.debug("Got broadcast from: " + msg.getIpAddress() + ":" + msg.getPort() + ", nodeid: " + msg.getNodeId().toString());
        synchronized (list) {
            for (NodeEventListener listener : list) {
                listener.NodeEventReceived(msg);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error in receiving broadcast", cause);
        ctx.close();
    }

    public void addListener(NodeEventListener listener) {
        list.add(listener);
    }

    public void removeListener(NodeEventListener listener) {
        list.remove(listener);
    }
}
