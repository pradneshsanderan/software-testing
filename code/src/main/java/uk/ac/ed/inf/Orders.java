package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;

import java.lang.reflect.Type;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.sql.*;
import java.sql.Date;

import java.util.*;

public class Orders {


    //Public Variables
    /**
     * a hashmap that maps an orders order number to the coordinates of the shops it need to pick the food from
     */
    public static HashMap<String, ArrayList<double[]>> pickUpCoordinates  = new HashMap<>();
    /**
     * a hashmap that maps an orders order number to the WhatThreeWords values of the shops it need to pick the food from
     */
    public static HashMap<String,HashSet<String>> pickUpWords = new HashMap<>();
    /**
     * a hashmap that maps an orders order number to the coordinates of the delivery location
     */
    public static HashMap<String,double[]> deliveryCoordinates = new HashMap<>();
    /**
     * a hashmap that maps an orders order number to the WhatThreeWords values of the delivery location
     */
    public static HashMap<String,String> deliveryAddress = new HashMap<>();
    /**
     * a hashmap that maps an orders order number to the list of item names ordered in that order
     */
    public static HashMap<String,ArrayList<String>> items = new HashMap<>();
    /**
     * a list of order numbers for the specific day
     */
    public static ArrayList<String> orderNos = new ArrayList<>();

    //Private Variables
    /**
     * the date that was specified in the input arguments
     */
    private static final Date deliveryDate = Date.valueOf(App.year+"-"+App.month+"-"+App.day);
    /**
     * The JDBC String Used to access the database. The Database Port is received as an input when the program is ran
     */
    private static final String jdbcString = "jdbc:derby://localhost:"+ App.databasePort+"/derbyDB";
    /**
     * the HttpClient that is shared between all HttpRequest
     */
    private static final HttpClient client = HttpClient.newHttpClient();



    //.................................................................................................................
    //.................................................................................................................
    //Public Methods that can be accessed by other classes


    /**
     * the method calls the helper methods which will fetch the data from the database and webserver and store the data in the
     * variables listed above. it was easier to make a method call all of the helper methods so that the helper method can remain
     * private and to decutter the App class.
     */
    public static void getCoordinatesAndOrders(){
        getOrders();
        getOrderDetails();
        getDeliveryCoordinates();
        getPickUpWords();
        getPickUpCoordinates();
    }

    //.................................................................................................................
    //.................................................................................................................
    //Private Methods

    /**
     * the method gets the order numbers and delivery address for a given date from the orders database and stores the order
     * numbers in the orderNos list and adds the delivery address to the deliveryAddress Hashmap
     */
    private static void getOrders(){
        try{
            Connection conn = DriverManager.getConnection(jdbcString);
            //get all columns for a certain date
            final String ordersQuery = "select * from orders where deliveryDate=(?)";
            PreparedStatement psCourseQuery = conn.prepareStatement(ordersQuery);
            psCourseQuery.setString(1, deliveryDate.toString());
            ResultSet rs = psCourseQuery.executeQuery();
            while (rs.next()) {
                //get the order number
                String orderNumber = rs.getString("orderNo");
                //get the delivery address
                String deliverTo = rs.getString("deliverTo");

                orderNos.add(orderNumber);

                deliveryAddress.put(orderNumber,deliverTo);

            }
        //Catches any SQL Exceptions
        }catch(java.sql.SQLException e){
            e.printStackTrace();
        }


    }


    /**
     * the method gets the items ordered in each order from the orderDetails database
     */
    private static void getOrderDetails(){
        try{
            Connection conn = DriverManager.getConnection(jdbcString);
            //selects all the colums for a certain order number
            final String ordersQuery = "select * from orderDetails where orderNo=(?)";
            PreparedStatement psCourseQuery = conn.prepareStatement(ordersQuery);
            //for each order, it gets the items ordered and stores the items in a list
            for (String orderNo : orderNos) {
                ArrayList<String> s = new ArrayList<>();
                psCourseQuery.setString(1, orderNo);
                ResultSet rs = psCourseQuery.executeQuery();
                //while there are more items,
                while (rs.next()) {

                    String item = rs.getString("item");
                    s.add(item);


                }
                // the order number and list of items ordered is put in the hashmap
                items.put(orderNo, s);
            }

        }catch(java.sql.SQLException e){
            e.printStackTrace();
        }


    }

    /**
     * based on the WhatThreeWords String stored in the deliveryAddress hashmap, the method check the webserver for the json file
     * and parses the json in the file to get the delivery coordinates for each order. the coordinates are stored in the hashmap deliveryCoordinates
     */
    private static void getDeliveryCoordinates(){
        for (String orderNo : orderNos) {
            String addressString = deliveryAddress.get(orderNo);
            // splits the WhatThreeWords address into 3 string
            String[] address = addressString.split("\\.");

            String word1 = address[0];
            String word2 = address[1];
            String word3 = address[2];
            String urlString = "http://localhost:" + App.webServerPort + "/words/" + word1 + "/" + word2 + "/" + word3 + "/details.json";

            try {
                //the request that would be sent to the website as a http request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(urlString))
                        .build();
                //once we sent the request, we save the response in "response"
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                //check the status code to check if the request had failed or not
                if (response.statusCode() != 200) {
                    System.out.println("Status code 200 returned; request failed to execute");
                }
                // parses the json string in the file
                WordsDetails deliveryDetails = new Gson().fromJson(response.body(), WordsDetails.class);
                // gets the delivery coordinates and puts it in the hashmap
                deliveryCoordinates.put(orderNo, new double[]{deliveryDetails.coordinates.lng, deliveryDetails.coordinates.lat});

                //catches any IO Exception or interrupted exception and prints the error.
            } catch (IOException e) {
                System.out.println("IOException " + e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("InterruptedException " + e.getMessage());
            }
        }

    }

    /**
     * the method searches the menus file in the web server for items that are the same as items ordered in a specific order.
     * it then get the WhatThreeWords address of that particular shop and saves that string in a list. the list is then added to the hashmap
     * pickUpWords
     */
    private static void getPickUpWords(){
        String urlString = "http://localhost:"+App.webServerPort+"/menus/menus.json";
        //for each order
        for (String orderNo : orderNos) {
            HashSet<String> words = new HashSet<>();
            ArrayList<String> itemsOrdered = items.get(orderNo);
            //for each item in the order
            for (String s : itemsOrdered) {
                try {
                    //the request that would be sent to the website as a http request
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(urlString))
                            .build();
                    //once we sent the request, we save the response in "response"
                    HttpResponse<String> response =
                            client.send(request, HttpResponse.BodyHandlers.ofString());
                    //check the status code to check if the request had failed or not
                    if (response.statusCode() != 200) {
                        System.out.println("Fatal error: Unable to connect to server at port " + App.webServerPort + ".");
                        System.exit(1); // Exit the application
                    }

                    Type listType = new TypeToken<ArrayList<Menus.Menu>>() {
                    }.getType();
                    //use the fromJson(String,Type) to get a list of the Menu.
                    ArrayList<Menus.Menu> menuList = new Gson().fromJson(response.body(), listType);
                    // searches the menus for the item. if it is found, the location (WhatThreeWords Address) is added to the list
                    for (Menus.Menu menu : menuList) {
                        int size = menu.menu.size();
                        for (int k = 0; k < size; k++) {
                            // the current item
                            String currItem = menu.menu.get(k).item;
                            if (currItem.equals(s)) {
                                words.add(menu.location);
                            }

                        }
                    }
                    //catches any IO Exception or interrupted exception and prints the error.
                } catch (IOException e) {
                    System.out.println("IOException " + e.getMessage());
                } catch (InterruptedException e) {
                    System.out.println("InterruptedException " + e.getMessage());
                }
            }
            // the list is added to the hashmap
            pickUpWords.put(orderNo, words);

        }


    }


    /**
     * the method uses the WhatThreeWords address stored in pickUpWords Hashmap to get the coordinates of the shops to pick up orders from
     * from the web server
     */
    private static void getPickUpCoordinates(){
        for (String orderNo : orderNos) {
            HashSet<String> curr = pickUpWords.get(orderNo);
            Iterator val = curr.iterator();
            ArrayList<double[]> addressList = new ArrayList<>();
            while (val.hasNext()) {
                //splits the WhatThreeWords string into 3 strings
                String[] currWords = val.next().toString().split("\\.");
                String word1 = currWords[0];
                String word2 = currWords[1];
                String word3 = currWords[2];
                String urlString = "http://localhost:" + App.webServerPort + "/words/" + word1 + "/" + word2 + "/" + word3 + "/details.json";
                try {
                    //the request that would be sent to the website as a http request
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(urlString))
                            .build();
                    //once we sent the request, we save the response in "response"
                    HttpResponse<String> response =
                            client.send(request, HttpResponse.BodyHandlers.ofString());
                    //check the status code to check if the request had failed or not
                    if (response.statusCode() != 200) {
                        System.out.println("Fatal error: Unable to connect to server at port " + App.webServerPort + ".");
                        System.exit(1); // Exit the application
                    }
                    //parses the json string according to the WordsDetails class.
                    WordsDetails pickUpDetails = new Gson().fromJson(response.body(), WordsDetails.class);
                    //gets the coordinates of the shop
                    double[] n = new double[]{pickUpDetails.coordinates.lng, pickUpDetails.coordinates.lat};
                    // the coordinates are added to the list
                    addressList.add(n);
                    //catches any IO Exception or interrupted exception and prints the error.
                } catch (IOException e) {
                    System.out.println("IOException " + e.getMessage());
                } catch (InterruptedException e) {
                    System.out.println("InterruptedException " + e.getMessage());
                }
            }
            // the list is added to the hashmap
            pickUpCoordinates.put(orderNo, addressList);


        }
    }



}
