package CarRentalSystem;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * ============================================================================================================
 * CAR RENTAL SYSTEM (ZOOMCAR) LLD - UML & ARCHITECTURE (SDE-1)
 * ============================================================================================================
 *
 * 1. UML CLASS DIAGRAM:
 * ---------------------
 * +------------------------------------+ +------------------------------------+
 * | CarRentalSystem | | Store |
 * +------------------------------------+ +------------------------------------+
 * | - stores : List<Store> | *------> | - storeId : String |
 * | - users : List<User> | | - location : Location |
 * +------------------------------------+ | - vehicleManager : VehicleManager |
 * | - reservationMgr : ReservationMgr |
 * +------------------------------------+
 * |
 * +---------------------------------------------+
 * | manages
 * v
 * +------------------------------------+ +------------------------------------+
 * | Reservation | | Vehicle |
 * +------------------------------------+ +------------------------------------+
 * | - reservationId : String | | - vehicleId : String |
 * | - user : User | -------> | - vehicleType : VehicleType |
 * | - vehicle : Vehicle | reserves | - make, model : String |
 * | - pickupDateTime : LocalDateTime | | - pricePerDay : double |
 * | - returnDateTime : LocalDateTime | | - status : VehicleStatus |
 * | - totalPrice : double | +------------------------------------+
 * | - status : ReservationStatus |
 * +------------------------------------+
 * |
 * | generates
 * v
 * +------------------------------------+ +------------------------------------+
 * | Bill | | <<interface>> |
 * +------------------------------------+ | PaymentStrategy |
 * | - billId : String | +------------------------------------+
 * | - reservation : Reservation | -------> | + pay(amount: double) : boolean |
 * | - amount : double | paid via +------------------------------------+
 * | - isPaid : boolean | ^ ^
 * +------------------------------------+ | |
 * [UPIPayment] [CreditCardPayment]
 *
 *
 * 2. END-TO-END BOOKING WORKFLOW:
 * -------------------------------
 * Step 1 (Search): User searches Store for AVAILABLE vehicles by VehicleType
 * (`store.searchAvailableVehicles(CAR)`).
 * Step 2 (Reserve): User selects a Vehicle & Dates ->
 * `ReservationManager.bookVehicle(...)`
 * - Calculates duration in days (`ChronoUnit.DAYS.between(pickup, return)`).
 * - Computes `totalPrice = days * vehicle.getPricePerDay()`.
 * - Marks `vehicle.setStatus(VehicleStatus.BOOKED)`.
 * Step 3 (Billing): Generates a `Bill` linked to the `Reservation`.
 * Step 4 (Payment): Processes payment using `PaymentStrategy` (`UPIPayment` or
 * `CreditCardPayment`).
 * Step 5 (Return): When trip completes (or cancels), `completeReservation()`
 * marks Vehicle `AVAILABLE` again!
 * ============================================================================================================
 */

enum VehicleType {
  CAR,
  BIKE,
  TRUCK
}

enum VehicleStatus {
  AVAILABLE,
  BOOKED,
  MAINTENANCE
}

enum ReservationStatus {
  SCHEDULED,
  IN_PROGRESS,
  COMPLETED,
  CANCELLED
}

class Vehicle {
  private final String vehicleId;
  private final VehicleType vehicleType;
  private final String make;
  private final String model;
  private final int year;
  private final double pricePerDay;
  private VehicleStatus status;

  public Vehicle(
      String vehicleId,
      VehicleType vehicleType,
      String make,
      String model,
      int year,
      double pricePerDay) {
    this.vehicleId = vehicleId;
    this.vehicleType = vehicleType;
    this.make = make;
    this.model = model;
    this.year = year;
    this.pricePerDay = pricePerDay;
    this.status = VehicleStatus.AVAILABLE;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public VehicleType getVehicleType() {
    return vehicleType;
  }

  public String getMake() {
    return make;
  }

  public String getModel() {
    return model;
  }

  public int getYear() {
    return year;
  }

  public double getPricePerDay() {
    return pricePerDay;
  }

  public VehicleStatus getStatus() {
    return status;
  }

  public void setStatus(VehicleStatus status) {
    this.status = status;
  }

  public boolean isAvailable() {
    return this.status == VehicleStatus.AVAILABLE;
  }
}

class VehicleManager {
  private final List<Vehicle> vehicles;

  public VehicleManager() {
    this.vehicles = new ArrayList<>();
  }

  public void addVehicle(Vehicle vehicle) {
    vehicles.add(vehicle);
  }

  public void removeVehicle(String vehicleId) {
    vehicles.removeIf(v -> v.getVehicleId().equals(vehicleId));
  }

  public List<Vehicle> searchAvailableVehicles(VehicleType type) {
    List<Vehicle> available = new ArrayList<>();
    for (Vehicle v : vehicles) {
      if (v.getVehicleType() == type && v.isAvailable()) {
        available.add(v);
      }
    }
    return available;
  }

  public List<Vehicle> getVehicles() {
    return vehicles;
  }
}

class User {
  private final String userId;
  private final String name;
  private final String drivingLicenseNumber;

  public User(String userId, String name, String drivingLicenseNumber) {
    this.userId = userId;
    this.name = name;
    this.drivingLicenseNumber = drivingLicenseNumber;
  }

  public String getUserId() {
    return userId;
  }

  public String getName() {
    return name;
  }

  public String getDrivingLicenseNumber() {
    return drivingLicenseNumber;
  }
}

class Location {
  private final String locationId;
  private final String city;
  private final String address;

  public Location(String locationId, String city, String address) {
    this.locationId = locationId;
    this.city = city;
    this.address = address;
  }

  public String getLocationId() {
    return locationId;
  }

  public String getCity() {
    return city;
  }

  public String getAddress() {
    return address;
  }
}

class Reservation {
  private final String reservationId;
  private final User user;
  private final Vehicle vehicle;
  private final Location pickupLocation;
  private final Location returnLocation;
  private final LocalDateTime pickupDateTime;
  private final LocalDateTime returnDateTime;
  private final double totalPrice;
  private ReservationStatus status;

  public Reservation(
      String reservationId,
      User user,
      Vehicle vehicle,
      Location pickupLocation,
      Location returnLocation,
      LocalDateTime pickupDateTime,
      LocalDateTime returnDateTime) {
    this.reservationId = reservationId;
    this.user = user;
    this.vehicle = vehicle;
    this.pickupLocation = pickupLocation;
    this.returnLocation = returnLocation;
    this.pickupDateTime = pickupDateTime;
    this.returnDateTime = returnDateTime;

    // Automatically calculate rental days (minimum 1 day)
    long days = Math.max(1, ChronoUnit.DAYS.between(pickupDateTime, returnDateTime));
    this.totalPrice = days * vehicle.getPricePerDay();
    this.status = ReservationStatus.SCHEDULED;
  }

  public String getReservationId() {
    return reservationId;
  }

  public User getUser() {
    return user;
  }

  public Vehicle getVehicle() {
    return vehicle;
  }

  public Location getPickupLocation() {
    return pickupLocation;
  }

  public Location getReturnLocation() {
    return returnLocation;
  }

  public LocalDateTime getPickupDateTime() {
    return pickupDateTime;
  }

  public LocalDateTime getReturnDateTime() {
    return returnDateTime;
  }

  public double getTotalPrice() {
    return totalPrice;
  }

  public ReservationStatus getStatus() {
    return status;
  }

  public void setStatus(ReservationStatus status) {
    this.status = status;
  }
}

class ReservationManager {
  private final List<Reservation> reservations;

  public ReservationManager() {
    this.reservations = new ArrayList<>();
  }

  public Reservation bookVehicle(
      User user,
      Vehicle vehicle,
      Location pickupLocation,
      Location returnLocation,
      LocalDateTime pickupDateTime,
      LocalDateTime returnDateTime) {
    if (!vehicle.isAvailable()) {
      throw new IllegalStateException(
          "Vehicle " + vehicle.getMake() + " " + vehicle.getModel() + " is currently unavailable!");
    }

    String resId = "RES-" + UUID.randomUUID().toString().substring(0, 6);
    Reservation reservation =
        new Reservation(
            resId, user, vehicle, pickupLocation, returnLocation, pickupDateTime, returnDateTime);
    reservations.add(reservation);
    vehicle.setStatus(VehicleStatus.BOOKED);

    System.out.println(
        "[BOOKING CONFIRMED] ID: "
            + resId
            + " | User: "
            + user.getName()
            + " | Vehicle: "
            + vehicle.getMake()
            + " "
            + vehicle.getModel()
            + " | Total Cost: Rs. "
            + reservation.getTotalPrice());
    return reservation;
  }

  public void completeReservation(String reservationId) {
    for (Reservation res : reservations) {
      if (res.getReservationId().equals(reservationId)) {
        res.setStatus(ReservationStatus.COMPLETED);
        res.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        System.out.println(
            "[VEHICLE RETURNED] Reservation "
                + reservationId
                + " completed. Vehicle "
                + res.getVehicle().getModel()
                + " is now AVAILABLE again.");
        return;
      }
    }
  }

  public void cancelReservation(String reservationId) {
    for (Reservation res : reservations) {
      if (res.getReservationId().equals(reservationId)) {
        res.setStatus(ReservationStatus.CANCELLED);
        res.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        System.out.println(
            "[CANCELLED] Reservation "
                + reservationId
                + " cancelled. Vehicle "
                + res.getVehicle().getModel()
                + " is now AVAILABLE.");
        return;
      }
    }
  }

  public List<Reservation> getReservationsByUser(User user) {
    List<Reservation> userReservations = new ArrayList<>();
    for (Reservation reservation : reservations) {
      if (reservation.getUser().getUserId().equals(user.getUserId())) {
        userReservations.add(reservation);
      }
    }
    return userReservations;
  }
}

class Bill {
  private final String billId;
  private final Reservation reservation;
  private final double amount;
  private boolean isPaid;

  public Bill(Reservation reservation) {
    this.billId = "BILL-" + UUID.randomUUID().toString().substring(0, 6);
    this.reservation = reservation;
    this.amount = reservation.getTotalPrice();
    this.isPaid = false;
  }

  public void markPaid() {
    this.isPaid = true;
  }

  public String getBillId() {
    return billId;
  }

  public double getAmount() {
    return amount;
  }

  public boolean isPaid() {
    return isPaid;
  }
}

// Strategy Pattern for Payment Processing
interface PaymentStrategy {
  boolean pay(double amount);
}

class UPIPayment implements PaymentStrategy {
  private final String upiId;

  public UPIPayment(String upiId) {
    this.upiId = upiId;
  }

  @Override
  public boolean pay(double amount) {
    System.out.println("  -> Paid Rs. " + amount + " via UPI (" + upiId + ")");
    return true;
  }
}

class CreditCardPayment implements PaymentStrategy {
  private final String cardNumber;
  private final String holderName;

  public CreditCardPayment(String cardNumber, String holderName) {
    this.cardNumber = cardNumber;
    this.holderName = holderName;
  }

  @Override
  public boolean pay(double amount) {
    System.out.println("  -> Paid Rs. " + amount + " via Credit Card (" + holderName + ")");
    return true;
  }
}

class Store {
  private final String storeId;
  private final Location location;
  private final VehicleManager vehicleManager;
  private final ReservationManager reservationManager;

  public Store(String storeId, Location location) {
    this.storeId = storeId;
    this.location = location;
    this.vehicleManager = new VehicleManager();
    this.reservationManager = new ReservationManager();
  }

  public String getStoreId() {
    return storeId;
  }

  public Location getLocation() {
    return location;
  }

  public VehicleManager getVehicleManager() {
    return vehicleManager;
  }

  public ReservationManager getReservationManager() {
    return reservationManager;
  }
}

public class CarRentalSystem {
  private final List<Store> stores = new ArrayList<>();
  private final List<User> users = new ArrayList<>();

  public void addStore(Store store) {
    stores.add(store);
  }

  public void addUser(User user) {
    users.add(user);
  }

  public Store findStoreByCity(String city) {
    for (Store store : stores) {
      if (store.getLocation().getCity().equalsIgnoreCase(city)) {
        return store;
      }
    }
    return null;
  }

  public static void main(String[] args) {
    System.out.println("==================================================");
    System.out.println("     CAR RENTAL SYSTEM (ZOOMCAR) - SDE-1 DEMO     ");
    System.out.println("==================================================\n");

    CarRentalSystem rentalSystem = new CarRentalSystem();

    // 1. Setup Store & Location in Bengaluru
    Location blrLocation = new Location("LOC-1", "Bengaluru", "MG Road, Indiranagar");
    Store blrStore = new Store("STR-BLR-01", blrLocation);
    rentalSystem.addStore(blrStore);

    // 2. Add Vehicles to the Store Inventory
    Vehicle v1 = new Vehicle("KA-01-AA-1111", VehicleType.CAR, "Toyota", "Fortuner", 2024, 3500.0);
    Vehicle v2 = new Vehicle("KA-01-BB-2222", VehicleType.CAR, "Honda", "City", 2023, 2000.0);
    Vehicle v3 = new Vehicle("KA-01-CC-3333", VehicleType.BIKE, "Royal Enfield", "Classic 350", 2024, 800.0);

    blrStore.getVehicleManager().addVehicle(v1);
    blrStore.getVehicleManager().addVehicle(v2);
    blrStore.getVehicleManager().addVehicle(v3);

    // 3. Register Users
    User user1 = new User("USR-101", "Om Trivedi", "DL-IND-998877");
    User user2 = new User("USR-102", "Gaurav Sharma", "DL-IND-112233");
    rentalSystem.addUser(user1);
    rentalSystem.addUser(user2);

    // 4. User 1 searches for available CARs in Bengaluru
    System.out.println("--- Step 1: Searching Available CARs in Bengaluru ---");
    List<Vehicle> availableCars = blrStore.getVehicleManager().searchAvailableVehicles(VehicleType.CAR);
    for (Vehicle car : availableCars) {
      System.out.println("  Available: " + car.getMake() + " " + car.getModel() + " (Rs. " + car.getPricePerDay() + "/day)");
    }

    // 5. User 1 books the Toyota Fortuner for 3 Days
    System.out.println("\n--- Step 2: Om Books Toyota Fortuner for 3 Days ---");
    LocalDateTime pickupTime = LocalDateTime.now();
    LocalDateTime returnTime = pickupTime.plusDays(3);

    Reservation res1 =
        blrStore
            .getReservationManager()
            .bookVehicle(user1, v1, blrLocation, blrLocation, pickupTime, returnTime);

    // 6. Generate Bill & Process Payment using Strategy Pattern (UPI)
    System.out.println("\n--- Step 3: Billing & Payment ---");
    Bill bill1 = new Bill(res1);
    System.out.println("  Generated Bill ID: " + bill1.getBillId() + " | Amount Due: Rs. " + bill1.getAmount());
    PaymentStrategy upi = new UPIPayment("om@okicici");
    if (upi.pay(bill1.getAmount())) {
      bill1.markPaid();
      System.out.println("  Bill Status: PAID (" + bill1.isPaid() + ")");
    }

    // 7. Edge Case: User 2 tries to book the SAME Toyota Fortuner while it is already booked
    System.out.println("\n--- Step 4: Testing Double-Booking Edge Case ---");
    try {
      blrStore
          .getReservationManager()
          .bookVehicle(user2, v1, blrLocation, blrLocation, pickupTime, returnTime);
    } catch (IllegalStateException e) {
      System.out.println("  [REJECTED] Gaurav's booking failed: " + e.getMessage());
    }

    // 8. User 1 returns the vehicle -> Vehicle becomes AVAILABLE again
    System.out.println("\n--- Step 5: Vehicle Return & Re-booking ---");
    blrStore.getReservationManager().completeReservation(res1.getReservationId());

    // Now User 2 can book the Toyota Fortuner for 2 days using Credit Card!
    Reservation res2 =
        blrStore
            .getReservationManager()
            .bookVehicle(user2, v1, blrLocation, blrLocation, pickupTime, pickupTime.plusDays(2));
    Bill bill2 = new Bill(res2);
    PaymentStrategy creditCard = new CreditCardPayment("4111-2222-3333-4444", "Gaurav Sharma");
    creditCard.pay(bill2.getAmount());
  }
}
