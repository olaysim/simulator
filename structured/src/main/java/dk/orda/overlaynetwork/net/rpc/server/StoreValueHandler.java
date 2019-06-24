package dk.orda.overlaynetwork.net.rpc.server;

import com.google.protobuf.ByteString;
import dk.orda.overlaynetwork.net.rpc.MessageType;
import dk.orda.overlaynetwork.net.rpc.SuperNode;
import dk.orda.overlaynetwork.net.rpc.protobuf.ProtoMessageWrapper;
import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import dk.orda.overlaynetwork.overlay.dht.DistributedHashTableImpl;
import dk.orda.overlaynetwork.overlay.dht.Number160;
import dk.orda.overlaynetwork.statistics.DhtStates;
import dk.orda.overlaynetwork.statistics.StatConfiguration;
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
public class StoreValueHandler extends SimpleChannelInboundHandler<RpcMessages.StoreValue> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final SuperNode sn;
    private StatLogger statLogger;

    public StoreValueHandler(SuperNode reference) {
        this.sn = reference;
        this.statLogger = reference.getStatLogger();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessages.StoreValue msg) throws Exception {
        long timestamp = System.currentTimeMillis();
        long timeStart = System.nanoTime();
        DhtStates dhtStates = new DhtStates(statLogger);

        Number160 queryId  = new Number160(statLogger, msg.getHeader().getQueryId());
        Number160 senderId = new Number160(statLogger, msg.getHeader().getSenderId());
        Number160 key = new Number160(statLogger, msg.getKey());
        ByteString bs = msg.getValue();
        String result = "0";

        RequestOverlay reqo = (RequestOverlay) sn.getOverlayNetwork(queryId);
        if (reqo == null) {
            log.error("Unable to get overlaynetwork from supernode");
            throw new IllegalArgumentException("Unable to find requested overlay network on this node");
        }

        if (bs != null && !bs.isEmpty()) {
            byte[] bytes = bs.toByteArray();
            reqo.setValue(key, bytes);
            result = "1";
        }

        RpcMessages.ValueOrNodes reply = RpcMessages.ValueOrNodes.newBuilder()
            .setHeader(RpcMessages.Header.newBuilder().setCorrelationId(msg.getHeader().getCorrelationId()).build())
            .setValue(ByteString.copyFromUtf8(result))
            .build();
        ProtoMessageWrapper wrapper = new ProtoMessageWrapper(MessageType.REPLY_VALUE_OR_NODES, reply);
        long total = System.nanoTime() - timeStart;
        addToStats(timestamp, total, bs != null ? bs.size() : 0);
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
    private void addToStats(long timestamp, long total, int bytesize) {
        List<Value> list = Arrays.asList(
            new Value<>("bytesize", timestamp, statLogger.getConfig(), null, bytesize),
            new Value<>("toalmethodtime", timestamp, statLogger.getConfig(), null, total)
        );
        this.statLogger.addValues("storevaluehandler", list);
    }
}
