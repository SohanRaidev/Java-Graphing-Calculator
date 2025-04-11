import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*; // Import Logger
import javax.swing.border.EmptyBorder; // Import Level

public class CalculatorMain extends JFrame {
    private final GraphingCalculator graphPanel;
    private final AdvancedCalculator calculator;
    
    private JTextField expressionField;
    private JTextArea resultArea;
    private JComboBox<String> functionComboBox;
    private JTextField functionNameField;
    private JButton saveButton;
    private JButton calculateButton;
    private JButton graphButton;
    private JButton clearGraphButton;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JButton resetViewButton;
    
    private final Map<String, Color> functionColors;
    private final Color[] availableColors = {
        Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.ORANGE,
        Color.CYAN, Color.PINK, new Color(128, 0, 128), // Purple
        new Color(165, 42, 42), // Brown
        new Color(0, 100, 0) // Dark Green
    };
    private int nextColorIndex = 0;
    
    public CalculatorMain() {
        // Initialize components
        calculator = new AdvancedCalculator();
        graphPanel = new GraphingCalculator();
        functionColors = new HashMap<>();
        
        // Set up the UI
        setupUI();
        
        setTitle("Advanced Graphing Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }
    
    private void setupUI() {
        // Main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // North panel for expressions
        JPanel northPanel = new JPanel(new BorderLayout(5, 5));
        
        // Expression input field
        expressionField = new JTextField();
        expressionField.setFont(new Font("Arial", Font.PLAIN, 16));
        expressionField.setToolTipText("Enter an expression (e.g., 2*x^2 + 3*x - 5)");
        northPanel.add(expressionField, BorderLayout.CENTER);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        
        calculateButton = new JButton("Calculate");
        calculateButton.setFont(new Font("Arial", Font.BOLD, 14));
        calculateButton.addActionListener(e -> {
            try {
                calculate();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (ArithmeticException ex) {
                JOptionPane.showMessageDialog(this, "Math error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (AdvancedCalculator.CalculatorException ex) {
                JOptionPane.showMessageDialog(this, "Calculation error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        graphButton = new JButton("Graph");
        graphButton.setFont(new Font("Arial", Font.BOLD, 14));
        graphButton.addActionListener(e -> graph());
        
        buttonPanel.add(calculateButton);
        buttonPanel.add(graphButton);
        northPanel.add(buttonPanel, BorderLayout.EAST);
        
        // Add to main panel
        mainPanel.add(northPanel, BorderLayout.NORTH);
        
        // Center panel - graphing area
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(graphPanel, BorderLayout.CENTER);
        
        // Zoom buttons
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        zoomInButton = new JButton("+");
        zoomOutButton = new JButton("-");
        resetViewButton = new JButton("Reset");
        clearGraphButton = new JButton("Clear Graphs");
        
        zoomInButton.addActionListener(e -> graphPanel.zoomIn());
        zoomOutButton.addActionListener(e -> graphPanel.zoomOut());
        resetViewButton.addActionListener(e -> graphPanel.resetView());
        clearGraphButton.addActionListener(e -> {
            graphPanel.clearFunctions();
            functionColors.clear();
            nextColorIndex = 0;
        });
        
        zoomPanel.add(zoomInButton);
        zoomPanel.add(zoomOutButton);
        zoomPanel.add(resetViewButton);
        zoomPanel.add(clearGraphButton);
        
        centerPanel.add(zoomPanel, BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // South panel - results and functions
        JPanel southPanel = new JPanel(new BorderLayout(5, 5));
        
        // Results area
        resultArea = new JTextArea(5, 40);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setEditable(false);
        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        southPanel.add(resultScrollPane, BorderLayout.CENTER);
        
        // Function saving panel
        JPanel functionPanel = new JPanel(new BorderLayout(5, 5));
        functionPanel.setBorder(BorderFactory.createTitledBorder("Save Functions"));
        
        JPanel functionInputPanel = new JPanel(new BorderLayout(5, 0));
        functionNameField = new JTextField();
        functionNameField.setToolTipText("Enter function name");
        saveButton = new JButton("Save Function");
        
        saveButton.addActionListener(e -> saveFunction());
        
        functionInputPanel.add(functionNameField, BorderLayout.CENTER);
        functionInputPanel.add(saveButton, BorderLayout.EAST);
        
        functionComboBox = new JComboBox<>();
        functionComboBox.setToolTipText("Select saved function");
        
        functionComboBox.addActionListener(e -> {
            String selectedFunc = (String) functionComboBox.getSelectedItem();
            if (selectedFunc != null && !selectedFunc.equals("--- Saved Functions ---")) {
                String expr = calculator.getFunctionExpression(selectedFunc);
                if (expr != null) {
                    expressionField.setText(expr);
                }
            }
        });
        
        functionPanel.add(functionInputPanel, BorderLayout.NORTH);
        functionPanel.add(functionComboBox, BorderLayout.SOUTH);
        
        southPanel.add(functionPanel, BorderLayout.SOUTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        
        // Help menu
        JMenuBar menuBar = new JMenuBar();
                // Complete the Help menu setup
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        // Add the main panel to the frame
        add(mainPanel);
    }

    private void showAboutDialog() {
        String aboutMessage = """
                Advanced Graphing Calculator

                Supported Functions: sin, cos, tan, log, ln, sqrt, abs
                Operators: +, -, *, /, ^ (power)
                Examples: 2*x^2 + 3*x - 5, sin(3.14*x), log(100)

                Usage:
                - Enter expressions in the text field
                - Click Calculate to evaluate at x=0
                - Click Graph to plot the function
                - Pan: Click & drag
                - Zoom: Mouse wheel or +/- buttons
                - Save functions with unique names

                Note: Use 'x' as the variable in functions
                """;
        JOptionPane.showMessageDialog(this, aboutMessage, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private void calculate() throws AdvancedCalculator.CalculatorException {
        try {
            String expression = expressionField.getText();
            double result = calculator.evaluate(expression);
            resultArea.append(expression + " = " + result + "\n");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (ArithmeticException ex) {
            JOptionPane.showMessageDialog(this, "Math error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (AdvancedCalculator.CalculatorException ex) {
            JOptionPane.showMessageDialog(this, "Calculation error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void graph() {
        try {
            String expression = expressionField.getText();
            String funcName = functionNameField.getText().trim();
            
            if (!funcName.isEmpty()) {
                calculator.saveFunction(funcName, expression);
                updateFunctionComboBox();
            }
            
            Function<Double, Double> function = calculator.createFunction(expression);
            Color color = getNextColor();
            graphPanel.addFunction(function, color);
            functionColors.put(expression, color);
            graphPanel.repaint();
        } catch (IllegalArgumentException | ArithmeticException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Unexpected runtime error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveFunction() {
        String name = functionNameField.getText().trim();
        String expression = expressionField.getText().trim();
        
        if (name.isEmpty() || expression.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and expression required!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            calculator.saveFunction(name, expression);
            updateFunctionComboBox();
            functionNameField.setText("");
            JOptionPane.showMessageDialog(this, "Function saved successfully!");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Invalid argument: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (ArithmeticException ex) {
            JOptionPane.showMessageDialog(this, "Math error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Unexpected runtime error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateFunctionComboBox() {
        functionComboBox.removeAllItems();
        functionComboBox.addItem("--- Saved Functions ---");
        for (String name : calculator.getSavedFunctionNames()) {
            functionComboBox.addItem(name);
        }
    }

    private Color getNextColor() {
        Color color = availableColors[nextColorIndex];
        nextColorIndex = (nextColorIndex + 1) % availableColors.length;
        return color;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(CalculatorMain.class.getName()).log(Level.SEVERE, null, ex); // Fixed Logger and Level
            }
            
            new CalculatorMain().setVisible(true);
        });
    }
}