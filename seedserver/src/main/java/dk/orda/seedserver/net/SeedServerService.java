package dk.orda.seedserver.net;

import dk.orda.overlaynetwork.net.Address;
import dk.orda.overlaynetwork.net.seed.PeerSeed;
import dk.orda.overlaynetwork.net.seed.Seeds;
import dk.orda.overlaynetwork.net.tcp.TcpServer;
import dk.orda.overlaynetwork.overlay.OverlayFactory;
import dk.orda.overlaynetwork.overlay.OverlayNetwork;
import dk.orda.overlaynetwork.overlay.RequestOverlay;
import dk.orda.overlaynetwork.overlay.dht.Node;
import dk.orda.seedserver.model.PeerMap;
import dk.orda.seedserver.model.SeedMapping;
import dk.orda.seedserver.util.XmlSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class SeedServerService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final static int PORT = 5007;

    private Seeds seeds;
    private TcpServer server;
    private XmlSerializer xmlSerializer;

    @Autowired
    public SeedServerService(XmlSerializer xmlSerializer) throws InterruptedException, IOException {
        this.xmlSerializer = xmlSerializer;
        this.seeds = new Seeds();
        initializeSeedMap();
        Address address = new Address(PORT, true);
        this.server = new TcpServer(address, null, 0L, 0L, seeds);
        this.server.start();
    }

    private void initializeSeedMap() throws IOException {
        SeedMapping map = (SeedMapping) xmlSerializer.xmlToObject(Paths.get("seedmap.xml").toString());
        for (Map.Entry<String, PeerMap> entry : map.getMap().entrySet()) {
            seeds.addConnection(entry.getKey(), entry.getValue().getPeers());
            System.out.println(entry.getKey() + ", " + entry.getValue().getPeers());
        }
    }
}
