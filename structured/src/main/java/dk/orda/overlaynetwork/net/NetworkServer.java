package dk.orda.overlaynetwork.net;

import dk.orda.overlaynetwork.net.broadcast.NodeEvent;
import dk.orda.overlaynetwork.net.broadcast.NodeEventBroadcaster;
import dk.orda.overlaynetwork.net.broadcast.NodeEventListener;
import dk.orda.overlaynetwork.net.broadcast.NodeEventMonitor;
import dk.orda.overlaynetwork.net.rpc.MessageType;
import dk.orda.overlaynetwork.net.rpc.Protocol;
import dk.orda.overlaynetwork.net.rpc.SuperNode;
import dk.orda.overlaynetwork.net.rpc.client.SeedsOrStatusFuture;
import dk.orda.overlaynetwork.net.rpc.protobuf.RpcMessages;
import dk.orda.overlaynetwork.net.tcp.TcpClient;
import dk.orda.overlaynetwork.net.tcp.TcpServer;
import dk.orda.overlaynetwork.overlay.KademliaOverlay;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import dk.orda.overlaynetwork.overlay.dht.Node;
import dk.orda.overlaynetwork.overlay.dht.Number160;
import dk.orda.overlaynetwork.overlay.peer.Peer;
import dk.orda.overlaynetwork.statistics.StatConfiguration;
import dk.orda.overlaynetwork.statistics.StatLogger;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class NetworkServer {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static NodeEventBroadcaster broadcaster;
    private static NodeEventMonitor monitor;
    private ExecutorService broadcastExecutor;
    private NetworkConfiguration config;
    private TcpServer server;
    private StatLogger statLogger;

    public NetworkServer(NetworkConfiguration config, StatLogger statLogger) {
        this.config = config;
        this.statLogger = statLogger;
    }

    public synchronized void start(SuperNode sn) {
        if (config == null || sn == null) {
            log.error("NetworkServer not initialized correctly, please set config and supernode");
            throw new RuntimeException("NetworkServer not initialized correctly, please set config and supernode");
        }

        // start local broadcasting of node on LAN
        if (config.isLocalBroadcasting()) {
            try {
                broadcastExecutor = Executors.newSingleThreadExecutor();
                // only one broadcast instance pr machine, so reuse if it already exists
                if (broadcaster == null) {
                    broadcaster = new NodeEventBroadcaster(new InetSocketAddress("255.255.255.255", NetworkConfiguration.BROADCAST_PORT));
                    broadcaster.start();
                }
                if (monitor == null) {
                    monitor = new NodeEventMonitor(new InetSocketAddress(NetworkConfiguration.BROADCAST_PORT));
                    monitor.bind();
                }

                // add listener to monitor
                NodeEventListener listener = new NodeEventListener() {
                    @Override
                    public void NodeEventReceived(NodeEvent event) {
                        Node node = new Node(event.getNodeId());
                        Peer peer = new Peer();
                        peer.setAddress(new Address(event.getIpAddress(), event.getPort()));
                        node.setPeer(peer);
//                        log.debug("LISTENER (" + overlay.getSelf().getId().toString() + "): Got broadcast from: " + event.getIpAddress() + ":" + event.getPort() + ", nodeid: " + event.getNodeId().toString());
                        sn.updateAllOverlaysWithNode(node);
                    }
                };
                monitor.addNodeEventListener(listener);

                // add event loop to broadcaster
                broadcastExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        while (!Thread.interrupted()) {
                            try {
                                for (RequestOverlay overlay : sn.getOverlayList()) {
                                    Peer peer = overlay.getSelf().getPeer();
                                    NodeEvent event = new NodeEvent(peer.getAddress().getAddress(), peer.getAddress().getPort().getPort(), overlay.getSelf().getId());
                                    broadcaster.sendEvent(event);
                                }
                                TimeUnit.SECONDS.sleep(config.getLocalBroadcastInterval());
                            } catch (InterruptedException e) {
                                log.warn("Failed to sleep broadcaster thread!", e);
                            }
                        }
                    }
                });

            } catch (InterruptedException e) {
                log.error("Failed to start node broadcast monitor");
                broadcaster.stop();
                monitor.stop();
            }
        }

        // get nodes from seed server
        if (config.isSeedServer()) {
            for (RequestOverlay overlay : sn.getOverlayList()) {
                getSeeds(overlay);
            }
        }

        // Start TCP Server
        try {
            if (server != null) {
//                log.info("TCP server already started, probably because a virtual network was added");
                return;
            }
            Address address = sn.getAddress();
            server = new TcpServer(address, sn);
            server.start();
        } catch (InterruptedException e) {
            log.error("failed to start tcp server", e);
        }

    }

    private void getSeeds(RequestOverlay overlay) {
        TcpClient client = new TcpClient();
        Channel ch = client.connect(new InetSocketAddress(config.getSeedServerAddress(), config.getSeedServerPort()), Protocol.SEEDS);
        UUID uuid = UUID.randomUUID();
        String tmpid = overlay.getSelf().getId().toString();
        if (config.getLocalSystemName() != null && !config.getLocalSystemName().isEmpty()) {
            tmpid = config.getLocalSystemName();
        }
        final String id = tmpid;

        boolean trucking = true;
        while (trucking) {
            try {
                RpcMessages.GetSeeds getSeeds = RpcMessages.GetSeeds.newBuilder()
                    .setCorrelationId(uuid.toString())
                    .setAddress(overlay.getSelf().getPeer().getAddress().getAddress())
                    .setId(id)
                    .setPort(overlay.getSelf().getPeer().getAddress().getPort().getPort())
                    .build();

                SeedsOrStatusFuture f = client.sendSeedRequest(ch, getSeeds, MessageType.GET_SEEDS, uuid);
                RpcMessages.SeedsOrStatus result = f.get(500, TimeUnit.MILLISECONDS);

                if (result.getReady()) {
                    for (RpcMessages.Peer peer : result.getPeersList()) {
                        Node n = new Node(Number160.createHash(statLogger, peer.getId()), peer.getIdStr());
                        Peer p = new Peer();
                        Address a = new Address(peer.getAddress(), peer.getPort());
                        p.setAddress(a);
                        n.setPeer(p);
                        overlay.updateNode(n);
                    }
                    trucking = false;
                    System.out.println("DHT has " + ((KademliaOverlay)overlay).getKBucketCount() + " nodes");
                } else {
                    log.warn("No usable result from get seeds, trying again");
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                log.error("TODO interrupted", e);
            } catch (ExecutionException e) {
                log.error("TODO exception", e);
            } catch (TimeoutException e) {
                log.error("Failed to get seeds, retrying");
            }
        }
        client.shutdown();
    }

    public void reset(List<RequestOverlay> overlays) {
        for (RequestOverlay overlay : overlays) {
            // get nodes from seed server
            if (config.isSeedServer()) {
                getSeeds(overlay);
            } else {
                System.out.println("NOT USING SEED SERVER");
            }
        }
    }

    @SuppressWarnings("Duplicates")
    public void stop() {
        // stop TCP server
        server.stop();

        // stop broadcasting (will stop for ALL instances... just know this)
        if (config.isLocalBroadcasting()) {
            try {
                broadcastExecutor.shutdown();
                broadcastExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.debug("broadcastExecutor took too long to shut down, interrupting thread.");
            } finally {
                if (!broadcastExecutor.isTerminated()) {
                    broadcastExecutor.shutdownNow();
                }
            }
        }
    }

    public void addNodeBroadcastListener(NodeEventListener listener) {
        if (monitor != null) {
            monitor.addNodeEventListener(listener);
        }
    }

    public void removeNodeBroadcastListener(NodeEventListener listener) {
        if (monitor != null) {
            monitor.removeNodeEventListener(listener);
        }
    }
}
