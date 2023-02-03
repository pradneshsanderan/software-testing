package uk.ac.ed.inf;

import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;

import java.util.*;

public class HexGraph {


    //public variable
    /**
     * the list of order numbers of orders that the drone was able to complete for the day
     */
    public static ArrayList<String> OrdersCompleted = new ArrayList<>();
    /**
     * a list of integers that represent the index of the movements in the drones movements when the drone was hovering
     */
    public static ArrayList<Integer> HoverLocation= new ArrayList<>();
    /**
     * a hashmap that maps an integer index to the order number string. the integer represents the index in the movement of the drone when
     * the drone has moved on to the next order
     */
    public static HashMap<Integer,String> OrderNumberChanges = new HashMap<>();

    //private variable
    /**
     * the height of the triangles created in the graph.
     */
    private static final double heightOfTriangle = Math.sqrt(Math.pow(LongLat.oneMove,2) - Math.pow(LongLat.oneMove/2,2));




    //.................................................................................................................
    //.................................................................................................................
    //Public Methods

    /**
     * the method gets the shortest route across a custom graph that would allow the drone to complete the orders. it first
     * build an equilateral triangle graph where each edge of the triangle is 1 move by the drone. the drone can move in 6 different directions
     * from a certain node. the method then used dijikstras algorithm to find the shortest path between the current position and the pick up
     * nodes and then the delivery nodes. while the drone still has moves and has not run out of battery, this continues. the movements of the drone
     * are added to a list of LongLat positions. the drones hover locations and index of the movements where it completes an order are also recorded .
     * @param drone the number of moves that the drone can move is gotten from the drone object
     * @return the list of positions the drone was in which correspond to its route around the map
     */
    public static List<LongLat> getRoute(Drone drone){
        if(drone==null){
            System.err.println("Drone cannot be null");
        }
        // generates a graph of equilateral triangles
        Graph<LongLat,NodeEdges> g = HexGraph.genValidHexGraph();
        //gets the nodes that are closest to each delivery coordinate
        HashMap<String,LongLat> delivery =HexGraph.getDeliveryNodes(g);
        // gets the nodes that are closest to each pickup coordinate
        HashMap<String,ArrayList<LongLat>> pickup = getPickUpNodes(g);
        //gets the order of the Orders by a certain TSP algorithm. the tsp can be changed by replacing the greedyApproach below
        ArrayList<String> orders = greedyApproach(Orders.items);
        // gets the node of the starting location
        LongLat appleton = getAppleton(g);
        List<LongLat> movements = new ArrayList<>();
        // dijikstras algorithm is used, if you want to use A* instead, comment this line and uncomment the A* lines
        DijkstraShortestPath<LongLat,NodeEdges> d= new DijkstraShortestPath<LongLat,NodeEdges>(g);
//        AStarAdmissibleHeuristic<LongLat> n= new AStarAdmissibleHeuristic<LongLat>() {
//            @Override
//            public double getCostEstimate(LongLat longLat, LongLat v1) {
//                return longLat.distanceTo(v1);
//            }
//        };
//
//        AStarShortestPath<LongLat,NodeEdges> aStarShortestPath = new AStarShortestPath<LongLat,NodeEdges>(g,n);
        //gets the number of moves from the drone object
        int movesLeft = drone.MovesLeft;
        // the drone hover off appleton
        HoverLocation.add(0);
        movesLeft--;
        //we move to the first order
        OrderNumberChanges.put(0,orders.get(0));
        assert appleton != null;
        //it completes the first order if it only has one pick up location
        if(pickup.get(orders.get(0)).size()==1){

            //get the movement from the current location to the pick up location
            movements = d.getPath(appleton,pickup.get(orders.get(0)).get(0)).getVertexList();
            //since we are hovering to pick up, we add the index to the list
            HoverLocation.add(movements.size());
            //remove one move for hovering
            movesLeft--;
            //get the movement from the current location to the delivery location
            movements.addAll(d.getPath(pickup.get(orders.get(0)).get(0),delivery.get(orders.get(0))).getVertexList());
            HoverLocation.add(movements.size());
            movesLeft--;

            //remove the number of moves it took for this order from moves left
            int pickUpmoves =d.getPath(appleton,pickup.get(orders.get(0)).get(0)).getVertexList().size();
            int deliverMoves = d.getPath(pickup.get(orders.get(0)).get(0),delivery.get(orders.get(0))).getVertexList().size();
            movesLeft = movesLeft- pickUpmoves-deliverMoves;
        }
        //it completes the first order if it has 2 pick up locations
        else{
            //get the movement from the current location to the pick up location
            movements = d.getPath(appleton,pickup.get(orders.get(0)).get(0)).getVertexList();

            HoverLocation.add(movements.size());
            movesLeft--;
            //get the movement from the current location to the next pick up location
            movements.addAll(d.getPath(pickup.get(orders.get(0)).get(0),pickup.get(orders.get(0)).get(1)).getVertexList());

            HoverLocation.add(movements.size());
            movesLeft--;
            //get the movement from the current location to the delivery location
            movements.addAll(d.getPath(pickup.get(orders.get(0)).get(1),delivery.get(orders.get(0))).getVertexList());

            HoverLocation.add(movements.size());
            movesLeft--;
            //remove the number of moves it took for this order from moves left
            int pickUpMoves = d.getPath(appleton,pickup.get(orders.get(0)).get(0)).getVertexList().size() + d.getPath(pickup.get(orders.get(0)).get(0),pickup.get(orders.get(0)).get(1)).getVertexList().size();
            int deliverMoves = d.getPath(pickup.get(orders.get(0)).get(1),delivery.get(orders.get(0))).getVertexList().size();
            movesLeft = movesLeft-pickUpMoves-deliverMoves;
        }
        //add the order number to the list since we have completed it
        OrdersCompleted.add(orders.get(0));
        int pointer =0;
        //go through each order
        for(int i=1;i<orders.size();i++){
            //if there is one pick up location
            if(pickup.get(orders.get(i)).size()==1){
                int pickUpMoves = d.getPath(delivery.get(orders.get(i-1)),pickup.get(orders.get(i)).get(0)).getVertexList().size();
                int deliveryMove = d.getPath(pickup.get(orders.get(i)).get(0),delivery.get(orders.get(i))).getVertexList().size();
                int returnHomeMoves = d.getPath(delivery.get(orders.get(i)),appleton).getVertexList().size();
                int hoverMoves = 2;
                // we first check if we have enough moves left tp complete the order and make it back to appleton. if we cant then we skip the order and move to the next
                if(movesLeft-hoverMoves-pickUpMoves-deliveryMove-returnHomeMoves>=0){
                    OrderNumberChanges.put(movements.size(),orders.get(i));
                    movements.addAll(d.getPath(delivery.get(orders.get(i-1)),pickup.get(orders.get(i)).get(0)).getVertexList());

                    HoverLocation.add(movements.size());
                    movements.addAll(d.getPath(pickup.get(orders.get(i)).get(0),delivery.get(orders.get(i))).getVertexList());

                    HoverLocation.add(movements.size());
                    movesLeft = movesLeft-pickUpMoves-deliveryMove-hoverMoves;
                    pointer =i;
                    OrdersCompleted.add(orders.get(i));
                }

            }
            // if there is 2 pick up location
            else{
                int pickUpMoves = d.getPath(delivery.get(orders.get(i-1)),pickup.get(orders.get(i)).get(0)).getVertexList().size()+d.getPath(pickup.get(orders.get(i)).get(0),pickup.get(orders.get(i)).get(1)).getVertexList().size();
                int deliveryMove = d.getPath(pickup.get(orders.get(i)).get(1),delivery.get(orders.get(i))).getVertexList().size();
                int returnHomeMoves = d.getPath(delivery.get(orders.get(i)),appleton).getVertexList().size();
                int hoverMoves = 3;
                // we first check if we have enough moves left tp complete the order and make it back to appleton. if we cant then we skip the order and move to the next
                if(movesLeft-hoverMoves-pickUpMoves-deliveryMove-returnHomeMoves>=0){
                    OrderNumberChanges.put(movements.size(),orders.get(i));
                    movements.addAll(d.getPath(delivery.get(orders.get(i-1)),pickup.get(orders.get(i)).get(0)).getVertexList());

                    HoverLocation.add(movements.size());
                    movements.addAll(d.getPath(pickup.get(orders.get(i)).get(0),pickup.get(orders.get(i)).get(1)).getVertexList());

                    HoverLocation.add(movements.size());
                    movements.addAll(d.getPath(pickup.get(orders.get(i)).get(1),delivery.get(orders.get(i))).getVertexList());

                    HoverLocation.add(movements.size());
                    movesLeft = movesLeft-pickUpMoves-deliveryMove-hoverMoves;
                    pointer=i;
                    OrdersCompleted.add(orders.get(i));
                }

            }
        }
        //we move back to appleton roof and hover down
        movements.addAll(d.getPath(delivery.get(orders.get(pointer)),appleton).getVertexList());
        drone.MovesLeft =movesLeft;
        //return the list of drone locations which correspond to the movement of the drone
        return movements;
    }

    //.................................................................................................................
    //Different Travelling SalesPerson approaches

    /**
     * the method find the order with the nearest pick up location to the drones current location and completes that order. it then
     * finds the next nearest pick up location and so on.it orders the order numbers accordingly
     * @param g the graph of equilateral triangles for the drone to traverse
     * @param pickUpNodes the nodes that are closeto the coordinates of the shops the drone has to visit
     * @param deliveryNodes the nodes that are closeto the delivery coordinates that for each order
     * @return a sorted list of order numbers
     */
    public static ArrayList<String> nearestNeighbourApproach(Graph<LongLat,NodeEdges> g,HashMap<String,ArrayList<LongLat>> pickUpNodes,HashMap<String,LongLat> deliveryNodes){
        if(g==null || pickUpNodes == null || deliveryNodes == null){
            System.err.println("Input cannot be null. please check the inputs");
        }
        ArrayList<String> orders = Orders.orderNos;
        Menus.getPrices();
        // gets the starting nodes coordinates
        LongLat appleton = getAppleton(g);
        // if we cant find appleton's node
        if(appleton == null){
            System.out.println("Could not associate start location with a node on the map");
            appleton = LongLat.appleton;
        }
        ArrayList<String> sortedOrders = new ArrayList<>();
        String closest = null;
        int ind = 0;
        double dist = Double.MAX_VALUE;
        //first find the order that has the pick up node closest to appleton
        for(String order:orders){
            ArrayList<LongLat> currentNodes = pickUpNodes.get(order);
            for(int i=0;i<currentNodes.size();i++){
                if(appleton.distanceTo(currentNodes.get(i))<dist){
                    closest = order;
                    dist = appleton.distanceTo(currentNodes.get(i));
                    ind=i;
                }
            }
        }
        //while not all of the order number has been added to the list of sorted orders. find the order with the pick up location that is closest
        // to the current location and is not already in the list of sorted orders.
        sortedOrders.add(closest);
        while (sortedOrders.size()!=orders.size()){
            closest = null;
            ind = 0;
            dist = Double.MAX_VALUE;
            for(String order:orders){
                //check if the order is already in the list
                if(!sortedOrders.contains(order)){
                    //list of shop nodes to pick up from
                    ArrayList<LongLat> currentNodes = pickUpNodes.get(order);
                    // goes through each node and checks if any of them are closest. if they are, update the closest variable
                    for(int i=0;i<currentNodes.size();i++){
                        if(deliveryNodes.get(sortedOrders.get(sortedOrders.size()-1)).distanceTo(currentNodes.get(i))<dist){
                            closest = order;
                            dist = deliveryNodes.get(sortedOrders.get(sortedOrders.size()-1)).distanceTo(currentNodes.get(i));
                            ind=i;
                        }
                    }
                }

            }
            sortedOrders.add(closest);
        }
        //System.out.println(Orders.orderNos);
        return sortedOrders;
    }

    /**
     * the method is the random approach for the tsp algo. its orders the list of order numbers randomly and returns the list
     * @return list of randomly sorted order numbers
     */
    public static ArrayList<String> randomAppraoch(){
        Menus.getPrices();
        ArrayList<String> random = new ArrayList<>(Orders.orderNos);
        //randomly sorts the list
        Collections.shuffle(random);
        return random;
    }

    /**
     * the method uses the greedy approach to the tsp problem where it calculates the cost in delivering each order and then
     * it prioritises the orders with the higher cost as it makes the most amount of money. so the order with the higher cost will be higher up in the list than
     * those with the lower cost
     * @param items the items ordered in each order
     * @return a sorted list of order numbers
     */
    public static ArrayList<String> greedyApproach(HashMap<String,ArrayList<String>> items){
        ArrayList<String> sortedOrders = new ArrayList<>();
        //list of order numbers
        ArrayList<String> ord = Orders.orderNos;
        String[] orderArray= new String[ord.size()];
        int[][] deliveryCosts = new int[ord.size()][2];
        Menus.getPrices();
        HashMap<String,Integer> itemPrices = Menus.prices;
        int i=0;
        //for each order, get the cost for delivery in pence
        for(String order :ord){
            int total = Menus.getChargeForOneOrder(order);
            deliveryCosts[i][0] = total;
            deliveryCosts[i][1] = i;
            orderArray[i] = order;
            i++;
        }
        // sort the arrays by the first element which is the cost of each delivery
        Arrays.sort(deliveryCosts, Comparator.comparingDouble(o -> o[0]));

        // sort the orders in descending order
        for(int k=deliveryCosts.length-1;k>=0;k--){
            int ind = deliveryCosts[k][1];
            sortedOrders.add(orderArray[ind]);
        }

        return sortedOrders;
    }






    //.................................................................................................................
    //.................................................................................................................
    //Private Methods
    //Building the graph

    /**
     * checks if the row that appleton is one will be shifted or not. if there are an odd number
     * of rows from appletons row to the top, then the row is shifted because appletons nodes row
     * should remain unshifted
     * @return true if the row is shifted
     */
    private static boolean shiftedRow(){
        double appletonLat = LongLat.appleton.latitude;
        int rowsCounter =0;
        //counts the number of rows
        while(appletonLat<LongLat.confAreaTop){
            appletonLat = appletonLat + heightOfTriangle;
            rowsCounter++;
        }
        return rowsCounter%2==1;

    }

    /**
     * the method finds the locations of the nodes that would be on the graph and stores the Longlat object of the node
     * in a 2d list. the graph will be hexagonal in shape since the triangles are equilateral and the angles would be 60 degrees. the method
     * moves from the top left of the confiment area to the bottom right to make sure that all nodes are inside the confinement area
     * @return a 2d list of Longlat objects stating the position of the nodes on the graph
     */
    private static ArrayList<ArrayList<LongLat>> genNodesOnHexGraph(){
        ArrayList<ArrayList<LongLat>> nodeLocations = new ArrayList<>();
        boolean shifted = shiftedRow();
        double  startLocLong = LongLat.appleton.longitude;
        double startLocLat = LongLat.appleton.latitude;
        //move the point to just out of the flyzone. we use the point from appletons coordinates so that appletons coordinates will always be a node
        while(startLocLong>LongLat.confAreaLeft){
            startLocLong = startLocLong - LongLat.oneMove;
        }
        while(startLocLat<LongLat.confAreaTop){
            startLocLat = startLocLat + heightOfTriangle;
        }
        //move from the top  to the bottom  of the confinement area. each move has a distance = heightOfTriangle
        for( double currentLat = startLocLat;currentLat>LongLat.confAreaBottom;currentLat = currentLat-heightOfTriangle){
            ArrayList<LongLat> currentRow = new ArrayList<>();
            //move from the left to the right of the confinement area. each move is equal to the distance the drone can move one time
            for(double currentLong = startLocLong;currentLong<LongLat.confAreaRight;currentLong = currentLong+LongLat.oneMove){
                if(shifted){
                    //if the row is shifted then we add half a move to it to shift it to the right. the distance remains equal to oneMove
                    currentRow.add(new LongLat(currentLong+LongLat.oneMove/2,currentLat));
                }
                else{
                    //if it is not shifted we just add the node
                    //add the node to the list for that row
                    currentRow.add(new LongLat(currentLong,currentLat));
                }
            }
            //the next rows value of shifted would be the opposite of what the current row is to form triangles
            shifted = !shifted;
            // add the list to the 2d list
            nodeLocations.add(currentRow);
        }
        return nodeLocations;
    }


    /**
     * the method forms nodeEdges between the nodes on the graph and its neighbour nodes. the nodes are then stored in a hashset
     * to prevent duplication since its an undirected graph.
     * @param nodesOnHexGraph the list of all nodes in the graph
     * @return hashset of node edges
     */
    private static HashSet<NodeEdges> genPathOnHexGraoh(ArrayList<ArrayList<LongLat>> nodesOnHexGraph){
        if(nodesOnHexGraph == null){
            System.err.println("Input cannot be null");
        }
        HashSet<NodeEdges> pathOnGraph = new HashSet<>();
        //checks if the row is shifted
        boolean shifted= shiftedRow();
        int numberOfRows = nodesOnHexGraph.size();
        //goes through each row
        for(int currentRow = 0;currentRow<numberOfRows;currentRow++){
            int numberofNodesInColumn = nodesOnHexGraph.get(currentRow).size();
            //goes through each node in the row
            for(int currentNode =0;currentNode<numberofNodesInColumn;currentNode++){
                //adds an edge from the current node to the node to its left
                if(currentNode>0){
                    pathOnGraph.add(new NodeEdges(nodesOnHexGraph.get(currentRow).get(currentNode),nodesOnHexGraph.get(currentRow).get(currentNode-1)));

                }
                //adds an edge from the current node to the node above
                if(currentRow>0){
                    pathOnGraph.add(new NodeEdges(nodesOnHexGraph.get(currentRow).get(currentNode),nodesOnHexGraph.get(currentRow-1).get(currentNode)));

                }
                if(shifted){
                    //add an edge to the node below and to the right
                   if(numberOfRows-1>currentRow && numberofNodesInColumn-1>currentNode){
                       pathOnGraph.add(new NodeEdges(nodesOnHexGraph.get(currentRow).get(currentNode),nodesOnHexGraph.get(currentRow+1).get(currentNode+1)));
                   }
                   //add an edge to the node above and right
                   if(currentRow>0 && numberofNodesInColumn-1>currentNode){
                       pathOnGraph.add(new NodeEdges(nodesOnHexGraph.get(currentRow).get(currentNode),nodesOnHexGraph.get(currentRow-1).get(currentNode+1)));
                   }

                }
            }
            shifted = !shifted;
        }

        return pathOnGraph;
    }


    /**
     * the method adds all the nodes and nodeEdge between those nodes that was previously found to a graph hence creating
     * a hexagonal graph with equilateral triangles
     * @return the hexagonal graph that was created
     */
    private static Graph<LongLat,NodeEdges> genDroneHexGraph(){
        //create an unpopulated graph
        DefaultUndirectedWeightedGraph<LongLat,NodeEdges> g = new DefaultUndirectedWeightedGraph<LongLat,NodeEdges>(NodeEdges.class);
        // list of nodes
        ArrayList<ArrayList<LongLat>> nodes = genNodesOnHexGraph();
        //set of node edges
        HashSet<NodeEdges> edges = genPathOnHexGraoh(nodes);
        // add all of the nodes to the graph
        for (ArrayList<LongLat> node : nodes) {
            for (LongLat longLat : node) {
                g.addVertex(longLat);
            }
        }
        // add all of the edges to the graph.the weight of each edge should be one move since the distance is 1 move
        for(NodeEdges edge:edges){
            g.addEdge(edge.node1,edge.node2,edge);
            g.setEdgeWeight(edge,LongLat.oneMove);
        }
        return g;
    }

    /**
     * to get a valid graph, the method removes the nodes and nodeEdges that the drone is not allowed to travel to
     * for each edge it check if it is in the confinement area and it is not in a no fly zone. if not it removes the node from the graph. for the
     * edges it checks if they are in the confinement area, not in the no fly zone and does not cross the no fly zone. if not it is also
     * removed. hence since there are no nodes or edges to travel to it is impossible for the drone to exit the confinement area
     * or go into the no fly zone
     * @return
     */
    private static Graph<LongLat,NodeEdges> genValidHexGraph(){
        Graph<LongLat, NodeEdges> droneHexGraph = genDroneHexGraph();
        DefaultUndirectedWeightedGraph<LongLat,NodeEdges> g = new DefaultUndirectedWeightedGraph<LongLat,NodeEdges>(NodeEdges.class);
        Set<LongLat> nodes = droneHexGraph.vertexSet();
        Set<NodeEdges> edges = droneHexGraph.edgeSet();
        //checks the nodes
        for(LongLat node:nodes){
            //check if the drone allowed to eb there
            if(!node.inNoFlyPolygon() && node.isConfined()){
                g.addVertex(node);
            }
        }
        //checks the edges
        for(NodeEdges edge:edges){
            LongLat node1 = edge.node1;
            LongLat node2 = edge.node2;
            //checks if the drone is allowed to fly there
            if(node1.isConfined()&& node2.isConfined()&&
                    !node1.inNoFlyPolygon()&&!node2.inNoFlyPolygon() &&
                    !node1.inNoFlyZone(node2) && !node2.inNoFlyZone(node1)){
                g.addEdge(edge.node1,edge.node2,edge);
                g.setEdgeWeight(edge,1);
            }
        }
        return g;
    }

    /**
     * since the drone can only move to nodes on the graph, the delivery coordinates of an order may not exactly be a node
     * the method finds the node that is closest to the delivery coordinate. since the distance between each node is 0.00015 degrees
     * there will definitely be a node that is considered "close to" the delivery coordinate
     * @param hexGraph the graph of valid nodes and edges
     * @return a hashmap of order numbers to the delivery nodes
     */
    private static HashMap<String,LongLat> getDeliveryNodes(Graph<LongLat,NodeEdges> hexGraph){
        if(hexGraph==null){
            System.err.println("Input cannot be null");
        }
        ArrayList<String> orderNos = Orders.orderNos;
        HashMap<String,double[]> deliveryCoordinates = Orders.deliveryCoordinates;
        HashMap<String,LongLat> deliveryNodes = new HashMap<>();
        // goes through each order
        for(String order : orderNos){
            LongLat currentOrder = new LongLat(deliveryCoordinates.get(order)[0],deliveryCoordinates.get(order)[1]);
            boolean found = false;
            //goes through each node on the graph until a node that is close to the coordinate is found
            for (LongLat nodes : hexGraph.vertexSet()) {
                if (currentOrder.closeTo(nodes)) {
                    deliveryNodes.put(order, nodes);
                    found = true;
                    break;
                }
            }
            //if it is unable to find a node, it prints out the message
            if(!found){
                System.out.println("cant find delivery node"+ order);
            }

        }
        return deliveryNodes;
    }


    /**
     * since the drone can only move to nodes on the graph, the pick up coordinates of an order may not exactly be a node
     * the method finds the node that is closest to the pick up coordinate. since the distance between each node is 0.00015 degrees
     * there will definitely be a node that is considered "close to" the pick up  coordinate
     * @param hexGraph he graph of valid nodes and edges
     * @return a hashmap of order numbers to the list of pick up nodes
     */
    private static HashMap<String,ArrayList<LongLat>> getPickUpNodes(Graph<LongLat,NodeEdges> hexGraph){
        if(hexGraph==null){
            System.err.println("Input cannot be null");
        }
        ArrayList<String> orderNos = Orders.orderNos;
        HashMap<String, ArrayList<double[]>> pickUpCoordinates= Orders.pickUpCoordinates;
        HashMap<String,ArrayList<LongLat>> pickUpNodes = new HashMap<>();
        //goes through each order
        for(String order:orderNos){
           ArrayList<double[]> pickUpShops = pickUpCoordinates.get(order);
           ArrayList<LongLat> nodes = new ArrayList<>();
           //goes through each pick up location for an order
           for (double[] shops:pickUpShops){
               boolean found = false;
               LongLat curr = new LongLat(shops[0],shops[1]);
               // goes through every node until a node close to the location is found
               for (LongLat node : hexGraph.vertexSet()) {
                   if (curr.closeTo(node)) {
                       nodes.add(node);
                       found = true;
                       break;
                   }
               }
               //if we cant find a node close to the location, the message is printed
               if(!found){
                   System.out.println("not found "+ order);
               }
           }
           pickUpNodes.put(order,nodes);
        }
        return pickUpNodes;
    }

    /**
     * the method gets the node on the graph that is closest to appleton tower
     * @param g the graph of valid nodes and edges
     * @return the LongLat coordinates of the node closest to appleton tower
     */
    private static LongLat getAppleton(Graph<LongLat,NodeEdges> g){
        if(g == null){
            System.err.println("Input cannot be null");

        }
        //searches through every node until a node close to appleton is found
        for (LongLat nodes : g.vertexSet()) {
            if (LongLat.appleton.closeTo(nodes)) {
                return nodes;

            }
        }
        return null;
    }


}
