package com.example.distr2;

import java.io.Serializable;
import java.util.Objects;

/** A Node's address */
public class Address implements Serializable {

    private String ip;
    private int port;

    public Address(String ip, int port) {
        this.ip = ip; this.port = port;
    }

    /** GETTERS */
    public String getIp() { return ip; }

    public int getPort() { return port; }

    /** SETTERS */
    public void setPort(int port) { this.port = port; }

    public void setIp(String ip) { this.ip = ip; }


    @Override
    public String toString() {
        return this.ip +" , "+this.port;
    }

    @Override
    public int hashCode() {
        return ip.hashCode() + String.valueOf(this.port).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return port == address.port && Objects.equals(ip, address.ip);
    }

}