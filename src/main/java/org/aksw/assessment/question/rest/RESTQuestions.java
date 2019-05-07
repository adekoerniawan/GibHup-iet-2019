/**
 * 
 */
package org.aksw.assessment.question.rest;

import com.google.common.base.Joiner;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Lorenz Buehmann
 * 
 */
@XmlRootElement(name = "questions")
public class RESTQuestions {

	// @XmlElement(name = "question", type = RESTQuestions.class)
	private List<RESTQuestion> questions;

	public List<RESTQuestion> getQuestions() {
		return questions;
	}

	public void setQuestions(List<RESTQuestion> questions) {
		this.questions = questions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return Joiner.on("\n##################\n").join(questions);
	}

}
