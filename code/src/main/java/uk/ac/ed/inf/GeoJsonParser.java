package uk.ac.ed.inf;

import com.mapbox.geojson.*;
import org.w3c.dom.Node;

import java.io.File;
import java.io.FileWriter;

import java.io.IOException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class GeoJsonParser {


    //Public Variables.................................................................................................

    /**
     * list of featues which are the no fly zones that the dron cannot pass into
     */
    public static List<Feature> noFlyZoneFeatures;

    /**
     * a list of features which are the landmarks on the map
     */
    public static List<Feature> landmarkFeatures;


    //Private Variables.................................................................................................
    /**
     * the HTTPClient used for accessing the webserver
     */
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * the Url String which is the Url for the no fly zones in the webserver
     */
    private static final String urlStringNoFlyZones = "http://localhost:"+App.webServerPort+"/buildings/no-fly-zones.geojson";

    /**
     * the url string which is the url for the landmarks in the webserver.
     */
    private static final String urlStringLandmarks = "http://localhost:"+App.webServerPort+"/buildings/landmarks.geojson";


    //.................................................................................................................
    //.................................................................................................................
    //Public Methods that can be accessed by other classes


    /**
     * the method accesses the webserver and parses the noFlyZone geojson. each feature which represents a no fly zone
     * polygon is then stored in a list as the public variable "noFlyZoneFeatures"
     */
    public static void getNoFlyZones(){
        try{
            //the request that would be sent to the website as a http request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStringNoFlyZones))
                    .build();
            //once we sent the request, we save the response in "response"
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            //check the status code to check if the request had failed or not
            if(response.statusCode()!=200){
                System.out.println("Fatal error: Unable to connect to server at port " + App.webServerPort + ".");
                System.exit(1); // Exit the application

            }
            //the contents of the file is parsed to a feature collection
            FeatureCollection featureCollection = FeatureCollection.fromJson(response.body());
            //each feature in the feature collection is store in noFlyZoneFeatures
            noFlyZoneFeatures = featureCollection.features();
            //catches any IO Exception or interrupted exception and prints the error.
        }catch (IOException e){
            System.out.println("IOException " + e.getMessage());
        }catch (InterruptedException e){
            System.out.println("InterruptedException " + e.getMessage());
        }
    }


    /**
     *the method accesses the webserver and parses the Landmarks geojson. each feature which represents a landmark
     * point is then stored in a list as the public variable "landmarkFeatures"
     */
    public static void getLandmarks(){
        try{
            //the request that would be sent to the website as a http request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStringLandmarks))
                    .build();
            //once we sent the request, we save the response in "response"
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            //check the status code to check if the request had failed or not
            if(response.statusCode()!=200){
                System.out.println("Fatal error: Unable to connect to server at port " + App.webServerPort + ".");
                System.exit(1); // Exit the application

            }
            //the contents of the file is parsed to a feature collection
            FeatureCollection featureCollection = FeatureCollection.fromJson(response.body());
            // each feature is stored in a list as the variable landmarkFeatures.
            landmarkFeatures = featureCollection.features();
            //catches any IO Exception or interrupted exception and prints the error.
        }catch (IOException e){
            System.out.println("IOException " + e.getMessage());
        }catch (InterruptedException e){
            System.out.println("InterruptedException " + e.getMessage());
        }
    }


    /**
     * given a list of moves made by the drone. the method parses the moves to a feature collection and then to a JSON String
     * so that the flight path
     * of the drone can be viewed on geojson.io
     * @param moves a list of moves made by the drone
     * @return a Json string which is the feature collection representing the movements made by the drone
     */
    public static String movesToFCCollection(List<LongLat> moves){
        if(moves==null){
            System.err.println("Input cannot be null");
        }
        List<Point> pointList = new ArrayList<>();
        //each location in the moves list is converted to a geojson point and added to a list
        for (LongLat move : moves) {
            pointList.add(move.point);
        }
        // the list of points is parsed to a lineString and then a geometry object
        Geometry geometry = (Geometry) LineString.fromLngLats(pointList);
        // the geometry object is converted to a feature and that feature is then converted to a feauture collection
        FeatureCollection fc = FeatureCollection.fromFeature(Feature.fromGeometry(geometry));
        // the JSON String of the feature collection is returned
        return fc.toJson();
    }


    /**
     * the method creates a geojson file in the current working directory and writes the json string of the feature collection
     * of the movements of the drone to it.
     * @param fc the json string of the feature collection of the movements of the drone.
     */
    public static void outputGeoJsonFile(String fc){
        if(fc == null){
            System.err.println("Input cannot be null");
        }
        //creates a new GeoJson file in the current working directory(windows)
        File gj = new File(Paths.get(".").toAbsolutePath().normalize().toString() +"\\drone-"+App.day+"-"+App.month+"-"+App.year+".geojson");
        //linux
        if(!gj.exists()){
            gj =new File(Paths.get(".").toAbsolutePath().normalize().toString() +"/drone-"+App.day+"-"+App.month+"-"+App.year+".geojson");
        }
        try{
            //tries to write the json string to the file that was just created
            FileWriter writer = new FileWriter("drone-"+App.day+"-"+App.month+"-"+App.year+".geojson");
            writer.write(fc);
            writer.close();
        // the method catches any errors that might get thrown.
        }catch (IOException e){
            System.err.println("IO Exception Erroe");
            e.printStackTrace();
        }

    }


}
