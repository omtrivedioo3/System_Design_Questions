import java.util.*;

/**
 * ============================================================================================================
 *                                  PARKING LOT LLD - ARCHITECTURE & FLOW DIAGRAM
 * ============================================================================================================
 *
 * 1. CREATIONAL FLOW (Factory Pattern):
 * -------------------------------------
 *   [VehicleFactory]  ---creates--->  [abstract Vehicle]  <|-- [Car] / [Bike] / [Truck]
 *
 *
 * 2. ENTRY WORKFLOW (When a vehicle arrives at EntryGate):
 * --------------------------------------------------------
 *   [Driver / Vehicle]
 *          |
 *          v  (1) processVehicleEntry(vehicle)
 *     [EntryGate]  ------------------------>  [ParkingSpotManager]
 *          |                                           |
 *          |                                           v  (2) findAvailableSpot(type)
 *          |                                     [ParkingSpot] (marks isOccupied = true)
 *          |
 *          +---> (3) generateTicket(vehicle, spot) ---> Returns [Ticket] to Driver
 *                                                       (holds ticketId, vehicle, spot, entryTime)
 *
 *
 * 3. EXIT & PAYMENT WORKFLOW (Strategy Pattern x2):
 * -------------------------------------------------
 *   [Driver presents Ticket]
 *          |
 *          v  (1) processVehicleExit(ticket)
 *     [ExitGate]
 *          |
 *          +---> Step A: generateReceipt(ticket)
 *          |        |---> calls [CostCalculatorStrategy]  <|-- [HourlyCharge] / [MinutesCharge]
 *          |        +---> creates unpaid [Receipt] (amount, exitTime, isPaid = false)
 *          |
 *          +---> Step B: processPayment(receipt.getAmount())
 *          |        |---> calls [PaymentProcessor] ---> [PaymentStrategy]  <|-- [UPIPayment] / [CreditCardPayment]
 *          |        +---> if paid: receipt.markAsPaid() (isPaid = true)
 *          |
 *          +---> Step C: Free the Spot
 *                   +---> calls [ParkingSpotManager.markSpotUnoccupied(spot)] ---> [ParkingSpot.unparkVehicle()]
 *
 *
 * 4. CLASS RELATIONSHIPS SUMMARY (HAS-A vs IS-A):
 * -----------------------------------------------
 *   - Car / Bike / Truck          --IS-A-->  Vehicle
 *   - HourlyCharge / MinutesCharge --IS-A--> CostCalculatorStrategy
 *   - UPIPayment / CreditCard     --IS-A-->  PaymentStrategy
 *   - ParkingSpotManager          --HAS-A--> List<ParkingSpot> (bikeSpots, carSpots, truckSpots)
 *   - Ticket                      --HAS-A--> Vehicle, ParkingSpot
 *   - Receipt                     --HAS-A--> Ticket
 *   - EntryGate                   --HAS-A--> ParkingSpotManager
 *   - ExitGate                    --HAS-A--> ParkingSpotManager, CostCalculatorStrategy, PaymentProcessor
 *   - PaymentProcessor            --HAS-A--> PaymentStrategy
 *
 *
 * 5. DESIGN PATTERNS USED:
 * ------------------------
 *   1. Simple Factory Pattern : VehicleFactory creates Car, Bike, or Truck without exposing instantiation logic.
 *   2. Strategy Pattern #1    : PaymentStrategy (UPIPayment, CreditCardPayment) injected into PaymentProcessor.
 *   3. Strategy Pattern #2    : CostCalculatorStrategy (HourlyCharge, MinutesCharge) used for dynamic fee calculation.
 *   4. Facade Pattern         : EntryGate & ExitGate provide simplified single-method workflows over spot, ticket,
 *                               receipt, cost calculation, and payment subsystems.
 * ============================================================================================================
 */

enum VehicleType {
    BIKE,
    CAR,
    TRUCK
}

// ============================================================================
// DESIGN PATTERN 1: SIMPLE FACTORY PATTERN (Creational)
// - Abstract Product : Vehicle
// - Concrete Products: Car, Bike, Truck
// - Factory          : VehicleFactory (centralizes object creation based on VehicleType)
// ============================================================================
abstract class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleType getType() {
        return type;
    }
}

class Car extends Vehicle {
    public Car(String licensePlate) {
        super(licensePlate, VehicleType.CAR);
    }
}

class Bike extends Vehicle {
    public Bike(String licensePlate) {
        super(licensePlate, VehicleType.BIKE);
    }
}

class Truck extends Vehicle {
    public Truck(String licensePlate) {
        super(licensePlate, VehicleType.TRUCK);
    }
}

// Factory Class: Encapsulates Vehicle instantiation logic
class VehicleFactory {
    public static Vehicle createVehicle(VehicleType type, String licensePlate) {
        switch (type) {
            case CAR:
                return new Car(licensePlate);
            case BIKE:
                return new Bike(licensePlate);
            case TRUCK:
                return new Truck(licensePlate);
            default:
                throw new IllegalArgumentException("Unsupported vehicle type: " + type);
        }
    }
}

class ParkingSpot {
    private final int spotNumber;
    private final VehicleType type;
    private boolean isOccupied;
    private Vehicle parkedVehicle;

    public ParkingSpot(int spotNumber, VehicleType type) {
        this.spotNumber = spotNumber;
        this.type = type;
    }

    public int getSpotNumber() {
        return spotNumber;
    }

    public VehicleType getType() {
        return type;
    }

    public boolean isOccupied() {
        return isOccupied;
    }

    public Vehicle getParkedVehicle() {
        return parkedVehicle;
    }

    public void parkVehicle(Vehicle vehicle) {
        this.parkedVehicle = vehicle;
        this.isOccupied = true;
    }

    public void unparkVehicle() {
        this.parkedVehicle = null;
        this.isOccupied = false;
    }
}

class ParkingSpotManager {
    private final List<ParkingSpot> bikeSpots;
    private final List<ParkingSpot> carSpots;
    private final List<ParkingSpot> truckSpots;

    public ParkingSpotManager(List<ParkingSpot> bikeSpots, List<ParkingSpot> carSpots, List<ParkingSpot> truckSpots) {
        this.bikeSpots = bikeSpots;
        this.carSpots = carSpots;
        this.truckSpots = truckSpots;
    }

    public ParkingSpot findAvailableSpot(VehicleType type) {
        switch (type) {
            case BIKE:
                return bikeSpots.stream().filter(spot -> !spot.isOccupied()).findFirst().orElse(null);
            case CAR:
                return carSpots.stream().filter(spot -> !spot.isOccupied()).findFirst().orElse(null);
            case TRUCK:
                return truckSpots.stream().filter(spot -> !spot.isOccupied()).findFirst().orElse(null);
            default:
                return null;
        }
    }

    public void markSpotUnoccupied(ParkingSpot spot) {
        spot.unparkVehicle();
    }
}

class Ticket {
    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final long entryTime;
    private long exitTime;

    public Ticket(String ticketId, Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.spot = spot;
        this.entryTime = System.currentTimeMillis();
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public long getEntryTime() {
        return entryTime;
    }

    public long getExitTime() {
        return exitTime;
    }

    public void setExitTime(long exitTime) {
        this.exitTime = exitTime;
    }
}

class Receipt {
    private final String receiptId;
    private final Ticket ticket;
    private final double amount;
    private final long exitTime;
    private boolean isPaid; // Not final, because it starts false and becomes true after payment!

    public Receipt(Ticket ticket, double amount, long exitTime) {
        this.receiptId = "RCP-" + UUID.randomUUID().toString().substring(0, 6);
        this.ticket = ticket;
        this.amount = amount;
        this.exitTime = exitTime;
        this.isPaid = false; // Generated as unpaid first
    }

    public void markAsPaid() {
        this.isPaid = true;
    }

    public double getAmount() {
        return amount;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public boolean isPaid() {
        return isPaid;
    }
}

// ============================================================================
// DESIGN PATTERN 2: STRATEGY PATTERN FOR PAYMENT (Behavioral)
// - Strategy Interface : PaymentStrategy
// - Concrete Strategies: CreditCardPayment, UPIPayment
// - Context Class      : PaymentProcessor (allows switching payment modes at runtime)
// ============================================================================
interface PaymentStrategy {
    boolean pay(double amount);
}

// Concrete Strategy 1: Credit Card Payment
class CreditCardPayment implements PaymentStrategy {
    private final String cardNumber;
    private final String cardHolderName;

    public CreditCardPayment(String cardNumber, String cardHolderName) {
        this.cardNumber = cardNumber;
        this.cardHolderName = cardHolderName;
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("Paid Rs. " + amount + " via Credit Card (" + cardHolderName + ")");
        return true;
    }
}

// Concrete Strategy 2: UPI Payment
class UPIPayment implements PaymentStrategy {
    private final String upiId;

    public UPIPayment(String upiId) {
        this.upiId = upiId;
    }

    @Override
    public boolean pay(double amount) {
        System.out.println("Paid Rs. " + amount + " via UPI (" + upiId + ")");
        return true;
    }
}

// Context Class: Delegates payment execution to the chosen PaymentStrategy
class PaymentProcessor {
    private PaymentStrategy paymentStrategy;

    public PaymentProcessor(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    // Allows changing the payment method dynamically at runtime
    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public boolean processPayment(double amount) {
        if (paymentStrategy == null) {
            throw new IllegalStateException("Payment strategy not selected!");
        }
        return paymentStrategy.pay(amount);
    }
}

// ============================================================================
// DESIGN PATTERN 3: FACADE PATTERN FOR ENTRY WORKFLOW (Structural)
// - EntryGate hides the complexity of finding a spot, parking the vehicle,
//   and generating a Ticket behind a single method: processVehicleEntry()
// ============================================================================
class EntryGate {
    private final ParkingSpotManager spotManager;

    // Constructor only needs the manager (no Ticket passed here!)
    public EntryGate(ParkingSpotManager spotManager) {
        this.spotManager = spotManager;
    }

    // Returns the newly generated Ticket to the driver
    public Ticket processVehicleEntry(Vehicle vehicle) {
        ParkingSpot spot = spotManager.findAvailableSpot(vehicle.getType());
        if (spot == null) {
            throw new RuntimeException("No available spots for " + vehicle.getType());
        }

        // 1. Mark spot as occupied
        spot.parkVehicle(vehicle);

        // 2. Generate and return a brand new ticket for this specific car & spot
        Ticket ticket = generateTicket(vehicle, spot);
        System.out.println("[ENTRY] Parked " + vehicle.getType() + " (" + vehicle.getLicensePlate()
                + ") at Spot #" + spot.getSpotNumber() + " | Ticket ID: " + ticket.getTicketId());
        return ticket;
    }

    // Helper method: takes vehicle & spot, returns a new Ticket
    private Ticket generateTicket(Vehicle vehicle, ParkingSpot spot) {
        String ticketId = "TKT-" + UUID.randomUUID().toString().substring(0, 6);
        return new Ticket(ticketId, vehicle, spot);
    }
}

// ============================================================================
// DESIGN PATTERN 4: STRATEGY PATTERN FOR COST CALCULATION (Behavioral)
// - Strategy Interface : CostCalculatorStrategy
// - Concrete Strategies: HourlyCharge, MinutesCharge
// - Context Class      : CostCalculatorStrategyManager / ExitGate
// ============================================================================
interface CostCalculatorStrategy {
    double calculateCost(Ticket ticket);
}

// Concrete Strategy 1: Hourly pricing calculation
class HourlyCharge implements CostCalculatorStrategy {

    @Override
    public double calculateCost(Ticket ticket) {
        long entryTime = ticket.getEntryTime();
        long exitTime = ticket.getExitTime() > 0 ? ticket.getExitTime() : System.currentTimeMillis();
        long diffInHours = Math.max(1, (exitTime - entryTime) / (1000 * 60 * 60)); // Minimum 1 hour charge
        return diffInHours * 100.0;
    }
}

// Concrete Strategy 2: Per-minute pricing calculation
class MinutesCharge implements CostCalculatorStrategy {
    @Override
    public double calculateCost(Ticket ticket) {
        long entryTime = ticket.getEntryTime();
        long exitTime = ticket.getExitTime() > 0 ? ticket.getExitTime() : System.currentTimeMillis();
        long diffInMinutes = Math.max(1, (exitTime - entryTime) / (1000 * 60)); // Minimum 1 minute charge
        return diffInMinutes * 50.0;
    }
}

// Context Class: Manages and executes the active CostCalculatorStrategy
class CostCalculatorStrategyManager {
    private CostCalculatorStrategy costCalculatorStrategy;

    public CostCalculatorStrategyManager(CostCalculatorStrategy costCalculatorStrategy) {
        this.costCalculatorStrategy = costCalculatorStrategy;
    }

    public void setCostCalculatorStrategy(CostCalculatorStrategy costCalculatorStrategy) {
        this.costCalculatorStrategy = costCalculatorStrategy;
    }

    public double calculateCost(Ticket ticket) {
        return costCalculatorStrategy.calculateCost(ticket);
    }
}

// ============================================================================
// DESIGN PATTERN 5: FACADE PATTERN FOR EXIT WORKFLOW (Structural)
// - ExitGate coordinates CostCalculatorStrategy, Receipt generation,
//   PaymentProcessor, and ParkingSpotManager behind processVehicleExit()
// ============================================================================
class ExitGate {
    private final PaymentProcessor paymentProcessor;
    private final ParkingSpotManager spotManager;
    private final CostCalculatorStrategy costCalculator;

    public ExitGate(
            PaymentProcessor paymentProcessor,
            ParkingSpotManager spotManager,
            CostCalculatorStrategy costCalculator) {
        this.paymentProcessor = paymentProcessor;
        this.spotManager = spotManager;
        this.costCalculator = costCalculator;
    }

    // Step 1: Generate Receipt first (calculates cost & sets exit time)
    public Receipt generateReceipt(Ticket ticket) {
        long exitTime = System.currentTimeMillis();
        ticket.setExitTime(exitTime);

        double charge = costCalculator.calculateCost(ticket);
        return new Receipt(ticket, charge, exitTime);
    }

    // Step 2: Process payment for the generated Receipt, then unpark the vehicle
    public Receipt processVehicleExit(Ticket ticket) {
        // 1. First generate the receipt
        Receipt receipt = generateReceipt(ticket);
        System.out.println("[RECEIPT GENERATED] ID: " + receipt.getReceiptId()
                + " | Vehicle: " + ticket.getVehicle().getLicensePlate()
                + " | Amount Due: Rs. " + receipt.getAmount());

        // 2. Process payment using the receipt's calculated amount
        boolean paymentSuccess = paymentProcessor.processPayment(receipt.getAmount());

        if (paymentSuccess) {
            receipt.markAsPaid();
            // 3. Free the parking spot only after payment succeeds
            spotManager.markSpotUnoccupied(ticket.getSpot());
            System.out.println("[EXIT] Vehicle " + ticket.getVehicle().getLicensePlate()
                    + " exited successfully. Spot #" + ticket.getSpot().getSpotNumber() + " is now free.\n");
        } else {
            throw new RuntimeException("Payment failed! Gate cannot open.");
        }

        return receipt;
    }
}

public class ParkingLot {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("      PARKING LOT LLD - END-TO-END TEST RUN       ");
        System.out.println("==================================================\n");

        // 1. Initialize Parking Spots (1 Bike spot, 2 Car spots, 1 Truck spot)
        List<ParkingSpot> bikeSpots = Arrays.asList(new ParkingSpot(101, VehicleType.BIKE));
        List<ParkingSpot> carSpots = Arrays.asList(
                new ParkingSpot(201, VehicleType.CAR),
                new ParkingSpot(202, VehicleType.CAR));
        List<ParkingSpot> truckSpots = Arrays.asList(new ParkingSpot(301, VehicleType.TRUCK));

        ParkingSpotManager spotManager = new ParkingSpotManager(bikeSpots, carSpots, truckSpots);

        // 2. Initialize Entry Gate & Exit Gate (with UPI Payment & Hourly Charge initially)
        EntryGate entryGate = new EntryGate(spotManager);
        PaymentProcessor paymentProcessor = new PaymentProcessor(new UPIPayment("om@okicici"));
        ExitGate exitGate = new ExitGate(paymentProcessor, spotManager, new HourlyCharge());

        // 3. Create Vehicles using Factory Pattern (VehicleFactory)
        Vehicle car1 = VehicleFactory.createVehicle(VehicleType.CAR, "MH-12-AB-1111");
        Vehicle car2 = VehicleFactory.createVehicle(VehicleType.CAR, "DL-01-XY-2222");
        Vehicle bike1 = VehicleFactory.createVehicle(VehicleType.BIKE, "KA-05-BK-3333");
        Vehicle car3 = VehicleFactory.createVehicle(VehicleType.CAR, "GJ-01-ZZ-9999");

        Ticket t1 = entryGate.processVehicleEntry(car1);
        Ticket t2 = entryGate.processVehicleEntry(car2);
        Ticket t3 = entryGate.processVehicleEntry(bike1);

        // 4. Test Parking Lot Full edge case for Car 3 (only 2 car spots exist!)
        System.out.println("\n--- Testing Full Lot Edge Case ---");
        try {
            entryGate.processVehicleEntry(car3);
        } catch (RuntimeException e) {
            System.out.println("[REJECTED] Car 3 (" + car3.getLicensePlate() + "): " + e.getMessage() + "\n");
        }

        // 5. Car 1 Exits (Generates Receipt -> Pays via UPI -> Frees Spot #201)
        System.out.println("--- Processing Exit for Car 1 (UPI + Hourly Charge) ---");
        Receipt r1 = exitGate.processVehicleExit(t1);

        // 6. Now Car 3 can enter because Spot #201 is free!
        System.out.println("--- Car 3 Retries Entry After Spot #201 Freed ---");
        Ticket t4 = entryGate.processVehicleEntry(car3);

        // 7. Switch Payment Strategy to Credit Card at runtime & Exit Bike 1
        System.out.println("\n--- Switching Payment Strategy to Credit Card for Bike 1 ---");
        paymentProcessor.setPaymentStrategy(new CreditCardPayment("4111-2222-3333-4444", "Om Trivedi"));
        Receipt r2 = exitGate.processVehicleExit(t3);
    }
}