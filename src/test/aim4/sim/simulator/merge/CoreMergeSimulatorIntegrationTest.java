package aim4.sim.simulator.merge;

import aim4.config.SimConfig;
import aim4.map.merge.MergeMapUtil;
import aim4.map.merge.S2SMergeMap;
import aim4.map.merge.SingleLaneOnlyMap;
import aim4.sim.setup.merge.enums.ProtocolType;
import aim4.vehicle.VehicleSpecDatabase;
import aim4.vehicle.merge.MergeVehicleSimModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Callum on 17/03/2017.
 */
public class CoreMergeSimulatorIntegrationTest {
    private final static double SPEED_LIMIT = 60.0;
    private final static double TIME_STEP = SimConfig.TIME_STEP;

    //SINGLE LANE PROPERTIES
    private final static double SINGLE_LANE_MAP_LANE_LENGTH = 250.0;

    //S2S PROPERTIES
    private final static double TARGET_LEAD_IN_DISTANCE = 150.0;
    private final static double TARGET_LEAD_OUT_DISTANCE = 150.0;
    private final static double MERGE_LEAD_IN_DISTANCE = 150.0;

    @Test
    public void testSingleLaneMap() {
        //Create Sim
        SingleLaneOnlyMap map = new SingleLaneOnlyMap(0, SPEED_LIMIT, SINGLE_LANE_MAP_LANE_LENGTH);
        MergeMapUtil.setSingleSpawnPoints(map, VehicleSpecDatabase.getVehicleSpecByName("COUPE"));
        CoreMergeSimulator sim = new CoreMergeSimulator(map, ProtocolType.NONE);

        //Useful Variables
        double centreOfLaneY = map.getSpawnPoints().get(0).getPosition().getY();

        //Before
        assertEquals(sim.getNumCompletedVehicles(), 0);
        assertEquals(sim.getSimulationTime(), 0, 0);

        //After One Step
        int stepsTaken = 0;
        sim.step(TIME_STEP);
        stepsTaken++;

        assertEquals(sim.getNumCompletedVehicles(), 0);
        assertEquals(sim.getSimulationTime(), stepsTaken * TIME_STEP, 0);
        assertEquals(sim.getVinToVehicles().size(), 1);

        int vin = sim.getVinToVehicles().keySet().iterator().next();
        MergeVehicleSimModel vehicle = sim.getActiveVehicle(vin);

        double startXPosition = vehicle.getPosition().getX();
        double startYPosition = vehicle.getPosition().getY();
        assertEquals(map.getEntranceDCLine().getTimes(vin).get(0), stepsTaken * TIME_STEP - TIME_STEP, 0);
        assertEquals(startXPosition, calculateNewXPosOnTargetRoad(startXPosition, stepsTaken, vehicle), 0);
        assertEquals(startYPosition, centreOfLaneY, 0);
        assertEquals(0, vehicle.getHeading(), 0);

        //After two steps
        sim.step(TIME_STEP);
        stepsTaken++;
        double currentXPosition = vehicle.getPosition().getX();
        double currentYPosition = vehicle.getPosition().getY();
        assertEquals(currentXPosition, calculateNewXPosOnTargetRoad(startXPosition, stepsTaken, vehicle), 0);
        assertEquals(currentYPosition, centreOfLaneY, 0);
        assertEquals(vehicle.getHeading(), 0, 0);

        //After Many Steps
        for(int i = 0; i < 200; i++) {
            sim.step(TIME_STEP);
            stepsTaken++;
        }

        double newXPosition = vehicle.getPosition().getX();
        double newYPosition = vehicle.getPosition().getY();
        assertEquals(newXPosition, calculateNewXPosOnTargetRoad(startXPosition, stepsTaken, vehicle), 0.001);
        assertEquals(newYPosition, centreOfLaneY,  0.001);
        assertEquals(vehicle.getHeading(), Double.MIN_VALUE,  0.001);

        //After reaching the end
        CoreMergeSimulator.CoreMergeSimStepResult result = null;
        while(vehicle.getShape().intersects(map.getDimensions())) {
            result = sim.step(TIME_STEP);
            stepsTaken++;
        }

        assertNotNull(result);
        assertTrue(result.getCompletedVehicles().keySet().contains(vin));
        assertEquals(sim.getNumCompletedVehicles(), 1);
        assertEquals(sim.getVinToVehicles().size(), 0);
        assertEquals(map.getExitDCLine().getTimes(vin).size(), 1);
    }

    @Test
    public void testS2SMapMergeOnly() {
        //Create Sim
        List<Double> testAngles = new ArrayList<Double>();
        testAngles.add(10.0);
        testAngles.add(20.0);
        testAngles.add(30.0);
        testAngles.add(40.0);
        testAngles.add(45.0);
        testAngles.add(50.0);
        testAngles.add(60.0);
        testAngles.add(70.0);
        testAngles.add(80.0);
        testAngles.add(90.0);
        for(double mergeAngle : testAngles) {
            double mergeAngleRad = Math.toRadians(mergeAngle);
            S2SMergeMap map = new S2SMergeMap(0,
                    SPEED_LIMIT, SPEED_LIMIT, TARGET_LEAD_IN_DISTANCE,
                    TARGET_LEAD_OUT_DISTANCE, MERGE_LEAD_IN_DISTANCE,
                    mergeAngle);
            MergeMapUtil.setSingleSpawnPointS2SMergeOnly(map, VehicleSpecDatabase.getVehicleSpecByName("COUPE"));
            CoreMergeSimulator sim = new CoreMergeSimulator(map, ProtocolType.TEST_MERGE);

            //Before
            assertEquals(sim.getNumCompletedVehicles(), 0);
            assertEquals(sim.getSimulationTime(), 0, 0);

            //After 1 Step
            int stepsTaken = 0;
            sim.step(TIME_STEP);
            stepsTaken++;

            assertEquals(0, sim.getNumCompletedVehicles());
            assertEquals(stepsTaken * TIME_STEP, sim.getSimulationTime(), 0);
            assertEquals(1, sim.getVinToVehicles().size());

            int vin = sim.getVinToVehicles().keySet().iterator().next();
            MergeVehicleSimModel vehicle = sim.getActiveVehicle(vin);

            double startXPosition = vehicle.getPosition().getX();
            double startYPosition = vehicle.getPosition().getY();
            assertEquals(2 * Math.PI - mergeAngleRad, vehicle.getHeading(), 0.001);


            //After 2 Steps
            sim.step(TIME_STEP);
            stepsTaken++;
            double currentXPos = vehicle.getPosition().getX();
            double currentYPos = vehicle.getPosition().getY();
            double currentHeading = vehicle.getHeading();

            double estimatedXPos = calculateNewXPosOnMergeRoad(startXPosition, mergeAngleRad, stepsTaken, vehicle);
            double estimatedYPos = calculateNewYPosOnMergeRoad(startYPosition, mergeAngleRad, stepsTaken, vehicle);

            assertEquals(estimatedXPos, currentXPos, 0.001);
            assertEquals(estimatedYPos, currentYPos, 0.001);
            assertEquals(2 * Math.PI - mergeAngleRad, currentHeading, 0.001);

            //After 20 Steps
            while (stepsTaken < 20) {
                sim.step(TIME_STEP);
                stepsTaken++;
            }
            currentXPos = vehicle.getPosition().getX();
            currentYPos = vehicle.getPosition().getY();
            currentHeading = vehicle.getHeading();

            estimatedXPos = calculateNewXPosOnMergeRoad(startXPosition, mergeAngleRad, stepsTaken, vehicle);
            estimatedYPos = calculateNewYPosOnMergeRoad(startYPosition, mergeAngleRad, stepsTaken, vehicle);

            assertEquals(estimatedXPos, currentXPos, 0.001);
            assertEquals(estimatedYPos, currentYPos, 0.001);
            assertEquals(2 * Math.PI - mergeAngleRad, currentHeading, 0.001);

            //At point of entry into Merge Zone
            while (!vehicle.getShape().intersects(map.getMergeConnections().get(0).getArea().getBounds2D())) {
                sim.step(TIME_STEP);
                stepsTaken++;
            }
            currentXPos = vehicle.getPosition().getX();
            currentYPos = vehicle.getPosition().getY();

            estimatedXPos = calculateNewXPosOnMergeRoad(startXPosition, mergeAngleRad, stepsTaken, vehicle);
            estimatedYPos = calculateNewYPosOnMergeRoad(startYPosition, mergeAngleRad, stepsTaken, vehicle);

            assertEquals(estimatedXPos, currentXPos, 0.001);
            assertEquals(estimatedYPos, currentYPos, 0.001);
            assertEquals(2 * Math.PI - mergeAngleRad, currentHeading, 0.001);

            //Within merge zone
            double targetYPos = map.getTargetSpawnPoint().getPosition().getY();
            double targetHeading = 0.0;
            while (vehicle.getShape().intersects(map.getMergeConnections().get(0).getArea().getBounds2D())) {
                double lastXPos = vehicle.getPosition().getX();
                double lastYPos = vehicle.getPosition().getY();
                double lastHeading = vehicle.getHeading();
                sim.step(TIME_STEP);
                stepsTaken++;
                //Vehicle should consistently move towards the exit and vehicle heading should shift towards 0.
                currentXPos = vehicle.getPosition().getX();
                currentYPos = vehicle.getPosition().getY();
                currentHeading = vehicle.getHeading();

                assertTrue(currentXPos > lastXPos);
            /*
            TODO: Currently disabled tests. The vehicle still tends towards the centre of the target lane but not cleanly.

            if(currentYPos != targetYPos) {
                assertTrue(currentYPos < lastYPos);
            }
            assertTrue(currentYPos >= targetYPos);

            if(currentHeading != targetHeading) {
                assertTrue(currentHeading > lastHeading);
            }
            assertTrue(currentHeading == targetHeading || currentHeading < 2*Math.PI);
            */
            }

            //Leaving merge zone
            while (vehicle.getShape().intersects(map.getDimensions())) {
                double lastXPos = vehicle.getPosition().getX();
                sim.step(TIME_STEP);
                stepsTaken++;
                currentXPos = vehicle.getPosition().getX();
                assertTrue(currentXPos > lastXPos);
            }
        }
    }

    @Test
    public void testS2SMapTargetOnly() {
        //Create Sim
        S2SMergeMap map = new S2SMergeMap(0,
                SPEED_LIMIT, SPEED_LIMIT, TARGET_LEAD_IN_DISTANCE,
                TARGET_LEAD_OUT_DISTANCE, MERGE_LEAD_IN_DISTANCE,
                45.0);
        MergeMapUtil.setSingleSpawnPointS2STargetOnly(map, VehicleSpecDatabase.getVehicleSpecByName("COUPE"));
        CoreMergeSimulator sim = new CoreMergeSimulator(map, ProtocolType.TEST_TARGET);

        //Useful Variables
        double centreOfTargetLaneY = map.getTargetSpawnPoint().getPosition().getY();

        //Before
        assertEquals(sim.getNumCompletedVehicles(), 0);
        assertEquals(sim.getSimulationTime(), 0, 0);

        //After 1 Step
        int stepsTaken = 0;
        sim.step(TIME_STEP);
        stepsTaken++;

        assertEquals(0, sim.getNumCompletedVehicles());
        assertEquals(stepsTaken * TIME_STEP, sim.getSimulationTime(), 0);
        assertEquals(1, sim.getVinToVehicles().size());

        int vin = sim.getVinToVehicles().keySet().iterator().next();
        MergeVehicleSimModel vehicle = sim.getActiveVehicle(vin);

        double startXPosition = vehicle.getPosition().getX();
        double startYPosition = vehicle.getPosition().getY();
        assertEquals(0, vehicle.getHeading(), 0);
        assertEquals(startYPosition, centreOfTargetLaneY, 0);

        //After 2 Steps
        sim.step(TIME_STEP);
        stepsTaken++;
        double currentXPos = vehicle.getPosition().getX();
        double currentYPos = vehicle.getPosition().getY();
        double currentHeading = vehicle.getHeading();

        double estimatedXPos = calculateNewXPosOnTargetRoad(startXPosition, stepsTaken, vehicle);

        assertEquals(estimatedXPos, currentXPos, 0);
        assertEquals(centreOfTargetLaneY, currentYPos, 0);
        assertEquals(0, currentHeading, 0);

        //After 20 Steps
        while(stepsTaken < 20) {
            sim.step(TIME_STEP);
            stepsTaken++;
        }
        currentXPos = vehicle.getPosition().getX();
        currentYPos = vehicle.getPosition().getY();
        currentHeading = vehicle.getHeading();

        estimatedXPos = calculateNewXPosOnTargetRoad(startXPosition, stepsTaken, vehicle);

        assertEquals(estimatedXPos, currentXPos, 0.001);
        assertEquals(centreOfTargetLaneY, currentYPos, 0);
        assertEquals(0, currentHeading, 0);

        //At point of entry into Merge Zone
        while(!vehicle.getShape().intersects(map.getMergeConnections().get(0).getArea().getBounds2D())){
            sim.step(TIME_STEP);
            stepsTaken++;
        }
        currentXPos = vehicle.getPosition().getX();
        currentYPos = vehicle.getPosition().getY();

        estimatedXPos = calculateNewXPosOnTargetRoad(startXPosition, stepsTaken, vehicle);

        assertEquals(estimatedXPos, currentXPos, 0.001);
        assertEquals(centreOfTargetLaneY, currentYPos, 0);
        assertEquals(0, currentHeading, 0);

        //Within merge zone
        while(vehicle.getShape().intersects(map.getMergeConnections().get(0).getArea().getBounds2D())){
            double lastXPos = vehicle.getPosition().getX();
            sim.step(TIME_STEP);
            stepsTaken++;
            //Vehicle should consistently move towards the exit and heading should not change.
            currentXPos = vehicle.getPosition().getX();
            currentYPos = vehicle.getPosition().getY();
            currentHeading = vehicle.getHeading();

            assertTrue(currentXPos > lastXPos);
            assertEquals(currentYPos, centreOfTargetLaneY, 0);
            assertEquals(currentHeading, 0, 0);
        }
    }

    private double calculateNewXPosOnMergeRoad(double startXPos, double mergeAngleRad, int stepsTaken, MergeVehicleSimModel vehicle){
        return startXPos + (vehicle.getVelocity() * ((stepsTaken-1) * TIME_STEP)) * Math.cos(mergeAngleRad);
        //-1 due to vehicle only moving on Step 2
    }

    private double calculateNewYPosOnMergeRoad(double startYPos, double mergeAngleRad, int stepsTaken, MergeVehicleSimModel vehicle){
        return startYPos - (vehicle.getVelocity() * ((stepsTaken-1) * TIME_STEP)) * Math.sin(mergeAngleRad);
        //-1 due to vehicle only moving on Step 2
    }

    private double calculateNewXPosOnTargetRoad(double startXPos, int stepsTaken, MergeVehicleSimModel vehicle){
        return startXPos + (vehicle.getVelocity() * (stepsTaken-1) * TIME_STEP);
        //-1 due to vehicle only moving on Step 2
    }

}
