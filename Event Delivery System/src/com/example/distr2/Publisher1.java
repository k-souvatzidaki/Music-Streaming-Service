package com.example.distr2;

import java.io.*;

public class Publisher1 {

    public static void main(String[] args) throws IOException {

        System.out.println("Creating new Publisher with Artist Names 0-j");
        Publisher pub = new Publisher(Node.publishers.get(0).getIp(), Node.publishers.get(0).getPort(),"../Project Distributed/0-J");

    }

}
