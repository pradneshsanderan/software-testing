package uk.ac.ed.inf;

import com.google.gson.Gson;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.HashMap;

public class Menus {

    //Public Variables
    /**
     * the name of the shop
     */
    public String name;
    /**
     * the port that the webserver is connected to
     */
    public String port;
    /**
     * a hashmap that stores the name and price of every item on the menu
     */
    public static HashMap<String,Integer> prices = new HashMap<>();


    //Private Variables
    /**
     * the Url String which is the Url for the menus file in the webserver
     */
    private static final String urlString = "http://localhost:"+App.webServerPort+"/menus/menus.json";
    /**
     * the HttpClient that is shared between all HttpRequest
     */
    private static final HttpClient client = HttpClient.newHttpClient();



    /**
     * The Menu class which is used to parse response.body.
     */
    public class Menu {
        String name;
        String location;
        ArrayList<uk.ac.ed.inf.Menu> menu;
    }

    //Standard Contructor
    /**
     * the Constructor for the Menus class which accepts 2 Strings which represent the name of the machine
     * and the port where the web server is running
     * @param name a String representing the name of the machine
     * @param port a String representing the port where the web server is running
     */
    Menus(String name, String port){
        this.name = name;
        this.port = port;
    }



    //.................................................................................................................
    //.................................................................................................................
    //Public Methods



    /**
     * A method which states the delivery cost for a valid order
     * @param items a collection of Strings where each String represents an item that was ordered
     * @return the total delivery cost for the order, including the price of the item and the standard delivery charge
     */
    public static int  getDeliveryCost(String ... items) {
        int standardDeliveryCharge = 50;
        try{
            //the request that would be sent to the website as a http request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .build();
            //once we sent the request, we save the response in "response"
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            //check the status code to check if the request had failed or not
            if(response.statusCode()!=200){
                System.out.println("Fatal error: Unable to connect to server at port " + App.webServerPort + ".");
                System.exit(1); // Exit the application
            }
            Type listType = new TypeToken<ArrayList<Menu>>() {}.getType();
            //use the fromJson(String,Type) to get a list of the Menu.
            ArrayList<Menu> menuList = new Gson().fromJson(response.body(), listType);

            // A hashmap to store the prices of each item.
            HashMap<String,Integer> prices = new HashMap<>();
            for (Menu menu : menuList) {
                int size = menu.menu.size();
                for (int j = 0; j < size; j++) {
                    // the current item
                    String currItem = menu.menu.get(j).item;
                    // the price of the current item
                    int currPence = menu.menu.get(j).pence;
                    //stores each item and the price in the hashmap, prices
                    prices.put(currItem, currPence);
                }
            }
            int total = standardDeliveryCharge;
            for (String item : items) {
                // for each item, add its price to the total delivery charge.
                total += prices.get(item);
            }
            return total;
        //catches any IO Exception or interrupted exception and prints the error.
        }catch (IOException e){
            System.out.println("IOException " + e.getMessage());
        }catch (InterruptedException e){
            System.out.println("InterruptedException " + e.getMessage());
        }
        return 0;

    }

    /**
     * the method gets the name and price of each item on the menu and stores it in the prices hashmap
     *
     */
    public static void  getPrices() {
        int standardDeliveryCharge = 50;
        try{
            //the request that would be sent to the website as a http request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .build();
            //once we sent the request, we save the response in "response"
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            //check the status code to check if the request had failed or not
            if(response.statusCode()!=200){
                System.out.println("error getting prices");

            }
            Type listType = new TypeToken<ArrayList<Menu>>() {}.getType();
            //use the fromJson(String,Type) to get a list of the Menu.
            ArrayList<Menu> menuList = new Gson().fromJson(response.body(), listType);

            //goes through each menu in the menu list
            for (Menu menu : menuList) {
                int size = menu.menu.size();
                //goes through each item in the menu
                for (int j = 0; j < size; j++) {
                    // the current item
                    String currItem = menu.menu.get(j).item;
                    // the price of the current item
                    int currPence = menu.menu.get(j).pence;
                    //stores each item and the price in the hashmap, prices
                    prices.put(currItem, currPence);
                }
            }
            //catches any IO Exception or interrupted exception and prints the error.
        }catch (IOException e){
            System.out.println("IOException " + e.getMessage());
        }catch (InterruptedException e){
            System.out.println("InterruptedException " + e.getMessage());
        }


    }

    /**
     * the method gets the total cost for delivering all the orders that are in the list of order numbers in the argument
     * @param OrderNos the list of order numbers
     * @return the total cost for delivering all of those orders, including the standard delivery charge
     */
    public static int getTotalChargeForOrders(ArrayList<String> OrderNos){
        if(OrderNos == null){
            System.err.println("Input cannot be null");
        }
        int total =0;
        int standardDeliveryCharge =50;
        // goes through each order
        for(String order :OrderNos){
            // gets the list of items in that order
            ArrayList<String> items = Orders.items.get(order);
            total = total+standardDeliveryCharge;
            // adds the price for each item ordered to the total
            for(String item:items){
                total = total+ prices.get(item);
            }
        }
        return total;
    }

    /**
     * the method gets the total cost for delivering a single order
     * @param order the order number that it is computing the cost for
     * @return the cost for delivering that order,including the standard delivery charge
     */
    public static int getChargeForOneOrder(String order){
        if(order == null){
            System.err.println("Input cannot be null");
        }
        int total = 50;
        //gets the list of items in that order
        ArrayList<String> items = Orders.items.get(order);
        //adds each items price to the total
        for(String item : items){
            total = total + prices.get(item);
        }
        return total;
    }
}
