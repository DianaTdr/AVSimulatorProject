package aim4.driver.rim;

import aim4.driver.AutoDriver;
import aim4.driver.BasicDriver;
import aim4.driver.Coordinator;
import aim4.driver.rim.coordinator.NoIntersectionCoordinator;
import aim4.driver.rim.coordinator.V2ICoordinator;
import aim4.im.rim.IntersectionManager;
import aim4.map.BasicRIMIntersectionMap;
import aim4.vehicle.AutoVehicleDriverModel;
import aim4.vehicle.rim.RIMAutoVehicleDriverModel;

import java.awt.geom.Area;

/**
 * An agent that drives a {@link AutoVehicleDriverModel} while coordinating with
 * {@link IntersectionManager}s and other Vehicles.  Such an agent consists
 * of two sub-agents, a {@link Coordinator} and a Pilot. The two
 * agents communicate by setting state in this class.
 */
public class RIMAutoDriver extends RIMDriver implements AutoDriverCoordinatorView, AutoDriver, AutoDriverPilotView {
    /////////////////////////////////
    // PRIVATE FIELDS
    /////////////////////////////////

    /** The vehicle this driver will control */
    private RIMAutoVehicleDriverModel vehicle;

    /** The sub-agent that controls coordination */
    private Coordinator coordinator;

    /** The map */
    private BasicRIMIntersectionMap basicRIMIntersectionMap;

    /**
     * The IntersectionManager with which the driver is currently interacting.
     */
    private IntersectionManager currentRIM;

    // Memoization Caches - big D double so they can be null

    private transient Double memoDistanceToNextIntersection;
    /** Memoization cache for {@link #distanceFromPrevIntersection()}. */
    private transient Double memoDistanceFromPrevIntersection;
    /** Memoization cache for {@link #nextIntersectionManager()}. */
    private transient IntersectionManager memoNextIntersectionManager;
    /** Memoization cache for {@link #inCurrentIntersection()}. */
    private transient Boolean memoInCurrentIntersection;

    /////////////////////////////////
    // CONSTRUCTORS
    /////////////////////////////////

    public RIMAutoDriver(RIMAutoVehicleDriverModel vehicle, BasicRIMIntersectionMap basicRIMIntersectionMap) {
        this.vehicle = vehicle;
        this.basicRIMIntersectionMap = basicRIMIntersectionMap;
        coordinator = null;
        currentRIM = null;
    }

    /////////////////////////////////
    // PUBLIC METHODS
    /////////////////////////////////

    /**
     * Take control actions for driving the agent's Vehicle.  This allows
     * both the Coordinator and the Pilot to act (in that order).
     */
    @Override
    public void act() {
        super.act();
        // Clear these because we need the latest information
        clearMemoizationCaches();

        if (coordinator == null || coordinator.isTerminated()) {
            IntersectionManager im = nextIntersectionManager();
            // TODO: need to check type of intersection
            if (im != null) {
                currentRIM = im;
                coordinator = new V2ICoordinator(vehicle, this, basicRIMIntersectionMap);
            } else {
                currentRIM = null;
                coordinator = new NoIntersectionCoordinator(vehicle, this);
            }
        }
        // the newly created coordinator can be called immediately.
        if (!coordinator.isTerminated()) {
            coordinator.act();
        }
    }

    /////////////////////////////////
    // PUBLIC METHODS
    /////////////////////////////////

    /**
     * Get the Vehicle this DriverAgent is controlling.
     *
     * @return the Vehicle this DriverAgent is controlling
     */
    @Override
    public RIMAutoVehicleDriverModel getVehicle() {
        return vehicle;
    }


    /**
     * Get the current coordinator of the vehicle.
     *
     * @return the current coordinator of the vehicle.
     */
    public Coordinator getCurrentCoordinator() {
        return coordinator;
    }

    /////////////////////////////////
    // PUBLIC METHODS
    /////////////////////////////////

    // current IM

    /**
     * Get the IntersectionManager with which the agent is currently
     * interacting.
     *
     * @return the IntersectionManager with which the agent is currently
     *         interacting
     *
     */
    @Override
    public IntersectionManager getCurrentRIM() {
        return currentRIM;
    }


    /////////////////////////////////
    // PUBLIC METHODS
    /////////////////////////////////

    // IM

    // TODO: these functions should be moved to pilot.

    /**
     * Find the next IntersectionManager that the Vehicle will need to
     * interact with, in this Lane. This version
     * overrides the version in {@link BasicDriver}, but only to memoize it for
     * speed.
     *
     * @return the nextIntersectionManager that the Vehicle will need
     *         to interact with, in this Lane
     */
    @Override
    public IntersectionManager nextIntersectionManager() {
        // TODO: maybe we should move this to the superclass
        if(memoNextIntersectionManager == null) {
            memoNextIntersectionManager = super.nextIntersectionManager();
        }
        return memoNextIntersectionManager;
    }

    /**
     * Find the distance to the next intersection in the Lane in which
     * the Vehicle is, from the position at which the Vehicle is.  This version
     * overrides the version in {@link BasicDriver}, but only to memoize it for
     * speed.
     *
     * @return the distance to the next intersection given the current Lane
     *         and position of the Vehicle.
     */
    @Override
    public double distanceToNextIntersection() {
        // TODO: maybe we should move this to the superclass
        if(memoDistanceToNextIntersection == null) {
            memoDistanceToNextIntersection = super.distanceToNextIntersection();
        }
        return memoDistanceToNextIntersection;
    }

    /**
     * Find the distance from the previous intersection in the Lane in which
     * the Vehicle is, from the position at which the Vehicle is.  This
     * subtracts the length of the Vehicle from the distance from the front
     * of the Vehicle.  It overrides the version in DriverAgent, but only to
     * memoize it.
     *
     * @return the distance from the previous intersection given the current
     *         Lane and position of the Vehicle.
     */
    @Override
    public double distanceFromPrevIntersection() {
        // TODO: maybe we should move this to the superclass
        if(memoDistanceFromPrevIntersection == null) {
            memoDistanceFromPrevIntersection = super.distanceFromPrevIntersection();
        }
        return memoDistanceFromPrevIntersection;
    }

    /**
     * Whether or not the Vehicle controlled by this driver agent
     * is inside the intersection managed by the current IntersectionManager.
     *
     * @return whether or not the Vehicle controlled by this
     *         CoordinatingDriverAgent is inside the intersection managed by the
     *         current IntersectionManager.
     */
    @Override
    public boolean inCurrentIntersection() {
        if(memoInCurrentIntersection == null) {
            memoInCurrentIntersection =
                    intersects(getVehicle(), currentRIM.getIntersection().getAreaPlus());
            //intersects(getVehicle(), currentIM.getIntersection().getArea());
        }
        return memoInCurrentIntersection;
    }


    /////////////////////////////////
    // PRIVATE METHODS
    /////////////////////////////////

    /**
     * Determine whether the given Vehicle is currently inside an area
     *
     * @param v     the vehicle
     * @param area  the area
     * @return      whether the Vehicle is currently in the area
     */
    private static boolean intersects(AutoVehicleDriverModel v, Area area) {
        // TODO: move this function to somewhere else.

        // As a quick check, see if the front or rear point is in the intersection
        // Most of the time this should work
        if(area.contains(v.gaugePosition()) || area.contains(v.gaugePointAtRear())){
            return true;
        } else {
            // We actually have to check to see if the Area of the
            // Vehicle and the Area of the IntersectionManager have a nonempty
            // intersection
            Area vehicleArea = new Area(v.gaugeShape());
            // Important that it is in this order, as it is destructive to the caller
            vehicleArea.intersect(area);
            return !vehicleArea.isEmpty();
        }
    }

    /**
     * Clear any caches we are using to memoize methods
     */
    private void clearMemoizationCaches() {
        memoNextIntersectionManager = null;
        memoInCurrentIntersection = null;
        memoDistanceToNextIntersection = null;
        memoDistanceFromPrevIntersection = null;
    }
}
