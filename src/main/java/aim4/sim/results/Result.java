package aim4.sim.results;

import java.util.List;

public class Result implements SimulatorResult{
    private List<VehicleResult> vehicleResults;
    private double throughput;
    private int completedVehicles;
    public Result(List<VehicleResult> vehicleResults) {
        this.vehicleResults = vehicleResults;

        this.completedVehicles = 0;
        double lastVehicleTime = 0;

        for (VehicleResult result : vehicleResults) {
            //Completed Vehicles
            this.completedVehicles++;
            //Throughput Help
            if (lastVehicleTime < result.getFinishTime())
                lastVehicleTime = result.getFinishTime();

            //Throughput
            this.throughput = completedVehicles / lastVehicleTime;
        }
    }

    public List<VehicleResult> getVehicleResults() {
        return vehicleResults;
    }

    public double getThroughput() {
        return throughput;
    }

    public double getCompletedVehicles() {
        return completedVehicles;
    }

    public String produceCSVString() {
        StringBuilder sb = new StringBuilder();
        //Global Stats
        sb.append(produceGlobalStatsCSVHeader());
        sb.append('\n');
        sb.append(produceGlobalStatsCSV());
        sb.append('\n');
        sb.append('\n');
        //Vehicles
        sb.append(produceVehicleStatsCSVHeader());
        sb.append('\n');
        sb.append(produceVehicleStatsCSV());
        sb.append('\n');

        return sb.toString();
    }

    public static String produceGlobalStatsCSVHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("Throughput");
        sb.append(',');
        sb.append("Completed Vehicles");

        return sb.toString();
    }

    public String produceGlobalStatsCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(getThroughput());
        sb.append(',');
        sb.append(getCompletedVehicles());

        return sb.toString();
    }

    public static String produceVehicleStatsCSVHeader(){
        StringBuilder sb = new StringBuilder();
        //Headings
        sb.append("VIN");
        sb.append(',');
        sb.append("Vehicle Spec");
        sb.append(',');
        sb.append("Start Time");
        sb.append(',');
        sb.append("Finish Time");
        sb.append(',');
        sb.append("Final Velocity");
        sb.append(',');
        sb.append("Max Velocity");
        sb.append(',');
        sb.append("Min Velocity");
        return sb.toString();
    }

    public String produceVehicleStatsCSV(){
        StringBuilder sb = new StringBuilder();
        for(VehicleResult vr : vehicleResults){
            sb.append(vr.getVin());
            sb.append(',');
            sb.append(vr.getSpecType());
            sb.append(',');
            sb.append(vr.getStartTime());
            sb.append(',');
            sb.append(vr.getFinishTime());
            sb.append(',');
            sb.append(vr.getFinalVelocity());
            sb.append(',');
            sb.append(vr.getMaxVelocity());
            sb.append(',');
            sb.append(vr.getMinVelocity());
            sb.append('\n');
        }
        return sb.toString();
    }

}