package aim4.rim.im.reservation;

import aim4.config.Debug;
import aim4.config.SimConfig;
import aim4.im.rim.RoadBasedIntersection;
import aim4.im.rim.v2i.reservation.ReservationGrid;
import aim4.im.rim.v2i.reservation.ReservationGridManager;
import aim4.map.Road;
import aim4.map.lane.ArcSegmentLane;
import aim4.map.lane.Lane;
import aim4.map.rim.RimIntersectionMap;
import aim4.msg.rim.v2i.Request;
import aim4.msg.rim.v2i.Request.VehicleSpecForRequestMsg;
import aim4.util.TiledRimArea;
import aim4.vehicle.VehicleSpecDatabase;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static aim4.im.rim.v2i.reservation.ReservationGridManager.*;

public class ReservationGridManagerTests {
    private static final double ARRIVAL_TIME = 4.0;
    private static final double STATIC_BUFFER_SIZE = 0.25;
    private static final double INTERNAL_TILE_TIME_BUFFER_SIZE = 0.1;
    private static final double GRANULARITY = 6.0;
    private static final List<Double> ROUNDABOUT_DIAMETER = Arrays.asList(30.0, 35.0, 40.0, 45.0);
    private static final double ENTRANCE_EXIT_RADIUS =  20.0;
    private static final double LANE_WIDTH =  3.014;
    private static final double LANE_SPEED_LIMIT =  19.44;
    private static final double ROUNDABOUT_SPEED_LIMIT =  6.04;
    private static final int VIN =  1;

    @Test
    public void query_withConstantVelocityLeft_returnsPlanWithTraversalProposal() {
        //arrange
        // Create map
        RimIntersectionMap map = getRimIntersectionMap();

        // Create config
        Config config = getConfig();

        // Create intersection
        RoadBasedIntersection roadBasedIntersection = new RoadBasedIntersection(map.getRoads());

        // Create tiles for intersection
        TiledRimArea tiledRimArea = new TiledRimArea(
                roadBasedIntersection.getMinimalCircle(),
                roadBasedIntersection.getMaximalCircle(),
                GRANULARITY);

        // Assign the tiles times
        ReservationGrid reservationGrid = new ReservationGrid(GRANULARITY, SimConfig.GRID_TIME_STEP);

        // Set vehicle spec
        Request.VehicleSpecForRequestMsg vehicleSpecForRequestMsg = new Request.VehicleSpecForRequestMsg(VehicleSpecDatabase.getVehicleSpecByName("COUPE"));

        // Set arrival and departure lanes
        Lane arrivalLane = getNorthRoad().getEntryApproachLane();
        Lane departureLane = getWestRoad().getExitApproachLane();

        // Set vehicle's driver query
        Query query = getQuery(vehicleSpecForRequestMsg, arrivalLane, departureLane, false, ROUNDABOUT_SPEED_LIMIT);

        // Set the manager to take the query
        ReservationGridManager reservationGridManager = new ReservationGridManager(
                config,
                roadBasedIntersection,
                tiledRimArea,
                reservationGrid
        );

        double idealTime = ARRIVAL_TIME + (
                                ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(2)).getLengthArcLaneDecomposition() +
                                ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(3)).getLengthArcLaneDecomposition() +
                                ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(4)).getLengthArcLaneDecomposition() +
                                ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(5)).getLengthArcLaneDecomposition() +
                                ((ArcSegmentLane)getWestRoad().getContinuousLanes().get(4)).getLengthArcLaneDecomposition() +
                                ((ArcSegmentLane)getWestRoad().getContinuousLanes().get(5)).getLengthArcLaneDecomposition() +
                                ((ArcSegmentLane)getWestRoad().getContinuousLanes().get(6)).getLengthArcLaneDecomposition()
                        ) / ROUNDABOUT_SPEED_LIMIT;
        Set expectedOccupiedTiles = new LinkedHashSet<>();
        expectedOccupiedTiles.add(9);
        expectedOccupiedTiles.add(10);
        expectedOccupiedTiles.add(11);
        expectedOccupiedTiles.add(0);
        expectedOccupiedTiles.add(1);
        expectedOccupiedTiles.add(2);
        expectedOccupiedTiles.add(3);
        expectedOccupiedTiles.add(4);
        expectedOccupiedTiles.add(5);

        //act
        Plan plan = reservationGridManager.query(query);

        //assert
        assert plan.getVin() == VIN; // plan for this vehicle
        assert plan.getAccelerationProfile().size() == 1; // only one acceleration profile
        Set occupiedTiles = new LinkedHashSet<>();
        plan.getWorkingList().forEach(tile -> occupiedTiles.add(tile.getTileId())); // unique list with occupied tiles (which keeps order)
        assert occupiedTiles.containsAll(expectedOccupiedTiles);
        assert expectedOccupiedTiles.containsAll(occupiedTiles);
        assert plan.getExitTime() - idealTime < 1; // due to the buffer the actual time is slightly bigger
        assert plan.getExitTime() > idealTime; // actual exit time should always be bigger than ideal
        assert (float)plan.getExitVelocity() == (float)ROUNDABOUT_SPEED_LIMIT;

    }

    @Test
    public void query_withAccelerationAndArrivalVelocityLessThanIdealTurnsLeft_returnsPlanWithTraversalProposal() {
        //arrange
        // Create map
        RimIntersectionMap map = getRimIntersectionMap();

        // Create config
        Config config = getConfig();

        // Create intersection
        RoadBasedIntersection roadBasedIntersection = new RoadBasedIntersection(map.getRoads());

        // Create tiles for intersection
        TiledRimArea tiledRimArea = new TiledRimArea(
                roadBasedIntersection.getMinimalCircle(),
                roadBasedIntersection.getMaximalCircle(),
                GRANULARITY);

        // Assign the tiles times
        ReservationGrid reservationGrid = new ReservationGrid(GRANULARITY, SimConfig.GRID_TIME_STEP);

        // Set vehicle spec
        Request.VehicleSpecForRequestMsg vehicleSpecForRequestMsg = new Request.VehicleSpecForRequestMsg(VehicleSpecDatabase.getVehicleSpecByName("COUPE"));

        // Set arrival and departure lanes
        Lane arrivalLane = getNorthRoad().getEntryApproachLane();
        Lane departureLane = getWestRoad().getExitApproachLane();

        // Set vehicle's driver query
        Query query = getQuery(vehicleSpecForRequestMsg, arrivalLane, departureLane, true, 3.04); // velocity less than roundabout ideal

        // Set the manager to take the query
        ReservationGridManager reservationGridManager = new ReservationGridManager(
                config,
                roadBasedIntersection,
                tiledRimArea,
                reservationGrid
        );

        double idealTime = ARRIVAL_TIME + (
                ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(2)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(3)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(4)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(5)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getWestRoad().getContinuousLanes().get(4)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getWestRoad().getContinuousLanes().get(5)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getWestRoad().getContinuousLanes().get(6)).getLengthArcLaneDecomposition()
        ) / ROUNDABOUT_SPEED_LIMIT;
        Set expectedOccupiedTiles = new LinkedHashSet<>();
        expectedOccupiedTiles.add(9);
        expectedOccupiedTiles.add(10);
        expectedOccupiedTiles.add(11);
        expectedOccupiedTiles.add(0);
        expectedOccupiedTiles.add(1);
        expectedOccupiedTiles.add(2);
        expectedOccupiedTiles.add(3);
        expectedOccupiedTiles.add(4);
        expectedOccupiedTiles.add(5);

        //act
        Plan plan = reservationGridManager.query(query);

        //assert
        assert plan.getVin() == VIN; // plan for this vehicle
        assert plan.getAccelerationProfile().size() == 2; // two acceleration profile
        Set occupiedTiles = new LinkedHashSet<>();
        plan.getWorkingList().forEach(tile -> occupiedTiles.add(tile.getTileId())); // unique list with occupied tiles (which keeps order)
        assert occupiedTiles.containsAll(expectedOccupiedTiles);
        assert expectedOccupiedTiles.containsAll(occupiedTiles);
        assert plan.getExitTime() - idealTime < 1; // due to the buffer the actual time is slightly bigger
        assert plan.getExitTime() > idealTime; // actual exit time should always be bigger than ideal
        assert (float)plan.getExitVelocity() == (float)ROUNDABOUT_SPEED_LIMIT;

    }

    @Test
    public void query_withConstantVelocityRight_returnsPlanWithTraversalProposal() {
        //arrange
        // Create map
        RimIntersectionMap map = getRimIntersectionMap();

        // Create config
        Config config = getConfig();

        // Create intersection
        RoadBasedIntersection roadBasedIntersection = new RoadBasedIntersection(map.getRoads());

        // Create tiles for intersection
        TiledRimArea tiledRimArea = new TiledRimArea(
                roadBasedIntersection.getMinimalCircle(),
                roadBasedIntersection.getMaximalCircle(),
                GRANULARITY);

        // Assign the tiles times
        ReservationGrid reservationGrid = new ReservationGrid(GRANULARITY, SimConfig.GRID_TIME_STEP);

        // Set vehicle spec
        Request.VehicleSpecForRequestMsg vehicleSpecForRequestMsg = new Request.VehicleSpecForRequestMsg(VehicleSpecDatabase.getVehicleSpecByName("COUPE"));

        // Set arrival and departure lanes
        Lane arrivalLane = getNorthRoad().getEntryApproachLane();
        Lane departureLane = getEastRoad().getExitApproachLane();

        // Set vehicle's driver query
        Query query = getQuery(vehicleSpecForRequestMsg, arrivalLane, departureLane, false, ROUNDABOUT_SPEED_LIMIT);

        // Set the manager to take the query
        ReservationGridManager reservationGridManager = new ReservationGridManager(
                config,
                roadBasedIntersection,
                tiledRimArea,
                reservationGrid
        );

        double idealTime = ARRIVAL_TIME + (
                ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(2)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(3)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getEastRoad().getContinuousLanes().get(6)).getLengthArcLaneDecomposition()
        ) / ROUNDABOUT_SPEED_LIMIT;
        Set expectedOccupiedTiles = new LinkedHashSet<>();
        expectedOccupiedTiles.add(9);
        expectedOccupiedTiles.add(10);
        expectedOccupiedTiles.add(11);


        //act
        Plan plan = reservationGridManager.query(query);

        //assert
        assert plan.getVin() == VIN; // plan for this vehicle
        assert plan.getAccelerationProfile().size() == 1; // only one acceleration profile
        Set occupiedTiles = new LinkedHashSet<>();
        plan.getWorkingList().forEach(tile -> occupiedTiles.add(tile.getTileId())); // unique list with occupied tiles (which keeps order)
        assert occupiedTiles.containsAll(expectedOccupiedTiles);
        assert expectedOccupiedTiles.containsAll(occupiedTiles);
        assert plan.getExitTime() - idealTime < 1; // due to the buffer the actual time is slightly bigger
        assert plan.getExitTime() > idealTime; // actual exit time should always be bigger than ideal
        assert (float)plan.getExitVelocity() == (float)ROUNDABOUT_SPEED_LIMIT;

    }

    @Test
    public void query_withAccelerationAndArrivalVelocityMoreThanIdealTurnsLeft_returnsPlanWithTraversalProposal() {
        //arrange
        // Create map
        RimIntersectionMap map = getRimIntersectionMap();

        // Create config
        Config config = getConfig();

        // Create intersection
        RoadBasedIntersection roadBasedIntersection = new RoadBasedIntersection(map.getRoads());

        // Create tiles for intersection
        TiledRimArea tiledRimArea = new TiledRimArea(
                roadBasedIntersection.getMinimalCircle(),
                roadBasedIntersection.getMaximalCircle(),
                GRANULARITY);

        // Assign the tiles times
        ReservationGrid reservationGrid = new ReservationGrid(GRANULARITY, SimConfig.GRID_TIME_STEP);

        // Set vehicle spec
        Request.VehicleSpecForRequestMsg vehicleSpecForRequestMsg = new Request.VehicleSpecForRequestMsg(VehicleSpecDatabase.getVehicleSpecByName("COUPE"));

        // Set arrival and departure lanes
        Lane arrivalLane = getNorthRoad().getEntryApproachLane();
        Lane departureLane = getWestRoad().getExitApproachLane();

        // Set vehicle's driver query
        Query query = getQuery(vehicleSpecForRequestMsg, arrivalLane, departureLane, true, 8.04); // velocity more than roundabout ideal

        // Set the manager to take the query
        ReservationGridManager reservationGridManager = new ReservationGridManager(
                config,
                roadBasedIntersection,
                tiledRimArea,
                reservationGrid
        );

        double idealTime = ARRIVAL_TIME + (
                ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(2)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(3)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(4)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(5)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getWestRoad().getContinuousLanes().get(4)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getWestRoad().getContinuousLanes().get(5)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getWestRoad().getContinuousLanes().get(6)).getLengthArcLaneDecomposition()
        ) / ROUNDABOUT_SPEED_LIMIT;
        Set expectedOccupiedTiles = new LinkedHashSet<>();
        expectedOccupiedTiles.add(9);
        expectedOccupiedTiles.add(10);
        expectedOccupiedTiles.add(11);
        expectedOccupiedTiles.add(0);
        expectedOccupiedTiles.add(1);
        expectedOccupiedTiles.add(2);
        expectedOccupiedTiles.add(3);
        expectedOccupiedTiles.add(4);
        expectedOccupiedTiles.add(5);

        //act
        Plan plan = reservationGridManager.query(query);

        //assert
        assert plan.getVin() == VIN; // plan for this vehicle
        assert plan.getAccelerationProfile().size() == 1; // one acceleration profile (no need to accelerate)
        Set occupiedTiles = new LinkedHashSet<>();
        plan.getWorkingList().forEach(tile -> occupiedTiles.add(tile.getTileId())); // unique list with occupied tiles (which keeps order)
        assert occupiedTiles.containsAll(expectedOccupiedTiles);
        assert expectedOccupiedTiles.containsAll(occupiedTiles);
        assert plan.getExitTime() - idealTime < 1; // due to the buffer the actual time is slightly bigger
        assert plan.getExitTime() < idealTime; // actual exit time should less than ideal as vehicle had higher velocity than max turn velocity
        assert (float)plan.getExitVelocity() == (float) 8.04;

    }

    @Test
    public void query_withConstantVelocityStraight_returnsPlanWithTraversalProposal() {
        //arrange
        // Create map
        RimIntersectionMap map = getRimIntersectionMap();

        // Create config
        Config config = getConfig();

        // Create intersection
        RoadBasedIntersection roadBasedIntersection = new RoadBasedIntersection(map.getRoads());

        // Create tiles for intersection
        TiledRimArea tiledRimArea = new TiledRimArea(
                roadBasedIntersection.getMinimalCircle(),
                roadBasedIntersection.getMaximalCircle(),
                GRANULARITY);

        // Assign the tiles times
        ReservationGrid reservationGrid = new ReservationGrid(GRANULARITY, SimConfig.GRID_TIME_STEP);

        // Set vehicle spec
        Request.VehicleSpecForRequestMsg vehicleSpecForRequestMsg = new Request.VehicleSpecForRequestMsg(VehicleSpecDatabase.getVehicleSpecByName("COUPE"));

        // Set arrival and departure lanes
        Lane arrivalLane = getNorthRoad().getEntryApproachLane();
        Lane departureLane = getNorthRoad().getExitApproachLane();

        // Set vehicle's driver query
        Query query = getQuery(vehicleSpecForRequestMsg, arrivalLane, departureLane, false, ROUNDABOUT_SPEED_LIMIT);

        // Set the manager to take the query
        ReservationGridManager reservationGridManager = new ReservationGridManager(
                config,
                roadBasedIntersection,
                tiledRimArea,
                reservationGrid
        );

        double idealTime = ARRIVAL_TIME + (
                ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(2)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(3)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(4)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(5)).getLengthArcLaneDecomposition() +
                        ((ArcSegmentLane)getNorthRoad().getContinuousLanes().get(6)).getLengthArcLaneDecomposition()
        ) / ROUNDABOUT_SPEED_LIMIT;
        Set expectedOccupiedTiles = new LinkedHashSet<>();
        expectedOccupiedTiles.add(9);
        expectedOccupiedTiles.add(10);
        expectedOccupiedTiles.add(11);
        expectedOccupiedTiles.add(0);
        expectedOccupiedTiles.add(1);
        expectedOccupiedTiles.add(2);


        //act
        Plan plan = reservationGridManager.query(query);

        //assert
        assert plan.getVin() == VIN; // plan for this vehicle
        assert plan.getAccelerationProfile().size() == 1; // only one acceleration profile
        Set occupiedTiles = new LinkedHashSet<>();
        plan.getWorkingList().forEach(tile -> occupiedTiles.add(tile.getTileId())); // unique list with occupied tiles (which keeps order)
        assert occupiedTiles.containsAll(expectedOccupiedTiles);
        assert expectedOccupiedTiles.containsAll(occupiedTiles);
        assert plan.getExitTime() - idealTime < 1; // due to the buffer the actual time is slightly bigger
        assert plan.getExitTime() > idealTime; // actual exit time should always be bigger than ideal
        assert (float)plan.getExitVelocity() == (float)ROUNDABOUT_SPEED_LIMIT;

    }

    private Query getQuery(VehicleSpecForRequestMsg vehicleSpecForRequestMsg, Lane arrivalLane, Lane departureLane, boolean acceleration, double arrivalVelocity) {
        return new Query(
                    VIN,
                    ARRIVAL_TIME,
                    arrivalVelocity, // arrival velocity
                    arrivalLane.getId(),
                    departureLane.getId(),
                    vehicleSpecForRequestMsg,
                    ROUNDABOUT_SPEED_LIMIT, // max turn velocity
                    acceleration);
    }

    private Config getConfig() {
        return new Config(SimConfig.TIME_STEP,
                    SimConfig.GRID_TIME_STEP,
                    STATIC_BUFFER_SIZE,
                    INTERNAL_TILE_TIME_BUFFER_SIZE,
                    GRANULARITY);
    }

    private RimIntersectionMap getRimIntersectionMap() {
        return new RimIntersectionMap(
                    0,
                    1,
                    1,
                    ROUNDABOUT_DIAMETER.get(0),
                    ENTRANCE_EXIT_RADIUS,
                    4,
                    LANE_WIDTH,
                    LANE_SPEED_LIMIT,
                    ROUNDABOUT_SPEED_LIMIT,
                    1,
                    0,
                    0);
    }

    private Road getNorthRoad(){
        return Debug.currentMap.getRoads().get(2);
    }

    private Road getEastRoad(){
        return Debug.currentMap.getRoads().get(0);
    }

    private Road getSouthRoad(){
        return Debug.currentMap.getRoads().get(3);
    }

    private Road getWestRoad(){
        return Debug.currentMap.getRoads().get(1);
    }
}
