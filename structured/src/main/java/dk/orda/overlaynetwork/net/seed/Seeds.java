package dk.orda.overlaynetwork.net.seed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Seeds {
    private Map<String, PeerSeed> peers;
    private Map<String, List<String>> connections;

    public Seeds() {
        peers = new HashMap<>();
        connections = new HashMap<>();
    }

    public List<PeerSeed> getPeersFor(String name) {
        if (peers.get(name) == null) return null;
        if (connections.get(name) == null) return null;
        List<String> peerlist = connections.get(name);
        List<PeerSeed> res = new ArrayList<>();
        for (String p : peerlist) {
            PeerSeed ps = peers.get(p);
            if (ps == null) return null;
            res.add(ps);
        }
        return res;
    }

    public void addConnection(String name, List<String> peers) {
        connections.put(name, peers);
    }

    public void addPeer(String name, String address, int port) {
        PeerSeed ps = new PeerSeed();
        ps.setId(name);
        ps.setAddress(address);
        ps.setPort(port);
        peers.put(name, ps);
    }

}
