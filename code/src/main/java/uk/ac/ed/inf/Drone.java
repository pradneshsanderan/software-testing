package uk.ac.ed.inf;

import java.util.ArrayList;

public class Drone {
    //Variables used
    /**
     * the number of moves the drone has left before it runs out of battery
     */
    public int MovesLeft;
    /**
     * the amount of money earned by completing the deliveries on a certain day
     */
    public static int totalAmountEarned;
    /**
     * percentage of money earned compared to the money that would  have been earned if all orders were completed
     */
    public static double percentagMonetaryValue;

    //Standard Contructors

    /**
     * the contructor for the drone class
     * @param movesLeft the number of moves the drone has left. specified at the start.
     */
    Drone(int movesLeft){
        this.MovesLeft = movesLeft;
    }

    //Public Methods

    /**
     * the method calculates the percentage of monetary value for the day. it first calculates the amount
     * of money that was earned and then it calculates the total amount that could have been earned if all the orders were completed.
     * it then returns the percentage. the method is used for my own checking of the efficiency of my program
     * @param OrdersComp a list of the orders completed on that day
     * @return the percentage of monetary value
     */
    public static double getPercentageMonetaryValue(ArrayList<String> OrdersComp){
        // money earned
        double totalCompleted = Menus.getTotalChargeForOrders(OrdersComp);
        totalAmountEarned = (int)totalCompleted;
        //total amount we could have earned
        double supposedAmount = Menus.getTotalChargeForOrders(Orders.orderNos);
        //percentage of monetary value
        double percent = (totalCompleted/supposedAmount)*100;
        percentagMonetaryValue = percent;
        return percent;
    }


}








