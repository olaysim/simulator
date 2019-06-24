package dk.orda.overlaynetwork.net.rpc.server;

import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
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
public class PingHandler extends SimpleChannelInboundHandler<RpcMessages.Ping> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private StatLogger statLogger;

    public PingHandler() {
        this.statLogger = null;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessages.Ping msg) throws Exception {
        long ttimestamp = System.currentTimeMillis();
        long timeStart = System.nanoTime();
        //DhtStates dhtStates = new DhtStates();

        String target  = msg.getHeader().getTargetId();
        String sender  = msg.getHeader().getSenderId();
        long timestamp = msg.getTimestamp();

        RpcMessages.Header header = RpcMessages.Header.newBuilder()
            .setCorrelationId(msg.getHeader().getCorrelationId())
            .setTargetId(sender)
            .setSenderId(target)
            .build();
        RpcMessages.Ping pingReply = RpcMessages.Ping.newBuilder()
            .setHeader(header)
            .setTimestamp(System.currentTimeMillis())
            .build();

        long total = System.nanoTime() - timeStart;
//        addToStats(ttimestamp, total);
        //dhtStates.add(((DistributedHashTableImpl)overlayNetworkReference.getDHT()).getFormattedKBuckets());
        //dhtStates.addToStatLogger(statConfiguration, "serverhandler_dhtstates");
        ctx.writeAndFlush(pingReply);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error in receiving Find Node request", cause);
        ctx.close();
    }

    @SuppressWarnings("Duplicates")
    private void addToStats(long timestamp, long total) {
        List<Value> list = Arrays.asList(
            new Value<>("toalmethodtime", timestamp, statLogger.getConfig(), null, total)
        );
        this.statLogger.addValues("pinghandler", list);
    }
}
