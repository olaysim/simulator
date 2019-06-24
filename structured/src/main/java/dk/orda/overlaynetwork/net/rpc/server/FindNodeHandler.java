package dk.orda.overlaynetwork.net.rpc.server;

import dk.orda.overlaynetwork.net.rpc.MessageType;
import dk.orda.overlaynetwork.net.rpc.SuperNode;
import dk.orda.overlaynetwork.net.rpc.protobuf.ProtoMessageWrapper;
import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import dk.orda.overlaynetwork.overlay.dht.DistributedHashTableImpl;
import dk.orda.overlaynetwork.statistics.DhtStates;
import dk.orda.overlaynetwork.statistics.StatLogger;
import dk.orda.overlaynetwork.statistics.Value;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

@ChannelHandler.Sharable
public class FindNodeHandler extends SimpleChannelInboundHandler<RpcMessages.FindNode> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final SuperNode sn;
    private StatLogger statLogger;

    public FindNodeHandler(SuperNode reference) {
        this.sn = reference;
        this.statLogger = reference.getStatLogger();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessages.FindNode msg) throws Exception {
        long timestamp = System.currentTimeMillis();
        long timeStart = System.nanoTime();
        DhtStates dhtStates = new DhtStates(statLogger);

        if (sn == null) {
            log.error("SuperNode reference is not set! cannot proceed");
            System.exit(-1);
        }
        long getnodes = System.nanoTime() - timeStart;
        long startbuildreply = System.nanoTime();

        RpcMessages.ValueOrNodes reply = sn.getServerHandler().handleFindNode(null, msg);

        ProtoMessageWrapper wrapper = new ProtoMessageWrapper(MessageType.REPLY_VALUE_OR_NODES, reply);
        long buildreply = System.nanoTime() - startbuildreply;
        long total = System.nanoTime() - timeStart;
        addToStats(timestamp, getnodes, buildreply, total);
        RequestOverlay reqo = sn.getOverlayNetwork(msg.getHeader().getQueryId()).getRequestOverlay();
        dhtStates.add(((DistributedHashTableImpl)reqo.getDHT()).getFormattedKBuckets());
        dhtStates.addToStatLogger(statLogger.getConfig(), "serverhandler_dhtstates");
        ctx.writeAndFlush(wrapper);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error in receiving Find Node request", cause);
        ctx.close();
    }

    @SuppressWarnings("Duplicates")
    private void addToStats(long timestamp, long getnodes, long buildreply, long total) {
        List<Value> list = Arrays.asList(
            new Value<>("getnodes", timestamp, statLogger.getConfig(), null, getnodes),
            new Value<>("buildreply", timestamp, statLogger.getConfig(), null, buildreply),
            new Value<>("toalmethodtime", timestamp, statLogger.getConfig(), null, total)
        );
        this.statLogger.addValues("findnodehandler", list);
    }
}
