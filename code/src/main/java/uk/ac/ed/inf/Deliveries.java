package uk.ac.ed.inf;

/**
 * class for the deliveries objects that will be added to the deliveries database
 */
public class Deliveries {
    /**
     * order number for each order
     */
    String orderNo;
    /**
     * WhatThreeWords address of the delivery location
     */
    String deliveredTo;
    /**
     * the cost in pence to complete the order. including the standard delivery charge.
     */
    int costInPence;
}
