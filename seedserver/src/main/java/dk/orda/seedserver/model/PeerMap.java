package dk.orda.seedserver.model;

import java.util.ArrayList;

public class PeerMap {
    private ArrayList<String> peers;

    public PeerMap() {
        peers = new ArrayList<>();
    }

    public ArrayList<String> getPeers() {
        return peers;
    }

    public void setPeers(ArrayList<String> peers) {
        this.peers = peers;
    }
}
