package model.match;

import java.io.Serializable;

public class Stadium implements Serializable{

    private int id;
    private String name;
    private int cityId;

    public Stadium(String name, int cityId) {
        this.name = name;
        this.cityId = cityId;
    }

    public Stadium(int id, String name, int cityId) {
        this.id = id;
        this.name = name;
        this.cityId = cityId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }

    public int getCityId() { return cityId; }

    @Override
    public String toString() {
        return name;
    }
}
