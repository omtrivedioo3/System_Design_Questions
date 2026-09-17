package ElevatorSystem;

import java.util.*;

/**
 * ============================================================================================================
 *                             ELEVATOR SYSTEM LLD - ARCHITECTURE & UML DIAGRAM (SDE-1)
 * ============================================================================================================
 *
 * 1. UML CLASS DIAGRAM:
 * ---------------------
 *   +------------------------------------+          +------------------------------------+
 *   |        ExternalDispatcher          |          |         ElevatorScheduler          |
 *   +------------------------------------+          +------------------------------------+
 *   | - scheduler : ElevatorScheduler    | -------> | - controllers : List<Controller>   |
 *   +------------------------------------+   uses   +------------------------------------+
 *   | + submitExternalRequest(flr, dir)  |          | + assignElevator(flr, dir)         |
 *   +------------------------------------+          +------------------------------------+
 *                                                                     |
 *                                                                     | selects best
 *                                                                     v
 *   +------------------------------------+          +------------------------------------+
 *   |            ElevatorCar             |          |        ElevatorController          |
 *   +------------------------------------+          +------------------------------------+
 *   | - id : int                         | <------* | - elevatorCar : ElevatorCar        |
 *   | - currentFloor : int               |  owns    | - upMinPQ   : PriorityQueue<Int>   |
 *   | - direction : Direction            |          | - downMaxPQ : PriorityQueue<Int>   |
 *   | - status : Status                  |          +------------------------------------+
 *   | - display : Display                |          | + submitRequest(floor, direction)  |
 *   | - internalButtons : InternalButtons|          | + processRequests() : void         |
 *   +------------------------------------+          +------------------------------------+
 *   | + move(targetFloor, dir) : void    |
 *   | + pressInternalButton(floor) : void|
 *   +------------------------------------+
 *
 *
 * 2. REQUEST FLOW (External vs Internal Requests):
 * ------------------------------------------------
 *   A. EXTERNAL REQUEST (Hallway Button Pressed on Floor X going UP/DOWN):
 *      [User on Floor X] ---> [ExternalDispatcher.submitExternalRequest(X, UP)]
 *                                  |
 *                                  v
 *                         [ElevatorScheduler.assignElevator(X, UP)]
 *                         (Picks controller with minimum load / closest distance)
 *                                  |
 *                                  v
 *                         [ElevatorController.submitRequest(X, UP)]
 *                         (Adds Floor X to `upMinPQ` or `downMaxPQ`)
 *
 *   B. INTERNAL REQUEST (Passenger inside Elevator presses Destination Floor Y):
 *      [Passenger inside Car] ---> [InternalButtons.pressButton(Y, controller)]
 *                                  |
 *                                  v
 *                         [ElevatorController.submitRequest(Y, dir)]
 *
 *   C. ELEVATOR MOVEMENT ALGORITHM (LOOK / SCAN using Two Heaps):
 *      - `upMinPQ`   (Min-Heap): Serves upward floors in ascending order (e.g., Floor 2 -> 5 -> 8)
 *      - `downMaxPQ` (Max-Heap): Serves downward floors in descending order (e.g., Floor 9 -> 6 -> 1)
 * ============================================================================================================
 */

enum Direction {
  UP,
  DOWN,
  NONE
}

enum Status {
  IDLE,
  MOVING
}

class Display {
  private int currentFloor; // Not final, updates as elevator moves
  private Direction direction;

  public Display(int currentFloor, Direction direction) {
    this.currentFloor = currentFloor;
    this.direction = direction;
  }

  public void updateDisplay(int floor, Direction direction) {
    this.currentFloor = floor;
    this.direction = direction;
  }

  public void showDisplay(int elevatorId) {
    System.out.println("  [Display - Elevator #" + elevatorId + "] Floor: " + currentFloor + " | Direction: " + direction);
  }
}

class InternalButtons {
  private final int totalFloors;

  public InternalButtons(int totalFloors) {
    this.totalFloors = totalFloors;
  }

  public void pressButton(int fromFloor, int destinationFloor, ElevatorController controller) {
    if (destinationFloor < 0 || destinationFloor > totalFloors) {
      System.out.println("❌ Invalid floor button pressed: " + destinationFloor);
      return;
    }
    Direction dir;
    if (destinationFloor >= fromFloor) {
      dir = Direction.UP;
    } else {
      dir = Direction.DOWN;
    }
    System.out.println("[INTERNAL BUTTON] Passenger boarding at Floor " + fromFloor
        + " inside Elevator #" + controller.getElevatorCar().getId()
        + " pressed destination Floor " + destinationFloor + " (" + dir + ")");
    controller.submitRequest(destinationFloor, dir);
  }
}

class ElevatorCar {
  private final int id;
  private int currentFloor;
  private Direction direction;
  private Status status;
  private final Display display;
  private final InternalButtons internalButtons;

  public ElevatorCar(int id, int totalFloors) {
    this.id = id;
    this.currentFloor = 0; // Starts at Ground Floor (0)
    this.direction = Direction.NONE;
    this.status = Status.IDLE;
    this.display = new Display(0, Direction.NONE);
    this.internalButtons = new InternalButtons(totalFloors);
  }

  public void move(int targetFloor) {
    if (targetFloor == currentFloor) {
      System.out.println("  🛎️ Elevator #" + id + " is already at Floor " + currentFloor + " (doors opening)\n");
      return;
    }
    Direction travelDir;
    if (targetFloor > currentFloor) {
      travelDir = Direction.UP;
    } else {
      travelDir = Direction.DOWN;
    }
    this.status = Status.MOVING;
    this.direction = travelDir;
    System.out.println("  -> Elevator #" + id + " moving " + travelDir + " from Floor " + currentFloor + " to Floor " + targetFloor);
    this.currentFloor = targetFloor;
    this.display.updateDisplay(currentFloor, travelDir);
    this.display.showDisplay(id);
    System.out.println("  🛎️ Elevator #" + id + " doors opening at Floor " + currentFloor + "\n");
  }

  public void setIdle() {
    this.status = Status.IDLE;
    this.direction = Direction.NONE;
    this.display.updateDisplay(currentFloor, Direction.NONE);
  }

  public int getId() {
    return id;
  }

  public int getCurrentFloor() {
    return currentFloor;
  }

  public Direction getDirection() {
    return direction;
  }

  public Status getStatus() {
    return status;
  }

  public InternalButtons getInternalButtons() {
    return internalButtons;
  }
}

class ElevatorController {
  private final ElevatorCar elevatorCar;
  // Min-Heap for UP requests (serves lowest floor first: e.g., 2 -> 4 -> 7)
  final PriorityQueue<Integer> upMinPQ;
  // Max-Heap for DOWN requests (serves highest floor first: e.g., 9 -> 5 -> 1)
  final PriorityQueue<Integer> downMaxPQ;

  public ElevatorController(ElevatorCar elevatorCar) {
    this.elevatorCar = elevatorCar;
    this.upMinPQ = new PriorityQueue<>();
    this.downMaxPQ = new PriorityQueue<>(Collections.reverseOrder());
  }

  public ElevatorCar getElevatorCar() {
    return elevatorCar;
  }

  public void submitRequest(int floor, Direction direction) {
    if (direction == Direction.UP) {
      if (!upMinPQ.contains(floor)) {
        upMinPQ.offer(floor);
      }
    } else {
      if (!downMaxPQ.contains(floor)) {
        downMaxPQ.offer(floor);
      }
    }
  }

  // Processes queued floor requests using the LOOK / SCAN algorithm
  public void processRequests() {
    System.out.println("--- Processing Requests for Elevator #" + elevatorCar.getId() + " ---");
    while (!upMinPQ.isEmpty() || !downMaxPQ.isEmpty()) {
      // 1. Serve all UP requests in ascending order
      while (!upMinPQ.isEmpty()) {
        int nextFloor = upMinPQ.poll();
        elevatorCar.move(nextFloor);
      }

      // 2. Serve all DOWN requests in descending order
      while (!downMaxPQ.isEmpty()) {
        int nextFloor = downMaxPQ.poll();
        elevatorCar.move(nextFloor);
      }
    }
    elevatorCar.setIdle();
  }
}

class ElevatorScheduler {
  private final List<ElevatorController> elevatorControllers;

  public ElevatorScheduler(List<ElevatorController> elevatorControllers) {
    this.elevatorControllers = elevatorControllers;
  }

  public void addElevatorController(ElevatorController elevatorController) {
    this.elevatorControllers.add(elevatorController);
  }

  // Assigns the best elevator based on Least Pending Load + Closest Distance
  public ElevatorController assignElevator(int floor, Direction direction) {
    ElevatorController bestController = null;
    int minScore = Integer.MAX_VALUE;

    for (ElevatorController controller : elevatorControllers) {
      int pendingLoad = controller.upMinPQ.size() + controller.downMaxPQ.size();
      int distance = Math.abs(controller.getElevatorCar().getCurrentFloor() - floor);
      int score = (pendingLoad * 10) + distance; // Prioritize low load, break ties by closest floor

      if (score < minScore) {
        minScore = score;
        bestController = controller;
      }
    }
    return bestController;
  }
}

class ExternalDispatcher {
  private final ElevatorScheduler scheduler;

  public ExternalDispatcher(ElevatorScheduler scheduler) {
    this.scheduler = scheduler;
  }

  public ElevatorController submitExternalRequest(int floor, Direction direction) {
    System.out.println("[EXTERNAL BUTTON] Hallway button pressed on Floor " + floor + " going " + direction);
    ElevatorController assignedController = scheduler.assignElevator(floor, direction);
    System.out.println("  -> Assigned to Elevator #" + assignedController.getElevatorCar().getId());
    assignedController.submitRequest(floor, direction);
    return assignedController;
  }
}

public class ElevatorSystem {
  public static void main(String[] args) {
    System.out.println("==================================================");
    System.out.println("       ELEVATOR SYSTEM LLD - SDE-1 DEMO           ");
    System.out.println("==================================================\n");

    // 1. Create 2 Elevator Cars (for a 10-floor building) and their Controllers
    ElevatorCar car1 = new ElevatorCar(1, 10);
    ElevatorCar car2 = new ElevatorCar(2, 10);

    ElevatorController controller1 = new ElevatorController(car1);
    ElevatorController controller2 = new ElevatorController(car2);

    List<ElevatorController> controllers = new ArrayList<>(Arrays.asList(controller1, controller2));

    // 2. Initialize Scheduler & External Dispatcher
    ElevatorScheduler scheduler = new ElevatorScheduler(controllers);
    ExternalDispatcher dispatcher = new ExternalDispatcher(scheduler);

    // 3. Scenario 1: User on Floor 3 presses UP button -> boards and presses Floor 7
    ElevatorController assigned1 = dispatcher.submitExternalRequest(3, Direction.UP);
    assigned1.getElevatorCar().getInternalButtons().pressButton(3, 7, assigned1);

    // 4. Scenario 2: While Elevator 1 has load, User on Floor 2 presses UP button
    // Scheduler automatically assigns Elevator 2 because Elevator 1 is busy!
    ElevatorController assigned2 = dispatcher.submitExternalRequest(2, Direction.UP);
    assigned2.getElevatorCar().getInternalButtons().pressButton(2, 5, assigned2);

    // 5. Scenario 3: User on Floor 9 presses DOWN button -> boards at 9 and presses Floor 1
    ElevatorController assigned3 = dispatcher.submitExternalRequest(9, Direction.DOWN);
    assigned3.getElevatorCar().getInternalButtons().pressButton(9, 1, assigned3);

    System.out.println();

    // 6. Execute movement simulation for both elevators
    controller1.processRequests();
    controller2.processRequests();
  }
}
