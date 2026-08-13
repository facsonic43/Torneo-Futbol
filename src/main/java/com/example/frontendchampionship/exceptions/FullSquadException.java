package com.example.frontendchampionship.exceptions;

public class FullSquadException extends RuntimeException {

    public FullSquadException() {
        super("The squad has reached its maximum limit of 18 players.");
    }
}