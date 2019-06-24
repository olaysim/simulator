package dk.orda.overlaynetwork.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Address {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private String address;
    private String listenAddress;
    private Port port;

    public Address() {
        this(getPreferredAddress());
    }

    public Address(boolean preferIPv4) {
        this(getPreferredAddress(), preferIPv4);
    }

    public Address(String address) {
        this(address, false);
    }

    public Address(String address, boolean preferIPv4) {
        this(address, Port.getAvailablePort().getPort(), preferIPv4);
    }

    public Address(String address, String listenAddress) {
        this(address, listenAddress, Port.getAvailablePort().getPort());
    }

      public Address(String address, String listenAddress, int port) {
        this.listenAddress = listenAddress;
        this.address = address;
        this.port = new Port(port);
    }

    public Address(int port) {
        this(port, false);
    }

    public Address(int port, boolean preferIPv4) {
        this(Address.getPreferredAddress(), port, preferIPv4);
    }

    public Address(String address, int port) {
        this(address, port, false);
    }

    public Address(String address, int port, boolean preferIPv4) {
        this.address = address;
        this.port = new Port(port);
        try {
            listenAddress = (isIPv4() || preferIPv4) ? "0.0.0.0" : "::";
        } catch (Exception e) {
            listenAddress = "0.0.0.0";
        }
    }




    public String getAddress() {
        return address;
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public Address setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
        return this;
    }

    public Port getPort() {
        return port;
    }


    public static String getPreferredAddress() {
        try {
            final DatagramSocket socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (UnknownHostException | SocketException e) {
            return "127.0.0.1"; // error
        }
    }

    public static List<String> getAllAddresses() {
        List<String> list = new ArrayList<>();
        try {
            for (Enumeration nis = NetworkInterface.getNetworkInterfaces(); nis.hasMoreElements();) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                for (Enumeration ias = ni.getInetAddresses(); ias.hasMoreElements();) {
                    InetAddress ia = (InetAddress)ias.nextElement();
                    if (!ia.isLoopbackAddress()) {
                        if (ia.isSiteLocalAddress()) {
                            list.add(ia.getHostAddress());
                        }
                    }
                }

            }
        } catch (SocketException e) {
            // ignore
        }
        return list;
    }

    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(address, port.getPort());
    }

    public Boolean isIPv6() throws UnknownHostException {
        return InetAddress.getByName(address) instanceof Inet6Address;
    }

    public Boolean isIPv4() throws UnknownHostException {
        return InetAddress.getByName(address) instanceof Inet4Address;
    }

    @Override
    public String toString() {
        return "Address: " + address + " " + port.toString();
    }
}
