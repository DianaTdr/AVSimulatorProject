package aim4.map.cpm;

import aim4.map.DataCollectionLine;
import aim4.map.Road;
import aim4.map.cpm.parking.ParkingArea;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A car park map with a parking area.
 */
public class CPMMapCarPark extends CPMBasicMap {

    private int numberOfParkingLanes;
    private double parkingLength;
    private double accessLength;
    private ParkingArea parkingArea;

    public CPMMapCarPark(int laneWidth, double speedLimit, double initTime,
                         int numberOfParkingLanes, double parkingLength,
                         double accessLength) {
        super(laneWidth, speedLimit, initTime);
        this.numberOfParkingLanes = numberOfParkingLanes;
        this.parkingLength = parkingLength;
        this.accessLength = accessLength;

        // Calculate the map dimensions
        double mapWidth = (BORDER*2) // The border used to pad the map
                + (laneWidth*2) // The 2 vertical roads either side of the parking area
                + (2*accessLength) // The length of the parking lane used for access (either side)
                + parkingLength; // The length of the parking lanes used for parking
        double mapHeight = (BORDER*2) // The border used to pad the map
                + laneWidth // The horizontal road running across the top of the parking area
                + (laneWidth*numberOfParkingLanes); // The number of horizontal parking lanes
        this.dimensions = new Rectangle2D.Double(0, 0, mapWidth, mapHeight);

        // Calculate the start point for the parking area
        double x = BORDER;
        double y = dimensions.getMaxY() - BORDER - laneWidth;
        Point2D startPoint = new Point2D.Double(x, y);

        // Create the parking area
        this.parkingArea = new ParkingArea(startPoint, this, numberOfParkingLanes,
                parkingLength, laneWidth, accessLength);

        // TODO Add all roads/lanes from parking area to the map's records?

        // Create the vertical Roads

        //SOUTH - ENTERS PARKING
        double x1 = BORDER + halfLaneWidth;
        double y1 = mapHeight - BORDER;
        double x2 = x1;
        double y2 = BORDER;
        Road southBoundRoad = createRoadWithOneLane("Southbound Road", x1, y1, x2, y2);
        verticalRoads.add(southBoundRoad);

        //NORTH - LEAVES PARKING
        x1 = mapWidth - BORDER - halfLaneWidth;
        y1 = BORDER;
        x2 = x1;
        y2 = mapHeight - BORDER;
        Road northBoundRoad = createRoadWithOneLane("Northbound Road", x1, y1, x2, y2);
        verticalRoads.add(northBoundRoad);

        // Create the horizontal Roads
        // WEST - EXITS CAR PARK
        x1 = mapWidth - BORDER;
        y1 = mapHeight - BORDER - halfLaneWidth;
        x2 = 0;
        y2 = y1;
        Road westBoundRoad = createRoadWithOneLane("Westbound Avenue", x1, y1, x2, y2);
        horizontalRoads.add(westBoundRoad);

        // Establish lanes that enter and exit the map
        // TODO CPM This is awful, come back and change
        entranceLane = parkingArea.getRoads().get(0).getLanes().get(0);
        exitLanes.add(westBoundRoad.getLanes().get(0));

        // Connect roads surrounding the parking area
        makeCorner(northBoundRoad, westBoundRoad);
        makeJunction(westBoundRoad, southBoundRoad);

        // Connect roads in the parking area with the roads surrounding it
        List<Road> roadsInParkingArea = parkingArea.getRoads();
        Road entryRoad = parkingArea.getEntryRoad();
        if (roadsInParkingArea.size() == 1) {
            makeJunction(entryRoad, southBoundRoad);
            makeCorner(entryRoad, northBoundRoad);
        } else {
            // Deal with entry road
            makeSimpleIntersection(entryRoad, southBoundRoad);
            makeJunction(entryRoad, northBoundRoad);

            // Deal with exit road
            Road lastRoad = parkingArea.getLastRoad();
            makeCorner(lastRoad, southBoundRoad);
            makeCorner(lastRoad, northBoundRoad);

            // Deal with the roads inbetween
            roadsInParkingArea.remove(parkingArea.getEntryRoad());
            roadsInParkingArea.remove(parkingArea.getLastRoad());
            for (Road road : roadsInParkingArea){
                makeJunction(road, southBoundRoad);
                makeJunction(road, northBoundRoad);
            }
        }

        // Set size of array for the data collection lines.
        // One on entry and one on exit
        dataCollectionLines = new ArrayList<DataCollectionLine>(2);
        // Create data collection lines
        x1 = entranceLane.getStartPoint().getX() + BORDER;
        y1 = entranceLane.getStartPoint().getY() + halfLaneWidth;
        x2 = x1;
        y2 = y1 - laneWidth;
        dataCollectionLines.add(
                new DataCollectionLine(
                        "Car Park Entrance",
                        dataCollectionLines.size(),
                        new Point2D.Double(x1, y1),
                        new Point2D.Double(x2, y2),
                        true));

        x1 = exitLanes.get(0).getEndPoint().getX() + BORDER;
        y1 = exitLanes.get(0).getEndPoint().getY() + halfLaneWidth;
        x2 = x1;
        y2 = y1 - laneWidth;
        dataCollectionLines.add(
                new DataCollectionLine(
                        "Car Park Exit",
                        dataCollectionLines.size(),
                        new Point2D.Double(x1, y1),
                        new Point2D.Double(x2, y2),
                        true));

        // Initialise the spawn point
        initializeSpawnPoints(initTime);
    }
}