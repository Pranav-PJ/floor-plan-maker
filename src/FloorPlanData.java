package src;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FloorPlanData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<Integer, List<Room>> floorRooms;
    private Map<Integer, ArrayList<FixtureManager.Fixture>> floorFixtures;
    private int cellLength;
    private String cellUnit;

    // Constructor
    public FloorPlanData(Map<Integer, List<Room>> floorRooms,
                        Map<Integer, ArrayList<FixtureManager.Fixture>> floorFixtures,
                        int cellLength, String cellUnit) {
        this.floorRooms = floorRooms;
        this.floorFixtures = floorFixtures;
        this.cellLength = cellLength;
        this.cellUnit = cellUnit;
    }

    // Getters
    public Map<Integer, List<Room>> getFloorRooms() {
        return floorRooms;
    }

    public Map<Integer, ArrayList<FixtureManager.Fixture>> getFloorFixtures() {
        return floorFixtures;
    }

    public int getCellLength() {
        return cellLength;
    }

    public String getCellUnit() {
        return cellUnit;
    }

    // Setters (optional, if needed)
}
