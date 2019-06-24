package dk.orda.overlaynetwork.overlaygeo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.*;

/**
 * This routine calculates the distance between two points (given the
 * latitude/longitude of those points). It is being used to calculate
 * the distance between two locations using GeoDataSource (TM) prodducts
 *
 * Definitions:
 *   South latitudes are negative, east longitudes are positive
 *
 * Passed to function:
 *   lat1, lon1 = Latitude and Longitude of point 1 (in decimal degrees)
 *   lat2, lon2 = Latitude and Longitude of point 2 (in decimal degrees)
 *   unit = the unit you desire for results
 *          where: 'M' is statute miles (default)
 *                 'K' is kilometers
 *                 'N' is nautical miles
 * Worldwide cities and other features databases with latitude longitude
 * are available at https://www.geodatasource.com
 *
 * For enquiries, please contact sales@geodatasource.com
 *
 * Official Web site: https://www.geodatasource.com
 *
 *          GeoDataSource.com (C) All Rights Reserved 2017
 */
public class DistanceCalculator {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        return distance(lat1, lon1, lat2, lon2, "K");
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit.equalsIgnoreCase("K")) {
            dist = dist * 1.609344;
        } else if (unit.equalsIgnoreCase("N")) {
            dist = dist * 0.8684;
        }

        return (dist);
    }

    /**
     * This function converts decimal degrees to radians
     */
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /**
     * This function converts radians to decimal degrees
     */
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }


    /**
     * returns distance in meters
     * assumes the world is flat, but is fast, use for short distances only
     * works by approximating the distance of a single Latitude and Longitude at the given Latitude and returning the Pythagorean distance in meters
     */
    public static double flatEarthDistance(double lat1, double lon1, double lat2, double lon2) {
        double a = (lat1 - lat2) * distPerLat(lat1);
        double b = (lon1 - lon2) * distPerLng(lat1);
        return Math.sqrt(a * a + b * b);
    }

    private static double distPerLng(double lat) {
        return 0.0003121092 * Math.pow(lat, 4)
            +0.0101182384 * Math.pow(lat, 3)
            -17.2385140059 * lat * lat
            +5.5485277537 * lat + 111301.967182595;
    }

    private static double distPerLat(double lat){
        return -0.000000487305676 * Math.pow(lat, 4)
            -0.0033668574 * Math.pow(lat, 3)
            +0.4601181791 * lat * lat
            -1.4558127346 * lat + 110579.25662316;
    }


    /**
     * The distance between two points formula derived from the Pythagorean Theorem
     */
    public static double pythagoreanDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow((x1-x2), 2) + Math.pow((y1-y2), 2));
    }
}
