package org.aksw.sparql2nl.nlp;


import org.aksw.sparql2nl.nlp.stemming.PlingStemmer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PlingStemmerTest {

    private PlingStemmer stemmer;

    @Before
    public void initStemmer() {
        stemmer = new PlingStemmer();;
    }

    /**
     * Check the function work properly for a plural word.
     * @result true, if the parameter is plural
     */
    @Test
    public void testIsPluralTrue() {
        boolean result = stemmer.isPlural("doggies");
        Assert.assertEquals(true, result);
    }

    /**
     * Check the function work properly for a singular word.
     * @result true, if the parameter is plural
     */
    @Test
    public void testIsPluralFalse() {
        boolean result = stemmer.isPlural("doggie");
        Assert.assertEquals(false, result);

    }
    /**
     * Check the function work properly for non-word.
     * @result true, if the parameter is plural
     */
    @Test
    public void testIsPluralError() {
        boolean result = stemmer.isPlural("12345");
        Assert.assertEquals(false, result);
    }

    /**
     * Check the function work properly for a singular word.
     * @result true, if the parameter is singular
     */
    @Test
    public void testIsSingularTrue() {
        boolean result = stemmer.isSingular("doggie");
        Assert.assertEquals(true, result);
    }

    /**
     * Check the function work properly for a plural word.
     * @result true, if the parameter is singular
     */
    @Test
    public void testIsSingularFalse() {
        boolean result = stemmer.isSingular("doggies");
        Assert.assertEquals(false, result);
    }

    /**
     * Check the function work properly for an irregular word, which can be plural and singular either
     * @result true, if the parameter is irregular
     */
    @Test
    public void testIsSingularAndPluralTrue() {
        boolean result = stemmer.isSingularAndPlural("statistics");
        Assert.assertEquals(true, result);
    }

    /**
     * Check the function work properly for a non-irregular word, which can not be plural and singular either
     * @result true, if the parameter is irregular
     */
    @Test
    public void testIsSingularAndPluralFalse() {
        boolean result = stemmer.isSingularAndPlural("horse");
        Assert.assertEquals(false, result);
    }

    /**
     * Check the function work properly
     * @result the word without its suffix
     */
    @Test
    public void testCut() {
        String result = stemmer.cut("helper", "er");
        Assert.assertEquals("help", result);
    }

    /**
     * Check the function work properly for a non-latin word
     * @result true, if the parameter is non-latin
     */
    @Test
    public void testNoLatinTrue() {
        boolean result = stemmer.noLatin("military"); //military comes from French
        Assert.assertEquals(true, result);
    }

    /**
     * Check the function work properly for a latin word
     * @result true, if the parameter is latin
     */
    @Test
    public void testNoLatinFalse() {
        boolean result = stemmer.noLatin("picture  "); //picture comes from Latin (picture word)
        Assert.assertEquals(false, result);
    }

    /**
     * Check the function work properly for a non-greek word
     * @result true, if the parameter is greek
     */
    @Test
    public void testGreekFalse() {
        boolean result = stemmer.greek("military"); //military comes from French
        Assert.assertEquals(false, result);
    }

    /**
     * Check the function work properly for a greek word
     * @result true, if the parameter is greek
     */
    @Test
    public void testGreekTrue() {
        boolean result = stemmer.greek("Hermaphrodite"); //Hermaphrodite comes from Greek
        Assert.assertEquals(true, result);
    }

    /**
     * Check the conversion works properly
     * @result the singular version of the word
     */
    @Test
    public void testStemForPlural() {
        String result = stemmer.stem("houses");
        Assert.assertEquals("house", result);
    }

    /**
     * Check the conversion works properly (nothing should happen)
     * @result the singular version of the word
     */
    @Test
    public void testStemForSingular() {
        String result = stemmer.stem("house");
        Assert.assertEquals("house", result);
    }

    /**
     * Check the conversion works properly (conversion to the singular version of the irregular word)
     * @result the singular version of the word
     */
    @Test
    public void testStemForSingularAndPlural() {
        String result = stemmer.stem("statistics");
        Assert.assertEquals("statistic", result);
    }

    /**
     * Check the conversion works properly (conversion to the singular version of the irregular word)
     * @result the singular version of the word
     */
    @Test
    public void testStemForWordEndsX() {
        String result = stemmer.stem("simplices"); //szimplex magyar szó (def: A szimplex a matematikában a háromszög illetve a tetraéder általánosítása végesdimenziós vektortérre.)
        Assert.assertEquals("simplex", result);
    }











}
