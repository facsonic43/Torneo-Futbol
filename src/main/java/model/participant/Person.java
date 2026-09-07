package model.participant;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

public abstract class Person {
    private String name;
    private int idNumber;
    private String idType;
    private LocalDate birthDate;
    private Country nationality;

    public Person(String name, int idNumber, String idType, LocalDate birthDate, Country nationality) {
        this.name = name;
        this.idNumber = idNumber;
        this.idType = idType;
        this.birthDate = birthDate;
        this.nationality = nationality;
    }

    public String getName() {
        return name;
    }

    public int getIdNumber() {
        return idNumber;
    }

    public String getIdType() {
        return idType;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Country getNationality() {
        return nationality;
    }

    public int getAge() {
        int age = 0;
        if (birthDate != null) {
            age = Period.between(birthDate, LocalDate.now()).getYears();
        }
        return age;
    }
}
