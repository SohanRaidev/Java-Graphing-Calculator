import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.*;

public class GraphingCalculator extends JPanel {
    private static final int TICK_SIZE = 5;
    
    private double xMin = -10;
    private double xMax = 10;
    private double yMin = -10;
    private double yMax = 10;
    
    private Point dragStart;
    private boolean isDragging = false;
    
    private final List<Function<Double, Double>> functions = new ArrayList<>();
    private final List<Color> functionColors = new ArrayList<>();
    
    public GraphingCalculator() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);
        
        setupMouseListeners();
    }
    
    private void setupMouseListeners() {
        // For panning the graph
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
                isDragging = true;
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });
        
        // For zooming and panning
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging) {
                    int dx = e.getX() - dragStart.x;
                    int dy = e.getY() - dragStart.y;
                    
                    // Convert pixel movement to coordinate movement
                    double xRange = xMax - xMin;
                    double yRange = yMax - yMin;
                    
                    double panX = -dx * xRange / getWidth();
                    double panY = dy * yRange / getHeight();
                    
                    xMin += panX;
                    xMax += panX;
                    yMin += panY;
                    yMax += panY;
                    
                    dragStart = e.getPoint();
                    repaint();
                }
            }
        });
        
        // For zooming with mouse wheel
        addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            
            // Calculate zoom factor
            double factor = (notches < 0) ? 0.9 : 1.1;
            
            // Get mouse position and calculate where to zoom
            Point mousePoint = e.getPoint();
            double mouseX = screenToWorldX(mousePoint.x);
            double mouseY = screenToWorldY(mousePoint.y);
            
            // Zoom around mouse position
            double newXMin = mouseX - (mouseX - xMin) * factor;
            double newXMax = mouseX + (xMax - mouseX) * factor;
            double newYMin = mouseY - (mouseY - yMin) * factor;
            double newYMax = mouseY + (yMax - mouseY) * factor;
            
            xMin = newXMin;
            xMax = newXMax;
            yMin = newYMin;
            yMax = newYMax;
            
            repaint();
        });
    }
    
    public void addFunction(Function<Double, Double> function, Color color) {
        functions.add(function);
        functionColors.add(color);
        repaint();
    }
    
    public void clearFunctions() {
        functions.clear();
        functionColors.clear();
        repaint();
    }
    
    private double screenToWorldX(int screenX) {
        return xMin + (screenX / (double) getWidth()) * (xMax - xMin);
    }
    
    private double screenToWorldY(int screenY) {
        return yMax - (screenY / (double) getHeight()) * (yMax - yMin);
    }
    
    private int worldToScreenX(double worldX) {
        return (int) ((worldX - xMin) / (xMax - xMin) * getWidth());
    }
    
    private int worldToScreenY(double worldY) {
        return (int) ((yMax - worldY) / (yMax - yMin) * getHeight());
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        drawGrid(g2);
        drawAxes(g2);
        drawFunctions(g2);
    }
    
    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(240, 240, 240));
        g2.setStroke(new BasicStroke(1));
        
        // Calculate grid spacing based on the range
        double xStep = calculateGridStep(xMax - xMin);
        double yStep = calculateGridStep(yMax - yMin);
        
        // Draw vertical grid lines
        double x = Math.ceil(xMin / xStep) * xStep;
        while (x <= xMax) {
            int screenX = worldToScreenX(x);
            g2.draw(new Line2D.Double(screenX, 0, screenX, getHeight()));
            x += xStep;
        }
        
        // Draw horizontal grid lines
        double y = Math.ceil(yMin / yStep) * yStep;
        while (y <= yMax) {
            int screenY = worldToScreenY(y);
            g2.draw(new Line2D.Double(0, screenY, getWidth(), screenY));
            y += yStep;
        }
    }
    
    private double calculateGridStep(double range) {
        // Dynamically calculate grid step based on the view range
        double rough = range / 10;
        double power = Math.pow(10, Math.floor(Math.log10(rough)));
        
        if (rough / power < 1.5) return power / 2;
        if (rough / power < 3) return power;
        if (rough / power < 7) return power * 2;
        return power * 5;
    }
    
    private void drawAxes(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        
        // Get coordinates of the origin (or where axes should cross)
        int xAxisY = worldToScreenY(0);
        int yAxisX = worldToScreenX(0);
        
        // Draw X axis if it's visible
        if (yMin <= 0 && 0 <= yMax) {
            g2.draw(new Line2D.Double(0, xAxisY, getWidth(), xAxisY));
        }
        
        // Draw Y axis if it's visible
        if (xMin <= 0 && 0 <= xMax) {
            g2.draw(new Line2D.Double(yAxisX, 0, yAxisX, getHeight()));
        }
        
        // Draw tick marks and labels
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        
        // Calculate tick spacing
        double xStep = calculateGridStep(xMax - xMin);
        double yStep = calculateGridStep(yMax - yMin);
        
        // Draw X axis ticks and labels
        double x = Math.ceil(xMin / xStep) * xStep;
        while (x <= xMax) {
            int screenX = worldToScreenX(x);
            
            // Don't draw the tick at origin (it's part of the y-axis)
            if (Math.abs(x) > 1e-10) {
                g2.draw(new Line2D.Double(screenX, xAxisY - TICK_SIZE, screenX, xAxisY + TICK_SIZE));
                
                // Format number for display (avoid -0.0)
                String label = String.format("%.1f", x);
                if (label.equals("-0.0")) label = "0.0";
                
                FontMetrics fm = g2.getFontMetrics();
                Rectangle2D rect = fm.getStringBounds(label, g2);
                g2.drawString(label, (int)(screenX - rect.getWidth()/2), xAxisY + TICK_SIZE + 15);
            }
            x += xStep;
        }
        
        // Draw Y axis ticks and labels
        double y = Math.ceil(yMin / yStep) * yStep;
        while (y <= yMax) {
            int screenY = worldToScreenY(y);
            
            // Don't draw the tick at origin (it's part of the x-axis)
            if (Math.abs(y) > 1e-10) {
                g2.draw(new Line2D.Double(yAxisX - TICK_SIZE, screenY, yAxisX + TICK_SIZE, screenY));
                
                // Format number for display (avoid -0.0)
                String label = String.format("%.1f", y);
                if (label.equals("-0.0")) label = "0.0";
                
                FontMetrics fm = g2.getFontMetrics();
                Rectangle2D rect = fm.getStringBounds(label, g2);
                g2.drawString(label, yAxisX - TICK_SIZE - (int)rect.getWidth() - 5, screenY + 5);
            }
            y += yStep;
        }
        
        // Draw origin label if the origin is visible
        if (xMin <= 0 && 0 <= xMax && yMin <= 0 && 0 <= yMax) {
            g2.drawString("0", yAxisX + 5, xAxisY + 15);
        }
    }
    
    private void drawFunctions(Graphics2D g2) {
        g2.setStroke(new BasicStroke(2));
        
        // Draw each function
        for (int i = 0; i < functions.size(); i++) {
            Function<Double, Double> function = functions.get(i);
            g2.setColor(functionColors.get(i));
            
            // Sample points along the x-axis
            int numPoints = getWidth();
            double dx = (xMax - xMin) / numPoints;
            
            // Arrays to store points to draw
            int[] xPoints = new int[numPoints];
            int[] yPoints = new int[numPoints];
            boolean[] valid = new boolean[numPoints];
            int validCount = 0;
            
            // Calculate points
            for (int j = 0; j < numPoints; j++) {
                double x = xMin + j * dx;
                try {
                    double y = function.apply(x);
                    
                    // Check if y is within bounds and not NaN or Infinity
                    if (!Double.isNaN(y) && !Double.isInfinite(y) && y >= yMin && y <= yMax) {
                        xPoints[j] = worldToScreenX(x);
                        yPoints[j] = worldToScreenY(y);
                        valid[j] = true;
                        validCount++;
                    } else {
                        valid[j] = false;
                    }
                } catch (Exception e) {
                    valid[j] = false;
                }
            }
            
            // Draw line segments
            for (int j = 0; j < numPoints - 1; j++) {
                if (valid[j] && valid[j + 1]) {
                    g2.drawLine(xPoints[j], yPoints[j], xPoints[j + 1], yPoints[j + 1]);
                }
            }
        }
    }
    
    public void zoomIn() {
        double centerX = (xMin + xMax) / 2;
        double centerY = (yMin + yMax) / 2;
        double rangeX = (xMax - xMin) * 0.8;
        double rangeY = (yMax - yMin) * 0.8;
        
        xMin = centerX - rangeX / 2;
        xMax = centerX + rangeX / 2;
        yMin = centerY - rangeY / 2;
        yMax = centerY + rangeY / 2;
        
        repaint();
    }
    
    public void zoomOut() {
        double centerX = (xMin + xMax) / 2;
        double centerY = (yMin + yMax) / 2;
        double rangeX = (xMax - xMin) * 1.25;
        double rangeY = (yMax - yMin) * 1.25;
        
        xMin = centerX - rangeX / 2;
        xMax = centerX + rangeX / 2;
        yMin = centerY - rangeY / 2;
        yMax = centerY + rangeY / 2;
        
        repaint();
    }
    
    public void resetView() {
        xMin = -10;
        xMax = 10;
        yMin = -10;
        yMax = 10;
        repaint();
    }
}