package aim4.rim.driver;

import aim4.config.Debug;
import aim4.config.SimConfig;
import aim4.driver.rim.RIMAutoDriver;
import aim4.driver.rim.coordinator.V2ICoordinator;
import aim4.im.rim.IntersectionManager;
import aim4.im.rim.RoadBasedIntersection;
import aim4.im.rim.RoadBasedTrackModel;
import aim4.im.rim.TrackModel;
import aim4.im.rim.v2i.RequestHandler.FCFSRequestHandler;
import aim4.im.rim.v2i.V2IManager;
import aim4.im.rim.v2i.policy.BasePolicy;
import aim4.im.rim.v2i.policy.Policy;
import aim4.im.rim.v2i.reservation.ReservationGridManager;
import aim4.map.Road;
import aim4.map.lane.ArcSegmentLane;
import aim4.map.lane.Lane;
import aim4.map.rim.RimIntersectionMap;
import aim4.msg.rim.v2i.Request;
import aim4.sim.simulator.rim.AutoDriverOnlySimulator;
import aim4.util.ArrayListRegistry;
import aim4.util.Registry;
import aim4.vehicle.VehicleSpec;
import aim4.vehicle.VehicleSpecDatabase;
import aim4.vehicle.VehicleUtil;
import aim4.vehicle.VinRegistry;
import aim4.vehicle.rim.RIMAutoVehicleSimModel;
import aim4.vehicle.rim.RIMBasicAutoVehicle;
import org.junit.Test;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class V2ICoordinatorTests {
    private static final double CURRENT_TIME = 1.0;
    private static final double ARRIVAL_TIME = 6.0;
    private static final double STATIC_BUFFER_SIZE = 0.25;
    private static final double INTERNAL_TILE_TIME_BUFFER_SIZE = 0.1;
    private static final double GRANULARITY = 6.0;
    private static final List<Double> ROUNDABOUT_DIAMETER = Arrays.asList(30.0, 35.0, 40.0, 45.0);
    private static final double ENTRANCE_EXIT_RADIUS =  20.0;
    private static final double LANE_WIDTH =  3.014;
    private static final double LANE_SPEED_LIMIT =  19.44;
    private static final double ROUNDABOUT_SPEED_LIMIT =  9.7222;

    @Test
    public void takeSteeringActionForTraversing_withReservationParameter_setsSteeringAction(){
        //arrange

        // Create map
        RimIntersectionMap map = getRimIntersectionMap(ROUNDABOUT_DIAMETER.get(0));

        // Create intersection
        RoadBasedIntersection roadBasedIntersection = new RoadBasedIntersection(map.getRoads());

        // Create track model
        TrackModel trackModel = new RoadBasedTrackModel(roadBasedIntersection);

        // IM Registry
        Registry<IntersectionManager> registry = new ArrayListRegistry<>();

        // Create IM
        IntersectionManager intersectionManager = new IntersectionManager(roadBasedIntersection, trackModel, CURRENT_TIME, registry);

        // Create config
        ReservationGridManager.Config config = getConfig();

        // Create request handler
        FCFSRequestHandler fcfsRequestHandler = new FCFSRequestHandler();

        // Set Arrival and Departure
        Lane arrivalLane = getNorthRoad().getEntryApproachLane();
        Lane departureLane = getWestRoad().getExitApproachLane();

        // Set vehicle spec
        VehicleSpec spec = VehicleSpecDatabase.getVehicleSpecByName("VAN");
        Request.VehicleSpecForRequestMsg vehicleSpecForRequestMsg = new Request.VehicleSpecForRequestMsg(spec);

        // Set traversal velocity
        double traversalVelocity = ROUNDABOUT_SPEED_LIMIT - 0.5;
        double arrivalTime = ARRIVAL_TIME;
        double maxTurnVelocity = VehicleUtil.maxTurnVelocity(spec, arrivalLane, departureLane, intersectionManager);

        // Setup the vehicle
        Point2D vehiclePosition = arrivalLane.getEndPoint();
        RIMBasicAutoVehicle vehicle = new RIMBasicAutoVehicle(spec, vehiclePosition,
                ((ArcSegmentLane) arrivalLane.getNextLane()).getArcLaneDecomposition().get(0).getInitialHeading(), // Heading
                0.0,  // Steering angle
                traversalVelocity, // velocity
                0.0, // target velocity
                0.0, // Acceleration
                0.0);

        // Setup the driver
        RIMAutoDriver rimAutoDriver = new RIMAutoDriver(vehicle, map);

        rimAutoDriver.setCurrentLane(arrivalLane);
        rimAutoDriver.setDestination(getWestRoad());

        int arrivalLaneId = arrivalLane.getId();
        int departureLaneId = departureLane.getId();

       // Set the proposal
        Request.Proposal proposal = new Request.Proposal(arrivalLaneId, departureLaneId, arrivalTime, traversalVelocity, maxTurnVelocity);

        // Set request
        int vin = 1; // the vehicle id
        int imId = 0; // the im id to which to send the message
        int requestId = 1; // request id
        List<Request.Proposal> proposals = new ArrayList<>();
        proposals.add(proposal);

        Request request = new Request(vin, imId, requestId, vehicleSpecForRequestMsg, proposals);

        // Register the vehicle
        RIMAutoVehicleSimModel vehicleSimModel = vehicle;
        VinRegistry.registerVehicleWithExistingVIN(vehicleSimModel, vin);

        // Set V21Manager
        V2IManager im = new V2IManager(roadBasedIntersection, trackModel, CURRENT_TIME, config, map.getImRegistry());

        // Set policy
        Policy policy = new BasePolicy(im, fcfsRequestHandler);
        im.setPolicy(policy);

        // Set manager
        map.setManager(0, 0, im);

        // Setup simulator
        AutoDriverOnlySimulator simulator = new AutoDriverOnlySimulator(map);

        // Setup V2I Message
        vehicle.send(request);

        // Get all active vehicles
        simulator.vinToVehicles.put(vin, vehicle);

        // Send the message to intersection manager
        simulator.deliverV2IMessages();

        // IM processes the messages
        im.act(SimConfig.TIME_STEP);

        // Send the message to vehicle's outbox
        simulator.deliverI2VMessages();

        // Setup the coordinator
        V2ICoordinator v2ICoordinator = new V2ICoordinator(vehicle, rimAutoDriver, map);

        //act
        v2ICoordinator.act();


    }

    private RimIntersectionMap getRimIntersectionMap(double roundaboutDiameter) {
        return new RimIntersectionMap(
                0,
                1,
                1,
                roundaboutDiameter,
                ENTRANCE_EXIT_RADIUS,
                4,
                LANE_WIDTH,
                LANE_SPEED_LIMIT,
                ROUNDABOUT_SPEED_LIMIT,
                1,
                0,
                0);
    }

    private ReservationGridManager.Config getConfig() {
        return new ReservationGridManager.Config(SimConfig.TIME_STEP,
                SimConfig.GRID_TIME_STEP,
                STATIC_BUFFER_SIZE,
                INTERNAL_TILE_TIME_BUFFER_SIZE,
                GRANULARITY);
    }

    private Road getNorthRoad(){ return Debug.currentRimMap.getRoads().get(2); }

    private Road getEastRoad(){
        return Debug.currentRimMap.getRoads().get(0);
    }

    private Road getSouthRoad(){
        return Debug.currentRimMap.getRoads().get(3);
    }

    private Road getWestRoad(){
        return Debug.currentRimMap.getRoads().get(1);
    }
}