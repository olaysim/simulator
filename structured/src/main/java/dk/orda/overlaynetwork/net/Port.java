package dk.orda.overlaynetwork.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Random;

public class Port {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final int MAX_PORT = 65535;
    private static final int MIN_DYN_PORT = 49152; // IANA defines port range 49152-65535 as ephemeral range for dynamic services
    private static final int RANGE = MAX_PORT - MIN_DYN_PORT;

    private int port = 0;

    public Port() { }

    public Port(final int port) {
        if (port < 1024 || port > 65535) {
            throw new IllegalArgumentException("Port must be within range 1024-65535");
        }
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public static Port getAvailablePort() {
        Random random = new Random();
        Port port = null;
        while (port == null) {
            int testPort = random.nextInt(RANGE) + MIN_DYN_PORT;
            if (available(testPort)) {
                port = new Port(testPort);
            }
        }
        return port;
    }

    /**
     * Checks to see if a specific port is available.
     *
     * @param port the port to check for availability
     */
    private static boolean available(int port) {
        if (port < 1024 || port > 65535) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                /* should not be thrown */
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Port: " + port;
    }
}
