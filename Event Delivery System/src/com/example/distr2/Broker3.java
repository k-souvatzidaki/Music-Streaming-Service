package com.example.distr2;

public class Broker3 {

    public static void main(String[] args) {

        System.out.println("Creating Broker #3");
        new Broker("192.168.2.5", Node.brokers_consumer_ports.get(2).getPort(),
                   Node.brokers_publisher_ports.get(2).getPort());

    }

}
