package dk.orda.overlaynetwork.overlay.peer;

import dk.orda.overlaynetwork.net.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Peer {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

//    private String listenAddress;
//    private int
    private Address address;
    private InetSocketAddress localAddress;
    private boolean useHolePunch;
    private boolean useRelay;
//    InetAddress addr;
//     public void test() {
//         sock = new InetSocketAddress();
//     }


    public Peer() {}

    public Peer(Peer peer) {
        this.address = peer.address;
        this.localAddress = peer.localAddress;
        this.useHolePunch = peer.useHolePunch;
        this.useRelay = peer.useRelay;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return address.toString();
    }
}
