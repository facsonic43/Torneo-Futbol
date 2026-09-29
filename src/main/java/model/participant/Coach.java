package model.participant;

import model.match.Formation;

import java.time.LocalDate;

public class Coach extends Person{
    private int titlesObtained;

    public Coach(String name, int idNumber, String idType, LocalDate birthDate, Country nationality, int titlesObtained) {
        super(name, idNumber, idType, birthDate, nationality);
        this.titlesObtained = titlesObtained;
    }

    public int getTitlesObtained() {
        return titlesObtained;
    }

    public Formation chooseFormation(Team team, Team opponent) {
        double powerDifference = opponent.getTeamPower() - team.getTeamPower();
        int formationIndex = titlesObtained;

        if (powerDifference >= 20) {
            Formation[] defensive = {
                    Formation.FIVE_FOUR_ONE,
                    Formation.FOUR_FIVE_ONE,
                    Formation.THREE_FIVE_TWO,
                    Formation.THREE_FIVE_ONE_ONE,
                    Formation.FOUR_ONE_FOUR_ONE,
                    Formation.FIVE_THREE_TWO,
                    Formation.THREE_FOUR_ONE_TWO
            };
            return defensive[formationIndex % defensive.length];
        }
        if (powerDifference >= 10) {
            Formation[] cautious = {
                    Formation.FOUR_FIVE_ONE,
                    Formation.THREE_FIVE_TWO,
                    Formation.FOUR_ONE_FOUR_ONE,
                    Formation.FOUR_FOUR_ONE_ONE,
                    Formation.FIVE_THREE_TWO
            };
            return cautious[formationIndex % cautious.length];
        }
        if (powerDifference >= 3) {
            Formation[] balancedDefensive = {
                    Formation.THREE_FIVE_TWO,
                    Formation.FOUR_FIVE_ONE,
                    Formation.FOUR_FOUR_ONE_ONE,
                    Formation.FOUR_ONE_TWO_ONE_TWO
            };
            return balancedDefensive[formationIndex % balancedDefensive.length];
        }
        if (powerDifference <= -20) {
            Formation[] offensive = {
                    Formation.FOUR_ONE_TWO_THREE,
                    Formation.THREE_FOUR_THREE,
                    Formation.FOUR_THREE_TWO_ONE,
                    Formation.FOUR_FOUR_TWO,
                    Formation.FIVE_TWO_THREE,
                    Formation.THREE_FOUR_ONE_TWO,
                    Formation.FOUR_TWO_FOUR
            };
            return offensive[formationIndex % offensive.length];
        }
        if (powerDifference <= -10) {
            Formation[] attacking = {
                    Formation.THREE_FOUR_THREE,
                    Formation.FOUR_ONE_TWO_THREE,
                    Formation.FOUR_FOUR_TWO,
                    Formation.FOUR_THREE_THREE,
                    Formation.FIVE_TWO_THREE
            };
            return attacking[formationIndex % attacking.length];
        }
        if (powerDifference <= -3) {
            Formation[] balancedAttacking = {
                    Formation.FOUR_FOUR_TWO,
                    Formation.FOUR_THREE_THREE,
                    Formation.FOUR_TWO_THREE_ONE,
                    Formation.FOUR_ONE_TWO_ONE_TWO,
                    Formation.FOUR_FOUR_ONE_ONE,
                    Formation.THREE_FOUR_ONE_TWO
            };
            return balancedAttacking[formationIndex % balancedAttacking.length];
        }
        Formation[] balanced = {
                Formation.FOUR_THREE_THREE,
                Formation.FOUR_TWO_THREE_ONE,
                Formation.FOUR_ONE_TWO_ONE_TWO,
                Formation.FOUR_FOUR_ONE_ONE,
                Formation.FOUR_THREE_TWO_ONE,
                Formation.FIVE_THREE_TWO,
                Formation.THREE_FOUR_ONE_TWO
        };
        return balanced[formationIndex % balanced.length];
    }

    @Override
    protected String getAditionalInfo() {
        return "Tittles obtained: " + titlesObtained;
    }
}
