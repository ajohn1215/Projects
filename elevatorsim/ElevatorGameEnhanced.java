package elevatorsim;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class ElevatorGameEnhanced extends Application {
    // Configuration constants
    private static final int NUM_FLOORS = 10;
    private static final int FLOOR_HEIGHT = 50;
    private static final int BUILDING_WIDTH = 300;
    private static final int ELEVATOR_WIDTH = 40;
    private static final int ELEVATOR_HEIGHT = FLOOR_HEIGHT - 10;

    private Pane simulationPane;
    private Rectangle elevatorRect;
    private Text statusText;
    
    // Control panel UI elements
    private Slider requestProbSlider;
    private Slider simSpeedSlider;
    private TextField overrideField;
    private Button overrideButton;
    private Button cancelRequestButton;
    
    // Simulation objects
    private RequestQueue requestQueue;
    private BooleanSource requestProb;
    private Elevator elevator;
    private int currentTime;
    private List<Integer> stats; // stats[0]: total wait time, stats[1]: served request count

    // Graphical representations for requests
    private List<Circle> requestCircles;
    
    // Timeline for simulation ticks
    private Timeline timeline;
    
    // Sound effects
    private AudioClip beepSound;

    @Override
    public void start(Stage primaryStage) {
        // Load sound effect (ensure beep.mp3 is in your resources folder)
        try {
            beepSound = new AudioClip(getClass().getResource("/beep.mp3").toString());
        } catch (Exception e) {
            System.out.println("Sound file not found. Sound effects disabled.");
            beepSound = null;
        }
        
        // Create simulation pane (graphical representation of the building)
        simulationPane = new Pane();
        simulationPane.setPrefSize(BUILDING_WIDTH, NUM_FLOORS * FLOOR_HEIGHT + 40);
        simulationPane.setStyle("-fx-background-color: linear-gradient(to bottom, #f0f8ff, #d3d3d3);");
        
        // Draw floors and labels
        for (int i = 0; i <= NUM_FLOORS; i++) {
            Line line = new Line(0, i * FLOOR_HEIGHT, BUILDING_WIDTH, i * FLOOR_HEIGHT);
            line.setStroke(Color.GRAY);
            simulationPane.getChildren().add(line);
            
            int floorNum = NUM_FLOORS - i + 1;
            Text floorLabel = new Text(5, i * FLOOR_HEIGHT - 5, "Floor " + floorNum);
            floorLabel.setFill(Color.DARKBLUE);
            simulationPane.getChildren().add(floorLabel);
        }
        
        // Initialize simulation variables
        requestQueue = new RequestQueue();
        requestProb = new BooleanSource(0.3); // initial probability
        elevator = new Elevator();
        currentTime = 0;
        stats = new ArrayList<>(Arrays.asList(0, 0));
        requestCircles = new ArrayList<>();
        
        // Create elevator graphic (a blue rectangle with a drop shadow)
        elevatorRect = new Rectangle(ELEVATOR_WIDTH, ELEVATOR_HEIGHT, Color.BLUE);
        elevatorRect.setX(50);
        DropShadow ds = new DropShadow();
        ds.setOffsetY(3.0);
        ds.setColor(Color.GRAY);
        elevatorRect.setEffect(ds);
        updateElevatorGraphic();
        simulationPane.getChildren().add(elevatorRect);
        
        // Status text to show simulation info
        statusText = new Text(10, NUM_FLOORS * FLOOR_HEIGHT + 20, "");
        simulationPane.getChildren().add(statusText);
        
        // Create a control panel with sliders and override buttons
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setPrefWidth(250);
        controlPanel.setStyle("-fx-background-color: #e6e6fa;");
        
        // Slider for adjusting request probability
        Label probLabel = new Label("Request Probability:");
        requestProbSlider = new Slider(0.0, 1.0, 0.3);
        requestProbSlider.setShowTickLabels(true);
        requestProbSlider.setShowTickMarks(true);
        requestProbSlider.setMajorTickUnit(0.1);
        requestProbSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            requestProb = new BooleanSource(newVal.doubleValue());
        });
        
        // Slider for simulation speed (adjusts timeline rate)
        Label speedLabel = new Label("Simulation Speed:");
        simSpeedSlider = new Slider(0.5, 2.0, 1.0);
        simSpeedSlider.setShowTickLabels(true);
        simSpeedSlider.setShowTickMarks(true);
        simSpeedSlider.setMajorTickUnit(0.5);
        simSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (timeline != null) {
                timeline.setRate(newVal.doubleValue());
            }
        });
        
        // Override controls: set elevator floor manually
        Label overrideLabel = new Label("Override Elevator Floor:");
        overrideField = new TextField();
        overrideField.setPromptText("Enter floor (1-" + NUM_FLOORS + ")");
        overrideButton = new Button("Set Floor");
        overrideButton.setOnAction(e -> overrideElevatorFloor());
        
        // Button to cancel the current elevator request
        cancelRequestButton = new Button("Cancel Current Request");
        cancelRequestButton.setOnAction(e -> cancelCurrentRequest());
        
        controlPanel.getChildren().addAll(probLabel, requestProbSlider,
                                          speedLabel, simSpeedSlider,
                                          overrideLabel, overrideField, overrideButton,
                                          cancelRequestButton);
        
        // Layout the scene with the simulation pane in the center and control panel on the right.
        BorderPane root = new BorderPane();
        root.setCenter(simulationPane);
        root.setRight(controlPanel);
        
        Scene scene = new Scene(root, BUILDING_WIDTH + 250, NUM_FLOORS * FLOOR_HEIGHT + 40);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Elevator Simulator Game - Enhanced");
        primaryStage.show();
        
        // Start the simulation timeline (ticks every 1 second)
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> gameTick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    
    // Simulation tick method called every second
    private void gameTick() {
        currentTime++;
        
        // Generate a new request based on probability
        if (requestProb.requestArrived()) {
            Request newRequest = new Request(NUM_FLOORS);
            newRequest.setTimeEntered(currentTime);
            requestQueue.enqueue(newRequest);
            addRequestGraphic(newRequest);
            if (beepSound != null) {
                beepSound.play();
            }
            System.out.println("Tick " + currentTime + ": Request from Floor " +
                               newRequest.getSourceFloor() + " to Floor " + newRequest.getDestinationFloor());
        } else {
            System.out.println("Tick " + currentTime + ": No new request.");
        }
        
        // If elevator is idle and there is a request, assign it.
        if (elevator.getElevatorState() == Elevator.IDLE && !requestQueue.isEmpty()) {
            Request req = requestQueue.dequeue();
            removeRequestGraphic(req);
            elevator.setRequest(req);
            elevator.setElevatorState(Elevator.TO_SOURCE);
        }
        
        // Move the elevator one step.
        elevator.shift(currentTime, stats);
        updateElevatorGraphic();
        updateStatusText();
        
        // Play a sound when the elevator arrives at its target (if idle)
        if (elevator.getElevatorState() == Elevator.IDLE && elevator.getRequest() == null) {
            if (beepSound != null) {
                beepSound.play();
            }
        }
    }
    
    // Update the graphical position of the elevator rectangle.
    private void updateElevatorGraphic() {
        int currentFloor = elevator.getCurrentFloor();
        double y = (NUM_FLOORS - currentFloor) * FLOOR_HEIGHT + 5;
        elevatorRect.setY(y);
    }
    
    // Update the status text with current simulation info.
    private void updateStatusText() {
        double avgWait = (stats.get(1) > 0) ? (double)stats.get(0) / stats.get(1) : 0.0;
        statusText.setText("Time: " + currentTime + " sec | Floor: " + elevator.getCurrentFloor() +
                           " | Queue: " + requestQueue.size() +
                           " | Avg Wait: " + String.format("%.2f", avgWait) + " sec");
    }
    
    // Add a red circle graphic representing a new request.
    private void addRequestGraphic(Request r) {
        int floor = r.getSourceFloor();
        double y = (NUM_FLOORS - floor) * FLOOR_HEIGHT + FLOOR_HEIGHT / 2.0;
        Circle circle = new Circle(10, Color.RED);
        circle.setCenterX(BUILDING_WIDTH - 30);
        circle.setCenterY(y);
        circle.setUserData(r);
        requestCircles.add(circle);
        simulationPane.getChildren().add(circle);
    }
    
    // Remove the request graphic once it is fulfilled.
    private void removeRequestGraphic(Request r) {
        Circle toRemove = null;
        for (Circle circle : requestCircles) {
            Request req = (Request) circle.getUserData();
            if (req == r) {
                toRemove = circle;
                break;
            }
        }
        if (toRemove != null) {
            requestCircles.remove(toRemove);
            simulationPane.getChildren().remove(toRemove);
        }
    }
    
    // Player override: set elevator floor manually.
    private void overrideElevatorFloor() {
        String input = overrideField.getText().trim();
        try {
            int floor = Integer.parseInt(input);
            if (floor < 1 || floor > NUM_FLOORS) {
                statusText.setText("Invalid floor. Enter 1-" + NUM_FLOORS);
                return;
            }
            elevator.setCurrentFloor(floor);
            elevator.setElevatorState(Elevator.IDLE);
            elevator.setRequest(null);
            updateElevatorGraphic();
            statusText.setText("Elevator floor overridden to " + floor);
            if (beepSound != null) {
                beepSound.play();
            }
        } catch (NumberFormatException e) {
            statusText.setText("Invalid input. Enter a valid floor number.");
        }
    }
    
    // Cancel the current elevator request.
    private void cancelCurrentRequest() {
        if (elevator.getRequest() != null) {
            elevator.setRequest(null);
            elevator.setElevatorState(Elevator.IDLE);
            statusText.setText("Current elevator request canceled.");
            if (beepSound != null) {
                beepSound.play();
            }
        } else {
            statusText.setText("No active elevator request to cancel.");
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
