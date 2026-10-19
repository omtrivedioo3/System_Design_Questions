/*
 * =====================================================================================
 *                     MEMENTO DESIGN PATTERN (BEHAVIORAL) - NOTES
 *               (SDE-1 LLD Interview Guide — Google / Amazon Style)
 * =====================================================================================
 *
 * WHAT
 *   Saves a "Snapshot" of an object's internal state so it can be restored (Undo / Rollback)
 *   later, WITHOUT violating encapsulation (without exposing private internal details).
 *
 * REAL-LIFE ANALOGY
 *   - Video Game Save Point: Before fighting a boss, the game creates a save file (Memento).
 *     The Memory Card (Caretaker) stores the save file without knowing your exact HP/inventory
 *     math. If you lose, the Game (Originator) loads the save file to restore your exact state.
 *   - Ctrl+Z in Text Editor / System Configuration Rollback.
 *
 * =====================================================================================
 *                  SDE-1 INTERVIEW FLOW & OBJECT IDENTIFICATION
 * =====================================================================================
 *
 * Step 1: How the Interviewer Phrases the Problem
 *   "Design an Application Configuration / Document Editor where a user can modify settings
 *    (height, width, theme, etc.), take checkpoints, and press Undo (Ctrl+Z) multiple times
 *    to revert back to previous states."
 *
 * Step 2: Why the Naive Approach Fails (Bad LLD)
 *   If an external `HistoryManager` class directly reads `config.height`, `config.width`,
 *   `config.privateKey` to back them up:
 *     1. Breaks Encapsulation: `Config` must expose all its private internal state publicly.
 *     2. Tight Coupling: Every time you add a new field to `Config`, `HistoryManager` breaks!
 *
 * Step 3: Object Identification (The 3 Classic Actors of Memento)
 *   1. Originator (`ConfigurationOriginator`)
 *      - The actual object whose state changes over time (`height`, `width`).
 *      - Only IT knows how to pack its state into a Memento (`createMemento()`) and
 *        unpack a Memento to restore itself (`restoreMemento(m)`).
 *
 *   2. Memento (`ConfigurationMemento`)
 *      - An IMMUTABLE "Snapshot Box" (`final int height, width`) representing state at T_i.
 *      - Has no business logic—just holds the saved state safely.
 *
 *   3. Caretaker (`ConfigurationCaretaker`)
 *      - The "Librarian / History Stack" (`Deque<ConfigurationMemento> history`).
 *      - Keeps track of saved Mementos and pops the latest one on `undo()`.
 *      - NEVER modifies or mutates what is inside a Memento!
 *
 * =====================================================================================
 *                                   UML DIAGRAM
 * =====================================================================================
 *
 *   +------------------------------------+           +------------------------------------+
 *   |       ConfigurationOriginator      |           |       ConfigurationMemento         |
 *   |            (Originator)            |---------->|       (Immutable Snapshot)         |
 *   +------------------------------------+  creates  +------------------------------------+
 *   | - height: int                      |  & reads  | - final height: int                |
 *   | - width: int                       |           | - final width: int                 |
 *   +------------------------------------+           +------------------------------------+
 *   | + setHeight(h) / setWidth(w)       |           | + getHeight(): int                 |
 *   | + createMemento(): Memento         |           | + getWidth(): int                  |
 *   | + restoreMemento(m: Memento): void |           +-----------------^------------------+
 *   +------------------------------------+                             |
 *                                                                      | stores (Stack)
 *                                                                      | (1 to *)
 *                                                    +-----------------+------------------+
 *                                                    |       ConfigurationCaretaker       |
 *                                                    |         (History Manager)          |
 *                                                    +------------------------------------+
 *                                                    | - history: Deque<Memento>          |
 *                                                    +------------------------------------+
 *                                                    | + addMemento(m: Memento): void     |
 *                                                    | + undo(): Memento                  |
 *                                                    +------------------------------------+
 *
 * FLOW: Save & Undo
 *
 *   [SAVE CHECKPOINT]
 *     1. Client calls `Memento snap = originator.createMemento()`  (Originator copies its state)
 *     2. Client calls `caretaker.addMemento(snap)`                 (Caretaker pushes to Stack)
 *
 *   [UNDO (Ctrl+Z)]
 *     1. Client calls `Memento prev = caretaker.undo()`            (Caretaker pops from Stack)
 *     2. Client calls `originator.restoreMemento(prev)`            (Originator overwrites fields)
 *
 * =====================================================================================
 *       CONFUSION BUSTER: MEMENTO vs COMMAND vs PROTOTYPE (INTERVIEW FAVORITE!)
 * =====================================================================================
 *
 * Interviewer Question:
 *   "Both Memento Pattern and Command Pattern are used to implement Undo (Ctrl+Z).
 *    How do you decide which one to use in an interview?"
 *
 * 1. MEMENTO PATTERN -> "Save Full State Snapshot (Checkpoint)"
 *    - How Undo Works : Before changing state, save a full copy `{height=10, width=20}`.
 *                       To undo, overwrite current values with the saved snapshot.
 *    - When to Use    : When operations are NOT easily reversible (e.g., applying a blur
 *                       filter on an image, complex state mutations, game checkpoints).
 *
 *    MEMENTO UNDO STRUCTURE (Stores State Snapshots in Stack):
 *
 *      Originator State: [h=5, w=10] ---> [h=7, w=12] ---> [h=99, w=99]
 *                             |                |                 | (Undo!)
 *                             v                v                 v
 *      Caretaker Stack : [Snap(5,10)] -> [Snap(7,12)] ------> Restores [h=7, w=12]
 *
 * -------------------------------------------------------------------------------------
 *
 * 2. COMMAND PATTERN -> "Save the Action & Execute Inverse Action (Do / Undo)"
 *    - How Undo Works : Instead of saving the whole object state, save the ACTION object
 *                       (`AddTextCommand("hello")`). To undo, call `command.undo()`, which
 *                       runs the exact opposite action (`deleteLastChars(5)`).
 *    - When to Use    : When the object is HUGE (e.g., a 100MB document where saving a full
 *                       snapshot on every keystroke would crash RAM) AND every action has a
 *                       clean mathematical inverse (`+5` <-> `-5`, `insert` <-> `delete`).
 *
 *    COMMAND UNDO STRUCTURE (Stores Action Objects in Stack):
 *
 *      +-------------------------+         +-----------------------------------+
 *      |  <<interface>> Command  |<--------|        InsertTextCommand          |
 *      +-------------------------+         +-----------------------------------+
 *      | + execute()             |         | + execute() { doc.append(text); } |
 *      | + undo()                |         | + undo()    { doc.delete(text); } |
 *      +-------------------------+         +-----------------------------------+
 *
 * -------------------------------------------------------------------------------------
 *
 * 3. PROTOTYPE PATTERN (Creational) -> "Clone a Brand-New Independent Object"
 *    - Why it seems similar: Both copy an object's fields!
 *    - Difference:
 *        * Prototype (`obj.clone()`) creates a SECOND full working `Originator` object so
 *          the client can use two separate objects independently.
 *        * Memento creates a lightweight, read-only state token (`ConfigurationMemento`)
 *          strictly to roll back the SAME `Originator` later.
 *
 * -------------------------------------------------------------------------------------
 *
 * QUICK INTERVIEW DECISION MATRIX:
 * +-----------+------------------------+----------------------------------------------+
 * | Pattern   | What is Stored?        | Best Used in Interviews When...              |
 * +-----------+------------------------+----------------------------------------------+
 * | Memento   | State Snapshot         | State is small/moderate OR actions cannot be |
 * |           | (Data values at T_i)   | reversed mathematically (Checkpoints).       |
 * +-----------+------------------------+----------------------------------------------+
 * | Command   | Operation / Action     | State is huge (100MB doc) AND every action   |
 * |           | (`execute()`/`undo()`) | has a clean opposite (`insert` vs `delete`). |
 * +-----------+------------------------+----------------------------------------------+
 * | Prototype | Full Cloned Object     | You need a duplicate working object instance |
 * |           | (`clone()`)            | quickly without running heavy constructors.  |
 * +-----------+------------------------+----------------------------------------------+
 *
 * BONUS INTERVIEW POINTS (Trade-offs to mention):
 *   - Memory Overhead: If Originator state is huge and users make 10,000 edits, the Caretaker
 *     stack can cause OutOfMemoryError.
 *     -> Solution: Use a bounded history (e.g., `Deque` capped at last 50 snapshots).
 *   - Memento fields MUST be `final` (immutable) so no one can corrupt historical snapshots.
 * =====================================================================================
 */
package Patterns.Behavioral.MementoPattern;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 1. Memento: Immutable snapshot of the Originator's state at a specific point
 * in time.
 */
class ConfigurationMemento {
  private final int height;
  private final int width;

  public ConfigurationMemento(int height, int width) {
    this.height = height;
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  public int getWidth() {
    return width;
  }
}

/**
 * 2. Originator: The object whose state changes.
 * Creates snapshots (`createMemento`) and restores its state from a snapshot
 * (`restoreMemento`).
 */
class ConfigurationOriginator {
  private int height;
  private int width;

  public ConfigurationOriginator(int height, int width) {
    this.height = height;
    this.width = width;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  // Saves current state inside a new immutable Memento
  public ConfigurationMemento createMemento() {
    System.out.println("  [Save] Creating snapshot -> {height=" + height + ", width=" + width + "}");
    return new ConfigurationMemento(this.height, this.width);
  }

  // Restores state from a given Memento
  public void restoreMemento(ConfigurationMemento mementoToBeRestored) {
    if (mementoToBeRestored == null) {
      System.out.println("  [Undo] Nothing left in history to restore!");
      return;
    }
    this.height = mementoToBeRestored.getHeight();
    this.width = mementoToBeRestored.getWidth();
    System.out.println("  [Undo] Restored state to -> {height=" + height + ", width=" + width + "}");
  }

  @Override
  public String toString() {
    return "Config{height=" + height + ", width=" + width + "}";
  }
}

/**
 * 3. Caretaker: Manages the history stack of Mementos.
 * Never modifies or inspects the internal contents of a Memento.
 */
class ConfigurationCaretaker {
  private final Deque<ConfigurationMemento> history = new ArrayDeque<>();

  public void addMemento(ConfigurationMemento memento) {
    history.push(memento);
  }

  public ConfigurationMemento undo() {
    if (!history.isEmpty()) {
      return history.pop();
    }
    return null;
  }
}

/**
 * Client: demonstrates saving checkpoints and undoing changes with Memento
 * Pattern.
 */
public class MementoPattern {
  public static void main(String[] args) {
    ConfigurationCaretaker caretaker = new ConfigurationCaretaker();

    // 1. Initial State: height = 5, width = 10
    ConfigurationOriginator config = new ConfigurationOriginator(5, 10);
    System.out.println("State 1 (Initial): " + config);
    caretaker.addMemento(config.createMemento()); // Save Checkpoint 1

    // 2. Update State: height = 7, width = 12
    config.setHeight(7);
    config.setWidth(12);
    System.out.println("\nState 2 (Updated): " + config);
    caretaker.addMemento(config.createMemento()); // Save Checkpoint 2

    // 3. Accidental / Bad Update: height = 99, width = 99 (NOT saved to caretaker!)
    config.setHeight(99);
    config.setWidth(99);
    System.out.println("\nState 3 (Bad Edit, Unsaved): " + config);

    // 4. First Undo (Ctrl+Z) -> reverts from (99, 99) back to Checkpoint 2 (7, 12)
    System.out.println("\n--- Pressing Ctrl+Z (1st Undo) ---");
    config.restoreMemento(caretaker.undo());
    System.out.println("Current Config: " + config);

    // 5. Second Undo (Ctrl+Z) -> reverts from (7, 12) back to Checkpoint 1 (5, 10)
    System.out.println("\n--- Pressing Ctrl+Z (2nd Undo) ---");
    config.restoreMemento(caretaker.undo());
    System.out.println("Current Config: " + config);
  }
}
