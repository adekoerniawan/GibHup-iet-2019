package org.aksw.sparql2nl.entitysummarizer.gender;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class LexiconBasedGenderDetectorTest {

    @Test
    public void testMaleName() {
        LexiconBasedGenderDetector genderDetector = new LexiconBasedGenderDetector();
        assertEquals(Gender.MALE , genderDetector.getGender("Zinedine Ngonga"));
    }

    @Test
    public void testFemaleName() {
        LexiconBasedGenderDetector genderDetector = new LexiconBasedGenderDetector();
        assertEquals(Gender.FEMALE , genderDetector.getGender("Nova Varga"));
    }

    @Test
    public void testUnknownName() {
        LexiconBasedGenderDetector genderDetector = new LexiconBasedGenderDetector();
        assertEquals(Gender.UNKNOWN , genderDetector.getGender("asd aas"));
    }

    @Test
    public void testShortName() {
        LexiconBasedGenderDetector genderDetector = new LexiconBasedGenderDetector();
        assertEquals(Gender.MALE , genderDetector.getGender("Renato"));
    }

    @Test
    public void testLongName() {
        LexiconBasedGenderDetector genderDetector = new LexiconBasedGenderDetector();
        assertEquals(Gender.MALE , genderDetector.getGender("Renato Alfonzo Rikardo Huan"));
    }

    @Test
    public void setMaleNamesPath() {
        LexiconBasedGenderDetector genderDetector = new LexiconBasedGenderDetector();
        genderDetector.setMaleNamesPath("gender/male.txt");
        assertEquals(Gender.MALE , genderDetector.getGender("Zinedine Ngonga"));
    }

    @Test
    public void setFemaleNamesPath() {
        LexiconBasedGenderDetector genderDetector = new LexiconBasedGenderDetector();
        genderDetector.setFemaleNamesPath("gender/female.txt");
        assertEquals(Gender.FEMALE , genderDetector.getGender("Nova Varga"));
    }

    @Test
    public void ownList(){
        String[] males ={"Adam", "Bela"};
        String[] females ={"Kata", "Juli"};
        LexiconBasedGenderDetector genderDetector = new LexiconBasedGenderDetector(
                new HashSet<>(Arrays.asList(males)),
                new HashSet<>(Arrays.asList(females)));
        assertEquals(Gender.FEMALE , genderDetector.getGender("Juli Varga"));
        assertEquals(Gender.MALE , genderDetector.getGender("Adam Varga"));
    }
}