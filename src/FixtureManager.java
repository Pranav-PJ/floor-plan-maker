package src;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class FixtureManager implements Serializable {
    private static final long serialVersionUID = 1L;

    // Define minimum dimensions for fixtures
    public static final int MIN_WIDTH = 10;
    public static final int MIN_HEIGHT = 10;  

    // List to hold all fixtures
    public ArrayList<Fixture> fixtures;
    private JPanel floorPlanPanel;
    public Fixture selectedFixture;
    private Point lastMousePosition;
    private boolean resizing;
    private boolean rotating;
    private boolean listenersAdded = false; // To prevent adding listeners multiple times

    // Variables for rotation
    private double initialAngle;
    private double initialRotation;

    private int initialX;
    private int initialY;

    public void setFixtures(ArrayList<Fixture> fixtures) {
        this.fixtures = fixtures;
    }

    public boolean isDoor() {
        return selectedFixture instanceof Door;
    }

    public boolean isWindow() {
        return selectedFixture instanceof Window;
    }

    public FixtureManager(ArrayList<Fixture> fixtures, JPanel floorPlanPanel) {
        this.fixtures = fixtures;
        this.floorPlanPanel = floorPlanPanel;
        addMouseListeners();
        addKeyListeners();
        initialAngle = 0.0;
        initialRotation = 0.0;
    }

    // Base Fixture class
    public class Fixture implements Serializable {

        protected int x, y, width, height;
        protected ImageIcon icon;
        protected boolean selected;
        protected double rotation; // in degrees

        public Fixture(ImageIcon icon, int x, int y, int width, int height) {
            this.icon = icon;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.selected = false;
            this.rotation = 0.0;
        }

        public List<double[]> getBoxNewCoordinates() {
            double nx = x;
            double ny = y;
            double nw = width;
            double nh = height;
            nx = nx/60-50;
            ny = 50-ny/60;
            nw = (nx+nw)/60;
            nh = (ny+nh)/60;
            List<double[]> edgelist = new ArrayList<>();
            edgelist.add(new double[]{nx, ny, nx+nw, ny});
            edgelist.add(new double[]{nx+nw, ny, nx+nw, ny-nh});
            edgelist.add(new double[]{nx+nw, ny-nh, nx, ny-nh});
            edgelist.add(new double[]{nx, ny-nh, nx, ny});
            return edgelist;
        }

        public Shape getTransformedShape() {
            AffineTransform at = new AffineTransform();
            at.translate(x + width / 2, y + height / 2);
            at.rotate(Math.toRadians(rotation));
            at.translate(-width / 2, -height / 2);
            return at.createTransformedShape(new Rectangle(0, 0, width, height));
        }

        public Shape getModifiedTransformedShape() {
            AffineTransform at = new AffineTransform();
            at.translate(x + width / 2, y + height / 2);
            at.rotate(Math.toRadians(rotation));
            at.translate(-width / 2, -height / 2);
            return at.createTransformedShape(new Rectangle(0, 0, width/20, height/20));
        }

        public void draw(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            AffineTransform originalTransform = g2d.getTransform();

            // Translate to the center of the fixture
            int centerX = x + width / 2;
            int centerY = y + height / 2;
            System.out.println("fixture center: "+centerX+" "+centerY);
            g2d.translate(centerX, centerY);
            g2d.rotate(Math.toRadians(rotation));

            // Draw the image centered at (0,0)
            if (icon != null && icon.getImage() != null) {
                g2d.drawImage(icon.getImage(), -width / 2, -height / 2, width, height, null);
            } else {
                // Optional: Draw a placeholder rectangle if the image is missing
                g2d.setColor(Color.GRAY);
                g2d.fillRect(-width / 2, -height / 2, width, height);
            }

            if (selected) {
                // Draw selection rectangle
                g2d.setColor(Color.RED);
                g2d.drawRect(-width / 2, -height / 2, width, height);
                // Draw resize handle at bottom-right corner
                g2d.setColor(Color.BLUE);
                g2d.fillRect(width / 2 - 10, height / 2 - 10, 10, 10);
                // Draw rotate handle above the fixture
                g2d.setColor(Color.GREEN);
                g2d.fillOval(-5, -height / 2 - 20, 10, 10);
            }

            // Restore the transform
            g2d.setTransform(originalTransform);
        }

        public boolean contains(Point p) {
            Point2D.Double localPoint = screenToLocal(p);
            if (localPoint != null) {
                return (localPoint.x >= 0 && localPoint.x <= width && localPoint.y >= 0 && localPoint.y <= height);
            }
            return false;
        }

        public boolean isInResizeHandle(Point p) {
            Point2D.Double localPoint = screenToLocal(p);
            if (localPoint != null) {
                return (localPoint.x >= width - 10 && localPoint.x <= width && localPoint.y >= height - 10 && localPoint.y <= height);
            }
            return false;
        }

        public boolean isInRotateHandle(Point p) {
            Point2D.Double localPoint = screenToLocal(p);
            if (localPoint != null) {
                return (localPoint.x >= width / 2 - 5 && localPoint.x <= width / 2 + 5 && localPoint.y >= -20 && localPoint.y <= -10);
            }
            return false;
        }

        // Transform screen point to fixture's local coordinate system
        public Point2D.Double screenToLocal(Point p) {
            AffineTransform at = new AffineTransform();
            at.translate(x + width / 2, y + height / 2);
            at.rotate(Math.toRadians(rotation));
            at.translate(-width / 2, -height / 2);
            try {
                AffineTransform inverse = at.createInverse();
                Point2D.Double localPoint = new Point2D.Double();
                inverse.transform(p, localPoint);
                return localPoint;
            } catch (NoninvertibleTransformException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    // Fixture subclasses
    public class Door extends Fixture {

        public Door(int x, int y) {
            super(new ImageIcon("fixtures_icons\\door.png"), x, y, 50, 100);
        }
    }

    public class Window extends Fixture {

        public Window(int x, int y) {
            super(new ImageIcon("fixtures_icons\\window.png"), x, y, 80, 60);
        }
    }

    public class Sofa extends Fixture {

        public Sofa(int x, int y) {
            super(new ImageIcon("fixtures_icons\\sofa.png"), x, y, 120, 60);
        }
    }

    public class Chair extends Fixture {

        public Chair(int x, int y) {
            super(new ImageIcon("fixtures_icons\\chair.png"), x, y, 50, 50);
        }
    }

    public class BedDouble extends Fixture {

        public BedDouble(int x, int y) {
            super(new ImageIcon("fixtures_icons\\bed_double.png"), x, y, 160, 200);
        }
    }

    public class BedSingle extends Fixture {

        public BedSingle(int x, int y) {
            super(new ImageIcon("fixtures_icons\\bed_single.png"), x, y, 100, 200);
        }
    }

    public class Sink extends Fixture {

        public Sink(int x, int y) {
            super(new ImageIcon("fixtures_icons\\sink.png"), x, y, 60, 60);
        }
    }

    public class Cupboard extends Fixture {

        public Cupboard(int x, int y) {
            super(new ImageIcon("fixtures_icons\\cupboard.png"), x, y, 100, 200);
        }
    }

    public class Table extends Fixture {

        public Table(int x, int y) {
            super(new ImageIcon("fixtures_icons\\table.png"), x, y, 120, 80);
        }
    }

    public class Toilet extends Fixture {

        public Toilet(int x, int y) {
            super(new ImageIcon("fixtures_icons\\toilet.png"), x, y, 60, 80);
        }
    }

    public class Stove extends Fixture {

        public Stove(int x, int y) {
            super(new ImageIcon("fixtures_icons\\stove.png"), x, y, 60, 80);
        }
    }

    public class Shower extends Fixture {

        public Shower(int x, int y) {
            super(new ImageIcon("fixtures_icons\\shower.png"), x, y, 60, 80);
        }
    }

    public class Diningset extends Fixture {

        public Diningset(int x, int y) {
            super(new ImageIcon("fixtures_icons\\diningset.png"), x, y, 60, 80);
        }
    }

    public class Kitchensink extends Fixture {

        public Kitchensink(int x, int y) {
            super(new ImageIcon("fixtures_icons\\kitchensink.png"), x, y, 60, 80);
        }
    }

    public class Staircase extends Fixture {

        public Staircase(int x, int y) {
            super(new ImageIcon("fixtures_icons\\staircase.png"), x, y, 60, 80);
        }
    }


    // Action listeners for buttons
    public ActionListener getAddDoorListener() {
        return e -> {
            Door door = new Door(0, 0); // Create a new door fixture
            placeFixtureUsingSpiral(door); // Try to place it at the center
        };
    }

    public ActionListener getAddWindowListener() {
        return e -> {
            Window window = new Window(0, 0); // Default position
            placeFixtureUsingSpiral(window);
        };
    }

    public ActionListener getAddSofaListener() {
        return e -> {
            Sofa sofa = new Sofa(0, 0); // Default position
            placeFixtureUsingSpiral(sofa);
        };
    }

    public ActionListener getAddChairListener() {
        return e -> {
            Chair chair = new Chair(0, 0); // Default position
            placeFixtureUsingSpiral(chair);
        };
    }

    public ActionListener getAddBedDoubleListener() {
        return e -> {
            BedDouble bedDouble = new BedDouble(0, 0); // Default position
            placeFixtureUsingSpiral(bedDouble);
        };
    }

    public ActionListener getAddBedSingleListener() {
        return e -> {
            BedSingle bedSingle = new BedSingle(0, 0); // Default position
            placeFixtureUsingSpiral(bedSingle);
        };
    }

    public ActionListener getAddBathroomsinkListener() {
        return e -> {
            Sink sink = new Sink(0, 0); // Default position
            placeFixtureUsingSpiral(sink);
        };
    }

    public ActionListener getAddCupboardListener() {
        return e -> {
            Cupboard cupboard = new Cupboard(0, 0); // Default position
            placeFixtureUsingSpiral(cupboard);
        };
    }

    public ActionListener getAddTableListener() {
        return e -> {
            Table table = new Table(0, 0); // Default position
            placeFixtureUsingSpiral(table);
        };
    }

    public ActionListener getAddToiletListener() {
        return e -> {
            Toilet toilet = new Toilet(0, 0); // Default position
            placeFixtureUsingSpiral(toilet);
        };
    }

    public ActionListener getAddStoveListener() {
        return e -> {
            Stove stove = new Stove(0, 0); // Default position
            placeFixtureUsingSpiral(stove);
        };
    }

    public ActionListener getAddShowerListener() {
        return e -> {
            Shower shower = new Shower(0, 0); // Default position
            placeFixtureUsingSpiral(shower);
        };
    }

    public ActionListener getAddDiningsetListener() {
        return e -> {
            Diningset diningset = new Diningset(0, 0); // Default position
            placeFixtureUsingSpiral(diningset);
        };
    }

    public ActionListener getAddKitchensinkListener() {
        return e -> {
            Kitchensink kitchensink = new Kitchensink(0, 0); // Default position
            placeFixtureUsingSpiral(kitchensink);
        };
    }

    public ActionListener getAddStaircaseListener() {
        return e -> {
            Staircase staircase = new Staircase(0, 0); // Default position
            placeFixtureUsingSpiral(staircase);
        };
    }

    private boolean isOverlapping(Fixture fixture) {
        Shape currentShape = fixture.getTransformedShape();
        for (Fixture other : fixtures) {
            if (other != fixture) {
                Shape otherShape = other.getTransformedShape();
                Area area1 = new Area(currentShape);
                area1.intersect(new Area(otherShape));
                if (!area1.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void storeInitialPosition(Fixture fixture) {
        this.initialX = fixture.x;
        this.initialY = fixture.y;
    }

    public void revertToInitialPosition(Fixture fixture) {
        fixture.x = this.initialX;
        fixture.y = this.initialY;
        floorPlanPanel.repaint(); // Refresh the panel to reflect changes
    }


    public boolean roomOverlapDoor(Room room) {
        if (isDoor()) {
            // Shape currentShape = selectedFixture.getModifiedTransformedShape();
            List<double[]> boxedge = selectedFixture.getBoxNewCoordinates();
            System.out.print("boxcord ");
            for (double[] d : boxedge) {
                System.out.print(d[0]+" "+d[1]+"\n");
            }
            System.out.println();
            Room overlapRoom = new Room(boxedge);
            Area area = overlapRoom.getArea();
            area.intersect(room.getArea());
            if (!area.isEmpty()) {
                return true;
            } else {
                System.out.println("selected is true");
                return false;
            }
            } else {
                return false;
        }
    }
    
    public boolean roomOverlapWindow(Room room) {
        if (isWindow()) {
            // Shape currentShape = selectedFixture.getModifiedTransformedShape();
            List<double[]> boxedge = selectedFixture.getBoxNewCoordinates();
            System.out.print("boxcord ");
            for (double[] d : boxedge) {
                System.out.print(d[0]+" "+d[1]+"\n");
            }
            System.out.println();
            Room overlapRoom = new Room(boxedge);
            Area area = overlapRoom.getArea();
            area.intersect(room.getArea());
            if (!area.isEmpty()) {
                return true;
            } else {
                System.out.println("selected is true");
                return false;
            }
            } else {
                return false;
        }
    }


    public void placeFixtureUsingSpiral(Fixture newFixture) {
        int maxAttempts = 1000; // Maximum positions to try
        double stepSize = 20.0; // Adjust step size for positioning
        SpiralPositionGenerator positionGenerator = new SpiralPositionGenerator(maxAttempts);
    
        boolean placed = false;
        while (positionGenerator.hasNext()) {
            Point pos = positionGenerator.next();
    
            // Calculate new position for the fixture
            int newX = (int) (floorPlanPanel.getWidth() / 2 + pos.x * stepSize);
            int newY = (int) (floorPlanPanel.getHeight() / 2 + pos.y * stepSize);
    
            System.out.println("Trying position: " + newX + ", " + newY);
    
            // Temporarily set the new position
            int originalX = newFixture.x;
            int originalY = newFixture.y;
            newFixture.x = newX;
            newFixture.y = newY;
    
            // Check for overlap
            if (!isOverlapping(newFixture)) {
                System.out.println("Placed at: " + newX + ", " + newY);
                fixtures.add(newFixture);
                floorPlanPanel.repaint();
                placed = true;
                break;
            }
    
            // Revert to original position if overlapping
            newFixture.x = originalX;
            newFixture.y = originalY;
        }
    
        if (!placed) {
            System.out.println("Unable to place the fixture without overlap.");
            JOptionPane.showMessageDialog(floorPlanPanel, "Unable to place the fixture without overlap.", "Placement Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    

    public boolean isDraggingFixture() {
        return (selectedFixture != null) && (lastMousePosition != null);
    }

    public void handleMouseClick(Point p) {
        selectedFixture = null;
        resizing = false;
        rotating = false;

        // Deselect all fixtures
        for (Fixture fixture : fixtures) {
            fixture.selected = false;
        }

        // Iterate in reverse order to select topmost fixture
        for (int i = fixtures.size() - 1; i >= 0; i--) {
            Fixture fixture = fixtures.get(i);
            if (fixture.isInResizeHandle(p)) {
                selectedFixture = fixture;
                resizing = true;
                fixture.selected = true;
                break;
            } else if (fixture.isInRotateHandle(p)) {
                selectedFixture = fixture;
                rotating = true;
                fixture.selected = true;
                initialAngle = Math.atan2(p.y - (fixture.y + fixture.height / 2), p.x - (fixture.x + fixture.width / 2));
                initialRotation = fixture.rotation;
                break;
            } else if (fixture.contains(p)) {
                selectedFixture = fixture;
                fixture.selected = true;
                break;
            }
        }
        floorPlanPanel.repaint();
    }

    public void moveOrResizeSelectedFixture(int deltaX, int deltaY) {
        if (selectedFixture != null) {
            int originalX = selectedFixture.x;
            int originalY = selectedFixture.y;
            int originalWidth = selectedFixture.width;
            int originalHeight = selectedFixture.height;

            if (resizing) {
                // Resize logic
                int newWidth = selectedFixture.width + deltaX;
                int newHeight = selectedFixture.height + deltaY;
                newWidth = Math.max(MIN_WIDTH, newWidth);
                newHeight = Math.max(MIN_HEIGHT, newHeight);
                selectedFixture.width = newWidth;
                selectedFixture.height = newHeight;
            } else {
                // Move logic
                selectedFixture.x += deltaX;
                selectedFixture.y += deltaY;
            }

            // Check for overlap
            if (isOverlapping(selectedFixture)) {
                // Revert to original position or size
                selectedFixture.x = originalX;
                selectedFixture.y = originalY;
                selectedFixture.width = originalWidth;
                selectedFixture.height = originalHeight;

                // Notify the user
                // JOptionPane.showMessageDialog(floorPlanPanel, "This fixture is overlapping with another fixture!", "Overlap Detected", JOptionPane.ERROR_MESSAGE);
            }

            floorPlanPanel.repaint();
        }
    }

    // Method to delete selected fixture
    public void deleteSelectedFixture() {
        if (selectedFixture != null) {
            fixtures.remove(selectedFixture);
            selectedFixture = null;
            floorPlanPanel.repaint();
        }
    }

    // Add mouse listeners to handle dragging, resizing, rotating, and selection
    private void addMouseListeners() {
        if (listenersAdded) {
            return; // Prevent adding listeners multiple times
        }

        floorPlanPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                boolean focused = floorPlanPanel.requestFocusInWindow();
                if (!focused) {
                    floorPlanPanel.requestFocus();
                }
                lastMousePosition = e.getPoint();
                handleMouseClick(e.getPoint());

                if (rotating && selectedFixture != null) {
                    // Compute initial angle between fixture center and mouse position
                    Point2D center = new Point2D.Double(selectedFixture.x + selectedFixture.width / 2, selectedFixture.y + selectedFixture.height / 2);
                    initialAngle = Math.atan2(e.getY() - center.getY(), e.getX() - center.getX());
                    initialRotation = selectedFixture.rotation;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastMousePosition = null;
                resizing = false;
                rotating = false;
            }
        });

        floorPlanPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (rotating && selectedFixture != null) {
                    Point2D center = new Point2D.Double(selectedFixture.x + selectedFixture.width / 2, selectedFixture.y + selectedFixture.height / 2);
                    double currentAngle = Math.atan2(e.getY() - center.getY(), e.getX() - center.getX());
                    double angleDelta = Math.toDegrees(currentAngle - initialAngle);

                    double originalRotation = selectedFixture.rotation;
                    selectedFixture.rotation = initialRotation + angleDelta;

                    // Check for overlap
                    if (isOverlapping(selectedFixture)) {
                        selectedFixture.rotation = originalRotation; // Revert rotation
                        JOptionPane.showMessageDialog(floorPlanPanel, "This fixture is overlapping with another fixture!", "Overlap Detected", JOptionPane.ERROR_MESSAGE);
                    }

                    floorPlanPanel.repaint();
                } else if (selectedFixture != null && lastMousePosition != null) {
                    int deltaX = e.getX() - lastMousePosition.x;
                    int deltaY = e.getY() - lastMousePosition.y;

                    moveOrResizeSelectedFixture(deltaX, deltaY);

                    lastMousePosition = e.getPoint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                boolean overRotateHandle = false;
                boolean overResizeHandle = false;
                for (int i = fixtures.size() - 1; i >= 0; i--) {
                    Fixture fixture = fixtures.get(i);
                    if (fixture.isInRotateHandle(e.getPoint())) {
                        overRotateHandle = true;
                        break;
                    } else if (fixture.isInResizeHandle(e.getPoint())) {
                        overResizeHandle = true;
                        break;
                    } else if (fixture.contains(e.getPoint())) {
                        break;
                    }
                }
                if (overRotateHandle) {
                    floorPlanPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else if (overResizeHandle) {
                    floorPlanPanel.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                } else {
                    floorPlanPanel.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        listenersAdded = true; // Mark that listeners have been added
    }

    // Add key listener to handle deleting selected fixture
    private void addKeyListeners() {
        floorPlanPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelectedFixture();
                }
            }
        });
        floorPlanPanel.setFocusable(true);
    }

    // Method to draw all fixtures
    public void drawFixtures(Graphics g) {
        for (Fixture fixture : fixtures) {
            fixture.draw(g);

        }
    }
}
