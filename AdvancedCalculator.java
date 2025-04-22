import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedCalculator {
    private static final String OPERATORS = "+-*/^";
    private static final String FUNCTIONS = "sin|cos|tan|log|ln|sqrt|abs";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("x");
    
    // For storing and evaluating custom functions
    private final Map<String, String> savedFunctions = new HashMap<>();
    
    public AdvancedCalculator() {}
    
    public double evaluate(String expression) throws CalculatorException {
        return evaluate(expression, 0);
    }
    
    public double evaluate(String expression, double xValue) throws CalculatorException {
        try {
            // Replace variables with the given value.
            expression = replaceVariables(expression, xValue);
            
            // Replace function names with stored expressions.
            for (Map.Entry<String, String> entry : savedFunctions.entrySet()) {
                String funcName = entry.getKey();
                String funcExpr = entry.getValue();
                
                // Replace function calls with their expressions
                if (expression.contains(funcName + "(")) {
                    Pattern pattern = Pattern.compile(funcName + "\\(([^)]+)\\)");
                    Matcher matcher = pattern.matcher(expression);
                    StringBuffer sb = new StringBuffer();
                    
                    while (matcher.find()) {
                        String arg = matcher.group(1);
                        // Evaluate the argument first
                        double argValue = evaluate(arg, xValue);
                        // Replace x in the function expression with the arg value
                        String replacement = replaceVariables(funcExpr, argValue);
                        // Evaluate the resulting expression
                        double result = evaluate(replacement, xValue);
                        matcher.appendReplacement(sb, Double.toString(result));
                    }
                    matcher.appendTail(sb);
                    expression = sb.toString();
                }
            }
            
            // Parse and evaluate the expression
            return parseExpression(tokenize(expression));
        } catch (IllegalArgumentException e) {
            throw new CalculatorException("Error evaluating expression: " + e.getMessage());
        } catch (CalculatorException e) {
            throw e;
        }
    }
    
    public Function<Double, Double> createFunction(String expression) {
        return x -> {
            try {
                return evaluate(expression, x);
            } catch (CalculatorException e) {
                return Double.NaN;
            }
        };
    }
    
    public void saveFunction(String name, String expression) {
        savedFunctions.put(name, expression);
    }
    
    public String getFunctionExpression(String name) {
        return savedFunctions.getOrDefault(name, null);
    }
    
    public Set<String> getSavedFunctionNames() {
        return savedFunctions.keySet();
    }
    
    private String replaceVariables(String expression, double value) {
        return VARIABLE_PATTERN.matcher(expression).replaceAll(Double.toString(value));
    }
    
    private List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        
        // Remove all whitespace
        expression = expression.replaceAll("\\s+", "");
        
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            
            if (c == '(' || c == ')' || OPERATORS.indexOf(c) >= 0) {
                // Handle special case for negative numbers
                if (c == '-' && (i == 0 || expression.charAt(i - 1) == '(' || OPERATORS.indexOf(expression.charAt(i - 1)) >= 0)) {
                    currentToken.append(c); // Negative sign is part of the number
                } else {
                    // Add the current token if any
                    if (currentToken.length() > 0) {
                        tokens.add(currentToken.toString());
                        currentToken = new StringBuilder();
                    }
                    tokens.add(Character.toString(c));
                }
            } else if (Character.isDigit(c) || c == '.') {
                currentToken.append(c);
            } else if (Character.isLetter(c)) {
                // Handle function names and variables
                currentToken.append(c);
                
                // Look ahead to collect the entire function name
                while (i + 1 < expression.length() && Character.isLetter(expression.charAt(i + 1))) {
                    currentToken.append(expression.charAt(++i));
                }
                
                // Check if this is a function or a variable
                String token = currentToken.toString();
                if (token.matches(FUNCTIONS) || savedFunctions.containsKey(token)) {
                    tokens.add(token);
                    currentToken = new StringBuilder();
                    
                    // If the next character is '(', add it as a separate token
                    if (i + 1 < expression.length() && expression.charAt(i + 1) == '(') {
                        tokens.add("(");
                        i++;
                    }
                } else if (token.equals("x")) {
                    tokens.add(token);
                    currentToken = new StringBuilder();
                } else {
                    throw new IllegalArgumentException("Unknown function or variable: " + token);
                }
            } else {
                throw new IllegalArgumentException("Invalid character in expression: " + c);
            }
        }
        
        // Add the last token if any
        if (currentToken.length() > 0) {
            tokens.add(currentToken.toString());
        }
        
        return tokens;
    }
    
    private double parseExpression(List<String> tokens) throws CalculatorException {
        if (tokens.isEmpty()) {
            throw new CalculatorException("Empty expression");
        }
        
        // Stack for numbers
        Stack<Double> numbers = new Stack<>();
        // Stack for operators
        Stack<String> operators = new Stack<>();
        
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            
            if (token.matches("-?\\d+(\\.\\d+)?")) {
                // Number
                numbers.push(Double.valueOf(token));
            } else if (token.equals("(")) {
                operators.push(token);
            } else if (token.equals(")")) {
                // Process all operators until opening parenthesis
                while (!operators.isEmpty() && !operators.peek().equals("(")) {
                    processOperator(numbers, operators);
                }
                
                // Remove the opening parenthesis
                if (!operators.isEmpty() && operators.peek().equals("(")) {
                    operators.pop();
                } else {
                    throw new CalculatorException("Mismatched parentheses");
                }
                
                // If the top of the operators stack is a function, process it
                if (!operators.isEmpty() && operators.peek().matches(FUNCTIONS)) {
                    processFunction(numbers, operators);
                }
            } else if (OPERATORS.contains(token)) {
                // Operator
                while (!operators.isEmpty() && precedence(operators.peek()) >= precedence(token)) {
                    processOperator(numbers, operators);
                }
                operators.push(token);
            } else if (token.matches(FUNCTIONS)) {
                // Function
                operators.push(token);
            } else if (token.equals("x")) {
                throw new CalculatorException("Variable 'x' encountered but no value provided");
            } else {
                throw new CalculatorException("Unknown token: " + token);
            }
        }
        
        // Process remaining operators
        while (!operators.isEmpty()) {
            if (operators.peek().equals("(")) {
                throw new CalculatorException("Mismatched parentheses");
            }
            processOperator(numbers, operators);
        }
        
        if (numbers.size() != 1) {
            throw new CalculatorException("Invalid expression");
        }
        
        return numbers.pop();
    }
    
    private void processOperator(Stack<Double> numbers, Stack<String> operators) throws CalculatorException {
        String operator = operators.pop();
        
        if (operator.matches(FUNCTIONS)) {
            processFunction(numbers, operators);
            return;
        }
        
        if (numbers.size() < 2) {
            throw new CalculatorException("Insufficient operands for operator: " + operator);
        }
        
        double b = numbers.pop();
        double a = numbers.pop();
        
        numbers.push(switch (operator) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*" -> a * b;
            case "/" -> {
                if (b == 0) {
                    throw new CalculatorException("Division by zero");
                }
                yield a / b;
            }
            case "^" -> Math.pow(a, b);
            default -> throw new CalculatorException("Unknown operator: " + operator);
        });
    }
    
    private void processFunction(Stack<Double> numbers, Stack<String> operators) throws CalculatorException {
        String function = operators.pop();
        
        if (numbers.isEmpty()) {
            throw new CalculatorException("Insufficient operands for function: " + function);
        }
        
        double a = numbers.pop();
        
        numbers.push(switch (function) {
            case "sin" -> Math.sin(a);
            case "cos" -> Math.cos(a);
            case "tan" -> Math.tan(a);
            case "log" -> {
                if (a <= 0) {
                    throw new CalculatorException("Log of non-positive number");
                }
                yield Math.log10(a);
            }
            case "ln" -> {
                if (a <= 0) {
                    throw new CalculatorException("Natural log of non-positive number");
                }
                yield Math.log(a);
            }
            case "sqrt" -> {
                if (a < 0) {
                    throw new CalculatorException("Square root of negative number");
                }
                yield Math.sqrt(a);
            }
            case "abs" -> Math.abs(a);
            default -> throw new CalculatorException("Unknown function: " + function);
        });
    }
    
    private int precedence(String operator) {
        if (operator.equals("+") || operator.equals("-")) {
            return 1;
        } else if (operator.equals("*") || operator.equals("/")) {
            return 2;
        } else if (operator.equals("^")) {
            return 3;
        } else if (operator.matches(FUNCTIONS)) {
            return 4;
        } else {
            return 0; // For '('
        }
    }
    
    // Custom exception class for calculator errors
    public static class CalculatorException extends Exception {
        public CalculatorException(String message) {
            super(message);
        }
    }
}