package dk.orda.seedserver.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Hashtable;

@XmlRootElement(name = "seedmap")
public class SeedMapping {
    private Hashtable<String, PeerMap> map;

    public SeedMapping() {
        map = new Hashtable<>();
    }

    public Hashtable<String, PeerMap> getMap() {
        return map;
    }

    public void setMap(Hashtable<String, PeerMap> map) {
        this.map = map;
    }
}
