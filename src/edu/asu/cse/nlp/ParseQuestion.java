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
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		//System.out.println("Here");
		
		String question = request.getParameter("question");
		String subject = request.getParameter("subject");
		
		
		
		//System.out.println("dasdasdadas " + subject);
		//System.out.println("ddsdadasdadas"+ question);
		//String type = request.getParameter("type");
		
		POS_tagger tagger = new POS_tagger(subject,question);
		String res = null;
		try {
			res = tagger.getResult();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnirestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("exepcted" +res);
		//process these inputs
		response.getWriter().write(res);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
