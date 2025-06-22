package src;

import java.awt.Point;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class SpiralPositionGenerator implements Iterator<Point> {
    private int x = 0;
    private int y = 0;
    private int dx = 0;
    private int dy = -1;
    private int maxIters;
    private int iter = 0;

    public SpiralPositionGenerator(int maxIters) {
        this.maxIters = maxIters;
    }

    @Override
    public boolean hasNext() {
        return iter < maxIters;
    }

    @Override
    public Point next() {
        if (iter >= maxIters) {
            throw new NoSuchElementException();
        }

        Point point = new Point(x, y);

        if (x == y || (x < 0 && x == -y) || (x > 0 && x == 1 - y)) {
            // Change direction
            int temp = dx;
            dx = -dy;
            dy = temp;
        }

        x += dx;
        y += dy;
        iter++;

        return point;
    }
}
