package uk.ac.ed.inf;

/**
 * the class for the flighpath object which will be used to populate the flightpath database later.
 */
public class FlightPath {
    /**
     * the order number of the order that the drone is completing for this current move
     */
    String orderNo ;
    /**
     * the longitude of the drone in its previous position
     */
    double fromLongitude;

    double fromLatitude ;
    /**
     * the angle the drone took to move from its previous position to current position. if it is -999 then the drone is hovering
     */
    int angle ;
    /**
     * the longitude of the drone in its current position
     */
    double toLongitude ;
    /**
     * the latitude of the drone in its current position
     */
    double toLatitude ;
}
