package src;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import java.util.List;

public class RoomDesignerGUIv5 extends JFrame {

    private DrawingPanel drawingPanel;
    private JLabel perimeterLabel;
    private JLabel areaLabel;
    private JLabel messageLabel;
    private JButton drawButton;
    private JButton undoButton;
    private JButton redoButton;
    private JButton clearButton;
    private JButton gridSnapButton;
    private JButton saveButton;
    private JComboBox<String> roomTypeComboBox; // Declare the JComboBox
    private int roomType = 0;
    private JTextField gridSizeInput;
    private ArrayList<Point> points = new ArrayList<>();
    private Stack<ArrayList<Point>> undoStack = new Stack<>();
    private Stack<ArrayList<Point>> redoStack = new Stack<>();
    private boolean selecting = false;
    private boolean closedFigureMade = false;
    private boolean gridSnapEnabled = false;
    private double gridSize = 1.0;
    private static int DEFAULT_GRID_SIZE = 80;
    ArrayList<Point> relativeCoordinates = new ArrayList<>();

    private double cellLength; // Grid unit length in the main frame
    private int displayGridSize; // Pixel size of each grid cell in the main frame

    private RoomCreationListener listener;

    public RoomDesignerGUIv5(RoomCreationListener listener,
                            double cellLength,
                            int displayGridSize) {
        this.listener = listener; // Initialize the listener
        this.cellLength=cellLength;
        this.displayGridSize=displayGridSize;
        setTitle("Room Designer");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        getContentPane().setBackground(Color.decode("#F0F0F0"));

        drawingPanel = new DrawingPanel();
        drawingPanel.setBorder(new LineBorder(Color.GRAY, 1, true));
        add(drawingPanel, BorderLayout.CENTER);

        JPanel controlPanel = createStyledPanel(new FlowLayout());
        controlPanel.setLayout(new FlowLayout());

        drawButton = createStyledButton("Draw");
        drawButton.addActionListener(e -> startDrawing());
        controlPanel.add(drawButton);

        clearButton = createStyledButton("Clear");
        clearButton.addActionListener(e -> clearDrawing());
        controlPanel.add(clearButton);

        undoButton = createStyledButton("Undo");
        undoButton.addActionListener(e -> undoAction());
        controlPanel.add(undoButton);

        redoButton = createStyledButton("Redo");
        redoButton.addActionListener(e -> redoAction());
        controlPanel.add(redoButton);

        gridSizeInput = new JTextField("1.0", 5);
        gridSizeInput.setFont(new Font("SansSerif", Font.PLAIN, 14));
        gridSizeInput.addActionListener(e -> updateGridSize());
        controlPanel.add(new JLabel("Grid Size (m): "));
        controlPanel.add(gridSizeInput);

        perimeterLabel = new JLabel("Perimeter: ");
        controlPanel.add(perimeterLabel);

        areaLabel = new JLabel("Area: ");
        controlPanel.add(areaLabel);

        add(controlPanel, BorderLayout.SOUTH);

        JPanel topPanel = createStyledPanel(new FlowLayout(FlowLayout.LEFT));

        saveButton = createStyledButton("Save");
        saveButton.addActionListener(e -> saveDrawing());
        topPanel.add(saveButton);

        gridSnapButton = createStyledButton("GridSnap");
        gridSnapButton.addActionListener(e -> toggleGridSnap());
        topPanel.add(gridSnapButton);

        roomTypeComboBox = new JComboBox<>(new String[]{"Common room", "Bedroom", "Bathroom", "Kitchen"});
        // roomTypeComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        roomTypeComboBox.setSelectedIndex(0); // Initially no selection
        roomTypeComboBox.addActionListener(e -> {
            roomType = roomTypeComboBox.getSelectedIndex();
            System.out.println("Room type selected: " + roomType); // For debugging
        });
        topPanel.add(new JLabel("Room Type: "));
        topPanel.add(roomTypeComboBox);

        messageLabel = createStyledLabel("");
        messageLabel.setForeground(Color.RED);
        topPanel.add(messageLabel);

        add(topPanel, BorderLayout.NORTH);

        setVisible(true);
    }

    private JPanel createStyledPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.decode("#F0F0F0"));
        return panel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }


    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(Color.BLACK);
        return label;
    }

    private void createRoomWithRelativeCoordinates() {
        if (points.size() > 2 && points.get(0).equals(points.get(points.size() - 1))) {
            relativeCoordinates.clear();
            Point firstPoint = points.get(0); // Take the first point as the origin
    
            for (Point point : points) {
                int normalizedX = (int) Math.round(((point.x - firstPoint.x) / (double) displayGridSize) * cellLength);
                int normalizedY = (int) -Math.round(((point.y - firstPoint.y) / (double) displayGridSize) * cellLength);
                relativeCoordinates.add(new Point(normalizedX, normalizedY));
            }
    
            System.out.println("Relative Coordinates (normalized):");
            for (Point p : relativeCoordinates) {
                System.out.println(p);
            }
    
            closedFigureMade = true;
        } else {
            JOptionPane.showMessageDialog(this, "Please draw a closed figure first.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    
    private void saveDrawing() {
        createRoomWithRelativeCoordinates();
        if (!closedFigureMade) {
            JOptionPane.showMessageDialog(this, "Please create a room first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        // Ensure coordinates are relative to the first point
        Point origin = relativeCoordinates.get(0);
        List<double[]> list = new ArrayList<>();
        for (int i = 0; i < relativeCoordinates.size() - 1; i++) {
            double[] arr = new double[4];
            arr[0] = (relativeCoordinates.get(i).getX() - origin.x) * cellLength;
            arr[1] = (relativeCoordinates.get(i).getY() - origin.y) * cellLength;
            arr[2] = (relativeCoordinates.get(i + 1).getX() - origin.x) * cellLength;
            arr[3] = (relativeCoordinates.get(i + 1).getY() - origin.y) * cellLength;
            list.add(arr);
        }
    
        Room customRoom = new Room(list);
        customRoom.setRoomtype(roomType);

        // Notify the listener instead of directly modifying floorRooms
        if (listener != null) {
            listener.onRoomCreated(customRoom);
            System.out.println("adding");
        } else {
            System.out.println("hello");
        }

        dispose(); // Close the custom room frame after saving
    }
    
    
    
    private void startDrawing() {
        if (closedFigureMade) {
            JOptionPane.showMessageDialog(this, "A closed figure already exists. Please clear it before drawing a new one.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        selecting = false;
        closedFigureMade = false;
    }

    private void clearDrawing() {
        saveStateToUndoStack();
        points.clear();
        drawingPanel.repaint();
        perimeterLabel.setText("Perimeter: ");
        areaLabel.setText("Area: ");
        messageLabel.setText("");
        closedFigureMade = false;
        gridSnapEnabled = false;  // Disable grid snap when clearing the drawing
        relativeCoordinates.clear();
    }

    private void updateGridSize() {
        try {
            gridSize = Double.parseDouble(gridSizeInput.getText());
            if (gridSize <= 0) {
                throw new NumberFormatException();
            }
            calculateAndDisplayMetrics();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid grid size. Please enter a valid positive number.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleGridSnap() {
        gridSnapEnabled = !gridSnapEnabled;
        if (gridSnapEnabled) {
            snapFigureToGrid();
        }
    }

    private void snapFigureToGrid() {
        if (points.isEmpty()) {
            return;
        }
        Point lastPoint = points.get(points.size() - 1);
        Point snappedLastPoint = drawingPanel.snapToGrid(lastPoint);
        int deltaX = snappedLastPoint.x - lastPoint.x;
        int deltaY = snappedLastPoint.y - lastPoint.y;

        for (int i = 0; i < points.size(); i++) {
            points.set(i, new Point(points.get(i).x + deltaX, points.get(i).y + deltaY));
        }
        drawingPanel.repaint();
    }

    private void undoAction() {
        if (!points.isEmpty()) {
            redoStack.push(new ArrayList<>(points));
            points.remove(points.size() - 1);
            drawingPanel.repaint();
            calculateAndDisplayMetrics();
            checkIfClosedFigureExists();
        }
    }

    private void redoAction() {
        if (!redoStack.isEmpty()) {
            undoStack.push(new ArrayList<>(points));
            points = redoStack.pop();
            drawingPanel.repaint();
            calculateAndDisplayMetrics();
            checkIfClosedFigureExists();
        }
    }

    private void saveStateToUndoStack() {
        if (undoStack.size() == 100) {
            undoStack.remove(0);
        }
        undoStack.push(new ArrayList<>(points));
        redoStack.clear();
    }

    private void calculateAndDisplayMetrics() {
        if (points.size() < 3) {
            perimeterLabel.setText("Perimeter: ");
            areaLabel.setText("Area: ");
            return;
        }
        double area = calculateArea(points) * gridSize * gridSize;
        double perimeter = calculatePerimeter(points) * gridSize;
        perimeterLabel.setText("Perimeter: " + String.format("%.2f m", perimeter));
        areaLabel.setText("Area: " + String.format("%.2f m²", area));
    }

    private double calculateArea(ArrayList<Point> points) {
        double area = 0.0;
        int n = points.size();
        for (int i = 0; i < n; i++) {
            Point p1 = points.get(i);
            Point p2 = points.get((i + 1) % n);
            area += p1.x * p2.y - p2.x * p1.y;
        }
        return Math.abs(area / 2.0) / (DEFAULT_GRID_SIZE * DEFAULT_GRID_SIZE);
    }

    private double calculatePerimeter(ArrayList<Point> points) {
        double perimeter = 0.0;
        for (int i = 0; i < points.size() - 1; i++) {
            perimeter += points.get(i).distance(points.get(i + 1)) / DEFAULT_GRID_SIZE;
        }
        if (points.size() > 1) {
            perimeter += points.get(points.size() - 1).distance(points.get(0)) / DEFAULT_GRID_SIZE;
        }
        return perimeter;
    }

    private boolean doesOverlap(Point newPoint) {
        if (points.size() < 2) {
            return false;
        }
        Line2D newLine = new Line2D.Double(points.get(points.size() - 1), newPoint);
        for (int i = 0; i < points.size() - 1; i++) {
            Line2D existingLine = new Line2D.Double(points.get(i), points.get(i + 1));
            if (newLine.intersectsLine(existingLine) && !newLine.getP1().equals(existingLine.getP1()) && !newLine.getP1().equals(existingLine.getP2()) && !newLine.getP2().equals(existingLine.getP1()) && !newLine.getP2().equals(existingLine.getP2())) {
                return true;
            }
        }
        return false;
    }

    private void checkIfClosedFigureExists() {
        if (points.size() > 2 && points.get(0).equals(points.get(points.size() - 1))) {
            closedFigureMade = true;
            displayClosedFigureMessage();
        } else {
            closedFigureMade = false;
        }
    }

    

    class DrawingPanel extends JPanel {

        public DrawingPanel() {
            // Ensure the grid size is initialized correctly
            displayGridSize = DEFAULT_GRID_SIZE;
    
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Point point = e.getPoint();
                    Point snappedPoint = gridSnapEnabled ? snapToGrid(point) : point;
                    if (points.isEmpty()){
                        points.add(snappedPoint);
                        saveStateToUndoStack();
                        repaint();
                    }
                    
                    Point firstPoint = points.get(0);
                    double distanceToFirstPoint = firstPoint.distance(snappedPoint);

                    if (!closedFigureMade && points.size() > 2 && distanceToFirstPoint <= displayGridSize / 2.0) {
                        // Snap to the first point if within half the grid size
                        snappedPoint = firstPoint;
            
                        saveStateToUndoStack();
                        points.add(snappedPoint); // Add the snapped point to close the figure
                        closedFigureMade = true; // Mark the figure as closed
                        repaint(); // Redraw the closed figure
                        displayClosedFigureMessage();
                        calculateAndDisplayMetrics(); // Update area and perimeter
                        return;
                    }


                    if (!selecting && !closedFigureMade) {
                        if (points.isEmpty()) {
                            points.add(snappedPoint); // Add the first point
                            saveStateToUndoStack(); // Save state after the first point
                            repaint(); // Repaint immediately after adding the first point
                            calculateAndDisplayMetrics(); // Calculate and display metrics
                            return; // Exit early to avoid additional checks for the first point
                        }
                        
                        if (doesOverlap(snappedPoint)) {
                            JOptionPane.showMessageDialog(RoomDesignerGUIv5.this, "Cannot draw line overlapping another line.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                
                        saveStateToUndoStack(); // Save state before adding subsequent points
                        points.add(snappedPoint); // Add the new point
                
                        repaint(); // Ensure immediate repainting after adding the point
                        calculateAndDisplayMetrics(); // Calculate metrics for display
                
                        // Check if the figure is closed
                        if (points.size() > 2 && snappedPoint.equals(points.get(0))) {
                            closedFigureMade = true;
                            displayClosedFigureMessage();
                        }
                    }
                }
            });
    
            addMouseWheelListener(e -> {
                int notches = e.getWheelRotation();
                if (notches < 0) {
                    displayGridSize = Math.min(displayGridSize + 5, 100); // Zoom in, max grid size 100px
                } else {
                    displayGridSize = Math.max(displayGridSize - 5, 10); // Zoom out, min grid size 10px
                }
                repaint();
            });
        }
    
        private Point snapToGrid(Point p) {
            int x = (p.x + displayGridSize / 2) / displayGridSize * displayGridSize;
            int y = (p.y + displayGridSize / 2) / displayGridSize * displayGridSize;
            return new Point(x, y);
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            setBackground(Color.WHITE);
    
            Graphics2D g2d = (Graphics2D) g;
    
            g2d.setColor(Color.LIGHT_GRAY);
    
            // Draw vertical lines
            for (int x = 0; x <= getWidth(); x += displayGridSize) {
                g2d.drawLine(x, 0, x, getHeight());
            }
    
            // Draw horizontal lines
            for (int y = 0; y <= getHeight(); y += displayGridSize) {
                g2d.drawLine(0, y, getWidth(), y);
            }
    
            // Draw points and lines
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            for (int i = 0; i < points.size() - 1; i++) {
                Point p1 = points.get(i);
                Point p2 = points.get(i + 1);
                g2d.fillOval(p1.x - 3, p1.y - 3, 6, 6);
                g2d.draw(new Line2D.Double(p1.x, p1.y, p2.x, p2.y));
            }
    
            // Close the polygon if it's a closed figure
            if (points.size() > 1 && points.get(0).equals(points.get(points.size() - 1))) {
                Point p1 = points.get(points.size() - 1);
                Point p2 = points.get(0);
                g2d.draw(new Line2D.Double(p1.x, p1.y, p2.x, p2.y));
            }
        }
    }
    

    private void displayClosedFigureMessage() {
        messageLabel.setText("One closed figure is made. To make a new one, discard the current figure.");
        Timer timer = new Timer(15000, e -> messageLabel.setText(""));
        timer.setRepeats(false);
        timer.start();
    }
}