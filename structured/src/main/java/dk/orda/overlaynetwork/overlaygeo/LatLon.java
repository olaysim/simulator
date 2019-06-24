package dk.orda.overlaynetwork.overlaygeo;

import java.util.Objects;

public class LatLon {
    private double lat;
    private double lon;

    public LatLon() {}

    public LatLon(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    @Override
    public String toString() {
        return "LatLon{" +
            "lat=" + lat +
            ", lon=" + lon +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LatLon)) return false;
        LatLon that = (LatLon) o;
        return Double.compare(that.lat, lat) == 0 &&
            Double.compare(that.lon, lon) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lat, lon);
    }
}
