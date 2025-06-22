package src;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.*;

import src.FixtureManager.Fixture;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.awt.geom.Path2D;
import java.awt.geom.Area;
import java.io.Serializable;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;


public class FloorPlanMaker extends JFrame implements RoomCreationListener, Serializable {
    private JTabbedPane tabbedPane;

    private Map<Integer, JPanel> floorCanvases = new HashMap<>(); //all floors
    public Map<Integer, List<Room>> floorRooms = new HashMap<>(); //all rooms per floor
    private Map<Integer, JScrollPane> floorScrollpanes = new HashMap<>(); //all Scrollpanes
    private Map<Integer, ArrayList<FixtureManager.Fixture>> floorFixtures = new HashMap<>(); // Fixtures per floor
    private Map<Integer, FixtureManager> floorFixtureManagers = new HashMap<>(); // FixtureManagers per floor
    
    private int floorCount = 1; 
    private int currentFloor;
    private Room currentlySelectedRoom = null;
    private boolean showGrid = true;
    private int cellLength = 1; // Length of each cell
    private String cellUnit = "Meters"; // Default unit
    private final List<String> units = List.of("Meters", "Centimeters", "Millimeters", "Yards", "Feet", "Inches");
    private final List<String> unitSymbols = List.of("m", "cm", "mm", "yd", "ft", "in");
    private final int displayGridSize = 20; // Display size of each cell in pixels
    private JLabel mousePositionLabel; // Label to show current mouse coordinates
    private JLabel roomArea = new JLabel("Area : " );
    private JLabel roomPerimeter = new JLabel("Perimeter : ");
    private String[] roomTypes = {"Common room", "Bedroom", "Bathroom", "Kitchen"};
    private JLabel roomLabel = new JLabel("Room type :");
    private JComboBox selectColor = new JComboBox<>(roomTypes);
    private final List<Color> colorBox = List.of(new Color(245, 240, 230),new Color(232, 223, 245),new Color(208, 240, 253),new Color(255, 229, 225));
    private Map<Integer, Double> zoomFactors; // Store zoom factors for each floor
    private List<JButton> allFixtureButtons = new ArrayList<>();

    private boolean isRPressed = false;
    private boolean isSPressed = false;
    private Point[] dragStartPoint = new Point[1];
    private Point[] lastMousePoint = new Point[1];
    private double[] roomCenter = new double[2];
    private double initialDistanceToCenter = 0.0;
    private List<double[]> lastValidEdges = new ArrayList<>();
    private Room[] draggedRoom = new Room[1];
    // private final double maxZoomFactor = 3.0; // Maximum zoom factor for customization
    // private final double minZoomFactor = 0.01; // Minimum zoom factor for customization
    public void saveToFile(String path) {
        // Create a FloorPlanData object with current data
        FloorPlanData data = new FloorPlanData(
            new HashMap<>(this.floorRooms),
            new HashMap<>(this.floorFixtures),
            this.cellLength,
            this.cellUnit
        );
    
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(data);
            JOptionPane.showMessageDialog(this, "Floor plan saved successfully!", "Save", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving floor plan: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    

    public void loadFromFile(String path) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            FloorPlanData data = (FloorPlanData) ois.readObject();
    
            // Update the current object's data
            floorCanvases.clear();
            floorScrollpanes.clear();
            floorFixtureManagers.clear();


            this.floorRooms = data.getFloorRooms();
            this.floorFixtures = data.getFloorFixtures();
            this.cellLength = data.getCellLength();
            this.cellUnit = data.getCellUnit();

            // for (int i=0;i<floorRooms.size();i++){
            //     addNewFloor();
            // }
            int tabCount = tabbedPane.getTabCount();
            for (int i = tabCount - 2; i >= 0; i--) { // Assuming last tab is '+'
                tabbedPane.remove(i);
            }
            // Reinitialize FixtureManagers with loaded fixtures
            for (Map.Entry<Integer, ArrayList<FixtureManager.Fixture>> entry : floorFixtures.entrySet()) {
                Integer floorNumber = entry.getKey();
                System.out.println(floorNumber);
                ArrayList<FixtureManager.Fixture> fixtures = entry.getValue();
                reconstructFloorPlan(floorRooms,floorFixtures,cellUnit,cellLength,fixtures,floorNumber);
            //     FixtureManager fixtureManager = floorFixtureManagers.get(floorNumber);
            //     if (fixtureManager != null) {
            //         fixtureManager.setFixtures(fixtures); // Ensure you have a setter
            //     } else {
            //         System.out.println("gcgc");
            //         try {
            //             fixtureManager = new FixtureManager(fixtures, floorCanvases.get(floorNumber));
            //         }
            //         catch(Exception e){
            //             System.out.println("gcgc34");
                        
            //         }

            //         floorFixtureManagers.put(floorNumber, fixtureManager);
                
            //      }
            }
            
            
            // Reconstruct GUI elements
            //reconstructFloorPlan();
    
            JOptionPane.showMessageDialog(this, "Floor plan loaded successfully!", "Load", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading floor plan: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    

    private void reconstructFloorPlan(Map<Integer, List<Room>> floorRooms,Map<Integer, ArrayList<FixtureManager.Fixture>> floorFixtures, String cellUnit, int cellLength,ArrayList<FixtureManager.Fixture> fixtures, Integer floorNumber) {
        zoomFactors.put(floorCount, 3.0);
        JPanel gridCanvas = createCanvas(floorCount);
        floorCanvases.put(floorCount, gridCanvas);
        
        FixtureManager fixtureManager = floorFixtureManagers.get(floorNumber);
        // Initialize fixtures list for the floor
        floorFixtures.put(floorCount, fixtures);

        // Initialize FixtureManager for the floor
        //FixtureManager fixtureManager = new FixtureManager(fixtures, gridCanvas);
        if (fixtureManager != null) {
            fixtureManager.setFixtures(fixtures); // Ensure you have a setter
        } else {
            System.out.println("gcgc");
            fixtureManager = new FixtureManager(fixtures, gridCanvas);
            floorFixtureManagers.put(floorNumber, fixtureManager);
        
        }
        floorCanvases.get(floorNumber).repaint();
        
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                synchronized (gridCanvas) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        if (e.getKeyCode() == KeyEvent.VK_R) {
                            isRPressed = true;
                        } else if (e.getKeyCode() == KeyEvent.VK_S) {
                            isSPressed = true;
                        }
                    } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                        if (e.getKeyCode() == KeyEvent.VK_R) {
                            isRPressed = false;
                        } else if (e.getKeyCode() == KeyEvent.VK_S) {
                            isSPressed = false;
                        }
                    }
                    return false;
                }
            }
        });

        // Ensure gridCanvas is focusable
        gridCanvas.setFocusable(true);
        gridCanvas.requestFocusInWindow();

        // Mouse Listener
        gridCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int i=0;
                
                System.out.println("Room 1"+i++);
                
                FixtureManager fixtureManager = floorFixtureManagers.get(floorNumber);
                if (fixtureManager != null && fixtureManager.isDraggingFixture()) {
                    // Let FixtureManager handle this event
                    return;
                }
                boolean insideRoom = false;
                System.out.println("Room 1"+i++);
                for (Room room : floorRooms.get(floorNumber)) {
                    System.out.println("Room 1"+i++);
                    if (room.checkInterior(getMouseX(floorNumber, e.getX()) + 50, 50 - getMouseY(floorNumber, e.getY()))) {
                        insideRoom = true;
                        room.setSelected(true);
                        System.out.println("in room");
                        draggedRoom[0] = room;
                        dragStartPoint[0] = e.getPoint();

                        lastValidEdges.clear();
                        for (double[] edge : draggedRoom[0].getEdges()) {
                            lastValidEdges.add(edge.clone());
                        }

                        // Store the center of the room
                        double[] center = draggedRoom[0].getCenter();
                        roomCenter[0] = center[0];
                        roomCenter[1] = center[1];

                        if (isRPressed) {
                            // For rotation
                            lastMousePoint[0] = e.getPoint();
                        } else if (isSPressed) {
                            // For scaling
                            double mouseX = getMouseX(floorNumber, e.getX()) + 50;
                            double mouseY = 50 - getMouseY(floorNumber, e.getY());
                            initialDistanceToCenter = distance(mouseX, mouseY, roomCenter[0], roomCenter[1]);

                            lastMousePoint[0] = e.getPoint();
                        }
                        break; // Room found, exit loop
                    } else {
                        room.setSelected(false);
                    }
                }
                gridCanvas.repaint();
                if (!insideRoom) {
                    draggedRoom[0] = null;
                    dragStartPoint[0] = e.getPoint(); // For canvas dragging
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggedRoom[0] != null) {
                    // Check for overlaps
                    List<Room> checkRooms = new ArrayList<>(floorRooms.get(floorNumber));
                    checkRooms.remove(draggedRoom[0]);
                    boolean hasOverlap = checkOverlap(checkRooms, draggedRoom[0]);

                    if (hasOverlap) {
                        // Revert to lastValidEdges
                        draggedRoom[0].setEdges(lastValidEdges);
                    }

                    // Reset variables
                    draggedRoom[0] = null;
                    lastValidEdges.clear();
                    lastMousePoint[0] = null;
                    initialDistanceToCenter = 0.0;

                    gridCanvas.repaint();
                } else if (dragStartPoint[0] != null) {
                    // Reset dragStartPoint for canvas dragging
                    dragStartPoint[0] = null;
                }
            }
        });

        // Mouse Motion Listener
        gridCanvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                FixtureManager fixtureManager = floorFixtureManagers.get(floorNumber);
                if (fixtureManager != null && fixtureManager.isDraggingFixture()) {
                    // Let FixtureManager handle dragging; do nothing for canvas
                    return;
                }
                if (draggedRoom[0] != null) {
                    if (isRPressed) {
                        // Handle rotation
                        Point currentMousePoint = e.getPoint();

                        // Convert last and current mouse points to room coordinates
                        double lastMouseX = getMouseX(floorNumber, lastMousePoint[0].getX()) + 50;
                        double lastMouseY = 50 - getMouseY(floorNumber, lastMousePoint[0].getY());

                        double currentMouseX = getMouseX(floorNumber, currentMousePoint.getX()) + 50;
                        double currentMouseY = 50 - getMouseY(floorNumber, currentMousePoint.getY());

                        // Compute angles relative to the room's center
                        double angleLast = Math.atan2(lastMouseY - roomCenter[1], lastMouseX - roomCenter[0]);
                        double angleCurrent = Math.atan2(currentMouseY - roomCenter[1], currentMouseX - roomCenter[0]);

                        double deltaAngle = angleCurrent - angleLast;

                        // Normalize deltaAngle
                        deltaAngle = ((deltaAngle + Math.PI) % (2 * Math.PI)) - Math.PI;

                        // Rotate the room
                        draggedRoom[0].rotateRoom(deltaAngle);

                        // Check for overlaps
                        List<Room> checkRooms = new ArrayList<>(floorRooms.get(floorNumber));
                        checkRooms.remove(draggedRoom[0]);
                        boolean hasOverlap = checkOverlap(checkRooms, draggedRoom[0]);

                        if (!hasOverlap) {
                            lastValidEdges.clear();
                            for (double[] edge : draggedRoom[0].getEdges()) {
                                lastValidEdges.add(edge.clone());
                            }
                        } else {
                            // Revert rotation
                            draggedRoom[0].setEdges(lastValidEdges);
                        }

                        // Update lastMousePoint
                        lastMousePoint[0] = currentMousePoint;

                        gridCanvas.repaint();

                    } else if (isSPressed) {
                        // Handle scaling
                        Point currentMousePoint = e.getPoint();

                        // Convert current mouse point to room coordinates
                        double currentMouseX = getMouseX(floorNumber, currentMousePoint.getX()) + 50;
                        double currentMouseY = 50 - getMouseY(floorNumber, currentMousePoint.getY());

                        // Calculate current distance to center
                        double currentDistanceToCenter = distance(currentMouseX, currentMouseY, roomCenter[0], roomCenter[1]);

                        if (initialDistanceToCenter == 0) {
                            initialDistanceToCenter = 0.001; // Prevent division by zero
                        }

                        // Calculate scaling factor
                        double scaleFactor = currentDistanceToCenter / initialDistanceToCenter;

                        // Limit the scale factor
                        scaleFactor = Math.max(scaleFactor, 0.1); // Minimum scale factor
                        scaleFactor = Math.min(scaleFactor, 10.0); // Maximum scale factor

                        // Scale the room
                        draggedRoom[0].scaleRoom(scaleFactor);

                        // Check for overlaps
                        List<Room> checkRooms = new ArrayList<>(floorRooms.get(floorNumber));
                        checkRooms.remove(draggedRoom[0]);
                        boolean hasOverlap = checkOverlap(checkRooms, draggedRoom[0]);

                        if (!hasOverlap) {
                            lastValidEdges.clear();
                            for (double[] edge : draggedRoom[0].getEdges()) {
                                lastValidEdges.add(edge.clone());
                            }
                            // Update initialDistanceToCenter
                            initialDistanceToCenter = currentDistanceToCenter;
                        } else {
                            // Revert scaling
                            draggedRoom[0].setEdges(lastValidEdges);
                        }

                        // Update lastMousePoint
                        lastMousePoint[0] = currentMousePoint;

                        gridCanvas.repaint();

                    } else {
                        // Handle moving
                        Point dragEndPoint = e.getPoint();
                        double dx = getMouseX(floorNumber, dragEndPoint.getX()) - getMouseX(floorNumber, dragStartPoint[0].getX());
                        double dy = -(getMouseY(floorNumber, dragEndPoint.getY()) - getMouseY(floorNumber, dragStartPoint[0].getY()));
                        draggedRoom[0].moveRoom(dx, dy);

                        // Check for overlaps
                        List<Room> checkRooms = new ArrayList<>(floorRooms.get(floorNumber));
                        checkRooms.remove(draggedRoom[0]);
                        boolean hasOverlap = checkOverlap(checkRooms, draggedRoom[0]);

                        if (!hasOverlap) {
                            lastValidEdges.clear();
                            for (double[] edge : draggedRoom[0].getEdges()) {
                                lastValidEdges.add(edge.clone());
                            }
                        } else {
                            // Revert movement
                            draggedRoom[0].setEdges(lastValidEdges);
                        }

                        // Update dragStartPoint
                        dragStartPoint[0] = dragEndPoint;

                        gridCanvas.repaint();
                    }
                } else if (dragStartPoint[0] != null) {
                    // Canvas dragging logic
                    Point dragEndPoint = e.getPoint();
                    int dx = (int) ((dragStartPoint[0].x - dragEndPoint.x) * 0.99);
                    int dy = (int) ((dragStartPoint[0].y - dragEndPoint.y) * 0.99);

                    JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, gridCanvas);
                    if (scrollPane != null) {
                        JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
                        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();

                        horizontalScrollBar.setValue(horizontalScrollBar.getValue() + dx);
                        verticalScrollBar.setValue(verticalScrollBar.getValue() + dy);
                    }

                    dragStartPoint[0] = dragEndPoint;
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(gridCanvas);
        floorScrollpanes.put(floorCount, scrollPane);
        scrollPane.setPreferredSize(new Dimension(1000, 1000));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI()); // Custom scrollbar color
        scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI()); // Custom scrollbar color
        tabbedPane.insertTab("Floor " + floorCount, null, scrollPane, null, tabbedPane.getTabCount() - 1);
        centerCanvas(floorCount, 0.0, 0.0);

        floorCount++;
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);

    
    }
    private double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    private void updateRoomProperties(Room room) {
        String unitToDisplay = unitSymbols.get(units.indexOf(cellUnit));
        roomArea.setText(String.format("Area : %.2f sq.%s", room.calculateArea(), unitToDisplay));
        roomPerimeter.setText(String.format("Perimeter : %.2f %s", room.calculatePerimeter(), unitToDisplay));
    }

    private void updateMousePositionDisplay(double mouseX, double mouseY) {
        String unitToDisplay = unitSymbols.get(units.indexOf(cellUnit));
        mousePositionLabel.setText(String.format("Mouse Position: (%.2f %s, %.2f %s)", mouseX, unitToDisplay, mouseY, unitToDisplay));
    }
    
    public double getMouseX(int floorNumber, double px) {
        double currentZoomFactor = zoomFactors.getOrDefault(floorNumber, 1.0);
        return (double) px / displayGridSize * cellLength / currentZoomFactor - 50 * cellLength;
    }
    
    public double getMouseY(int floorNumber, double py) {
        double currentZoomFactor = zoomFactors.getOrDefault(floorNumber, 1.0);
        return 50 * cellLength - (double) py / displayGridSize * cellLength / currentZoomFactor;
    }  

    public double getPixelX(int floorNumber, double userX) {
        double currentZoomFactor = zoomFactors.getOrDefault(floorNumber, 1.0);
        return (userX + 50 * cellLength) * displayGridSize * currentZoomFactor / cellLength;
    }
    
    public double getPixelY(int floorNumber, double userY) {
        double currentZoomFactor = zoomFactors.getOrDefault(floorNumber, 1.0);
        return (50 * cellLength - userY) * displayGridSize * currentZoomFactor / cellLength;
    }

    private int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public boolean checkOverlap(List<Room> rooms, Room newRoom) {
        Area newRoomArea = newRoom.getArea();
    
        for (Room room : rooms) {
            Area roomArea = room.getArea();
    
            // Create a copy of the Areas to avoid modifying the originals
            Area intersection = new Area(newRoomArea);
            intersection.intersect(roomArea);
    
            if (!intersection.isEmpty()) {
                // There is some overlap
                return true;
            }
        }
    
        return false; // No overlaps found
    }

    public Room boundingBoxMaker(Room room) {
        List<double[]> vertices = room.getVertices();

        if (vertices.isEmpty()) {
            throw new IllegalArgumentException("The room has no vertices.");
        }

        // Initialize min and max with the first vertex
        double minX = vertices.get(0)[0];
        double minY = vertices.get(0)[1];
        double maxX = vertices.get(0)[0];
        double maxY = vertices.get(0)[1];

        // Iterate through all vertices to find the min and max coordinates
        for (double[] vertex : vertices) {
            double x = vertex[0];
            double y = vertex[1];

            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
        }
        minX-=0.5;
        maxX+=0.5;
        minY-=0.5;
        maxY+=0.5;

        List<double[]> boundingBoxEdges = new ArrayList<>();
        boundingBoxEdges.add(new double[]{minX, minY, maxX, minY});
        boundingBoxEdges.add(new double[]{maxX, minY, maxX, maxY});
        boundingBoxEdges.add(new double[]{maxX, maxY, minX, maxY});
        boundingBoxEdges.add(new double[]{minX, maxY, minX, minY});
        
        Room boundingBox = new Room(boundingBoxEdges);
        boundingBox.setEdges(boundingBoxEdges);

        return boundingBox;
    }

    public void addRoomToFloor(Room newRoom, int currentFloor) {
        double stepSize = 1.0; // Adjust as necessary
        int maxAttempts = 1000; // Maximum number of positions to try
        boolean placed = false;

        List<Room> existingRooms = floorRooms.get(currentFloor);

        // Create the spiral position generator
        SpiralPositionGenerator positionGenerator = new SpiralPositionGenerator(maxAttempts);

        // Store the initial edges of the room for resetting
        List<double[]> originalEdges = new ArrayList<>();
        for (double[] edge : newRoom.getEdges()) {
            originalEdges.add(edge.clone());
        }

        while (positionGenerator.hasNext()) {
            Point pos = positionGenerator.next();

            // Reset the room to its original position
            newRoom.setEdges(originalEdges);

            // The origin of the room is the first vertex
            double initialX = originalEdges.get(0)[0] - 50.0; // x1 of first edge
            double initialY = -(originalEdges.get(0)[1] - 50.0); // y1 of first edge

            // // Calculate new position
            double newX = pos.x * stepSize;
            double newY = pos.y * stepSize;

            // Calculate movement delta
            double dx = newX - initialX;
            double dy = newY - initialY;
            System.out.println(initialX+" "+initialY+" "+dx+" "+dy);

            // Move the room to the new position
            newRoom.moveRoom(dx, dy);

            // Check for overlap
            if (!checkOverlap(existingRooms, boundingBoxMaker(newRoom))) {
                // No overlap, place the room
                existingRooms.add(newRoom);
                floorCanvases.get(currentFloor).repaint();
                placed = true;
                break;
            }

            // If overlap, continue to next position
        }

        if (!placed) {
            System.out.println("Unable to place the room without overlap.");
        }

    }


    public FloorPlanMaker() {
        setTitle("Floor Plan Builder");
        setSize(1400, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 245, 245)); // Frame background color

        zoomFactors = new HashMap<>();

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBackground(new Color(220, 220, 220)); // Button panel background color

        JButton newFrameButton = createStyledButton("New Frame");
        JButton openButton = createStyledButton("Open");
        JButton saveButton = createStyledButton("Save");
        JButton closeButton = createStyledButton("Close");
        JButton gridPropertiesButton = createStyledButton("Grid Properties");

        // Adding buttons to panel
        buttonPanel.add(newFrameButton);
        buttonPanel.add(openButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);
        buttonPanel.add(gridPropertiesButton);

        add(buttonPanel, BorderLayout.NORTH);

        
        

        tabbedPane = new JTabbedPane();

        // Create an initial panel with a message
        JPanel initialPanel = new JPanel(new BorderLayout());
        JLabel messageLabel = new JLabel("To begin, open a saved project or click + to add a new floor", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        messageLabel.setForeground(Color.GRAY);
        initialPanel.add(messageLabel, BorderLayout.CENTER);
        tabbedPane.addTab("", initialPanel);  // Add this panel initially

        // Add + button tab
        JPanel addTabPanel = new JPanel(new BorderLayout());
        JButton addTabButton = new JButton("+");
        addTabButton.setFocusPainted(false);
        addTabButton.setBorderPainted(false);
        addTabButton.setContentAreaFilled(false);
        addTabButton.setForeground(new Color(70, 130, 180)); // Button color
        addTabButton.setFont(new Font("Arial", Font.BOLD, 20));
        addTabButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Remove initial message if it's still present
                if (tabbedPane.getTabCount() > 1 && tabbedPane.getComponentAt(0) == initialPanel) {
                    tabbedPane.remove(0);
                }
                addNewFloor(); // Call your method to add a new floor
            }
        });
        addTabPanel.add(addTabButton, BorderLayout.CENTER);
        tabbedPane.addTab("", null);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, addTabPanel);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int selectedIndex = tabbedPane.getSelectedIndex();
                if (selectedIndex != -1) {
                    System.out.println("Tab changed. Current index: " + selectedIndex);
                    currentFloor = selectedIndex + 1;
                    if (currentlySelectedRoom != null) {
                        currentlySelectedRoom.setSelected(false);
                        currentlySelectedRoom = null;
                    }
                    selectColor.setVisible(false);
                    roomLabel.setVisible(false);
                    roomArea.setVisible(false);
                    roomPerimeter.setVisible(false);
                    
                }
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        // Adding Side Panel
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new GridLayout(3, 1, 0, 5)); // Equal-sized rows
        sidePanel.setPreferredSize(new Dimension(400, 600));
        sidePanel.setBackground(new Color(240, 248, 255)); 

        // Set fixed height for each part of the side panel
        int sidePanelHeight = 600 / 3;

        // Set fixed button dimensions for all panels
        int buttonWidth = 350;
        int buttonHeight = 40;

        // Rooms Panel
        JPanel roomsPanel = new JPanel();
        roomsPanel.setLayout(new BoxLayout(roomsPanel, BoxLayout.Y_AXIS));
        roomsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Rooms"),
            BorderFactory.createEmptyBorder(0, 0, 0, 20) // No padding
        ));
        roomsPanel.setPreferredSize(new Dimension(400, sidePanelHeight));
        roomsPanel.setBackground(new Color(240, 248, 255)); // Rooms panel background color

        JButton addRectangleRoomButton = createStyledButton("Add Rectangle Room");
        JButton addTShapeRoomButton = createStyledButton("Add T-Shape Room");
        JButton addUShapeRoomButton = createStyledButton("Add U-Shape Room");
        JButton addLShapeRoomButton = createStyledButton("Add L-Shape Room");
        JButton addCustomRoomButton = createStyledButton("Add Custom Room");

        addRectangleRoomButton.setMaximumSize(new Dimension(buttonWidth, buttonHeight));
        addTShapeRoomButton.setMaximumSize(new Dimension(buttonWidth, buttonHeight));
        addUShapeRoomButton.setMaximumSize(new Dimension(buttonWidth, buttonHeight));
        addLShapeRoomButton.setMaximumSize(new Dimension(buttonWidth, buttonHeight));
        addCustomRoomButton.setMaximumSize(new Dimension(buttonWidth, buttonHeight));
        addRectangleRoomButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addTShapeRoomButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addUShapeRoomButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addLShapeRoomButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        addCustomRoomButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Adding buttons directly without vertical glue
        roomsPanel.add(addRectangleRoomButton);
        roomsPanel.add(Box.createVerticalStrut(10));
        roomsPanel.add(addTShapeRoomButton);
        roomsPanel.add(Box.createVerticalStrut(10));
        roomsPanel.add(addUShapeRoomButton);
        roomsPanel.add(Box.createVerticalStrut(10));
        roomsPanel.add(addLShapeRoomButton);
        roomsPanel.add(Box.createVerticalStrut(10));
        roomsPanel.add(addCustomRoomButton);

        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(roomsPanel, BorderLayout.CENTER);
        containerPanel.setPreferredSize(new Dimension(400, sidePanelHeight));
        containerPanel.setBackground(new Color(240, 248, 255)); // Match background color

        JScrollPane roomsScrollPane = new JScrollPane(containerPanel);
        roomsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        roomsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        roomsScrollPane.setPreferredSize(new Dimension(400, sidePanelHeight));
        roomsScrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI()); // Custom scrollbar color



        // Fixtures Panel
        JPanel fixturesPanel = new JPanel();
        fixturesPanel.setLayout(new GridLayout(0, 2, 10, 10)); // 2 columns with spacing
        fixturesPanel.setPreferredSize(null); // Let the scroll pane determine size
        fixturesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding around fixtures
        fixturesPanel.setBackground(new Color(240, 248, 255)); // Fixtures panel background color

        // Add room category buttons
        String[] roomCategories = {"All", "Common Room", "Bedroom", "Kitchen", "Bathroom"};
        ButtonGroup buttonGroup = new ButtonGroup();
        JPanel roomCategoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roomCategoryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Padding around buttons
        roomCategoryPanel.setBackground(new Color(240, 248, 255)); // Match fixtures panel background color

        // Add category buttons and filter logic
        JToggleButton[] categoryButtons = new JToggleButton[roomCategories.length];
        for (int i = 0; i < roomCategories.length; i++) {
            String category = roomCategories[i];
            JToggleButton button = new JToggleButton(category);
            button.setBackground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            buttonGroup.add(button);

            // Action Listener for category buttons
            button.addActionListener(e -> {
                if (category.equals("All")) {
                    showAllFixtures(fixturesPanel); // Show all fixtures for "All" category
                } else {
                    filterFixturesByRoom(fixturesPanel, category); // Filter fixtures by selected category
                }

                // Highlight the selected button
                for (JToggleButton b : categoryButtons) {
                    b.setBackground(b == button ? new Color(173, 216, 230) : Color.WHITE);
                }
            });

            categoryButtons[i] = button;
            roomCategoryPanel.add(button);
        }

        // Add "Clear Filter" button
        // JButton clearFilterButton = new JButton("Clear Filter");
        // clearFilterButton.setFocusPainted(false);
        // clearFilterButton.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        // clearFilterButton.setBackground(Color.WHITE);
        // clearFilterButton.addActionListener(e -> {
        //     buttonGroup.clearSelection(); // Clear selected category
        //     showAllFixtures(fixturesPanel); // Reset to show all fixtures
        //     for (JToggleButton b : categoryButtons) {
        //         b.setBackground(Color.WHITE); // Reset button colors
        //     }
        // });
        // roomCategoryPanel.add(clearFilterButton);

        // Fixtures Panel Container
        JPanel fixturesContainer = new JPanel(new BorderLayout());
        fixturesContainer.add(roomCategoryPanel, BorderLayout.NORTH); // Add room category buttons at the top
        fixturesContainer.add(fixturesPanel, BorderLayout.CENTER);    // Add the fixtures panel in the center

        // Create buttons with icons for each fixture
        JButton addDoorButton         = createStyledButtonWithIcon("Add Door",         "fixtures_icons\\door.png",         Arrays.asList("Common Room", "Bedroom", "Kitchen", "Bathroom"));
        JButton addChairButton        = createStyledButtonWithIcon("Add Chair",        "fixtures_icons\\chair.png",        Arrays.asList("Common Room", "Bedroom"));
        JButton addStoveButton        = createStyledButtonWithIcon("Add Stove",        "fixtures_icons\\stove.png",        Arrays.asList("Kitchen"));
        JButton addShowerButton       = createStyledButtonWithIcon("Add Shower",       "fixtures_icons\\shower.png",       Arrays.asList("Bathroom"));
        JButton addTableButton        = createStyledButtonWithIcon("Add Table",        "fixtures_icons\\table.png",        Arrays.asList("Common Room", "Bedroom"));
        JButton addWindowButton       = createStyledButtonWithIcon("Add Window",       "fixtures_icons\\window.png",       Arrays.asList("Common Room", "Kitchen", "Bedroom", "Bathroom"));
        JButton addSofaButton         = createStyledButtonWithIcon("Add Sofa",         "fixtures_icons\\sofa.png",         Arrays.asList("Common Room"));
        JButton addBedSingleButton    = createStyledButtonWithIcon("Add Bed Single",   "fixtures_icons\\bed_single.png",   Arrays.asList("Bedroom"));
        JButton addBedDoubleButton    = createStyledButtonWithIcon("Add Bed Double",   "fixtures_icons\\bed_double.png",   Arrays.asList("Bedroom"));
        JButton addCupboardButton     = createStyledButtonWithIcon("Add Cupboard",     "fixtures_icons\\cupboard.png",     Arrays.asList("Common Room", "Bedroom"));
        JButton addBathroomsinkButton = createStyledButtonWithIcon("Add Bathroom Sink","fixtures_icons\\sink.png",         Arrays.asList("Bathroom"));
        JButton addKitchensinkButton  = createStyledButtonWithIcon("Add Kitchen Sink", "fixtures_icons\\kitchensink.png",  Arrays.asList("Kitchen"));
        JButton addDiningsetButton    = createStyledButtonWithIcon("Add Dining Set",   "fixtures_icons\\diningset.png",    Arrays.asList("Common Room", "Kitchen"));
        JButton addToiletButton       = createStyledButtonWithIcon("Add Toilet",       "fixtures_icons\\toilet.png",       Arrays.asList("Bathroom"));
        JButton addStaircaseButton    = createStyledButtonWithIcon("Add Staircase",    "fixtures_icons\\staircase.png",    Arrays.asList("Common Room"));

        // Add fixture buttons to the panel
        JButton[] fixtureButtons = {addDoorButton, addChairButton, addStoveButton, addShowerButton, addTableButton,
            addWindowButton, addSofaButton, addBedSingleButton, addBedDoubleButton, addCupboardButton,
            addBathroomsinkButton, addKitchensinkButton, addDiningsetButton, addToiletButton, addStaircaseButton};

        for (JButton button : fixtureButtons) {
            allFixtureButtons.add(button); // Add to the master list
        }

        // Ensure the initial state of the fixtures panel shows all fixtures
        showAllFixtures(fixturesPanel);

        JScrollPane fixturesScrollPane = new JScrollPane(fixturesContainer);
        fixturesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        fixturesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        fixturesScrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI()); // Custom scrollbar color
        fixturesScrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());

        // Properties Panel
        JPanel propertiesPanel = new JPanel();
        propertiesPanel.setLayout(new BoxLayout(propertiesPanel, BoxLayout.Y_AXIS));
        propertiesPanel.setBorder(BorderFactory.createTitledBorder("Properties"));
        propertiesPanel.setPreferredSize(new Dimension(400, sidePanelHeight));
        propertiesPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        propertiesPanel.setBackground(new Color(240, 248, 255)); // Properties panel background color

        JLabel propertyLabel = new JLabel("Select an item to view/edit properties");
        propertyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mousePositionLabel = new JLabel("Mouse Position: (0.0, 0.0)");
        mousePositionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        roomArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        roomPerimeter.setAlignmentX(Component.CENTER_ALIGNMENT);
        roomLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        selectColor.setMaximumSize(new Dimension(250, selectColor.getPreferredSize().height));

        selectColor.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e){
                for (Room room : floorRooms.get(currentFloor)){
                   // "Common room", "Bedroom", "bathroom", "kitchen"
                    if(room.getSelected()){

                         //set color 
                         if(selectColor.getSelectedItem()=="Common room"){
                            room.setRoomtype(0);
                           System.out.println(room.getRoomtype());

                        }
                        else if(selectColor.getSelectedItem()=="Bedroom"){
                            room.setRoomtype(1);
                            System.out.println(room.getRoomtype());
                        }
                        else if(selectColor.getSelectedItem()=="Bathroom"){
                            room.setRoomtype(2);
                            System.out.println(room.getRoomtype());
                        }
                        else if(selectColor.getSelectedItem()=="Kitchen"){
                            room.setRoomtype(3);;
                            System.out.println(room.getRoomtype());
                        }
                        floorCanvases.get(currentFloor).repaint();
                    }
                }

            }
        });;

        selectColor.setVisible(false);
        roomLabel.setVisible(false);
        roomArea.setVisible(false);
        roomPerimeter.setVisible(false);
        // roomLabel.setEnabled(false);

        propertiesPanel.add(Box.createVerticalGlue());
        propertiesPanel.add(propertyLabel);
        propertiesPanel.add(Box.createVerticalStrut(10));
        propertiesPanel.add(mousePositionLabel);
        propertiesPanel.add(Box.createVerticalStrut(10));
        propertiesPanel.add(roomArea);
        propertiesPanel.add(Box.createVerticalStrut(10));
        propertiesPanel.add(roomPerimeter);
        propertiesPanel.add(Box.createVerticalStrut(10));
        propertiesPanel.add(roomLabel);
        propertiesPanel.add(Box.createVerticalStrut(10));
        propertiesPanel.add(selectColor);
        
        
        propertiesPanel.add(Box.createVerticalGlue());
        JScrollPane propertiesScrollPane = new JScrollPane(propertiesPanel);
        propertiesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        propertiesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        propertiesScrollPane.setPreferredSize(new Dimension(400, sidePanelHeight));
        propertiesScrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI()); // Custom scrollbar color

        sidePanel.add(roomsScrollPane);
        sidePanel.add(fixturesScrollPane);
        sidePanel.add(propertiesScrollPane);

        add(sidePanel, BorderLayout.EAST);

        // Action Listeners
        newFrameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("New Frame button clicked");
                for (Room room : floorRooms.get(currentFloor)) {
                    System.out.print(room+" ");
                }
                System.out.println();
            }
        });

        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Open button clicked");
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showOpenDialog(FloorPlanMaker.this); // Use the enclosing JFrame
                if (option == JFileChooser.APPROVE_OPTION) {
                    String filePath = fileChooser.getSelectedFile().getPath();
                    loadFromFile(filePath);
                }
            }
        });

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Save button clicked");
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showSaveDialog(FloorPlanMaker.this);
                if (option == JFileChooser.APPROVE_OPTION) {
                    String filePath = fileChooser.getSelectedFile().getPath();
                    // Ensure the file has a .fp extension or any preferred extension
                    if (!filePath.endsWith(".fp")) {
                        filePath += ".fp";
                    }
                    saveToFile(filePath);
                }
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Close button clicked");
                dispose();
            }
        });

        
        gridPropertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
        
                // Create panel
                JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
                
                // Checkbox setup
                JCheckBox showGridCheckBox = new JCheckBox("Show Grid", showGrid);
                
                // Length input field setup
                JPanel lengthPanel = new JPanel(new GridLayout(1, 3, 5, 5));
                JTextField inputField = new JTextField();
                ((AbstractDocument) inputField.getDocument()).setDocumentFilter(new DocumentFilter() {
                    @Override
                    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                        if (string.matches("\\d+")) { // Only allow digits
                            super.insertString(fb, offset, string, attr);
                        }
                    }
        
                    @Override
                    public void replace(FilterBypass fb, int offset, int length, String string, AttributeSet attr) throws BadLocationException {
                        if (string.matches("\\d+")) { // Only allow digits
                            super.replace(fb, offset, length, string, attr);
                        }
                    }
                });
                inputField.setText(String.valueOf(cellLength));
        
                // Unit selection dropdown setup
                JComboBox<String> unitComboBox = new JComboBox<>(units.toArray(new String[0]));
                unitComboBox.setSelectedItem(cellUnit);
        
                // Length panel
                lengthPanel.add(new JLabel("Cell length:"));
                lengthPanel.add(inputField);
                lengthPanel.add(unitComboBox);
        
                // Main panel
                panel.add(showGridCheckBox);
                panel.add(lengthPanel);
        
                // Show dialog
                int option = JOptionPane.showConfirmDialog(null, panel, "Grid properties", JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.OK_OPTION) {
                    cellLength = Integer.parseInt(inputField.getText());
                    cellUnit = (String) unitComboBox.getSelectedItem();

                    for (Room room : floorRooms.get(currentFloor)) {
                        if (room.getSelected()) { // Assuming there's a `isSelected()` method
                            updateRoomProperties(room);
                            break;
                        }
                    }


                    showGrid = showGridCheckBox.isSelected();
                    System.out.println("Cell length set to: " + cellLength + " " + cellUnit);
                    System.out.println("Show grid: " + showGrid);

                    for (JPanel canvas : floorCanvases.values()) {
                        canvas.repaint();
                    }
                }
            }
        });        
        


        // Adding action listeners for room and fixture buttons
        addRectangleRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String[] labelsRectShape = {"a", "b"};
                String imagePathRectShape = "RoomImages/rectangleRoomImage.png"; // Update this path

                // Create and display the dynamic dialog                 
                DynamicInputDialog dialog = new DynamicInputDialog(null, 
                                                                   "Add Rectangle Room", imagePathRectShape, labelsRectShape, 
                                                                   300, 300, unitSymbols.get(units.indexOf(cellUnit)));
                dialog.setVisible(true);

                // After the dialog is closed, retrieve the entered values
                if (dialog.isConfirmed()) {
                    HashMap<String, Double> lengths = dialog.getInputValues();
                    Room rectRoom = new Room(Arrays.asList(
                        new double[]{0.0, 0.0, lengths.get("a"), 0.0},
                        new double[]{lengths.get("a"), 0.0, lengths.get("a"), lengths.get("b")},
                        new double[]{lengths.get("a"), lengths.get("b"), 0.0, lengths.get("b")},
                        new double[]{0.0, lengths.get("b"), 0.0, 0.0}
                    ));
                    rectRoom.setRoomtype(dialog.getRoomtype());
                    addRoomToFloor(rectRoom, currentFloor);
                    double[] rCenter = rectRoom.getCenter(); 
                    System.out.println(rCenter[0]+" "+rCenter[1]);
                    centerCanvas(currentFloor, rCenter[0]-50, 50-rCenter[1]);

                } else {
                    System.out.println("Room addition cancelled.");
                }
            }
        });

        addTShapeRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String[] labelsTShape = {"a", "b", "c", "d", "e"};
                String imagePathTShape = "RoomImages/TRoomImage.png"; // Update this path

                // Create and display the dynamic dialog                 
                DynamicInputDialog dialog = new DynamicInputDialog(null, 
                                                                   "Add T-Shape Room", imagePathTShape, labelsTShape, 
                                                                   300, 240, unitSymbols.get(units.indexOf(cellUnit)));
                dialog.setVisible(true);

                // After the dialog is closed, retrieve the entered values
                if (dialog.isConfirmed()) {
                    HashMap<String, Double> lengths = dialog.getInputValues();
                    Room TRoom = new Room(Arrays.asList(
                        new double[]{lengths.get("a"), 0.0, lengths.get("a")+lengths.get("b"), 0.0},
                        new double[]{lengths.get("a")+lengths.get("b"), 0.0, lengths.get("a")+lengths.get("b"), lengths.get("e")},
                        new double[]{lengths.get("a")+lengths.get("b"), lengths.get("e"), lengths.get("a")+lengths.get("b")+lengths.get("c"), lengths.get("e")},
                        new double[]{lengths.get("a")+lengths.get("b")+lengths.get("c"), lengths.get("e"), lengths.get("a")+lengths.get("b")+lengths.get("c"), lengths.get("e")+lengths.get("d")},
                        new double[]{lengths.get("a")+lengths.get("b")+lengths.get("c"), lengths.get("e")+lengths.get("d"), 0.0, lengths.get("d")+lengths.get("e")},
                        new double[]{0.0, lengths.get("d")+lengths.get("e"), 0.0, lengths.get("e")},
                        new double[]{0.0, lengths.get("e"), lengths.get("a"), lengths.get("e")},
                        new double[]{lengths.get("a"), lengths.get("e"), lengths.get("a"), 0.0}
                    ));
                    TRoom.setRoomtype(dialog.getRoomtype());
                    addRoomToFloor(TRoom, currentFloor);
                    double[] rCenter = TRoom.getCenter(); 
                    System.out.println(rCenter[0]+" "+rCenter[1]);
                    centerCanvas(currentFloor, rCenter[0]-50, 50-rCenter[1]);

                } else {
                    System.out.println("Room addition cancelled.");
                }
            }
        });

        addUShapeRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String[] labelsUShape = {"a", "b", "c", "d", "e"};
                String imagePathUShape = "RoomImages/URoomImage.png"; // Update this path

                // Create and display the dynamic dialog                 
                DynamicInputDialog dialog = new DynamicInputDialog(null, 
                                                                   "Add U-Shape Room", imagePathUShape, labelsUShape, 
                                                                   300, 300, unitSymbols.get(units.indexOf(cellUnit)));
                dialog.setVisible(true);

                // After the dialog is closed, retrieve the entered values
                if (dialog.isConfirmed()) {
                    HashMap<String, Double> lengths = dialog.getInputValues();
                    Room URoom = new Room(Arrays.asList(
                        new double[]{0.0, 0.0, lengths.get("a")+lengths.get("b")+lengths.get("c"), 0.0},
                        new double[]{lengths.get("a")+lengths.get("b")+lengths.get("c"), 0.0, lengths.get("a")+lengths.get("b")+lengths.get("c"), lengths.get("e")},
                        new double[]{lengths.get("a")+lengths.get("b")+lengths.get("c"), lengths.get("e"), lengths.get("a")+lengths.get("b")+lengths.get("c"), lengths.get("e")+lengths.get("d")},
                        new double[]{lengths.get("a")+lengths.get("b")+lengths.get("c"), lengths.get("e")+lengths.get("d"), lengths.get("a")+lengths.get("b"), lengths.get("e")+lengths.get("d")},
                        new double[]{lengths.get("a")+lengths.get("b"), lengths.get("e")+lengths.get("d"), lengths.get("a")+lengths.get("b"), lengths.get("e")},
                        new double[]{lengths.get("a")+lengths.get("b"), lengths.get("e"), lengths.get("a"), lengths.get("e")},
                        new double[]{lengths.get("a"), lengths.get("e"), lengths.get("a"), lengths.get("e")+lengths.get("d")},
                        new double[]{lengths.get("a"), lengths.get("e")+lengths.get("d"), 0.0, lengths.get("e")+lengths.get("d")},
                        new double[]{0.0, lengths.get("e")+lengths.get("d"), 0.0, 0.0}
                    ));
                    URoom.setRoomtype(dialog.getRoomtype());
                    addRoomToFloor(URoom, currentFloor);
                    double[] rCenter = URoom.getCenter(); 
                    System.out.println(rCenter[0]+" "+rCenter[1]);
                    centerCanvas(currentFloor, rCenter[0]-50, 50-rCenter[1]);

                } else {
                    System.out.println("Room addition cancelled.");
                }
            }
        });

        addLShapeRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final String[] labelsLShape = {"a", "b", "c", "d"};
                String imagePathLShape = "RoomImages/LRoomImage.png"; // Update this path

                // Create and display the dynamic dialog                 
                DynamicInputDialog dialog = new DynamicInputDialog(null, 
                                                                   "Add L-Shape Room", imagePathLShape, labelsLShape, 
                                                                   300, 300, unitSymbols.get(units.indexOf(cellUnit)));
                dialog.setVisible(true);

                // After the dialog is closed, retrieve the entered values
                if (dialog.isConfirmed()) {
                    HashMap<String, Double> lengths = dialog.getInputValues();
                    Room LRoom = new Room(Arrays.asList(
                        new double[]{0.0, 0.0, lengths.get("a")+lengths.get("b"), 0.0},
                        new double[]{lengths.get("a")+lengths.get("b"), 0.0, lengths.get("a")+lengths.get("b"), lengths.get("d")},
                        new double[]{lengths.get("a")+lengths.get("b"), lengths.get("d"), lengths.get("a"), lengths.get("d")},
                        new double[]{lengths.get("a"), lengths.get("d"), lengths.get("a"), lengths.get("d")+lengths.get("c")},
                        new double[]{lengths.get("a"), lengths.get("d")+lengths.get("c"), 0.0, lengths.get("d")+lengths.get("c")},
                        new double[]{0.0, lengths.get("d")+lengths.get("c"), 0.0, 0.0}
                    ));
                    LRoom.setRoomtype(dialog.getRoomtype());
                    addRoomToFloor(LRoom, currentFloor);
                    double[] rCenter = LRoom.getCenter(); 
                    System.out.println(rCenter[0]+" "+rCenter[1]);
                    centerCanvas(currentFloor, rCenter[0]-50, 50-rCenter[1]);

                } else {
                    System.out.println("Room addition cancelled.");
                }
            }
        });

        addCustomRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Custom Room added to the floor plan");
                new RoomDesignerGUIv5(FloorPlanMaker.this, cellLength, displayGridSize); // Pass null if MainCanvas is not needed
                System.out.println("1");
                // SwingUtilities.invokeLater(() -> {
                    
                // });
                System.out.println("2");
            }
        });

        addDoorButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddDoorListener().actionPerformed(e);
            }
        });

        addWindowButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddWindowListener().actionPerformed(e);
            }
        });

        addSofaButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddSofaListener().actionPerformed(e);
            }
        });

        addChairButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddChairListener().actionPerformed(e);
            }
        });

        addBedDoubleButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddBedDoubleListener().actionPerformed(e);
            }
        });

        addBedSingleButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddBedSingleListener().actionPerformed(e);
            }
        });

        addBathroomsinkButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddBathroomsinkListener().actionPerformed(e);
            }
        });

        addCupboardButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddCupboardListener().actionPerformed(e);
            }
        });

        addTableButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddTableListener().actionPerformed(e);
            }
        });

        addToiletButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddToiletListener().actionPerformed(e);
            }
        });

        addKitchensinkButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddKitchensinkListener().actionPerformed(e);
            }
        });

        addShowerButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddShowerListener().actionPerformed(e);
            }
        });

        addDiningsetButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddDiningsetListener().actionPerformed(e);
            }
        });

        addStoveButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddStoveListener().actionPerformed(e);
            }
        });

        addStaircaseButton.addActionListener(e -> {
            FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
            if (fixtureManager != null) {
                fixtureManager.getAddStaircaseListener().actionPerformed(e);
            }
        });
    }

    private void showAllFixtures(JPanel fixturesPanel) {
        fixturesPanel.removeAll(); // Clear existing content

        int buttonSize = 100; // Fixed button size
        int columns = 2; // Number of columns
        int spacing = 10; // Spacing between buttons

        // Set a consistent layout for the fixtures panel
        fixturesPanel.setLayout(new GridLayout(0, columns, spacing, spacing));

        // Add all buttons to the panel
        for (JButton button : allFixtureButtons) {
            button.setPreferredSize(new Dimension(buttonSize, buttonSize)); // Fixed size
            fixturesPanel.add(button);
        }

        // Recalculate panel size based on the total number of buttons
        int rows = (allFixtureButtons.size() + columns - 1) / columns;
        fixturesPanel.setPreferredSize(new Dimension(
                (buttonSize + spacing) * columns, // Width based on columns
                (buttonSize + spacing) * rows // Height based on rows
        ));

        // Refresh the panel
        fixturesPanel.revalidate();
        fixturesPanel.repaint();
    }

    private void setupRoomCategoryButtons(JPanel roomCategoryPanel, JPanel fixturesPanel) {
        // Define room categories
        String[] roomTypes = {"Common Room", "Bedroom", "Kitchen", "Bathroom"};
        ButtonGroup buttonGroup = new ButtonGroup(); // To manage selection

        for (String roomType : roomTypes) {
            JToggleButton roomButton = new JToggleButton(roomType);
            roomButton.setBackground(Color.LIGHT_GRAY);

            // Add a listener to filter the fixtures based on the room type
            roomButton.addActionListener(e -> {
                if (roomButton.isSelected()) {
                    roomButton.setBackground(Color.CYAN); // Highlight selected button
                    filterFixturesByRoom(fixturesPanel, roomType); // Filter fixtures
                } else {
                    roomButton.setBackground(Color.LIGHT_GRAY); // Reset color
                    showAllFixtures(fixturesPanel); // Show all fixtures
                }
            });

            buttonGroup.add(roomButton); // Add to button group
            roomCategoryPanel.add(roomButton); // Add to the room category panel
        }

        // Add a "Clear Filter" button to reset the fixtures panel
        JButton clearFilterButton = new JButton("Clear Filter");
        clearFilterButton.setBackground(Color.WHITE);
        clearFilterButton.addActionListener(e -> {
            buttonGroup.clearSelection(); // Clear any selected room button
            showAllFixtures(fixturesPanel); // Show all fixtures
        });
        roomCategoryPanel.add(clearFilterButton); // Add to the room category panel
    }

    private void filterFixturesByRoom(JPanel fixturesPanel, String selectedRoomType) {
        fixturesPanel.removeAll(); // Clear existing content

        int buttonSize = 100; // Fixed button size
        int columns = 2; // Number of columns
        int spacing = 10; // Spacing between buttons

        // Set a consistent layout for the fixtures panel
        fixturesPanel.setLayout(new GridLayout(0, columns, spacing, spacing));

        // Add buttons matching the selected room category
        int filteredCount = 0;
        for (JButton button : allFixtureButtons) {
            @SuppressWarnings("unchecked")
            List<String> categories = (List<String>) button.getClientProperty("categories");

            if (categories != null && categories.contains(selectedRoomType)) {
                button.setPreferredSize(new Dimension(buttonSize, buttonSize)); // Fixed size
                fixturesPanel.add(button);
                filteredCount++;
            }
        }

        // Recalculate panel size based on the filtered number of buttons
        int rows = (filteredCount + columns - 1) / columns;
        fixturesPanel.setPreferredSize(new Dimension(
                (buttonSize + spacing) * columns, // Width based on columns
                (buttonSize + spacing) * rows // Height based on rows
        ));

        // Refresh the panel
        fixturesPanel.revalidate();
        fixturesPanel.repaint();
    }

    private JButton createStyledButtonWithIcon(String text, String iconPath, List<String> roomTypes) {
        JButton button = new JButton();
        button.putClientProperty("categories", roomTypes); // Store categories as a property
        button.setLayout(new BorderLayout());
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);

        // Add icon to the button
        ImageIcon icon = new ImageIcon(iconPath);
        Image scaledIcon = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        button.setIcon(new ImageIcon(scaledIcon));

        // Add text below the icon
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        button.add(label, BorderLayout.SOUTH);

        // Style the button
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        button.setFocusable(false);

        return button;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(70, 130, 180)); // Button background color
        button.setForeground(Color.WHITE); // Button text color
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        return button;
    }
    
    private void addNewFloor() {
        zoomFactors.put(floorCount, 3.0);
        JPanel gridCanvas = createCanvas(floorCount);
        floorCanvases.put(floorCount, gridCanvas);
        floorRooms.put(floorCount, new ArrayList<>());

        // Initialize fixtures list for the floor
        ArrayList<FixtureManager.Fixture> fixtures = new ArrayList<>();
        floorFixtures.put(floorCount, fixtures);

        // Initialize FixtureManager for the floor
        FixtureManager fixtureManager = new FixtureManager(fixtures, gridCanvas);
        floorFixtureManagers.put(floorCount, fixtureManager);
        // Variable to hold the room being dragged
        // final Room[] draggedRoom = {null};
        // final Point[] dragStartPoint = {null};
        // List<double[]> lastValidEdges = new ArrayList<>();

        // KeyListener to detect 'R' and 'S' keys
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                synchronized (gridCanvas) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        if (e.getKeyCode() == KeyEvent.VK_R) {
                            isRPressed = true;
                        } else if (e.getKeyCode() == KeyEvent.VK_S) {
                            isSPressed = true;
                        }
                    } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                        if (e.getKeyCode() == KeyEvent.VK_R) {
                            isRPressed = false;
                        } else if (e.getKeyCode() == KeyEvent.VK_S) {
                            isSPressed = false;
                        }
                    }
                    return false;
                }
            }
        });

        // Ensure gridCanvas is focusable
gridCanvas.setFocusable(true);
gridCanvas.requestFocusInWindow();

        // Mouse Listener
        gridCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
                if (fixtureManager != null && fixtureManager.isDraggingFixture()) {
                    // FixtureManager will handle this event
                    return;
                }
                boolean insideRoom = false;
                for (Room room : floorRooms.get(currentFloor)) {
                    if (room.checkInterior(getMouseX(currentFloor, e.getX()) + 50, 50 - getMouseY(currentFloor, e.getY()))) {
                        insideRoom = true;
                        String unitToDisplay = unitSymbols.get(units.indexOf(cellUnit));


                        if (currentlySelectedRoom != null) {
                            currentlySelectedRoom.setSelected(false);
                        }
        
                        // Select the clicked room
                        currentlySelectedRoom = room;
                        currentlySelectedRoom.setSelected(true);



                        //display properties 
                        roomArea.setText(String.format("Area : %.2f sq.%s ",room.calculateArea(),unitToDisplay));
                        roomPerimeter.setText(String.format("Perimeter : %.2f %s",room.calculatePerimeter(),unitToDisplay));
                        selectColor.setVisible(true);
                        roomLabel.setVisible(true);
                        roomArea.setVisible(true);
                        roomPerimeter.setVisible(true);
                        selectColor.setSelectedItem(roomTypes[room.getRoomtype()]);
                        
                        room.setSelected(true);
                        System.out.println("in room");
                        draggedRoom[0] = room;
                        dragStartPoint[0] = e.getPoint();

                        lastValidEdges.clear();
                        for (double[] edge : draggedRoom[0].getEdges()) {
                            lastValidEdges.add(edge.clone());
                        }

                        // Store the center of the room
                        double[] center = draggedRoom[0].getCenter();
                        roomCenter[0] = center[0];
                        roomCenter[1] = center[1];

                        if (isRPressed) {
                            // For rotation
                            lastMousePoint[0] = e.getPoint();
                        } else if (isSPressed) {
                            // For scaling
                            double mouseX = getMouseX(currentFloor, e.getX()) + 50;
                            double mouseY = 50 - getMouseY(currentFloor, e.getY());
                            initialDistanceToCenter = distance(mouseX, mouseY, roomCenter[0], roomCenter[1]);

                            lastMousePoint[0] = e.getPoint();
                        }
                        break; // Room found, exit loop
                    } //else {
                       //room.setSelected(false);
                    //}
                }
                gridCanvas.repaint();
                if (!insideRoom) {

                    if (currentlySelectedRoom != null) {
                        currentlySelectedRoom.setSelected(false);
                        currentlySelectedRoom = null;
                    }
                    selectColor.setVisible(false);
                    roomLabel.setVisible(false);
                    roomArea.setVisible(false);
                    roomPerimeter.setVisible(false);

                    draggedRoom[0] = null;
                    dragStartPoint[0] = e.getPoint(); // For canvas dragging
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggedRoom[0] != null) {
                    // Check for overlaps
                    List<Room> checkRooms = new ArrayList<>(floorRooms.get(currentFloor));
                    checkRooms.remove(draggedRoom[0]);
                    boolean hasOverlap = checkOverlap(checkRooms, draggedRoom[0]);

                    if (hasOverlap) {
                        // Revert to lastValidEdges
                        draggedRoom[0].setEdges(lastValidEdges);
                    }

                    // Reset variables
                    draggedRoom[0] = null;
                    lastValidEdges.clear();
                    lastMousePoint[0] = null;
                    initialDistanceToCenter = 0.0;

                    gridCanvas.repaint();
                } else if (dragStartPoint[0] != null) {
                    // Reset dragStartPoint for canvas dragging
                    dragStartPoint[0] = null;
                }
                
                boolean correctdoorPlacement = true;
                for (Room room : floorRooms.get(currentFloor)) {
                    if ((room.getRoomtype() == 1 || room.getRoomtype() == 2) && fixtureManager.roomOverlapDoor(room)) {
                        correctdoorPlacement = false;
                        // System.out.println(fixtureManager.roomOverlapDoor(room));
                        for (Room inroom : floorRooms.get(currentFloor)) {
                            if (inroom != room && fixtureManager.roomOverlapDoor(inroom)) {
                                correctdoorPlacement = true;
                                // System.out.println("correct placement");
                            }
                        }
                    }
                }
                if (!correctdoorPlacement) {
                    fixtureManager.deleteSelectedFixture();
                    JOptionPane.showMessageDialog(gridCanvas, "A door for a bathroom or a bedroom must be connected to another room.", "Door Deleted", JOptionPane.ERROR_MESSAGE);

                }

                boolean correctwindowPlacement = true;
                int windowTouches = 0;
                for (Room room : floorRooms.get(currentFloor)) {
                    if (fixtureManager.roomOverlapWindow(room)) {
                        windowTouches += 1;
                    }
                }
                if (windowTouches > 1) {
                    fixtureManager.deleteSelectedFixture();
                    JOptionPane.showMessageDialog(gridCanvas, "A window cannot be placed between two rooms.", "Window Deleted", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Mouse Motion Listener
        gridCanvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                FixtureManager fixtureManager = floorFixtureManagers.get(currentFloor);
                if (fixtureManager != null && fixtureManager.isDraggingFixture()) {
                    // FixtureManager is handling the drag; do nothing here
                    return;
                }
                if (draggedRoom[0] != null) {
                    if (isRPressed) {
                        // Handle rotation
                        Point currentMousePoint = e.getPoint();

                        // Convert last and current mouse points to room coordinates
                        double lastMouseX = getMouseX(currentFloor, lastMousePoint[0].getX()) + 50;
                        double lastMouseY = 50 - getMouseY(currentFloor, lastMousePoint[0].getY());

                        double currentMouseX = getMouseX(currentFloor, currentMousePoint.getX()) + 50;
                        double currentMouseY = 50 - getMouseY(currentFloor, currentMousePoint.getY());

                        // Compute angles relative to the room's center
                        double angleLast = Math.atan2(lastMouseY - roomCenter[1], lastMouseX - roomCenter[0]);
                        double angleCurrent = Math.atan2(currentMouseY - roomCenter[1], currentMouseX - roomCenter[0]);

                        double deltaAngle = angleCurrent - angleLast;

                        // Normalize deltaAngle
                        deltaAngle = ((deltaAngle + Math.PI) % (2 * Math.PI)) - Math.PI;

                        // Rotate the room
                        draggedRoom[0].rotateRoom(deltaAngle);

                        // Check for overlaps
                        List<Room> checkRooms = new ArrayList<>(floorRooms.get(currentFloor));
                        checkRooms.remove(draggedRoom[0]);
                        boolean hasOverlap = checkOverlap(checkRooms, draggedRoom[0]);

                        if (!hasOverlap) {
                            lastValidEdges.clear();
                            for (double[] edge : draggedRoom[0].getEdges()) {
                                lastValidEdges.add(edge.clone());
                            }
                        } else {
                            // Revert rotation
                            draggedRoom[0].setEdges(lastValidEdges);
                        }

                        // Update lastMousePoint
                        lastMousePoint[0] = currentMousePoint;

                        gridCanvas.repaint();

                    } else if (isSPressed) {
                        // Handle scaling
                        Point currentMousePoint = e.getPoint();

                        // Convert current mouse point to room coordinates
                        double currentMouseX = getMouseX(currentFloor, currentMousePoint.getX()) + 50;
                        double currentMouseY = 50 - getMouseY(currentFloor, currentMousePoint.getY());

                        // Calculate current distance to center
                        double currentDistanceToCenter = distance(currentMouseX, currentMouseY, roomCenter[0], roomCenter[1]);

                        if (initialDistanceToCenter == 0) {
                            initialDistanceToCenter = 0.001; // Prevent division by zero
                        }

                        // Calculate scaling factor
                        double scaleFactor = currentDistanceToCenter / initialDistanceToCenter;

                        // Limit the scale factor
                        scaleFactor = Math.max(scaleFactor, 0.1); // Minimum scale factor
                        scaleFactor = Math.min(scaleFactor, 10.0); // Maximum scale factor

                        // Scale the room
                        draggedRoom[0].scaleRoom(scaleFactor);

                        // Check for overlaps
                        List<Room> checkRooms = new ArrayList<>(floorRooms.get(currentFloor));
                        checkRooms.remove(draggedRoom[0]);
                        boolean hasOverlap = checkOverlap(checkRooms, draggedRoom[0]);

                        if (!hasOverlap) {
                            lastValidEdges.clear();
                            for (double[] edge : draggedRoom[0].getEdges()) {
                                lastValidEdges.add(edge.clone());
                            }
                            // Update initialDistanceToCenter
                            initialDistanceToCenter = currentDistanceToCenter;
                        } else {
                            // Revert scaling
                            draggedRoom[0].setEdges(lastValidEdges);
                        }

                        // Update lastMousePoint
                        lastMousePoint[0] = currentMousePoint;

                        gridCanvas.repaint();

                    } else {
                        // System.out.println("draggingggg");
                        // Handle moving
                        Point dragEndPoint = e.getPoint();
                        double dx = getMouseX(currentFloor, dragEndPoint.getX()) - getMouseX(currentFloor, dragStartPoint[0].getX());
                        double dy = -(getMouseY(currentFloor, dragEndPoint.getY()) - getMouseY(currentFloor, dragStartPoint[0].getY()));
                        draggedRoom[0].moveRoom(dx, dy);
                        
                        List<double[]> edges = draggedRoom[0].getEdges();
                        for (double[] edge : edges) {
                            System.out.println(edge[0]+" "+edge[1]+" "+edge[2]+" "+edge[3]);
                        }
                        //  System.out.println(dx+" "+dy);
                        // Check for overlaps
                        List<Room> checkRooms = new ArrayList<>(floorRooms.get(currentFloor));
                        checkRooms.remove(draggedRoom[0]);
                        boolean hasOverlap = checkOverlap(checkRooms, draggedRoom[0]);

                        if (!hasOverlap) {
                            lastValidEdges.clear();
                            for (double[] edge : draggedRoom[0].getEdges()) {
                                lastValidEdges.add(edge.clone());
                            }
                        } else {
                            // Revert movement
                            System.out.println("reverting?");
                            draggedRoom[0].setEdges(lastValidEdges);
                        }

                        // Update dragStartPoint
                        dragStartPoint[0] = dragEndPoint;

                        gridCanvas.repaint();
                    }
                } else if (dragStartPoint[0] != null) {
                    // Canvas dragging logic
                    Point dragEndPoint = e.getPoint();
                    int dx = (int) ((dragStartPoint[0].x - dragEndPoint.x) * 0.99);
                    int dy = (int) ((dragStartPoint[0].y - dragEndPoint.y) * 0.99);

                    JScrollPane scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, gridCanvas);
                    if (scrollPane != null) {
                        JScrollBar horizontalScrollBar = scrollPane.getHorizontalScrollBar();
                        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();

                        horizontalScrollBar.setValue(horizontalScrollBar.getValue() + dx);
                        verticalScrollBar.setValue(verticalScrollBar.getValue() + dy);
                    }

                    dragStartPoint[0] = dragEndPoint;
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(gridCanvas);
        floorScrollpanes.put(floorCount, scrollPane);
        scrollPane.setPreferredSize(new Dimension(1000, 1000));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI()); // Custom scrollbar color
        scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI()); // Custom scrollbar color
        tabbedPane.insertTab("Floor " + floorCount, null, scrollPane, null, tabbedPane.getTabCount() - 1);
        centerCanvas(floorCount, 0.0, 0.0);
    
        floorCount++;
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);

        
    }
    
    private void centerCanvas(int floorNumber, double targetX, double targetY) {
        SwingUtilities.invokeLater(() -> {

            JScrollPane scrollPane = floorScrollpanes.get(floorNumber);

            // Convert target user coordinates to pixel coordinates
            double pixelX = getPixelX(floorNumber, targetX);
            double pixelY = getPixelY(floorNumber, targetY);

            // Access the viewport of the JScrollPane
            JViewport viewport = scrollPane.getViewport();
            int viewportWidth = viewport.getWidth();
            int viewportHeight = viewport.getHeight();

            // Ensure the viewport dimensions are valid
            if (viewportWidth > 0 && viewportHeight > 0) {
                // Calculate the top-left position to center the target coordinate
                int viewPosX = (int) Math.round(pixelX - viewportWidth / 2.0);
                int viewPosY = (int) Math.round(pixelY - viewportHeight / 2.0);

                // Access the view component (canvas) to get its size
                Component view = viewport.getView();
                Dimension viewSize = view.getPreferredSize(); // Ensure this reflects the current size

                // Clamp the view position to ensure it's within the canvas bounds
                viewPosX = clamp(viewPosX, 0, viewSize.width - viewportWidth);
                viewPosY = clamp(viewPosY, 0, viewSize.height - viewportHeight);

                // Set the new view position
                viewport.setViewPosition(new Point(viewPosX, viewPosY));
            }
        });
    }
    
    private JPanel createCanvas(int floorNumber) {
        JPanel gridCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                double currentZoomFactor = zoomFactors.getOrDefault(floorNumber, 1.0);
                int scaledDisplayGridSize = (int) (displayGridSize * currentZoomFactor);
        
                Graphics2D g2 = (Graphics2D) g;
                
                if (showGrid) {
                    g2.setColor(Color.LIGHT_GRAY);
                    for (int i = 0; i < getWidth(); i += scaledDisplayGridSize) {
                        if (i == 50 * scaledDisplayGridSize) {
                            g2.setStroke(new BasicStroke(4));
                        } else {
                            g2.setStroke(new BasicStroke(2));
                        }
                        g2.drawLine(i, 0, i, getHeight());
                    }
        
                    for (int i = 0; i < getHeight(); i += scaledDisplayGridSize) {
                        if (i == 50 * scaledDisplayGridSize) {
                            g2.setStroke(new BasicStroke(4));
                        } else {
                            g2.setStroke(new BasicStroke(2));
                        }
                        g2.drawLine(0, i, getWidth(), i);
                    }
                }
                
                // Draw rooms
                List<Room> currentRooms = floorRooms.get(floorNumber);
                if (currentRooms != null){
                    for (Room room : currentRooms) {
                        g2.setColor(colorBox.get(room.getRoomtype()));
                        Path2D.Double path = new Path2D.Double();
        
                        List<double[]> edges = room.getEdges(); 
                        if (!edges.isEmpty()) {
                            double[] firstEdge = edges.get(0);
                            path.moveTo(firstEdge[0] * scaledDisplayGridSize, firstEdge[1] * scaledDisplayGridSize);
        
                            for (double[] edge : edges) {
                                path.lineTo(edge[2] * scaledDisplayGridSize, edge[3] * scaledDisplayGridSize);
                            }
        
                            path.closePath();
        
                            g2.fill(path);
                            if (room.getSelected()) {
                                g2.setColor(Color.BLUE);
                                int vertexSize = 8;     // Size of the vertex marker
                                for (double[] edge : edges) {
                                    double x = edge[0] * scaledDisplayGridSize;
                                    double y = edge[1] * scaledDisplayGridSize;
                                    g2.fillOval((int)(x - vertexSize / 2), (int)(y - vertexSize / 2), vertexSize, vertexSize);
                                }
                            } else {
                                g2.setColor(Color.BLACK);
                            }
                            g2.setStroke(new BasicStroke(3));
                            g2.draw(path);
                        }
                    }
                }
                FixtureManager fixtureManager = floorFixtureManagers.get(floorNumber);
                if (fixtureManager != null) {
                    fixtureManager.drawFixtures(g2);
                }
                
            }
        };
        
        double currentZoomFactor = zoomFactors.getOrDefault(floorNumber, 1.0);
        gridCanvas.setPreferredSize(new Dimension((int) (displayGridSize * 100 * currentZoomFactor), (int) (displayGridSize * 100 * currentZoomFactor)));
    
        // Adding Mouse Motion Listener to update coordinates
        gridCanvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                double mouseX = getMouseX(floorNumber, e.getX());
                double mouseY = getMouseY(floorNumber, e.getY());
                String unitToDisplay = unitSymbols.get(units.indexOf(cellUnit));
                mousePositionLabel.setText(String.format("Mouse Position: (%.2f %s, %.2f %s)", mouseX, unitToDisplay, mouseY, unitToDisplay));
            }
        });
        
        
        return gridCanvas;
    }

    @Override
    public void onRoomCreated(Room room) {
        // Add the room to floorRooms
        addRoomToFloor(room, currentFloor);
        
        double[] rCenter = room.getCenter(); 
        System.out.println(rCenter[0]+" "+rCenter[1]);
        centerCanvas(currentFloor, rCenter[0]-50, 50-rCenter[1]);
    }
    

    public static void main(String[] args) {


        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                FloorPlanMaker FPM = new FloorPlanMaker();
                FPM.setVisible(true);
            }
        });
    }
}

class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = new Color(70, 130, 180); // Scrollbar thumb color
    }
}