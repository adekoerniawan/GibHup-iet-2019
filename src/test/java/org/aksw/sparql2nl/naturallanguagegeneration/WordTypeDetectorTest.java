package org.aksw.sparql2nl.naturallanguagegeneration;

import org.junit.Test;

public class WordTypeDetectorTest {

    private WordTypeDetector wtd = new WordTypeDetector();

    @Test
    public void testIsNoun(){
        String testNoun = "Table";
        assert(wtd.isNoun(testNoun));
    }

    @Test
    public void testIsNotNoun(){
        String testVerb = "To work";
        assert(!wtd.isNoun(testVerb));
    }
}
