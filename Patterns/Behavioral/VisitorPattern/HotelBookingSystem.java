/*
 * =====================================================================================
 *                     VISITOR DESIGN PATTERN (BEHAVIORAL) - NOTES
 *               (SDE-1 LLD Interview Guide — Google / Amazon Style)
 * =====================================================================================
 *
 * WHAT
 *   Separates OPERATIONS (algorithms/behaviors) from the OBJECTS (data structures)
 *   on which they operate.
 *   -> Lets you add NEW operations to existing classes WITHOUT modifying those classes!
 *
 * REAL-LIFE / HOTEL BOOKING ANALOGY
 *   - Fixed Objects (Elements): A hotel has 3 room types: SingleRoom, DoubleRoom, DeluxeRoom.
 *     Room types rarely change.
 *   - Ever-Growing Operations (Visitors): Today we need Room Pricing. Tomorrow the business
 *     asks for Room Maintenance, Housekeeping, Fire Safety Audit, Festive Discount, etc.
 *   - Instead of editing SingleRoom, DoubleRoom, and DeluxeRoom every single week to add
 *     new methods, each operation becomes a "Visitor" (like a staff member visiting a room).
 *     The room simply opens the door (`accept(visitor)`), and the visitor performs its job!
 *
 * =====================================================================================
 *                  SDE-1 INTERVIEW FLOW & OBJECT IDENTIFICATION
 * =====================================================================================
 *
 * Step 1: How the Interviewer Phrases the Problem
 *   "We have a Hotel system with Single, Double, and Deluxe rooms. Right now we calculate
 *    price and do maintenance. In the future, product teams will frequently add new room
 *    operations (insurance valuation, cleaning checklists, tax audits). How do you design
 *    this so you don't touch the Room classes every time?"
 *
 * Step 2: Why the Naive Approach Fails (Bad LLD)
 *   class SingleRoom {
 *     void calculatePrice() { ... }
 *     void doMaintenance()  { ... }
 *     void cleanRoom()      { ... }   // <-- Every new feature forces modifying ALL room
 *     void auditFireSafety(){ ... }   //     classes! Violates Open-Closed Principle (OCP)
 *   }                                 //     and Single Responsibility Principle (SRP).
 *
 * Step 3: Object Identification (4 Parts of Visitor Pattern)
 *   1. Element Interface    -> RoomElement            : declares `accept(RoomVisitor v)`
 *   2. Concrete Elements    -> SingleRoom, DoubleRoom,
 *                              DeluxeRoom             : holds room data (`roomPrice`),
 *                                                       implements `v.visit(this)`
 *   3. Visitor Interface    -> RoomVisitor            : overloaded `visit(SingleRoom)`,
 *                                                       `visit(DoubleRoom)`, `visit(DeluxeRoom)`
 *   4. Concrete Visitors    -> RoomPricingVisitor,
 *                              RoomMaintenanceVisitor : contains the actual operation logic
 *                                                       for each room type!
 *
 * =====================================================================================
 *                THE SECRET INTERVIEW CONCEPT: "DOUBLE DISPATCH"
 * =====================================================================================
 *
 * Question: Why can't the client just call `visitor.visit(room)` directly?
 * Answer  : Because in Java:
 *           - Method OVERRIDING (`room.accept(v)`) is resolved at RUNTIME (Dynamic Binding).
 *           - Method OVERLOADING (`visitor.visit(SingleRoom)` vs `visit(DoubleRoom)`) is
 *             resolved at COMPILE TIME based on reference type!
 *
 *   If you have `RoomElement room = new SingleRoom();` and call `visitor.visit(room)`,
 *   the compiler only sees `RoomElement`, NOT `SingleRoom` -> Compile Error!
 *
 *   How `room.accept(visitor)` solves it in 2 hops (Double Dispatch):
 *     Hop 1 (Runtime)     : `room.accept(visitor)` -> Java jumps into `SingleRoom.accept()`
 *     Hop 2 (Compile-time): Inside `SingleRoom`, `this` is known to be `SingleRoom`, so
 *                           `visitor.visit(this)` calls `visit(SingleRoom)`!
 *
 * =====================================================================================
 *                                   UML DIAGRAM
 * =====================================================================================
 *
 *   +------------------------------------+           +----------------------------------------+
 *   |     <<interface>> RoomElement      |           |       <<interface>> RoomVisitor        |
 *   +------------------------------------+           +----------------------------------------+
 *   | + accept(visitor: RoomVisitor)     |. . . . . >| + visit(room: SingleRoom)              |
 *   +-----------------^------------------+   uses    | + visit(room: DoubleRoom)              |
 *                     | implements                   | + visit(room: DeluxeRoom)              |
 *        +------------+------------+                 +-------------------^--------------------+
 *        |            |            |                                     | implements
 *   +----+-------+ +--+---------+ ++-----------+            +------------+-------------+
 *   | SingleRoom | | DoubleRoom | | DeluxeRoom |            |                          |
 *   +------------+ +------------+ +------------+   +--------+-------------+ +----------+-------------+
 *   | + roomPrice| | + roomPrice| | + roomPrice|   |  RoomPricingVisitor  | | RoomMaintenanceVisitor |
 *   +------------+ +------------+ +------------+   +----------------------+ +------------------------+
 *   | + accept(v)| | + accept(v)| | + accept(v)|   | + visit(SingleRoom)  | | + visit(SingleRoom)    |
 *   |  {         | |  {         | |  {         |   | + visit(DoubleRoom)  | | + visit(DoubleRoom)    |
 *   |   v.visit( | |   v.visit( | |   v.visit( |   | + visit(DeluxeRoom)  | | + visit(DeluxeRoom)    |
 *   |     this); | |     this); | |     this); |   +----------------------+ +------------------------+
 *   |  }         | |  }         | |  }         |
 *   +------------+ +------------+ +------------+
 *
 * FLOW: singleRoom.accept(pricingVisitor)
 *
 *     Client calls: singleRoom.accept(pricingVisitor)
 *          |
 *     Inside SingleRoom.accept(): calls pricingVisitor.visit(this)  [where `this` = SingleRoom]
 *          |
 *     Inside RoomPricingVisitor.visit(SingleRoom): sets singleRoom.roomPrice = 1000
 *
 * =====================================================================================
 *       CONFUSION BUSTER: VISITOR vs STRATEGY vs DECORATOR (INTERVIEW FAVORITE!)
 * =====================================================================================
 *
 * Why do candidates confuse Visitor and Strategy?
 *   Both extract algorithms/behaviors into separate classes! Here is the exact difference:
 *
 * 1. VISITOR PATTERN -> "Many Object Types x Many Unrelated Operations (Matrix)"
 *    - You have MULTIPLE different element classes (`SingleRoom`, `DoubleRoom`, `DeluxeRoom`).
 *    - You want to run DIFFERENT unrelated operations (`Pricing`, `Maintenance`, `Cleaning`)
 *      across all of them without touching the room classes.
 *    - Structure: 1 Visitor class handles ALL element types via overloaded `visit(...)` methods.
 *
 *    VISITOR STRUCTURE (1 Operation class knows how to handle N Element types):
 *
 *          +------------------------+        visits        +--------------+
 *          |                        |--------------------->|  SingleRoom  |
 *          |   RoomPricingVisitor   |        visits        +--------------+
 *          |  (1 Operation for ALL  |--------------------->|  DoubleRoom  |
 *          |      room types)       |        visits        +--------------+
 *          |                        |--------------------->|  DeluxeRoom  |
 *          +------------------------+                      +--------------+
 *
 * -------------------------------------------------------------------------------------
 *
 * 2. STRATEGY PATTERN -> "1 Context Object x Swappable Ways to Do ONE Job"
 *    - You have ONE operation (e.g., `pay(amount)` or `sort(list)`), and you want to swap
 *      HOW that single operation is done at runtime (`UpiPayment`, `CreditCardPayment`).
 *    - The Context (`ShoppingCart`) HAS-A single Strategy and calls `strategy.pay(amount)`.
 *
 *    STRATEGY UML (1 Context swaps between N ways of doing the SAME operation):
 *
 *      +----------------------+   has-a    +-------------------------------+
 *      |     ShoppingCart     |----------->|  <<interface>> PaymentStrategy|
 *      +----------------------+            +-------------------------------+
 *      | - strategy           |            | + pay(amount: int)            |
 *      +----------------------+            +---------------^---------------+
 *      | + checkout() {       |                            | implements
 *      |    strategy.pay(amt);|               +------------+------------+
 *      | }                    |               |                         |
 *      +----------------------+     +---------+---------+     +---------+---------+
 *                                   |    UpiPayment     |     | CreditCardPayment |
 *                                   +-------------------+     +-------------------+
 *
 * -------------------------------------------------------------------------------------
 *
 * 3. DECORATOR PATTERN -> "Stacking Add-ons Dynamically (Russian Doll)"
 *    - Wraps an object inside another object of the SAME interface to stack features
 *      additively (e.g., `new ExtraCheese(new Mushroom(new MargheritaPizza()))`).
 *
 * -------------------------------------------------------------------------------------
 *
 * QUICK INTERVIEW DECISION MATRIX:
 * +-----------+----------------------------+------------------------------------------+
 * | Pattern   | Core Intent                | Use in Interview When...                 |
 * +-----------+----------------------------+------------------------------------------+
 * | Visitor   | Add NEW operations to a    | Element classes (SingleRoom, DoubleRoom) |
 * |           | fixed set of classes       | are fixed, but operations keep growing!  |
 * +-----------+----------------------------+------------------------------------------+
 * | Strategy  | Swap DIFFERENT algorithms  | There is 1 job (Payment/Sorting/Routing) |
 * |           | for the SAME single job    | and multiple ways to execute it.         |
 * +-----------+----------------------------+------------------------------------------+
 *
 * WHEN NOT TO USE VISITOR (Trade-off to mention in Interview for Bonus Points!):
 *   - If new Element types (e.g., `PresidentialSuiteRoom`, `VillaRoom`) are added frequently,
 *     Visitor is PAINFUL because you must update `RoomVisitor` interface and EVERY Concrete
 *     Visitor class!
 *   - Rule of thumb:
 *       * Operations change often, Element classes are stable -> Use VISITOR.
 *       * Element classes change often, Operations are stable -> Use normal Polymorphism.
 * =====================================================================================
 */
package Patterns.Behavioral.VisitorPattern;

/** 1. Element Interface: every room type must accept a RoomVisitor. */
interface RoomElement {
  void accept(RoomVisitor visitor);
}

/** 2a. Concrete Element: SingleRoom */
class SingleRoom implements RoomElement {
  public int roomPrice = 0;

  @Override
  public void accept(RoomVisitor visitor) {
    // Double Dispatch: `this` is known at compile time as SingleRoom
    visitor.visit(this);
  }
}

/** 2b. Concrete Element: DoubleRoom */
class DoubleRoom implements RoomElement {
  public int roomPrice = 0;

  @Override
  public void accept(RoomVisitor visitor) {
    visitor.visit(this);
  }
}

/** 2c. Concrete Element: DeluxeRoom */
class DeluxeRoom implements RoomElement {
  public int roomPrice = 0;

  @Override
  public void accept(RoomVisitor visitor) {
    visitor.visit(this);
  }
}

/** 3. Visitor Interface: declares a visit() method for each concrete RoomElement. */
interface RoomVisitor {
  void visit(SingleRoom singleRoom);

  void visit(DoubleRoom doubleRoom);

  void visit(DeluxeRoom deluxeRoom);
}

/**
 * 4a. Concrete Visitor 1: Calculates pricing for each room type.
 * All pricing logic lives here instead of being scattered across room classes.
 */
class RoomPricingVisitor implements RoomVisitor {
  @Override
  public void visit(SingleRoom singleRoom) {
    singleRoom.roomPrice = 1000;
    System.out.println("  [PricingVisitor] Computed SingleRoom price: Rs. " + singleRoom.roomPrice);
  }

  @Override
  public void visit(DoubleRoom doubleRoom) {
    doubleRoom.roomPrice = 2000;
    System.out.println("  [PricingVisitor] Computed DoubleRoom price: Rs. " + doubleRoom.roomPrice);
  }

  @Override
  public void visit(DeluxeRoom deluxeRoom) {
    deluxeRoom.roomPrice = 5000;
    System.out.println("  [PricingVisitor] Computed DeluxeRoom price: Rs. " + deluxeRoom.roomPrice);
  }
}

/**
 * 4b. Concrete Visitor 2: Performs maintenance on each room type.
 * Added WITHOUT modifying SingleRoom, DoubleRoom, or DeluxeRoom!
 */
class RoomMaintenanceVisitor implements RoomVisitor {
  @Override
  public void visit(SingleRoom singleRoom) {
    System.out.println("  [MaintenanceVisitor] Performing basic AC & plumbing check for SingleRoom.");
  }

  @Override
  public void visit(DoubleRoom doubleRoom) {
    System.out.println("  [MaintenanceVisitor] Performing twin-bed & balcony check for DoubleRoom.");
  }

  @Override
  public void visit(DeluxeRoom deluxeRoom) {
    System.out.println("  [MaintenanceVisitor] Performing jacuzzi, mini-bar & suite check for DeluxeRoom.");
  }
}

/** Client: demonstrates the Hotel Booking System using the Visitor Pattern. */
public class HotelBookingSystem {
  public static void main(String[] args) {
    // 1. Create the Room Elements
    RoomElement singleRoom = new SingleRoom();
    RoomElement doubleRoom = new DoubleRoom();
    RoomElement deluxeRoom = new DeluxeRoom();

    // 2. Operation 1: Calculate prices using RoomPricingVisitor
    System.out.println("=== 1. Running Room Pricing Visitor ===");
    RoomVisitor pricingVisitor = new RoomPricingVisitor();
    singleRoom.accept(pricingVisitor);
    doubleRoom.accept(pricingVisitor);
    deluxeRoom.accept(pricingVisitor);

    // 3. Operation 2: Run maintenance using RoomMaintenanceVisitor
    // Notice how we run a completely new operation on the same rooms without changing Room classes!
    System.out.println("\n=== 2. Running Room Maintenance Visitor ===");
    RoomVisitor maintenanceVisitor = new RoomMaintenanceVisitor();
    singleRoom.accept(maintenanceVisitor);
    doubleRoom.accept(maintenanceVisitor);
    deluxeRoom.accept(maintenanceVisitor);
  }
}
