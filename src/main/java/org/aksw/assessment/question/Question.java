/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.assessment.question;

import com.hp.hpl.jena.query.Query;
import org.aksw.assessment.question.answer.Answer;

import java.util.List;

/**
 *
 * @author ngonga
 */
public interface Question {
    String getText();
    List<Answer> getCorrectAnswers();
    List<Answer> getWrongAnswers();    
    int getDifficulty();
    Query getQuery();
    QuestionType getType();
}
