package com.example.distr2;

import java.io.IOException;

public class Publisher2 {

    public static void main(String[] args) throws IOException {

        System.out.println("Creating new Publisher with Artists Names K-Z");
        Publisher pub = new Publisher(Node.publishers.get(1).getIp(), Node.publishers.get(1).getPort(),"../Project Distributed/K-Z");

    }

}
