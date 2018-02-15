package aim4.sim.simulator.rim;

import aim4.map.rim.RimIntersectionMap;
import aim4.sim.Simulator;
import aim4.sim.results.AIMResult;
import aim4.vehicle.aim.ProxyVehicleSimModel;
import aim4.vehicle.rim.RimVehicleSimModel;

import java.util.Set;

public interface RIMSimulator extends Simulator {
    /**
     * Get the set of all active vehicles in the simulation.
     *
     * @return the set of all active vehicles in the simulation
     */
    Set<RimVehicleSimModel> getActiveVehicles();

    @Override
    RimIntersectionMap getMap();

    /**
     * Add the proxy vehicle to the simulator for the mixed reality experiments.
     *
     * @param vehicle  the proxy vehicle
     */
    void addProxyVehicle(ProxyVehicleSimModel vehicle);

    AIMResult produceResult();
}
