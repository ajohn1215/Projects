package elevatorsim;

public class Request {
    private int sourceFloor;
    private int destinationFloor;
    private int timeEntered;

    public Request() {
        sourceFloor = 0;
        destinationFloor = 0;
        timeEntered = 0;
    }

    // Constructs a request with random source and destination floors.
    public Request(int floors) {
        sourceFloor = (int)(Math.random() * floors + 1);
        destinationFloor = (int)(Math.random() * floors + 1);
        timeEntered = 0;
    }

    public int getSourceFloor() { return sourceFloor; }
    public void setSourceFloor(int sourceFloor) { this.sourceFloor = sourceFloor; }
    public int getDestinationFloor() { return destinationFloor; }
    public void setDestinationFloor(int destinationFloor) { this.destinationFloor = destinationFloor; }
    public int getTimeEntered() { return timeEntered; }
    public void setTimeEntered(int time) { this.timeEntered = time; }
}
