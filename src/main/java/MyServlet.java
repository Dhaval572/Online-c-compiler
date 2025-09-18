import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/MyServlet")
public class MyServlet extends HttpServlet 
{
    private String db_url;
    private String db_user;
    private String db_password;

    @Override
    public void init() throws ServletException 
    {
        db_url = getServletConfig().getInitParameter("dbUrl");
        db_user = getServletConfig().getInitParameter("dbUser");
        db_password = getServletConfig().getInitParameter("dbPassword");
    }

    @Override
    protected void doGet
    (
        HttpServletRequest request, 
        HttpServletResponse response
    )
            throws ServletException, IOException 
    {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<html><body>");
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try 
        {
            // Load JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            conn = DriverManager.getConnection(db_url, db_user, db_password);
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT id, name FROM emp");

            out.println("<p>Connection successful!</p>");
            out.println("<h2>Query Results:</h2>");

            while (rs.next()) 
            {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                out.printf("<p>ID: %d, Name: %s</p>%n", id, name);
            }
        } 
        catch (ClassNotFoundException | SQLException e) 
        {
            out.println("<p>Error: " + e.getMessage() + "</p>");
            e.printStackTrace(out);
        }
        finally 
        {
            // Close resources in reverse order
            try 
            {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            }
            catch (SQLException e) 
            {
                // Ignore cleanup errors
            }
        }

        out.println("</body></html>");
    }
}