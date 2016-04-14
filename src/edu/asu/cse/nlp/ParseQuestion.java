	package edu.asu.cse.nlp;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;

import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Servlet implementation class ParseQuestion
 */

@WebServlet("/ParseQuestion")
public class ParseQuestion extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public ParseQuestion() {
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String question = request.getParameter("question");
		String subject = request.getParameter("subject");
		POS_tagger tagger = new POS_tagger(subject,question);
		String res = null;
		try {
			res = tagger.getResult();
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (UnirestException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("exepcted" +res);
		response.getWriter().write(res);
	}
}
