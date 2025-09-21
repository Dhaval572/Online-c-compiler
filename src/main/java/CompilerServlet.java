import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/CompilerServlet")
public class CompilerServlet extends HttpServlet 
{
    // Security patterns to detect potentially dangerous code
    private static final String[] DANGEROUS_PATTERNS = 
    {
        "\\b(system|exec|fork|kill|popen|system_call)\\b",
        "\\b(/bin/|/usr/bin/|/etc/|/root/)\\b",
        "\\b(sh|bash|zsh|ksh|csh|tcsh)\\b",
        "\\b(fopen|fwrite|fread|open|write|read)\\b.*(/etc/|/root/)",
        "\\b(include)\\b.*[<\"](/etc/|/root/)",
        "\\b(fork|exec|system)\\s*\\(",
        "\\bsystem\\s*\\(",
        "\\bpopen\\s*\\(",
        "\\bexec\\s*\\("
    };
    
    @Override
    protected void doPost
    (
        HttpServletRequest request, 
        HttpServletResponse response
    )
        throws ServletException, IOException 
    {
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        
        String action = request.getParameter("action");
        String code = request.getParameter("code");
        String input = request.getParameter("input"); // Get input parameter
        
        if (action == null || code == null) 
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("Error: Missing action or code parameter");
            return;
        }
        
        // Security check
        if (containsDangerousCode(code)) 
        {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            out.println("Error: Code contains potentially dangerous operations and has been blocked for security reasons.");
            
        }
        
        // Handle compilation request
        else if ("compile".equals(action)) 
        {
            compileCode(code, input, out);
        }
        // Handle execution request
        else if ("run".equals(action)) 
        {
            runCode(code, input, out);
        }
        else 
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("Error: Invalid action. Use 'compile' or 'run'");
        }
    }
    
    private boolean containsDangerousCode(String code) 
    {
        String lower_code = code.toLowerCase();
        
        // Check for dangerous patterns
        for (String pattern : DANGEROUS_PATTERNS) 
        {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(lower_code).find()) 
            {
                return true;
            }
        }
        
        // Check for file system access attempts
        return lower_code.contains("#include") 
            && (lower_code.contains("/etc/") 
            || lower_code.contains("/root/") 
            || lower_code.contains("../") 
            || lower_code.contains("..\\"));
    }
    
    private void compileCode(String code, String input, PrintWriter out) 
    {
        File temp_file = null;
        String file_name = null;
        String base_name = null;
        
        try 
        {
            // Create a temporary file for the C code
            temp_file = File.createTempFile("program", ".c");
            file_name = temp_file.getAbsolutePath();
            base_name = file_name.substring(0, file_name.lastIndexOf('.'));
            
            // Write the code to the temporary file
            try (FileWriter writer = new FileWriter(temp_file)) 
            {
                writer.write(code);
            }
            
            // Compile the C code using gcc
            ProcessBuilder pb = new ProcessBuilder("gcc", file_name, "-o", base_name);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // Read the compilation output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) 
            {
                String line;
                while ((line = reader.readLine()) != null) 
                {
                    output.append(line).append("\n");
                }
            }
            
            int exit_code = process.waitFor();
            
            if (exit_code == 0) 
            {
                out.println("Compilation successful!");
            } 
            else 
            {
                out.println("Compilation failed:");
                out.println(output.toString());
            }
        }
        catch (IOException | InterruptedException e) 
        {
            out.println("Compilation error: " + e.getMessage());
        }
        finally 
        {
            // Clean up temporary files
            if (temp_file != null) 
            {
                temp_file.delete();
            }
            
            if (base_name != null) 
            {
                new File(base_name + ".exe").delete(); // Windows executable extension
            }
        }
    }
    
    private void runCode(String code, String input, PrintWriter out) 
    {
        File temp_file = null;
        String file_name = null;
        String base_name = null;
        
        try 
        {
            // Create a temporary file for the C code
            temp_file = File.createTempFile("program", ".c");
            file_name = temp_file.getAbsolutePath();
            base_name = file_name.substring(0, file_name.lastIndexOf('.'));
            
            // Write the code to the temporary file
            try (FileWriter writer = new FileWriter(temp_file)) 
            {
                writer.write(code);
            }
            
            // Compile the C code using gcc
            ProcessBuilder compile_pb = new ProcessBuilder("gcc", file_name, "-o", base_name);
            compile_pb.redirectErrorStream(true);
            Process compile_process = compile_pb.start();
            
            // Read the compilation output
            StringBuilder compile_output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(compile_process.getInputStream()))) 
            {
                String line;
                while ((line = reader.readLine()) != null) 
                {
                    compile_output.append(line).append("\n");
                }
            }
            
            int compile_exit_code = compile_process.waitFor();
            
            if (compile_exit_code != 0) 
            {
                out.println("Compilation failed:");
                out.println(compile_output.toString());
                return;
            }
            
            // Run the compiled program with a timeout
            ProcessBuilder run_pb = new ProcessBuilder(base_name);
            run_pb.redirectErrorStream(true);
            Process run_process = run_pb.start();
            
            // If input is provided, send it to the program
            if (input != null && !input.trim().isEmpty()) {
                try (PrintWriter processInput = new PrintWriter(run_process.getOutputStream(), true)) {
                    processInput.print(input);
                    processInput.flush();
                }
            }
            // Close the output stream to signal end of input
            run_process.getOutputStream().close();
            
            // Set a timeout for execution (10 seconds for interactive programs)
            boolean finished = run_process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!finished) {
                run_process.destroyForcibly();
                out.println("Error: Program execution timed out (10 seconds limit)");
                return;
            }
            
            // Read the program output
            StringBuilder run_output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(run_process.getInputStream()))) 
            {
                String line;
                while ((line = reader.readLine()) != null) 
                {
                    run_output.append(line).append("\n");
                }
            }
            
            int exit_code = run_process.exitValue();
            if (exit_code == 0) {
                out.println(run_output.toString());
            } else {
                out.println("Program exited with code: " + exit_code);
                out.println(run_output.toString());
            }
        }
        catch (IOException | InterruptedException e) 
        {
            out.println("Execution error: " + e.getMessage());
        }
        finally 
        {
            // Clean up temporary files
            if (temp_file != null) 
            {
                temp_file.delete();
            }
            
            if (base_name != null) 
            {
                new File(base_name + ".exe").delete(); // Windows executable extension
            }
        }
    }
}