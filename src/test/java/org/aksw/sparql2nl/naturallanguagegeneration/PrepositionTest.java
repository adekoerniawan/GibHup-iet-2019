package org.aksw.sparql2nl.naturallanguagegeneration;

import org.junit.Test;

import java.io.ByteArrayInputStream;

public class PrepositionTest {

    @Test
    public void testPreposition()
    {
        Preposition preposition = new Preposition( new ByteArrayInputStream(("first\nsecond\nTHIRD\nfourth fifth").getBytes()));
        assert (preposition.isPreposition("first"));
        assert (preposition.isPreposition("second"));
        assert (preposition.isPreposition("third"));
        assert (!preposition.isPreposition("THIRD"));
        assert (preposition.isPreposition("fourth fifth"));
    }

}
