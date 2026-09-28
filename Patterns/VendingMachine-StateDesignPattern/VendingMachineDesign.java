import java.util.*;

/*
 * ============================================================================================================
 *                      VENDING MACHINE LLD - STATE DESIGN PATTERN & UML DIAGRAM
 * ============================================================================================================
 *
 * 1. DETAILED STATE DESIGN PATTERN FLOW (How State Changes Step-by-Step):
 * -----------------------------------------------------------------------
 *
 *   +========================================================================+
 *   |                          1. [ IdleState ]                              |
 *   |------------------------------------------------------------------------|
 *   | Allowed Actions : clickOnInsertCoinButton(), updateInventory()         |
 *   | Entry Action    : Clears coinList (machine.setCoinList(new ArrayList)) |
 *   +========================================================================+
 *                                      |
 *                                      | User calls: state.clickOnInsertCoinButton(machine)
 *                                      | Executes  : machine.setVendingMachineState(new HasMoneyState())
 *                                      v
 *   +========================================================================+
 *   |                        2. [ HasMoneyState ]                            |
 *   |------------------------------------------------------------------------|
 *   | Allowed Actions : insertCoin(), clickOnStartProductSelectionButton(),  |
 *   |                   refundFullMoney()                                    |
 *   +========================================================================+
 *        |              ^              |                              |
 *        | insertCoin() |              | User calls:                  | User cancels:
 *        +--------------+              | clickOnStartProduct          | refundFullMoney(machine)
 *        (Loops in same state,         | SelectionButton(machine)     | -> Returns all coins
 *         adds coin to coinList)       | Executes:                    | -> Sets new IdleState(machine)
 *                                      | setVendingMachineState(      |
 *                                      |   new SelectionState())      +------------------------+
 *                                      v                                                       |
 *   +========================================================================+                 |
 *   |                        3. [ SelectionState ]                           |                 |
 *   |------------------------------------------------------------------------|                 |
 *   | Allowed Actions : chooseProduct(machine, code), getChange(),           |                 |
 *   |                   refundFullMoney()                                    |                 |
 *   +========================================================================+                 |
 *                   |                                      |                                   |
 *                   | CASE A: Paid >= Item Price           | CASE B: Paid < Item Price         |
 *                   | 1. If paid > price -> getChange()    |         OR User Cancels           |
 *                   | 2. machine.setVendingMachineState(   | 1. Calls refundFullMoney(machine) |
 *                   |      new DispenseState())            | 2. Sets new IdleState(machine)    |
 *                   | 3. Calls dispenseProduct(...)        +---------------------------------->|
 *                   v                                                                          |
 *   +========================================================================+                 |
 *   |                        4. [ DispenseState ]                            |                 |
 *   |------------------------------------------------------------------------|                 |
 *   | Allowed Actions : dispenseProduct(machine, codeNumber)                 |                 |
 *   | Automatic Flow  :                                                      |                 |
 *   |   1. Fetches Item from Inventory                                       |                 |
 *   |   2. Marks shelf as SOLD OUT (updateSoldOutItem(code))                 |                 |
 *   |   3. Resets state -> machine.setVendingMachineState(new IdleState())   |---------------->+
 *   +========================================================================+                 |
 *                                                                                              |
 *   <------------------------------------------------------------------------------------------+
 *   (Machine is back in [IdleState] with an empty coin tray, ready for the next customer!)
 *
 *
 * 2. STATE vs ACTION MATRIX (What is Allowed vs Blocked in Each State):
 * ---------------------------------------------------------------------
 *   +-------------------------------------+-------------+---------------+----------------+---------------+
 *   | Method / Action                     |  IdleState  | HasMoneyState | SelectionState | DispenseState |
 *   +-------------------------------------+-------------+---------------+----------------+---------------+
 *   | clickOnInsertCoinButton()           | -> HasMoney |      [X]      |      [X]       |      [X]      |
 *   | updateInventory()                   |  Allowed    |      [X]      |      [X]       |      [X]      |
 *   | insertCoin()                        |     [X]     | Stays HasMoney|      [X]       |      [X]      |
 *   | clickOnStartProductSelectionButton()|     [X]     | -> Selection  |      [X]       |      [X]      |
 *   | refundFullMoney()                   |     [X]     | -> IdleState  | -> IdleState   |      [X]      |
 *   | chooseProduct()                     |     [X]     |      [X]      | -> Dispense    |      [X]      |
 *   | getChange()                         |     [X]     |      [X]      |  Allowed       |      [X]      |
 *   | dispenseProduct()                   |     [X]     |      [X]      |      [X]       | -> IdleState  |
 *   +-------------------------------------+-------------+---------------+----------------+---------------+
 *   ([X] = Throws Exception via base abstract class `State` default implementation)
 *
 *
 * 3. UML CLASS DIAGRAM:
 * ---------------------
 *   +------------------------------------+          +------------------------------------+
 *   |          VendingMachine            |          |        <<abstract>> State          |
 *   +------------------------------------+          +------------------------------------+
 *   | - vendingMachineState: State       |--------->| + clickOnInsertCoinButton()        |
 *   | - inventory: Inventory             |          | + clickOnStartProductSelectionBtn()|
 *   | - coinList: List<Coins>            |          | + insertCoin(machine, coin)        |
 *   +------------------------------------+          | + chooseProduct(machine, code)     |
 *                     |                             | + getChange(returnChangeMoney)     |
 *                     | HAS-A                       | + dispenseProduct(machine, code)   |
 *                     v                             | + refundFullMoney(machine)         |
 *   +------------------------------------+          | + updateInventory(machine, ...)    |
 *   |             Inventory              |          +------------------------------------+
 *   +------------------------------------+                             ^
 *   | - inventory: ItemShelf[]           |                             | IS-A (Extends)
 *   +------------------------------------+          +------------------+-----------------+------------------+
 *   | + initialEmptyInventory()          |          |                  |                 |                  |
 *   | + addItem(item, codeNumber)        |   +-------------+  +----------------+  +----------------+  +---------------+
 *   | + getItem(codeNumber): Item        |   |  IdleState  |  | HasMoneyState  |  | SelectionState |  | DispenseState |
 *   | + updateSoldOutItem(codeNumber)    |   +-------------+  +----------------+  +----------------+  +---------------+
 *   +------------------------------------+
 *                     | 1..*
 *                     v
 *   +------------------------------------+          +------------------------------------+
 *   |             ItemShelf              |          |                Item                |
 *   +------------------------------------+          +------------------------------------+
 *   | - code: int                        |--------->| - type: ItemType                   |
 *   | - item: Item                       |  HAS-A   | - price: int                       |
 *   | - isSoldOut: boolean               |          +------------------------------------+
 *   +------------------------------------+
 *
 * ============================================================================================================
 */

// 1. Enum representing supported coin denominations
enum Coins {
    PENNY(1),
    NICKEL(5),
    DIME(10),
    QUARTER(25);

    private final int value;

    Coins(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}

// 2. Enum representing item categories and their default prices
enum ItemType {
    Coke(25),
    PEPSI(35),
    JUICE(45),
    SODA(20),
    SNACK(15);

    private final int price;

    ItemType(int price) {
        this.price = price;
    }

    public int getPrice() {
        return this.price;
    }
}

// 3. Item entity representing a product inside the vending machine
class Item {
    private ItemType type;
    private int price;

    public Item() {
    }

    public Item(ItemType type, int price) {
        this.type = type;
        this.price = price;
    }

    public ItemType getType() {
        return this.type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public int getPrice() {
        return this.price;
    }

    public void setPrice(int price) {
        this.price = price;
    }
}

// 4. ItemShelf represents a physical slot/code in the vending machine
class ItemShelf {
    int code;
    Item item;
    boolean isSoldOut;

    public ItemShelf() {
    }

    public ItemShelf(int code, Item item, boolean isSoldOut) {
        this.code = code;
        this.item = item;
        this.isSoldOut = isSoldOut;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public boolean isSoldOut() {
        return isSoldOut;
    }

    public void setSoldOut(boolean isSoldOut) {
        this.isSoldOut = isSoldOut;
    }
}

// 5. Inventory manages all ItemShelves in the Vending Machine
class Inventory {
    ItemShelf[] inventory = null;

    public Inventory(int itemCount) {
        inventory = new ItemShelf[itemCount];
        initialEmptyInventory();
    }

    public ItemShelf[] getInventory() {
        return inventory;
    }

    public void setInventory(ItemShelf[] inventory) {
        this.inventory = inventory;
    }

    public void initialEmptyInventory() {
        int startCode = 101;
        for (int i = 0; i < inventory.length; i++) {
            ItemShelf space = new ItemShelf();
            space.setCode(startCode);
            space.setSoldOut(true);
            inventory[i] = space;
            startCode++;
        }
    }

    public void addItem(Item item, int codeNumber) throws Exception {
        for (ItemShelf itemShelf : inventory) {
            if (itemShelf.getCode() == codeNumber) {
                if (itemShelf.isSoldOut()) {
                    itemShelf.setItem(item);
                    itemShelf.setSoldOut(false);
                    return;
                } else {
                    throw new Exception("Item is already present on shelf " + codeNumber + ", cannot add item here.");
                }
            }
        }
        throw new Exception("Invalid Shelf Code: " + codeNumber);
    }

    public Item getItem(int codeNumber) throws Exception {
        for (ItemShelf itemShelf : inventory) {
            if (itemShelf.getCode() == codeNumber) {
                if (itemShelf.isSoldOut()) {
                    throw new Exception("Item at code " + codeNumber + " is already SOLD OUT!");
                } else {
                    return itemShelf.getItem();
                }
            }
        }
        throw new Exception("Invalid Shelf Code: " + codeNumber);
    }

    public void updateSoldOutItem(int codeNumber) {
        for (ItemShelf itemShelf : inventory) {
            if (itemShelf.getCode() == codeNumber) {
                itemShelf.setSoldOut(true);
            }
        }
    }
}

// ============================================================================
// STATE DESIGN PATTERN (Behavioral)
// - Abstract State : State
// - Concrete States : IdleState, HasMoneyState, SelectionState, DispenseState
// - Context Class : VendingMachine
// ============================================================================

abstract class State {
    public void clickOnInsertCoinButton(VendingMachine machine) throws Exception {
        throw new Exception("Action not allowed: Cannot click Insert Coin button in current state.");
    }

    public void clickOnStartProductSelectionButton(VendingMachine machine) throws Exception {
        throw new Exception("Action not allowed: Cannot start product selection in current state.");
    }

    public void insertCoin(VendingMachine machine, Coins coin) throws Exception {
        throw new Exception("Action not allowed: Cannot insert coin in current state.");
    }

    public void chooseProduct(VendingMachine machine, int codeNumber) throws Exception {
        throw new Exception("Action not allowed: Cannot choose product in current state.");
    }

    public int getChange(int returnChangeMoney) throws Exception {
        throw new Exception("Action not allowed: Cannot return change in current state.");
    }

    public Item dispenseProduct(VendingMachine machine, int codeNumber) throws Exception {
        throw new Exception("Action not allowed: Cannot dispense product in current state.");
    }

    public List<Coins> refundFullMoney(VendingMachine machine) throws Exception {
        throw new Exception("Action not allowed: Cannot refund money in current state.");
    }

    public void updateInventory(VendingMachine machine, Item item, int codeNumber) throws Exception {
        throw new Exception("Action not allowed: Cannot update inventory in current state.");
    }
}

// Concrete State 1: IdleState (Waiting for user to press "Insert Coin")
class IdleState extends State {
    public IdleState() {
        System.out.println("[STATE] Vending Machine is now in IdleState.");
    }

    public IdleState(VendingMachine machine) {
        System.out.println("[STATE] Vending Machine is now in IdleState.");
        machine.setCoinList(new ArrayList<>());
    }

    @Override
    public void clickOnInsertCoinButton(VendingMachine machine) throws Exception {
        machine.setVendingMachineState(new HasMoneyState());
    }

    @Override
    public void updateInventory(VendingMachine machine, Item item, int codeNumber) throws Exception {
        machine.getInventory().addItem(item, codeNumber);
    }
}

// Concrete State 2: HasMoneyState (Accepting coins from user)
class HasMoneyState extends State {
    public HasMoneyState() {
        System.out.println("[STATE] Vending Machine is now in HasMoneyState.");
    }

    @Override
    public void insertCoin(VendingMachine machine, Coins coin) throws Exception {
        System.out.println("[COIN INSERTED] Accepted " + coin.name() + " (Value: " + coin.getValue() + ")");
        machine.getCoinList().add(coin);
    }

    @Override
    public void clickOnStartProductSelectionButton(VendingMachine machine) throws Exception {
        machine.setVendingMachineState(new SelectionState());
    }

    @Override
    public List<Coins> refundFullMoney(VendingMachine machine) throws Exception {
        System.out.println("[REFUND] Returning full money from HasMoneyState: " + machine.getCoinList());
        List<Coins> refund = new ArrayList<>(machine.getCoinList());
        machine.setVendingMachineState(new IdleState(machine));
        return refund;
    }
}

// Concrete State 3: SelectionState (User enters item code, validates payment &
// calculates change)
class SelectionState extends State {
    public SelectionState() {
        System.out.println("[STATE] Vending Machine is now in SelectionState.");
    }

    @Override
    public void chooseProduct(VendingMachine machine, int codeNumber) throws Exception {
        // 1. Fetch item for this codeNumber
        Item item = machine.getInventory().getItem(codeNumber);

        // 2. Calculate total amount paid by user
        int paidByUser = 0;
        for (Coins coin : machine.getCoinList()) {
            paidByUser += coin.getValue();
        }

        // 3. Check if user paid enough for the selected item
        if (paidByUser < item.getPrice()) {
            System.out.println("[INSUFFICIENT FUNDS] Product Price: " + item.getPrice()
                    + " | Paid by User: " + paidByUser);
            refundFullMoney(machine);
            throw new Exception("Insufficient money paid for selected product!");
        } else {
            // 4. Return any extra change
            if (paidByUser > item.getPrice()) {
                getChange(paidByUser - item.getPrice());
            }
            // 5. Transition to DispenseState and dispense the product
            DispenseState dispenseState = new DispenseState();
            machine.setVendingMachineState(dispenseState);
            dispenseState.dispenseProduct(machine, codeNumber);
        }
    }

    @Override
    public int getChange(int returnChangeMoney) throws Exception {
        System.out.println("[CHANGE] Returning extra change in coin tray: " + returnChangeMoney);
        return returnChangeMoney;
    }

    @Override
    public List<Coins> refundFullMoney(VendingMachine machine) throws Exception {
        System.out.println("[REFUND] Returning full money in coin tray: " + machine.getCoinList());
        List<Coins> refund = new ArrayList<>(machine.getCoinList());
        machine.setVendingMachineState(new IdleState(machine));
        return refund;
    }
}

// Concrete State 4: DispenseState (Dispenses product, marks shelf sold out, and
// returns to IdleState)
class DispenseState extends State {
    public DispenseState() {
        System.out.println("[STATE] Vending Machine is now in DispenseState.");
    }

    @Override
    public Item dispenseProduct(VendingMachine machine, int codeNumber) throws Exception {
        Item item = machine.getInventory().getItem(codeNumber);
        System.out.println("[DISPENSED] Product dispensed: " + item.getType()
                + " (Code: " + codeNumber + ", Price: " + item.getPrice() + ")");
        machine.getInventory().updateSoldOutItem(codeNumber);
        machine.setVendingMachineState(new IdleState(machine));
        return item;
    }
}

// Context Class: Holds the current State, Inventory, and inserted Coins
class VendingMachine {
    private State vendingMachineState;
    private Inventory inventory;
    private List<Coins> coinList;

    public VendingMachine() {
        vendingMachineState = new IdleState();
        inventory = new Inventory(10);
        coinList = new ArrayList<>();
    }

    public State getVendingMachineState() {
        return vendingMachineState;
    }

    public void setVendingMachineState(State vendingMachineState) {
        this.vendingMachineState = vendingMachineState;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<Coins> getCoinList() {
        return coinList;
    }

    public void setCoinList(List<Coins> coinList) {
        this.coinList = coinList;
    }
}

// Driver Class: End-to-End Test Run for SDE-1 LLD Interview
public class VendingMachineDesign {
    public static void main(String[] args) {
        VendingMachine vendingMachine = new VendingMachine();

        try {
            System.out.println("==================================================");
            System.out.println("  VENDING MACHINE LLD - STATE DESIGN PATTERN DEMO ");
            System.out.println("==================================================\n");

            System.out.println("--- 1. Filling up the Inventory ---");
            fillUpInventory(vendingMachine);
            displayInventory(vendingMachine);

            // Scenario 1: Happy Path with Change Return (Buying PEPSI at Code 104, Price 35
            // with 2 QUARTERs = 50)
            System.out.println("\n--- 2. Scenario A: Buy PEPSI (Code 104, Price 35) with 50 cents ---");
            State state = vendingMachine.getVendingMachineState();
            state.clickOnInsertCoinButton(vendingMachine);

            state = vendingMachine.getVendingMachineState();
            state.insertCoin(vendingMachine, Coins.QUARTER); // 25
            state.insertCoin(vendingMachine, Coins.QUARTER); // 25 (Total = 50)

            state.clickOnStartProductSelectionButton(vendingMachine);

            state = vendingMachine.getVendingMachineState();
            state.chooseProduct(vendingMachine, 104); // Price 35 -> Returns 15 change & dispenses PEPSI

            // Scenario 2: Insufficient Funds Edge Case (Try buying JUICE at Code 106, Price
            // 45 with only 1 DIME = 10)
            System.out.println("\n--- 3. Scenario B: Insufficient Funds for JUICE (Code 106, Price 45) ---");
            state = vendingMachine.getVendingMachineState();
            state.clickOnInsertCoinButton(vendingMachine);

            state = vendingMachine.getVendingMachineState();
            state.insertCoin(vendingMachine, Coins.DIME); // 10
            state.clickOnStartProductSelectionButton(vendingMachine);

            state = vendingMachine.getVendingMachineState();
            try {
                state.chooseProduct(vendingMachine, 106);
            } catch (Exception e) {
                System.out.println("[EXCEPTION CAUGHT] " + e.getMessage());
            }

            System.out.println("\n--- 4. Final Inventory Status ---");
            displayInventory(vendingMachine);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void fillUpInventory(VendingMachine vendingMachine) {
        ItemShelf[] slots = vendingMachine.getInventory().getInventory();
        for (int i = 0; i < slots.length; i++) {
            Item newItem = new Item();
            if (i < 3) {
                newItem.setType(ItemType.Coke);
                newItem.setPrice(ItemType.Coke.getPrice());
            } else if (i < 5) {
                newItem.setType(ItemType.PEPSI);
                newItem.setPrice(ItemType.PEPSI.getPrice());
            } else if (i < 7) {
                newItem.setType(ItemType.JUICE);
                newItem.setPrice(ItemType.JUICE.getPrice());
            } else {
                newItem.setType(ItemType.SODA);
                newItem.setPrice(ItemType.SODA.getPrice());
            }
            slots[i].setItem(newItem);
            slots[i].setSoldOut(false);
        }
    }

    private static void displayInventory(VendingMachine vendingMachine) {
        ItemShelf[] slots = vendingMachine.getInventory().getInventory();
        for (ItemShelf slot : slots) {
            System.out.println("Code: " + slot.getCode()
                    + " | Item: " + String.format("%-6s", slot.getItem().getType())
                    + " | Price: " + slot.getItem().getPrice()
                    + " | SoldOut: " + slot.isSoldOut());
        }
    }
}
