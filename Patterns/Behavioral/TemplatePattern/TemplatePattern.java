/*
 * =====================================================================================
 *                 TEMPLATE METHOD DESIGN PATTERN (BEHAVIORAL) - NOTES
 *               (SDE-1 LLD Interview Guide — Google / Amazon Style)
 * =====================================================================================
 *
 * WHAT
 *   Defines the SKELETON (fixed sequence of steps) of an algorithm in a base `abstract`
 *   class, while allowing subclasses to override specific steps WITHOUT changing the
 *   overall order of execution.
 *
 * REAL-LIFE / FINTECH ANALOGY (UPI Payment Flow: PayToFriend vs PayToMerchant)
 *   Every UPI money transfer MUST follow the exact same 4 steps in strict order:
 *     Step 1: Validate Request (check UPI PIN, account status)
 *     Step 2: Debit Amount from Sender
 *     Step 3: Calculate Platform Fees (0% for Friend, 2% MDR for Merchant)
 *     Step 4: Credit Remaining Amount to Receiver
 *
 *   How each step works differs between `PayToFriend` and `PayToMerchant`,
 *   but NO subclass is ever allowed to skip Step 1 or run Step 4 before Step 2!
 *   So the base class locks the sequence inside a `public final void sendMoney()` method.
 *
 * =====================================================================================
 *                  SDE-1 INTERVIEW FLOW & OBJECT IDENTIFICATION
 * =====================================================================================
 *
 * Step 1: How the Interviewer Phrases the Problem
 *   "We have multiple workflows (e.g., Payment processing, Data file ingestion CSV/JSON/XML,
 *    or Order fulfillment) that all follow the exact same multi-step pipeline, but individual
 *    steps behave differently per type. How do you guarantee every workflow executes the
 *    steps in the exact same order without code duplication?"
 *
 * Step 2: Why the Naive Approach Fails (Bad LLD)
 *   If `PayToFriend` and `PayToMerchant` each write their own `sendMoney()` pipeline:
 *     1. Code Duplication: Every class repeats the 4-step orchestration logic.
 *     2. Bug Risk: A developer writing a new `PayInternational` class might accidentally
 *        call `creditAmount()` BEFORE `validateRequest()` or `debitAmount()`!
 *
 * Step 3: Object Identification (2 Core Parts + 3 Method Types)
 *   1. Abstract Class (`PaymentFlow`)
 *      - Abstract Step Methods : `validateRequest()`, `debitAmount()`, `calculateFees()`,
 *                                `creditAmount()` -> subclasses MUST implement these.
 *      - Hook Method (Optional): `sendNotification()` -> has a default body in base class;
 *                                subclasses CAN override if needed, or leave as-is.
 *      - Template Method       : `public final void sendMoney()` -> marked `final` so no
 *                                subclass can alter the step order!
 *
 *   2. Concrete Classes (`PayToFriend`, `PayToMerchant`)
 *      - Extend `PaymentFlow` and provide their own logic for the individual steps.
 *
 * =====================================================================================
 *                                   UML DIAGRAM
 * =====================================================================================
 *
 *               +--------------------------------------------------+
 *               |            <<abstract>> PaymentFlow              |
 *               +--------------------------------------------------+
 *               | + final sendMoney(): void   <-- TEMPLATE METHOD  |
 *               |     {                                            |
 *               |       validateRequest();    // Step 1            |
 *               |       debitAmount();        // Step 2            |
 *               |       calculateFees();      // Step 3            |
 *               |       creditAmount();       // Step 4            |
 *               |       sendNotification();   // Step 5 (Hook)     |
 *               |     }                                            |
 *               | # abstract validateRequest(): void               |
 *               | # abstract debitAmount(): void                   |
 *               | # abstract calculateFees(): void                 |
 *               | # abstract creditAmount(): void                  |
 *               | # sendNotification(): void (default hook)        |
 *               +-------------------------^------------------------+
 *                                         | extends
 *                        +----------------+----------------+
 *                        |                                 |
 *     +------------------+---------------+ +---------------+------------------+
 *     |           PayToFriend            | |          PayToMerchant           |
 *     +----------------------------------+ +----------------------------------+
 *     | # validateRequest()              | | # validateRequest()              |
 *     | # debitAmount()                  | | # debitAmount()                  |
 *     | # calculateFees() -> 0% fee      | | # calculateFees() -> 2% fee      |
 *     | # creditAmount()  -> 100% credit | | # creditAmount()  -> 98% credit  |
 *     +----------------------------------+ +----------------------------------+
 *
 * FLOW: payToFriend.sendMoney()
 *
 *     Client calls: payToFriend.sendMoney()  (defined as `final` in PaymentFlow)
 *          |
 *          +--> 1. calls PayToFriend.validateRequest()
 *          +--> 2. calls PayToFriend.debitAmount()
 *          +--> 3. calls PayToFriend.calculateFees()
 *          +--> 4. calls PayToFriend.creditAmount()
 *          +--> 5. calls PaymentFlow.sendNotification() (or subclass override)
 *
 * =====================================================================================
 *     CONFUSION BUSTER: TEMPLATE METHOD vs STRATEGY vs FACTORY METHOD (INTERVIEW!)
 * =====================================================================================
 *
 * Why do candidates confuse Template Method and Strategy?
 *   Both let you customize algorithm behavior! Here is how to distinguish them:
 *
 * 1. TEMPLATE METHOD (Behavioral) -> "Fixed Skeleton, Custom Steps via INHERITANCE"
 *    - Mechanism : Inheritance (`extends PaymentFlow`). Resolved at COMPILE TIME.
 *    - Granularity: All subclasses share the SAME multi-step skeleton (`Step1 -> Step2 -> Step3`)
 *                   and only override specific steps inside that skeleton.
 *    - Control   : "Hollywood Principle" ("Don't call us, we'll call you") — the parent class
 *                  `sendMoney()` calls the child class methods.
 *
 *    TEMPLATE METHOD STRUCTURE (Parent locks step order, child fills in individual steps):
 *
 *      +-----------------------------------------------+
 *      |             PaymentFlow (Parent)              |
 *      |  final sendMoney() {                          |
 *      |    step1_validate();  ---> [Child overrides]  |
 *      |    step2_debit();     ---> [Child overrides]  |
 *      |    step3_fee();       ---> [Child overrides]  |
 *      |    step4_credit();    ---> [Child overrides]  |
 *      |  }                                            |
 *      +-----------------------------------------------+
 *
 * -------------------------------------------------------------------------------------
 *
 * 2. STRATEGY PATTERN (Behavioral) -> "Swap the ENTIRE Algorithm via COMPOSITION"
 *    - Mechanism : Composition (`has-a PaymentStrategy`). Can be swapped at RUNTIME!
 *    - Granularity: Replaces the WHOLE algorithm (`strategy.pay(amount)`), not just one step
 *                   inside a fixed parent pipeline.
 *
 *    STRATEGY STRUCTURE (Context delegates the entire action to a swappable interface):
 *
 *      +----------------------+   has-a    +-------------------------------+
 *      |     ShoppingCart     |----------->|  <<interface>> PaymentStrategy|
 *      +----------------------+            +-------------------------------+
 *      | - strategy           |            | + pay(amount: double)         |
 *      +----------------------+            +---------------^---------------+
 *                                                          | implements
 *                                             +------------+------------+
 *                                             |                         |
 *                                   +---------+---------+     +---------+---------+
 *                                   |    UpiStrategy    |     |   CardStrategy    |
 *                                   +-------------------+     +-------------------+
 *
 * -------------------------------------------------------------------------------------
 *
 * 3. FACTORY METHOD (Creational) -> "A Template Method Specialized for Creating Objects"
 *    - Factory Method is actually a special case of Template Method where the abstract step
 *      (`createVehicle()`) returns a newly created product object!
 *
 * -------------------------------------------------------------------------------------
 *
 * QUICK INTERVIEW DECISION MATRIX:
 * +-----------------+-------------------+----------------------+---------------------------+
 * | Pattern         | OOP Principle     | What Changes?        | Use in Interview When...  |
 * +-----------------+-------------------+----------------------+---------------------------+
 * | Template Method | Inheritance       | Individual steps of  | Mandatory multi-step      |
 * |                 | (`extends`)       | a FIXED pipeline     | order (ETL, Payment flow) |
 * +-----------------+-------------------+----------------------+---------------------------+
 * | Strategy        | Composition       | The ENTIRE algorithm | You want to swap behavior |
 * |                 | (`has-a` field)   | at runtime           | dynamically at runtime    |
 * +-----------------+-------------------+----------------------+---------------------------+
 *
 * KEY INTERVIEW POINTS TO MENTION:
 *   1. Always mark the Template Method (`sendMoney()`) as `final` so subclasses cannot
 *      override and break the sequence of steps!
 *   2. Keep step methods `protected` (`#` in UML) so outside clients cannot call `creditAmount()`
 *      directly—clients must only call the public `sendMoney()` template method.
 *   3. Mention "Hook Methods": optional methods with empty/default bodies in the abstract class
 *      that subclasses can optionally override to hook into the pipeline.
 * =====================================================================================
 */
package Patterns.Behavioral.TemplatePattern;

/**
 * 1. Abstract Class defining the Template Method (`sendMoney`) and abstract steps.
 */
abstract class PaymentFlow {

  // Abstract steps that every concrete payment flow MUST implement
  protected abstract void validateRequest();

  protected abstract void debitAmount();

  protected abstract void calculateFees();

  protected abstract void creditAmount();

  // Optional Hook Method: provides default behavior, subclasses MAY override if needed
  protected void sendNotification() {
    System.out.println("  [Hook] Sending standard SMS payment receipt to user.");
  }

  /**
   * THE TEMPLATE METHOD:
   * Marked `final` so NO subclass can change the order of execution!
   */
  public final void sendMoney() {
    // Step 1
    validateRequest();

    // Step 2
    debitAmount();

    // Step 3
    calculateFees();

    // Step 4
    creditAmount();

    // Step 5 (Hook)
    sendNotification();
  }
}

/**
 * 2a. Concrete Subclass 1: Peer-to-Peer transfer (0% platform fee).
 */
class PayToFriend extends PaymentFlow {
  @Override
  protected void validateRequest() {
    System.out.println("  [PayToFriend] Step 1: Validating friend's UPI ID & sender's PIN.");
  }

  @Override
  protected void debitAmount() {
    System.out.println("  [PayToFriend] Step 2: Debiting Rs. 1000 from sender's bank account.");
  }

  @Override
  protected void calculateFees() {
    System.out.println("  [PayToFriend] Step 3: 0% fee charged for P2P friend transfer.");
  }

  @Override
  protected void creditAmount() {
    System.out.println("  [PayToFriend] Step 4: Crediting full Rs. 1000 to friend's account.");
  }
}

/**
 * 2b. Concrete Subclass 2: Merchant payment (2% platform fee + custom GST invoice hook).
 */
class PayToMerchant extends PaymentFlow {
  @Override
  protected void validateRequest() {
    System.out.println("  [PayToMerchant] Step 1: Validating merchant QR code & KYB status.");
  }

  @Override
  protected void debitAmount() {
    System.out.println("  [PayToMerchant] Step 2: Debiting Rs. 1000 from customer's account.");
  }

  @Override
  protected void calculateFees() {
    System.out.println("  [PayToMerchant] Step 3: Deducting 2% platform fee (Rs. 20).");
  }

  @Override
  protected void creditAmount() {
    System.out.println("  [PayToMerchant] Step 4: Crediting remaining Rs. 980 to merchant's account.");
  }

  // Overriding the optional hook method
  @Override
  protected void sendNotification() {
    System.out.println("  [Hook] Sending SMS receipt + GST tax invoice to customer & merchant.");
  }
}

/** Client: executes the fixed PaymentFlow template method on different subclasses. */
public class TemplatePattern {
  public static void main(String[] args) {
    System.out.println("=== 1. UPI Transfer to a Friend ===");
    PaymentFlow payToFriend = new PayToFriend();
    payToFriend.sendMoney();

    System.out.println("\n=== 2. UPI Payment to a Merchant ===");
    PaymentFlow payToMerchant = new PayToMerchant();
    payToMerchant.sendMoney();
  }
}
