package dk.orda.overlaynetwork.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkConfiguration {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public final static int BROADCAST_PORT = 59595;

    private boolean localBroadcasting;
    private int localBroadcastInterval; // in minutes
    private boolean preferIPv4;
    private boolean seedServer;
    private String seedServerAddress;
    private int seedServerPort;
    private String localSystemName;


    public NetworkConfiguration() {
        // default configuration
        localBroadcasting = true; // enable node broadcasting on LAN
        localBroadcastInterval = 20; // during dev, it's in seconds...// default interval to 2 minutes
        preferIPv4 = true;
        seedServer = false;
        seedServerAddress = "seed.syslab.dk";
        seedServerPort = 5007;
    }




    public boolean isLocalBroadcasting() {
        return localBroadcasting;
    }

    public void setLocalBroadcasting(boolean localBroadcasting) {
        this.localBroadcasting = localBroadcasting;
    }

    public int getLocalBroadcastInterval() {
        return localBroadcastInterval;
    }

    public void setLocalBroadcastInterval(int localBroadcastInterval) {
        this.localBroadcastInterval = localBroadcastInterval;
    }

    public boolean preferIPv4() {
        return preferIPv4;
    }

    public boolean isSeedServer() {
        return seedServer;
    }

    public void setSeedServer(boolean seedServer) {
        this.seedServer = seedServer;
    }

    public String getSeedServerAddress() {
        return seedServerAddress;
    }

    public void setSeedServerAddress(String seedServerAddress) {
        this.seedServerAddress = seedServerAddress;
    }

    public int getSeedServerPort() {
        return seedServerPort;
    }

    public void setSeedServerPort(int seedServerPort) {
        this.seedServerPort = seedServerPort;
    }

    public String getLocalSystemName() {
        return localSystemName;
    }

    public void setLocalSystemName(String localSystemName) {
        this.localSystemName = localSystemName;
    }
}
