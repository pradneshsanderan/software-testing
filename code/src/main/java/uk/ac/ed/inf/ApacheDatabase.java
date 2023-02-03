package uk.ac.ed.inf;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ApacheDatabase {
    //Variables Used by the Method

    /**
     * The JDBC String Used to access the database. The Database Port is received as an input when the program is ran
     */
    private static final String jdbcString = "jdbc:derby://localhost:"+ App.databasePort+"/derbyDB";



    //.................................................................................................................
    //.................................................................................................................
    //Public Methods that can be used by other classes

    /**
     * the method used the create the "deliveries" database. The method first checks if a deliveries database already
     * exits. if it does then it deletes it and creates a new database. It then populates the database with the deliveries
     * information that it gets from getDeliveryList(). if it encounters an sql error, it throws the error.
     */
    public static void createDeliveriesDatabase(){
        try{
            Connection conn = DriverManager.getConnection(jdbcString);
            // Create a statement object that we can use for running various
            // SQL statement commands against the database.
            Statement statement = conn.createStatement();
            DatabaseMetaData databaseMetadata = conn.getMetaData();
            ResultSet resultSet = databaseMetadata.getTables(null, null, "DELIVERIES", null);
            // If the resultSet is not empty then the table exists, so we can drop it
            if (resultSet.next()) {
                statement.execute("drop table deliveries");
            }
            //create the deliveries database
            statement.execute(
                    "create table deliveries(" +
                            "orderNo char(8), " +
                            "deliveredTo varchar(19), " +
                            "costInPence int)");
            PreparedStatement psDeliveries = conn.prepareStatement("insert into deliveries values (?, ?, ?)");
            // get the list of delivery objects
            ArrayList<Deliveries> deliveries = getDeliveryList();
            // add each object to the table
            for (Deliveries delivery : deliveries) {
                psDeliveries.setString(1, delivery.orderNo);
                psDeliveries.setString(2, delivery.deliveredTo);
                psDeliveries.setInt(3, delivery.costInPence);
                psDeliveries.execute();
            }
        //catch any exceptions that might get thrown
        }catch(java.sql.SQLException e){
            System.err.println(e);
            e.printStackTrace();
        }
    }

    /**
     *the method used the create the "flightpath" database. The method first checks if a flightpath database already
     * exits. if it does then it deletes it and creates a new database. It then populates the database with the flightpath
     * information that it gets from createFlightPathsList(). if it encounters an sql error, it throws the error.
     * @param moves a list of moves taken by the drone to make the deliveries. Each move is recorded in the database
     */
    public static void createFlightPathDatabase(List<LongLat> moves){
        if(moves==null){
            System.err.println("Input cannot be null");
        }
        try{
            Connection conn = DriverManager.getConnection(jdbcString);
            // Create a statement object that we can use for running various
            // SQL statement commands against the database.
            Statement statement = conn.createStatement();
            DatabaseMetaData databaseMetadata = conn.getMetaData();
            ResultSet resultSet = databaseMetadata.getTables(null, null, "FLIGHTPATH", null);
            // If the resultSet is not empty then the table exists, so we can drop it
            if (resultSet.next()) {
                statement.execute("drop table flightpath");
            }
            //create the flightpath table
            statement.execute(
                    "create table flightpath(" +
                            "orderNo char(8), " +
                            "fromLongitude double, " +
                            "fromLatitude double, "+
                            "angle integer, "+
                            "toLongitude double, "+
                            "toLatitude double)");
            PreparedStatement psFlightPaths = conn.prepareStatement("insert into flightpath values (?, ?, ?, ?, ?, ?)");
            // get the list of flightpath objects
            ArrayList<FlightPath> flightPaths = createFlightPathsList(moves);
            // we add each obejct into the table
            for (FlightPath flightPath : flightPaths) {
                psFlightPaths.setString(1, flightPath.orderNo);
                psFlightPaths.setDouble(2, flightPath.fromLongitude);
                psFlightPaths.setDouble(3, flightPath.fromLatitude);
                psFlightPaths.setInt(4, flightPath.angle);
                psFlightPaths.setDouble(5, flightPath.toLongitude);
                psFlightPaths.setDouble(6, flightPath.toLatitude);
                psFlightPaths.execute();
            }
        //catch any exceptions that might get thrown
        }catch(java.sql.SQLException e){
            e.printStackTrace();
        }
    }


    //.................................................................................................................
    //.................................................................................................................
    //Private Methods that are helper methods to the main methods


    /**
     * the method checks which orders were completed for the day and based on the delivery class, it creates a list of deliveries.
     * the list consists of the orders that were completed for the day which includes the order number, where it was delivered to and
     * how its cost to deliver the order
     * @return the list of deliveries completed for the day
     */
    private static ArrayList<Deliveries> getDeliveryList(){
        //a list of all the orders completed on that day since not all orders are usually completed
        ArrayList<String> orders = HexGraph.OrdersCompleted;
        ArrayList<Deliveries> deliveries = new ArrayList<>();
        //run the get prices method in the Menus class to get the price for each item on the menu
        Menus.getPrices();
        for(String order: orders){
            //for each order, we create a new delivery object and add it to the list.
            Deliveries d = new Deliveries();
            d.orderNo = order;
            // we get the delivery address from the deliveryAddress hashmap in the Orders class
            d.deliveredTo = Orders.deliveryAddress.get(order);
            // we get the cost for each delivery from the getChargeForOneOrder() method in the menus class
            d.costInPence = Menus.getChargeForOneOrder(order);
            deliveries.add(d);
        }
        return deliveries;
    }

    /**
     * the method creates a list of flightpath moves where each moves corresponds to a move that the drone made in delivering the
     * orders for the day. the method gets a list of moves mades by the drone when delivering the orders and parses it according to
     * the flightpath class. it then adds the move to a list of moves and returns that list to be added to the database
     * @param moves the moves(positions) made by the drone in delivering the orders.
     * @return the list of moves that have been parsed according to the FlightPath Class.
     */
    private static ArrayList<FlightPath> createFlightPathsList (List<LongLat> moves){
        if(moves==null){
            System.err.println("Input cannot be null");
        }
        ArrayList<FlightPath> flightPaths = new ArrayList<>();
        //indexes which show when the drone was hovering and not moving
        ArrayList<Integer> hoverLocations = HexGraph.HoverLocation;
        //the first order that was delivered by the drone
        String currOrder = HexGraph.OrderNumberChanges.get(0);
        // traverse through every move made by the drone
        for(int currMove=1;currMove<moves.size();currMove++){
            FlightPath f = new FlightPath();
            //if the hashmap contains the index, it means that the drone is delivering the next order so currOrder must be updated
            if(HexGraph.OrderNumberChanges.containsKey(currMove)){
                currOrder = HexGraph.OrderNumberChanges.get(currMove);
            }
            f.orderNo = currOrder;
            //we get the coordinates of the the previous location
            f.fromLatitude = moves.get(currMove-1).latitude;
            f.fromLongitude = moves.get(currMove-1).longitude;
            //if the list contains the index,then we are currently hovering so the angle must be the junk value of -999
            if(hoverLocations.contains(currMove)){
                f.angle=-999;
            }
            else{
                f.angle=moves.get(currMove-1).angleDirectionTo(moves.get(currMove));
            }
            // we get the coordinates of the current location
            f.toLatitude = moves.get(currMove).latitude;
            f.toLongitude = moves.get(currMove).longitude;
            //add this flightpath to the list of flightpath
            flightPaths.add(f);
        }
        // the last move would be to return to appleton. at this step we would have already moved to appleton so all that is left is to hover it down to the roof.
        FlightPath f = new FlightPath();
        f.orderNo=currOrder;
        f.toLongitude = moves.get(moves.size()-1).longitude;
        f.toLatitude = moves.get(moves.size()-1).latitude;
        f.fromLongitude = moves.get(moves.size()-1).longitude;
        f.fromLatitude = moves.get(moves.size()-1).latitude;
        f.angle = -999;
        flightPaths.add(f);
        //return the list of flightpaths
        return flightPaths;
    }

}
