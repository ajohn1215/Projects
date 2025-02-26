package elevatorsim;
import java.util.List;

public class Elevator {
    public static final int IDLE = 1;
    public static final int TO_SOURCE = 2;
    public static final int TO_DESTINATION = 3;

    private int currentFloor;
    private int elevatorState;
    private Request request;

    public Elevator() {
        currentFloor = 1;
        elevatorState = IDLE;
        request = null;
    }

    public int getCurrentFloor() { return currentFloor; }
    public void setCurrentFloor(int floor) { currentFloor = floor; }
    public int getElevatorState() { return elevatorState; }
    public void setElevatorState(int state) {
        if (!(state == IDLE || state == TO_SOURCE || state == TO_DESTINATION))
            throw new IllegalArgumentException("Invalid elevator state");
        elevatorState = state;
    }
    public Request getRequest() { return request; }
    public void setRequest(Request r) { request = r; }

    // Moves the elevator one step toward its target floor.
    public void shift(int currentTime, List<Integer> stats) {
        if (request == null) return;

        int targetFloor = (elevatorState == TO_SOURCE) ? request.getSourceFloor() :
                           (elevatorState == TO_DESTINATION) ? request.getDestinationFloor() : currentFloor;

        if (currentFloor == targetFloor) {
            if (elevatorState == TO_SOURCE) {
                elevatorState = TO_DESTINATION;
                stats.set(0, stats.get(0) + (currentTime - request.getTimeEntered()));
                stats.set(1, stats.get(1) + 1);
            } else { // Arrived at destination
                request = null;
                elevatorState = IDLE;
            }
        } else {
            currentFloor += (currentFloor < targetFloor) ? 1 : -1;
        }
    }
}
