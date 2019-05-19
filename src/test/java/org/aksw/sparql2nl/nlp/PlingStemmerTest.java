package org.aksw.sparql2nl.nlp;


import org.aksw.sparql2nl.nlp.stemming.PlingStemmer;
import org.junit.Assert;
import org.junit.Test;

public class PlingStemmerTest {

    @Test
    public void testIsPluralTrue() {
        PlingStemmer stemmer = new PlingStemmer();
        boolean result = stemmer.isPlural("doggies");
        Assert.assertEquals(true, result);
    }

    @Test
    public void testIsPluralFalse() {
        PlingStemmer stemmer = new PlingStemmer();
        boolean result = stemmer.isPlural("doggie");
        Assert.assertEquals(false, result);
    }

    @Test
    public void testIsSinularTrue() {
        PlingStemmer stemmer = new PlingStemmer();
        boolean result = stemmer.isSingular("doggie");
        Assert.assertEquals(true, result);
    }

    @Test
    public void testIsSingularFalse() {
        PlingStemmer stemmer = new PlingStemmer();
        boolean result = stemmer.isSingular("doggies");
        Assert.assertEquals(false, result);
    }

    @Test
    public void testIsSingularAndPluralTrue() {
        PlingStemmer stemmer = new PlingStemmer();
        boolean result = stemmer.isSingularAndPlural("statistics");
        Assert.assertEquals(true, result);
    }

    @Test
    public void testIsSingularAndPluralFalse() {
        PlingStemmer stemmer = new PlingStemmer();
        boolean result = stemmer.isSingularAndPlural("horse");
        Assert.assertEquals(false, result);
    }

    @Test
    public void testCut() {
        PlingStemmer stemmer = new PlingStemmer();
        String result = stemmer.cut("helper", "er");
        Assert.assertEquals("help", result);
    }

    @Test
    public void testNoLatinTrue() {
        PlingStemmer stemmer = new PlingStemmer();
        boolean result = stemmer.noLatin("military"); //military comes from French
        Assert.assertEquals(true, result);
    }

    @Test
    public void testNoLatinFalse() {
        PlingStemmer stemmer = new PlingStemmer();
        boolean result = stemmer.noLatin("picture  "); //picture comes from Latin (picture word)
        Assert.assertEquals(false, result);
    }

    @Test
    public void testGreekFalse() {
        PlingStemmer stemmer = new PlingStemmer();
        boolean result = stemmer.greek("military"); //military comes from French
        Assert.assertEquals(false, result);
    }

    @Test
    public void testGreekTrue() {
        PlingStemmer stemmer = new PlingStemmer();
        boolean result = stemmer.greek("Hermaphrodite"); //Hermaphrodite comes from Greek
        Assert.assertEquals(true, result);
    }

    @Test
    public void testStemForPlural() {
        PlingStemmer stemmer = new PlingStemmer();
        String result = stemmer.stem("statistics");
        Assert.assertEquals("house", result);
    }

    @Test
    public void testStemForSingular() {
        PlingStemmer stemmer = new PlingStemmer();
        String result = stemmer.stem("house");
        Assert.assertEquals("house", result);
    }

    @Test
    public void testStemForSingularAndPlural() {
        PlingStemmer stemmer = new PlingStemmer();
        String result = stemmer.stem("statistics");
        Assert.assertEquals("statistic", result);
    }

    @Test
    public void testStemForWordEndsX() {
        PlingStemmer stemmer = new PlingStemmer();
        String result = stemmer.stem("simplices"); //szimplex magyar szó (def: A szimplex a matematikában a háromszög illetve a tetraéder általánosítása végesdimenziós vektortérre.)
        Assert.assertEquals("simplex", result);
    }











}
