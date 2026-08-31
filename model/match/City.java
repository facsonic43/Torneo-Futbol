package model.match;

import model.participant.Country;
import java.util.Objects;

public class City {

    private int id;
    private String name;
    private String country;

    public City(String name, String country) {
        this.name = name;
        this.country = country;
    }

    public City(int id, String name, String country) {
        this.id = id;
        this.name = name;
        this.country = country;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    @Override
    public String toString() {
        return name + " (" + country + ")";
    }
}
