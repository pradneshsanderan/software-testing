package uk.ac.ed.inf;


public class NodeEdges {
    /**
     * a node on the graph
     */
    public LongLat node1;
    /**
     * a node on the graph
     */
    public LongLat node2;

    /**
     * the constructor for the nodeEdge class
     * @param node1 the node on the graph that node2 is connected to
     * @param node2 the node on the graph that node1 is connected to
     */
    public NodeEdges(LongLat node1,LongLat node2){
        this.node1 = node1;
        this.node2 = node2;
    }

}
