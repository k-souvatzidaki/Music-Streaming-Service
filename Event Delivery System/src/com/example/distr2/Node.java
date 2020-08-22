package com.example.distr2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/** the basic system component - a Node */
public abstract class Node {

    //The zookeeper ip and address
    static protected final Address zookeeper = new Address("192.168.2.5",6000);
    //Broker addresses - consumer ports and publisher ports
    static protected final ArrayList<Address> brokers_consumer_ports = new ArrayList<Address>(
            Arrays.asList(new Address("192.168.2.5",4887), new Address("192.168.2.5",4897), new Address("192.168.2.5",4907))
    );
    static protected final ArrayList<Address> brokers_publisher_ports = new ArrayList<Address>(
            Arrays.asList(new Address("192.168.2.5",4888), new Address("192.168.2.5",4898), new Address("192.168.2.5",4908))
    );
    //Publisher addresses
    static protected final ArrayList<Address> publishers = new ArrayList<Address>(
            Arrays.asList(new Address("192.168.2.5", 5000),new Address("192.168.2.5", 5001))
    );


    /** Node initialization and connection establishment (calls connect() method) */
    abstract void init() throws IOException;
    /** Creates the required connections of the Node - socket creation etc */
    abstract void connect() throws IOException;
    /** Disconnection - closing sockets, objectI/Ostreams etc */
    abstract void disconnect() throws IOException;

}
