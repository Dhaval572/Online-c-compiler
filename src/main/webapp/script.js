document.addEventListener('DOMContentLoaded', function() {
    // Initialize CodeMirror editor
    var editor = CodeMirror.fromTextArea(document.getElementById("code-editor"), {
        lineNumbers: true,
        theme: "dracula",
        mode: "text/x-csrc",
        matchBrackets: true,
        autoCloseBrackets: true,
        styleActiveLine: true,
        lineWrapping: true,
        indentUnit: 4,
        tabSize: 4,
        indentWithTabs: true,
        extraKeys: {
            "Ctrl-Space": "autocomplete",
            "Ctrl-/": "toggleComment",
            "Cmd-/": "toggleComment",
            "Shift-Tab": "indentLess",
            "Tab": "indentMore"
        },
        hintOptions: {
            hint: CodeMirror.hint.anyword,
            completeSingle: false
        }
    });
    
    // Set editor height
    editor.setSize(null, 400);
    
    // Store editor in global scope for access by other functions
    window.editor = editor;
    
    // Add C language keywords and functions for autocomplete
    var cKeywords = [
        "auto", "break", "case", "char", "const", "continue", "default", "do",
        "double", "else", "enum", "extern", "float", "for", "goto", "if",
        "int", "long", "register", "return", "short", "signed", "sizeof", "static",
        "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while",
        "#include", "#define", "#ifdef", "#ifndef", "#endif", "#if", "#else", "#elif"
    ];
    
    var cFunctions = [
        "printf", "scanf", "fprintf", "fscanf", "sprintf", "sscanf",
        "malloc", "calloc", "realloc", "free",
        "strlen", "strcpy", "strncpy", "strcmp", "strncmp", "strcat", "strncat",
        "memcpy", "memmove", "memcmp", "memset",
        "fopen", "fclose", "fread", "fwrite", "fgetc", "fgets", "fputc", "fputs",
        "feof", "fflush", "fseek", "ftell", "rewind",
        "exit", "abort", "atexit", "system",
        "rand", "srand", "time", "clock",
        "abs", "labs", "div", "ldiv",
        "atoi", "atol", "atof", "strtol", "strtod"
    ];
    
    // Custom autocomplete function
    CodeMirror.registerHelper("hint", "c", function(editor, options) {
        var cur = editor.getCursor();
        var token = editor.getTokenAt(cur);
        var start = token.start;
        var end = cur.ch;
        var word = token.string.slice(0, end - start);
        
        // Combine keywords and functions
        var completions = cKeywords.concat(cFunctions);
        
        // Filter based on current word
        var result = {
            list: completions.filter(function(item) {
                return item.toLowerCase().indexOf(word.toLowerCase()) === 0;
            }),
            from: CodeMirror.Pos(cur.line, start),
            to: CodeMirror.Pos(cur.line, end)
        };
        
        return result;
    });
    
    // Enable C hinting
    editor.setOption("hintOptions", {hint: CodeMirror.hint.c});
    
    // Show hint when typing
    editor.on("inputRead", function(editor, change) {
        if (change.text[0] === "." || change.text[0] === " " || change.text[0] === "(") {
            return;
        }
        CodeMirror.commands.autocomplete(editor, null, {completeSingle: false});
    });
});

function showStatus(message, isSuccess) {
    const statusDiv = document.getElementById('status');
    statusDiv.textContent = message;
    statusDiv.className = 'status ' + (isSuccess ? 'success' : 'error');
    statusDiv.style.display = 'block';
    
    // Hide status after 5 seconds
    setTimeout(() => {
        statusDiv.style.display = 'none';
    }, 5000);
}

function compileCode() {
    const code = window.editor.getValue();
    const outputDiv = document.getElementById('output');
    
    outputDiv.textContent = 'Compiling...';
    
    fetch('CompilerServlet', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: 'action=compile&code=' + encodeURIComponent(code)
    })
    .then(response => response.text())
    .then(data => {
        outputDiv.textContent = data;
        showStatus('Compilation completed', !data.includes('error'));
    })
    .catch(error => {
        outputDiv.textContent = 'Error: ' + error;
        showStatus('Compilation failed', false);
    });
}

function runCode() {
    const code = window.editor.getValue();
    const input = document.getElementById('program-input').value || ""; // Send empty string if no input
    const inputTextarea = document.getElementById('program-input'); // Get reference to input textarea
    const outputDiv = document.getElementById('output');
    
    outputDiv.textContent = 'Running...';
    
    fetch('CompilerServlet', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: 'action=run&code=' + encodeURIComponent(code) + '&input=' + encodeURIComponent(input) // Include input in the request
    })
    .then(response => response.text())
    .then(data => {
        outputDiv.textContent = data;
        showStatus('Execution completed', !data.includes('error'));
        // Clear the input textarea after running
        inputTextarea.value = "";
    })
    .catch(error => {
        outputDiv.textContent = 'Error: ' + error;
        showStatus('Execution failed', false);
        // Clear the input textarea even if there's an error
        inputTextarea.value = "";
    });
}

function formatCode() {
    const code = window.editor.getValue();
    // Simple formatting - in a real implementation, you might want to use a proper C formatter
    let formatted = code
        .replace(/\{\s*/g, "{\n    ")
        .replace(/;\s*/g, ";\n    ")
        .replace(/\s*\}/g, "\n}")
        .replace(/\n\s*\n/g, "\n")
        .trim();
        
    window.editor.setValue(formatted);
    showStatus('Code formatted successfully', true);
}

// Function to clear the input textarea
function clearInput() {
    document.getElementById('program-input').value = "";
    showStatus('Input cleared', true);
}

// Function to load a sample program that requires user input
function loadSampleInputProgram() {
    const sampleCode = `#include <stdio.h>

int main() {
    int num1, num2;
    
    printf("Enter first number: ");
    fflush(stdout);
    scanf("%d", &num1);
    
    printf("You entered: %d\\n", num1);
    
    printf("Enter second number: ");
    fflush(stdout);
    scanf("%d", &num2);
    
    printf("You entered: %d\\n", num2);
    printf("Sum of %d and %d is %d\\n", num1, num2, num1 + num2);
    
    return 0;
}`;

    window.editor.setValue(sampleCode);
    document.getElementById('program-input').value = ""; // Don't pre-fill input
    showStatus('Sample program loaded. Enter input values separated by newlines (e.g., "10\\n20") in the input box.', true);
}