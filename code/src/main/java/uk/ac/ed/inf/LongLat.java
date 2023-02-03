package uk.ac.ed.inf;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfJoins;

import java.awt.geom.Line2D;
import java.util.List;

public class LongLat {
    /**
     * the longitude of the current position
     */
    public double longitude;
    /**
     * the latitude of the current position
     */
    public double latitude;
    /**
     * the GeoJson Point value of the current location
     */
    public Point point;
    /**
     * the distance value of one move that is made by the drone in degrees
     */
    public static double oneMove = 0.00015;


    //the coordinates of the edges of the confinement area
    /**
     * the longitude values of either end of the confinement area
     */
    public static  double confAreaRight = -3.184319;
    public static  double confAreaLeft = -3.192473;
    /**
     * the latitude values of either end of the confinement area
     */
    public static  double confAreaTop = 55.946233;
    public static  double confAreaBottom = 55.942617;

    /**
     * the coordinates of appleton
     */
    public static LongLat appleton = new LongLat(-3.186874,55.944494);




    //Standard Contructor
    /**
     * The Constructor for the LongLat class which accepts 2 double precision numbers which are the
     * longitude and latitude
     * @param longitude a double precision number that represents the longitude of a specific point
     * @param latitude a double precision number that represents the latitude of a specific point
     */
    LongLat(double longitude,double latitude){
        this.longitude = longitude;
        this.latitude = latitude;
        this.point = Point.fromLngLat(this.longitude,this.latitude);
    }

    //.................................................................................................................
    //.................................................................................................................
    //Public Methods

    /**
     * A Method that states if the drone is within the drone confinement area.
     * @return true if the drone is within the confinement area and false otherwise
     */
    public boolean isConfined(){

        //checks if the drone is in the confinement area.
        return ((longitude< confAreaRight && longitude> confAreaLeft) && (latitude< confAreaTop && latitude> confAreaBottom));
    }

    /**
     * A Method that calculates the Pythagorean distance between the point l1 and the current point of the instance
     * @param l1 a LongLat object
     * @return the Pythagorean distance between the LongLat object l1 and the current point of the instance
     */
    public double distanceTo(LongLat l1){
        if(l1==null){
            System.err.println("Input cannot be null");
        }
        LongLat l2 = new LongLat(longitude,latitude);
        //calculates the Pythagorean distance between the 2 points.
        return Math.sqrt(Math.pow((l1.latitude-l2.latitude),2)+Math.pow((l1.longitude-l2.longitude),2));

    }

    /**
     * A method which checks if a LongLat object is close to (distance is less than 0.00015) the current point of the instance.
     * @param l1 a LongLat object
     * @return true if the Longlat object is close to the current point of the instance and false otherwise.
     */
    public boolean closeTo(LongLat l1){
        return distanceTo(l1)<0.00015;
    }

    /**
     *  A method that shows the next position that the drone would be in if it makes one move (moves a distance of 0.00015)
     *  in the current angle that it is facing.
     * @param angle the current angle that the drone is facing
     * @return a LongLat object that represents the next position the drone would be in if it makes one move in the angle
     * given as a parameter
     */
    public LongLat nextPosition(int angle){

        // the length in degrees of 1 move.
        final double dist = 0.00015;
        // if the angle is -999 then the drone is just hovering and does not move anywhere
        if(angle == -999){
            return new LongLat(longitude,latitude);
        }
        if(angle==0){
            return new LongLat(longitude+0.00015,latitude);
        }
        // the angle is between 0 and 90
        //yMoved= distance moved vertically
        //xMoved = distance moved horizontally
        else if(angle<90){
            double radians = Math.toRadians(angle);
            double xMoved = dist * Math.cos(radians);
            double yMoved = dist * Math.sin(radians);
            return new LongLat(longitude+xMoved,latitude+yMoved);
        }
        else if(angle ==90){
            return new LongLat(longitude,latitude+0.00015);
        }
        //the angle is between 90 and 180
        else if(angle<180){
            angle = angle - 90;
            double radians = Math.toRadians(angle);
            double xMoved = dist * Math.sin(radians);
            double yMoved = dist * Math.cos(radians);
            return new LongLat(longitude-xMoved,latitude+yMoved);
        }
        else if(angle ==180){
            return new LongLat(longitude-0.00015,latitude);
        }
        // the angle is between 180 and 270
        else if(angle<270){
            angle = angle - 180;
            double radians = Math.toRadians(angle);
            double xMoved = dist * Math.cos(radians);
            double yMoved = dist * Math.sin(radians);
            return new LongLat(longitude-xMoved,latitude-yMoved);
        }
        else if(angle==270){
            return new LongLat(longitude,latitude-0.00015);
        }
        // the angle is between 270 and 360
        else{
            angle = angle -270;
            double radians = Math.toRadians(angle);
            double xMoved = dist * Math.sin(radians);
            double yMoved = dist * Math.cos(radians);
            return new LongLat(longitude+xMoved,latitude-yMoved);
        }
    }


    /**
     * the method checks if a drone is about to cross into the no fly zone. it checks if the line that its movement
     * makes to the next position crosses any of the edges of any of the polygons of the no fly zones.
     * @param nextPosition the longlat object of the next position that the drone will be in
     * @return boolean if the line made by the drones movements crosses any of the edges of the no fly zone polygons.
     */
    public boolean inNoFlyZone(LongLat nextPosition){
        if(nextPosition==null){
            System.err.println("Input cannot be null");
        }
        LongLat currentPosition = new LongLat(longitude,latitude);
        List<Feature> noFlyZones = GeoJsonParser.noFlyZoneFeatures;
        //the line the drone would make as it moves to the next position
        Line2D movement = new Line2D.Double(currentPosition.longitude,currentPosition.latitude,nextPosition.longitude,nextPosition.latitude);
        // for each polygon that is in the no fly zone
        for (Feature noFlyZone : noFlyZones) {
            if (noFlyZone.geometry() != null) {
                Polygon polygon = (Polygon) noFlyZone.geometry();

                for (int j = 0; j < polygon.coordinates().get(0).size() - 1; j++) {
                    int nextI = j + 1;
                    List<Double> coordinates1 = polygon.coordinates().get(0).get(j).coordinates();
                    List<Double> coordinates2 = polygon.coordinates().get(0).get(nextI).coordinates();
                    //for each coordinate in each of the polygons, it creates a line between the current coordinate and the next and check if
                    // the drones line crosses that line
                    Line2D edge = new Line2D.Double(coordinates1.get(0), coordinates1.get(1), coordinates2.get(0), coordinates2.get(1));
                    //if it does cross the line, the loop breaks and it returns true;
                    if (movement.intersectsLine(edge)) {
                        return true;
                    }
                }

            }
        }
        // if all of the polygons have been checked and the loop hasnt been brokem then the drone does not cross into the no fly
        //zone so false is returned
        return false;
    }


    /**
     * the method calculates the angle between the current  position of the drone and the next position of the drone
     * @param nextNode the next position of the drone
     * @return the integer value of the angle between the two positions
     */
    public int angleDirectionTo(LongLat nextNode){
        if(nextNode==null){
            System.err.println("Input cannot be null");
        }
        // calculates the angle between the 2 postions
        double angleDirection = Math.toDegrees(Math.atan2((nextNode.longitude-longitude),(nextNode.latitude-latitude)));
        // if it is negative, add 360
        if(angleDirection<0){
            angleDirection = angleDirection +360;
        }
        //returns the integer value of the angles rounded to nearest 10
        return (int) Math.round(angleDirection);
    }

    /**
     * the method checks if the current point is in any of the no fly zone polygons
     * @return boolean of whether the current point is in the no fly zone
     */
    public boolean inNoFlyPolygon(){
        List<Feature> zones= GeoJsonParser.noFlyZoneFeatures;
        //for each polygon, it check if the angle is in the polygon with TurfJoins
        for(Feature zone :zones){
            Polygon currzone = (Polygon) zone.geometry();
            if(currzone!=null){
                if(TurfJoins.inside(point,currzone)){
                    return true;
                }
            }

        }
        return false;
    }

}
