package model.match;

import java.util.Objects;

public class Stadium {
    private String name;
    private City city;
    private int capacity;

    public Stadium(String name, City city, int capacity) {
        this.name = name;
        this.city = city;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public City getCity() {
        return city;
    }

    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean equals(Object obj) {
        boolean isEqual = false;
        if (this == obj) {
            isEqual = true;
        } else if (obj != null && getClass() == obj.getClass()) {
            Stadium other = (Stadium) obj;
            isEqual = Objects.equals(name, other.name) && Objects.equals(city, other.city);
        }
        return isEqual;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, city);
    }

    @Override
    public String toString() {
        return name + " [" + city.getName() + ", Cap: " + capacity + "]";
    }
}
