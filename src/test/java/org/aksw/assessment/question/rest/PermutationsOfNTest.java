package org.aksw.assessment.question.rest;


import org.aksw.assessment.question.PermutationsOfN;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class PermutationsOfNTest
{

    @Test
    public void testGetSubsetsOfSizeN()
    {
        List<Integer> myList = new ArrayList<>();
        myList.add(1);
        myList.add(2);
        myList.add(3);
        myList.add(4);
        List<List<Integer>> result = PermutationsOfN.getSubsetsOfSizeN(myList, 2);

        assert(result.size() == 6);
        List <Integer> assertSubset = new ArrayList<>();
        assertSubset.add(1);
        assertSubset.add(3);
        assert(result.contains(assertSubset));

        List <Integer> assertNotExistingSubset = new ArrayList<>();
        assertNotExistingSubset.add(1);

        assert(!result.contains(assertNotExistingSubset));
    }

    @Test
    public void testGetPermutationsOfSizeN()
    {
        List<Integer> myList = new ArrayList<>();
        myList.add(1);
        myList.add(2);
        myList.add(3);
        myList.add(4);

        Collection<List<Integer>> result = PermutationsOfN.getPermutationsOfSizeN(myList, 4);

        assert (result.size() == 24);
        List <Integer> assertCollection = new ArrayList<>();
        assertCollection.add(1);
        assertCollection.add(3);
        assertCollection.add(2);
        assertCollection.add(4);

        assert(result.contains(assertCollection));

        List <Integer> assertNotExistingCollection = new ArrayList<>();
        assertNotExistingCollection.add(1);
        assertNotExistingCollection.add(1);
        assertNotExistingCollection.add(1);
        assertNotExistingCollection.add(1);

        assert(!result.contains(assertNotExistingCollection));
    }

    @Test
    public void testLargerSubsetsAndPermutation()
    {
        List<Integer> myList = new ArrayList<>();
        myList.add(1);
        myList.add(2);
        myList.add(3);
        myList.add(4);

        List<List<Integer>> result = PermutationsOfN.getSubsetsOfSizeN(myList, 6);

        Collection<List<Integer>> result2 = PermutationsOfN.getPermutationsOfSizeN(myList, 6);

        assert (result.size() == 1);
        assert (result2.size() == 24);

        List <Integer> assertCollection = new ArrayList<>();
        assertCollection.add(1);
        assertCollection.add(3);
        assertCollection.add(2);
        assertCollection.add(4);

        assert (result2.contains(assertCollection));

        List <Integer> assertSubset = new ArrayList<>();
        assertSubset.add(1);
        assertSubset.add(2);
        assertSubset.add(3);
        assertSubset.add(4);
        assert(result.contains(assertSubset));
    }

    @Test
    public void testSmallerPermutations()
    {
        List<Integer> myList = new ArrayList<>();
        myList.add(1);
        myList.add(2);
        myList.add(3);
        myList.add(4);

        Collection<List<Integer>> result2 = PermutationsOfN.getPermutationsOfSizeN(myList, 3);

        assert (result2.size() == 24);

        List <Integer> assertCollection = new ArrayList<>();
        assertCollection.add(1);
        assertCollection.add(3);
        assertCollection.add(4);

        assert(result2.contains(assertCollection));
    }
}
