package model.participant;

import java.util.Objects;


// compara los paises por su nombre para poder validar correctamente
public class Country {
    private String name;

    public Country(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Country country = (Country) obj;
        return Objects.equals(name, country.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}