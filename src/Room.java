package src;

import java.awt.geom.Path2D;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.Serializable;

public class Room implements Serializable {
    private List<double[]> edges;
    private boolean isSelected;
    private double area;
    private double perimeter; 
    private List<Integer> colorList = List.of(0,1,2,3);
    private int roomType ;
    // 0 - common room 
    // 1 - bedroom
    // 2 - kitchen 
    // 3 - bathroom 
    

    public Room(List<double[]> edgesList) {
        isSelected = false;
        edges = new ArrayList<>();
        for (double[] edge : edgesList) {
            edges.add(new double[]{edge[0] + 50, 50 - edge[1], edge[2] + 50, 50 - edge[3]});
        }
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public boolean getSelected() {
        return this.isSelected;
    }

    public int getRoomtype(){
        return this.roomType;
    }
    public void setRoomtype(int roomIndex){
        this.roomType = roomIndex;
    }

    public void addEdge(double x1, double y1, double x2, double y2) {
        edges.add(new double[]{x1, y1, x2, y2});
    }

    public void setEdges(List<double[]> edgesList) {
        edges.clear();
        for (double[] edge : edgesList) {
            edges.add(new double[]{edge[0], edge[1], edge[2], edge[3]});
        }
    }

    public List<double[]> getEdges() {
        return edges;
    }

    public boolean checkInterior(double x, double y) {
        Path2D.Double path = new Path2D.Double();

        if (edges.isEmpty()) {
            return false;
        }

        double[] firstEdge = edges.get(0);
        path.moveTo(firstEdge[0], firstEdge[1]);

        for (double[] edge : edges) {
            path.lineTo(edge[2], edge[3]);
        }

        path.closePath();

        return path.contains(x, y);
    }

    public void moveRoom(double dx, double dy) {
        for (int i = 0; i < edges.size(); i++) {
            double[] edge = edges.get(i);
            edges.set(i, new double[]{
                edge[0] + dx, edge[1] + dy,
                edge[2] + dx, edge[3] + dy
            });
        }
    }

    public void rotateRoom(double angle) {
        double[] center = getCenter();
        double cx = center[0];
        double cy = center[1];
        double cosTheta = Math.cos(angle);
        double sinTheta = Math.sin(angle);

        for (int i = 0; i < edges.size(); i++) {
            double[] edge = edges.get(i);

            double[] rotatedP1 = rotatePoint(edge[0], edge[1], cx, cy, cosTheta, sinTheta);
            double[] rotatedP2 = rotatePoint(edge[2], edge[3], cx, cy, cosTheta, sinTheta);

            edges.set(i, new double[]{
                rotatedP1[0], rotatedP1[1],
                rotatedP2[0], rotatedP2[1]
            });
        }
    }

    private double[] rotatePoint(double x, double y, double cx, double cy, double cosTheta, double sinTheta) {
        double dx = x - cx;
        double dy = y - cy;
        double xRot = dx * cosTheta - dy * sinTheta + cx;
        double yRot = dx * sinTheta + dy * cosTheta + cy;
        return new double[]{xRot, yRot};
    }

    public void scaleRoom(double scaleFactor) {
        double[] center = getCenter();
        double cx = center[0];
        double cy = center[1];

        for (int i = 0; i < edges.size(); i++) {
            double[] edge = edges.get(i);

            double[] scaledP1 = scalePoint(edge[0], edge[1], cx, cy, scaleFactor);
            double[] scaledP2 = scalePoint(edge[2], edge[3], cx, cy, scaleFactor);

            edges.set(i, new double[]{
                scaledP1[0], scaledP1[1],
                scaledP2[0], scaledP2[1]
            });
        }
    }

    private double[] scalePoint(double x, double y, double cx, double cy, double scaleFactor) {
        double dx = x - cx;
        double dy = y - cy;
        double xScaled = dx * scaleFactor + cx;
        double yScaled = dy * scaleFactor + cy;
        return new double[]{xScaled, yScaled};
    }

    public double[] getCenter() {
        List<double[]> vertices = getVertices();
        double sumX = 0;
        double sumY = 0;
        for (double[] vertex : vertices) {
            sumX += vertex[0];
            sumY += vertex[1];
        }
        return new double[]{sumX / vertices.size(), sumY / vertices.size()};
    }

    public List<double[]> getVertices() {
        Set<String> uniquePoints = new HashSet<>();
        List<double[]> vertices = new ArrayList<>();
        for (double[] edge : edges) {
            double[] p1 = new double[]{edge[0], edge[1]};
            double[] p2 = new double[]{edge[2], edge[3]};

            String key1 = p1[0] + "," + p1[1];
            String key2 = p2[0] + "," + p2[1];

            if (uniquePoints.add(key1)) {
                vertices.add(p1);
            }
            if (uniquePoints.add(key2)) {
                vertices.add(p2);
            }
        }
        return vertices;
    }

    public Area getArea() {
        Path2D.Double path = new Path2D.Double();

        if (edges.isEmpty()) {
            return new Area();
        }

        double[] firstEdge = edges.get(0);
        path.moveTo(firstEdge[0], firstEdge[1]);

        for (double[] edge : edges) {
            path.lineTo(edge[2], edge[3]);
        }

        path.closePath();

        return new Area(path);
    }

    public double calculatePerimeter() {
        double perimeter = 0;
        for (double[] edge : edges) {
            double x1 = edge[0];
            double y1 = edge[1];
            double x2 = edge[2];
            double y2 = edge[3];
            perimeter += Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        }
        return perimeter;
    }

    public double calculateArea() {
        List<double[]> vertices = getVertices();
        if (vertices.size() < 3) {
            // A polygon must have at least 3 vertices
            return 0;
        }

        double area = 0;
        for (int i = 0; i < vertices.size(); i++) {
            double[] current = vertices.get(i);
            double[] next = vertices.get((i + 1) % vertices.size()); // Wrap around to the first vertex
            area += current[0] * next[1] - current[1] * next[0];
        }
        return Math.abs(area) / 2.0;
    }
}