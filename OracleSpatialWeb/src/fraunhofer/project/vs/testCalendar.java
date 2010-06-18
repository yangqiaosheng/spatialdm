package fraunhofer.project.vs;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class testCalendar
 */
public class testCalendar extends HttpServlet {	
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public testCalendar() {
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    /*doPost doen't have a max bound of the data which can receive.*/
    /* doGet has 4Kb the max amount of data that can receive */
    
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setContentType("text");		
		// for parameter xml2 parameter
		String s="";
		s = request.getParameter("xml2");
		if(s!=null)
		{
			System.out.println("xml2: " + s);
		// 	this to modify all the time.
			String path = "C:\\java_file\\eclipse\\MyEclipse\\project2\\WebRoot\\xml";
			try {
				BufferedWriter out1 = new BufferedWriter(new FileWriter(path+ "xml2.xml"));
				out1.write(s);
				out1.close();
				System.out.println("the string that I wrote in the file is: " + s);
			} catch (IOException e) {
				System.out.println("Exception " + e.getMessage());
			}
		}
		if (request.getParameter("xml") != null) {
			// for parameter xml parameter
			PrintWriter out = response.getWriter();
			out.print(request.getParameter("xml"));// out send the response
			out.println("<p>I am on the server side</p>");
		}
	}
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	//	response.setContentType("text");
		//PrintWriter out = response.getWriter();
	//	out.print(request.getParameter("xml"));
	//	out.println("<p>I am on the server side</p>");				 		
			
		}	
}
