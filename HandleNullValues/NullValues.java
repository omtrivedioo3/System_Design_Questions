package HandleNullValues;

/**
 * ============================================================================================================
 *                                  NULL OBJECT DESIGN PATTERN (Behavioral)
 * ============================================================================================================
 *
 * 1. WHAT EXACT PROBLEM DOES THIS PATTERN SOLVE?
 * ----------------------------------------------
 *   - THE PROBLEM (Without Null Object Pattern):
 *     When a method like `factory.getVehicle(type)` cannot find a valid object (e.g., unknown type "Truck"
 *     or `null` input), it traditionally returns `null`.
 *     Because of this, EVERY caller across the entire codebase is forced to write defensive null checks:
 *
 *         Vehicle v = factory.getVehicle(input);
 *         if (v != null) {                     // <-- Repetitive boilerplate everywhere!
 *             System.out.println(v.getTankCapacity());
 *         }
 *
 *     If a developer forgets `if (v != null)` in even ONE place, the program crashes at runtime with a
 *     `java.lang.NullPointerException` (NPE).
 *
 *   - THE SOLUTION (With Null Object Pattern):
 *     Instead of returning `null`, the factory returns a `NullObject` that implements the same `Vehicle`
 *     interface and provides safe default / no-op behavior (returning `0` capacity and `0` miles).
 *     Now, client code can treat real objects (`Car`, `Bike`) and missing objects (`NullObject`) uniformly
 *     without a single `if (vehicle != null)` check, completely eliminating `NullPointerException`!
 *
 *
 * 2. ARCHITECTURE & ROLES:
 * ------------------------
 *   [VehicleFactory] ---> returns ---> [interface Vehicle]
 *                                              ^
 *                         +--------------------+--------------------+
 *                         |                    |                    |
 *                      [Car]                [Bike]             [NullObject]
 *                (Real Behavior)      (Real Behavior)     (Safe Default: returns 0)
 * ============================================================================================================
 */

// 1. Target Interface: Defines the contract for both Real Objects and the Null Object
interface Vehicle {
    int getTankCapacity();

    int getMiles();
}

// 2. Concrete Real Object 1: Car
class Car implements Vehicle {
    private final int name;
    private final int fuelInTank;

    public Car(int name, int fuelInTank) {
        this.name = name;
        this.fuelInTank = fuelInTank;
    }

    @Override
    public int getTankCapacity() {
        return 40;
    }

    @Override
    public int getMiles() {
        return 15;
    }
}

// 3. Concrete Real Object 2: Bike
class Bike implements Vehicle {
    private final int name;
    private final int fuelInTank;

    public Bike(int name, int fuelInTank) {
        this.name = name;
        this.fuelInTank = fuelInTank;
    }

    @Override
    public int getTankCapacity() {
        return 10;
    }

    @Override
    public int getMiles() {
        return 45;
    }
}

// 4. Null Object: Replaces `null` reference with safe default ("do-nothing") behavior
class NullObject implements Vehicle {
    @Override
    public int getTankCapacity() {
        return 0; // Safe default instead of throwing NullPointerException
    }

    @Override
    public int getMiles() {
        return 0; // Safe default instead of throwing NullPointerException
    }
}

// 5. Factory Class: Always returns a non-null Vehicle (either a real Vehicle or NullObject)
class VehicleFactory {
    public Vehicle getVehicle(String type) {
        if ("Car".equalsIgnoreCase(type)) {
            return new Car(1, 10);
        }
        if ("Bike".equalsIgnoreCase(type)) {
            return new Bike(1, 10);
        }
        // Instead of `return null;`, return `new NullObject()`
        return new NullObject();
    }
}

// 6. Client Code: Calls methods directly WITHOUT needing `if (vehicle != null)` checks
public class NullValues {
    public static void main(String[] args) {
        VehicleFactory factory = new VehicleFactory();

        Vehicle car = factory.getVehicle("Car");
        Vehicle bike = factory.getVehicle("Bike");
        Vehicle unknownVehicle = factory.getVehicle("Truck"); // Returns NullObject
        Vehicle nullVehicle = factory.getVehicle(null);       // Returns NullObject

        printVehicleDetails("Car", car);
        printVehicleDetails("Bike", bike);
        printVehicleDetails("Truck (Unknown)", unknownVehicle);
        printVehicleDetails("Null Input", nullVehicle);
    }

    private static void printVehicleDetails(String label, Vehicle vehicle) {
        // Notice: NO `if (vehicle != null)` check is needed here!
        System.out.println(label
                + " -> Tank Capacity: " + vehicle.getTankCapacity()
                + "L | Mileage: " + vehicle.getMiles() + " MPG");
    }
}

