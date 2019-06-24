package dk.orda.overlaynetwork.net.rpc.server;

import dk.orda.overlaynetwork.net.rpc.MessageType;
import dk.orda.overlaynetwork.net.rpc.protobuf.ProtoMessageWrapper;
import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
import dk.orda.overlaynetwork.net.seed.PeerSeed;
import dk.orda.overlaynetwork.net.seed.Seeds;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@ChannelHandler.Sharable
public class SeedHandler extends SimpleChannelInboundHandler<RpcMessages.GetSeeds> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final Seeds seeds;

    public SeedHandler(Seeds seeds) {
        this.seeds = seeds;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessages.GetSeeds msg) throws Exception {
        RpcMessages.SeedsOrStatus reply;

        // update seeds
        seeds.addPeer(msg.getId(), msg.getAddress(), msg.getPort());

        // query seeds
        List<PeerSeed> peers = seeds.getPeersFor(msg.getId());
        if (peers == null) {
            reply = RpcMessages.SeedsOrStatus.newBuilder()
                .setReady(false)
                .setCorrelationId(msg.getCorrelationId())
                .build();
        } else {
            List<RpcMessages.Peer> resPeers = new ArrayList<>();
            for (PeerSeed ps : peers) {
                RpcMessages.Peer p = RpcMessages.Peer.newBuilder()
                    .setId(ps.getId())
                    .setAddress(ps.getAddress())
                    .setPort(ps.getPort())
                    .build();
                resPeers.add(p);
            }
            reply = RpcMessages.SeedsOrStatus.newBuilder()
                .setReady(true)
                .addAllPeers(resPeers)
                .setCorrelationId(msg.getCorrelationId())
                .build();
        }

        ProtoMessageWrapper wrapper = new ProtoMessageWrapper(MessageType.REPLY_SEEDS_OR_STATUS, reply);
        ctx.writeAndFlush(wrapper);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Error in receiving Get Seeds request", cause);
        ctx.close();
    }
}
