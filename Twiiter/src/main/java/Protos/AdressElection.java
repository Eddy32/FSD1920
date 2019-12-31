package Protos;

import io.atomix.utils.net.Address;

public class AdressElection {

    private Address address;
    private int id_starter;

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public int getId_starter() {
        return id_starter;
    }

    public void setId_starter(int id_starter) {
        this.id_starter = id_starter;
    }

    public AdressElection(Address address, int id_starter) {
        this.address = address;
        this.id_starter = id_starter;
    }
}
