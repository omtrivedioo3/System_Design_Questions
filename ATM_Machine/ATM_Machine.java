package ATM_Machine;

/*
 * ============================================================================================================
 *             ATM MACHINE LLD - STATE DESIGN PATTERN + CHAIN OF RESPONSIBILITY & UML DIAGRAM
 * ============================================================================================================
 *
 * 1. STATE DESIGN PATTERN FLOW (How ATM State Changes Step-by-Step):
 * ------------------------------------------------------------------
 *
 *   +========================================================================+
 *   |                          1. [ IdleState ]                              |
 *   |------------------------------------------------------------------------|
 *   | Allowed Action : insertCard(atmRoom)                                   |
 *   +========================================================================+
 *                                      |
 *                                      | User inserts card
 *                                      | Executes: atm.setATMState(new HasCardState())
 *                                      v
 *   +========================================================================+
 *   |                        2. [ HasCardState ]                             |
 *   |------------------------------------------------------------------------|
 *   | Allowed Actions: insertPin(atmRoom, pin), removeCard(atmRoom)          |
 *   +========================================================================+
 *                  |                                        |
 *                  | PIN is Valid                           | Invalid PIN OR Cancel
 *                  | Executes:                              | Executes: removeCard(atmRoom)
 *                  | atm.setATMState(new SelectionState())  | -> atm.setATMState(new IdleState())
 *                  v                                        +------------------------------------+
 *   +========================================================================+                   |
 *   |                       3. [ SelectionState ]                            |                   |
 *   |------------------------------------------------------------------------|                   |
 *   | Allowed Actions: selectOption(atmRoom, option), removeCard(atmRoom)    |                   |
 *   +========================================================================+                   |
 *          |                                   |                                   |             |
 *          | Option = "withdraw"               | Option = "deposit"                | Cancel      |
 *          | setATMState(new WithdrawState())  | setATMState(new DepositState())   +------------>|
 *          v                                   v                                                 |
 *   +===============================+   +===============================+                        |
 *   |     4A. [ WithdrawState ]     |   |     4B. [ DepositState ]      |                        |
 *   |-------------------------------|   |-------------------------------|                        |
 *   | 1. Validates ATM & User funds |   | 1. Adds amount to BankAccount |                        |
 *   | 2. Triggers CoR Processor:    |   | 2. Adds cash to ATM total     |                        |
 *   |    2000 -> 500 -> 100 Notes   |   | 3. Ejects card & resets state |                        |
 *   | 3. Deducts BankAccount & ATM  |   +===============================+                        |
 *   | 4. Ejects card & resets state |                   |                                        |
 *   +===============================+                   |                                        |
 *          |                                            |                                        |
 *          +--------------------------------------------+--------------------------------------->+
 *                                                                                                |
 *   <--------------------------------------------------------------------------------------------+
 *   (ATM is back in [IdleState], Card is ejected, ready for the next user!)
 *
 *
 * 2. CHAIN OF RESPONSIBILITY (CoR) FLOW FOR CASH WITHDRAWAL (Example: Withdraw Rs. 2700):
 * ---------------------------------------------------------------------------------------
 *   WithdrawState calls:
 *   CashWithdrawProcessor processor = new TwoThousandWithdrawProcessor(
 *                                       new FiveHundredWithdrawProcessor(
 *                                         new OneHundredWithdrawProcessor(null)));
 *   processor.withdraw(atm, 2700);
 *
 *   [ Request: Withdraw Rs. 2700 ]
 *                 |
 *                 v
 *   +---------------------------------------------------------------+
 *   | Handler 1: TwoThousandWithdrawProcessor (Rs. 2000 Notes)      |
 *   |---------------------------------------------------------------|
 *   | - Needed = 2700 / 2000 = 1 note  | Remaining = 2700 % 2000    |
 *   | - Dispenses: 1 x Rs.2000 note    | Balance to pass = Rs. 700  |
 *   +---------------------------------------------------------------+
 *                 |
 *                 | nextProcessor.withdraw(atm, 700)
 *                 v
 *   +---------------------------------------------------------------+
 *   | Handler 2: FiveHundredWithdrawProcessor (Rs. 500 Notes)       |
 *   |---------------------------------------------------------------|
 *   | - Needed = 700 / 500 = 1 note    | Remaining = 700 % 500      |
 *   | - Dispenses: 1 x Rs.500 note     | Balance to pass = Rs. 200  |
 *   +---------------------------------------------------------------+
 *                 |
 *                 | nextProcessor.withdraw(atm, 200)
 *                 v
 *   +---------------------------------------------------------------+
 *   | Handler 3: OneHundredWithdrawProcessor (Rs. 100 Notes)        |
 *   |---------------------------------------------------------------|
 *   | - Needed = 200 / 100 = 2 notes   | Remaining = 200 % 100 = 0  |
 *   | - Dispenses: 2 x Rs.100 notes    | Balance = 0 (DONE!)        |
 *   +---------------------------------------------------------------+
 *
 *
 * 3. UML CLASS DIAGRAM:
 * ---------------------
 *   +------------------------+         +-----------------------------+         +-------------------------------+
 *   |        ATMRoom         |         |             ATM             |         |    <<abstract>> ATMState      |
 *   +------------------------+         +-----------------------------+         +-------------------------------+
 *   | - atm: ATM             |-------->| - atmState: ATMState        |-------->| + insertCard(atmRoom)         |
 *   | - user: User           |         | - totalAmount: int          |         | + insertPin(atmRoom, pin)     |
 *   +------------------------+         | - twoThousandNotes: int     |         | + selectOption(atmRoom, opt)  |
 *               |                      | - fiveHundredNotes: int     |         | + withdraw(atmRoom, amount)   |
 *               v                      | - oneHundredNotes: int      |         | + deposit(atmRoom, amount)    |
 *   +------------------------+         +-----------------------------+         | + checkBalance(atmRoom)       |
 *   |          User          |                                                 | + removeCard(atmRoom)         |
 *   +------------------------+                                                 +-------------------------------+
 *   | - name: String         |                                                                 ^
 *   | - card: Card           |                                                                 | IS-A
 *   | - bankAccount: Account |                                       +-------------------------+-------------------------+
 *   +------------------------+                                       |            |               |            |         |
 *               |                                               +-----------+ +--------------+ +--------------+ +--------------+
 *               v                                               | IdleState | | HasCardState | |SelectionState| |WithdrawState |
 *   +------------------------+      +------------------------+  +-----------+ +--------------+ +--------------+ +--------------+
 *   |          Card          |      |      BankAccount       |                                                         |
 *   +------------------------+      +------------------------+                                                         | USES
 *   | - cardNumber: String   |----->| - balance: int         |                                                         v
 *   | - pin: int             |      | + withdraw(amount)     |                         +---------------------------------------+
 *   | - bankAccount: Account |      | + deposit(amount)      |                         | <<abstract>> CashWithdrawProcessor    |
 *   +------------------------+      +------------------------+                         +---------------------------------------+
 *                                                                                      | - nextProcessor: CashWithdrawProcessor|
 *                                                                                      | + withdraw(atm, remainingAmount)      |
 *                                                                                      +---------------------------------------+
 *                                                                                                          ^
 *                                                                                                          | IS-A
 *                                                                            +-----------------------------+-----------------------------+
 *                                                                            |                             |                             |
 *                                                              +------------------------------+ +-----------------------------+ +----------------------------+
 *                                                              | TwoThousandWithdrawProcessor | | FiveHundredWithdrawProcessor| | OneHundredWithdrawProcessor|
 *                                                              +------------------------------+ +-----------------------------+ +----------------------------+
 * ============================================================================================================
 */

// ============================================================================
// 1. DOMAIN ENTITIES: BankAccount, Card, User, ATM, ATMRoom
// ============================================================================

class BankAccount {
    private int balance;

    public BankAccount(int balance) {
        this.balance = balance;
    }

    public int getBalance() {
        return balance;
    }

    public void withdraw(int amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            System.out.println("[BANK ACCOUNT] Rs. " + amount + " debited. Remaining Account Balance: Rs. " + this.balance);
        } else {
            System.out.println("[BANK ACCOUNT] Insufficient Balance!");
        }
    }

    public void deposit(int amount) {
        this.balance += amount;
        System.out.println("[BANK ACCOUNT] Rs. " + amount + " credited. Updated Account Balance: Rs. " + this.balance);
    }
}

class Card {
    private String cardNumber;
    private int pin;
    private String name;
    private BankAccount bankAccount;

    public Card(String cardNumber, int pin, String name, BankAccount bankAccount) {
        this.cardNumber = cardNumber;
        this.pin = pin;
        this.name = name;
        this.bankAccount = bankAccount;
    }

    public boolean isCardValid(String enteredCardNumber, int enteredPin) {
        return this.cardNumber.equals(enteredCardNumber) && this.pin == enteredPin;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public int getPIN() {
        return pin;
    }

    public String getName() {
        return name;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }
}

class User {
    private String name;
    private Card card;
    private BankAccount bankAccount;

    public User(String name, Card card, BankAccount bankAccount) {
        this.name = name;
        this.card = card;
        this.bankAccount = bankAccount;
    }

    public String getName() {
        return name;
    }

    public Card getCard() {
        return card;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }
}

class ATM {
    private int atmId;
    private String location;
    private int totalAmount;
    private int twoThousandNotes;
    private int fiveHundredNotes;
    private int oneHundredNotes;
    private ATMState atmState;

    public ATM(int atmId, String location, int twoThousandNotes, int fiveHundredNotes, int oneHundredNotes) {
        this.atmId = atmId;
        this.location = location;
        this.twoThousandNotes = twoThousandNotes;
        this.fiveHundredNotes = fiveHundredNotes;
        this.oneHundredNotes = oneHundredNotes;
        this.totalAmount = (twoThousandNotes * 2000) + (fiveHundredNotes * 500) + (oneHundredNotes * 100);
        this.atmState = new IdleState();
    }

    public int getAtmId() {
        return atmId;
    }

    public String getLocation() {
        return location;
    }

    public void setATMState(ATMState atmState) {
        this.atmState = atmState;
    }

    public ATMState getATMState() {
        return atmState;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void addTwoThousandNotes(int count) {
        this.twoThousandNotes += count;
        this.totalAmount += count * 2000;
    }

    public void dispenseTwoThousandNotes(int count) {
        this.twoThousandNotes -= count;
        this.totalAmount -= count * 2000;
    }

    public int getTwoThousandNotes() {
        return twoThousandNotes;
    }

    public void addFiveHundredNotes(int count) {
        this.fiveHundredNotes += count;
        this.totalAmount += count * 500;
    }

    public void dispenseFiveHundredNotes(int count) {
        this.fiveHundredNotes -= count;
        this.totalAmount -= count * 500;
    }

    public int getFiveHundredNotes() {
        return fiveHundredNotes;
    }

    public void addOneHundredNotes(int count) {
        this.oneHundredNotes += count;
        this.totalAmount += count * 100;
    }

    public void dispenseOneHundredNotes(int count) {
        this.oneHundredNotes -= count;
        this.totalAmount -= count * 100;
    }

    public int getOneHundredNotes() {
        return oneHundredNotes;
    }

    public void printATMStatus() {
        System.out.println("------------------------------------------------------------");
        System.out.println("[ATM INVENTORY] Location: " + location + " | Total Cash: Rs. " + totalAmount);
        System.out.println("  Rs. 2000 Notes : " + twoThousandNotes);
        System.out.println("  Rs. 500  Notes : " + fiveHundredNotes);
        System.out.println("  Rs. 100  Notes : " + oneHundredNotes);
        System.out.println("------------------------------------------------------------");
    }
}

class ATMRoom {
    private ATM atm;
    private User user;

    public ATMRoom(ATM atm, User user) {
        this.atm = atm;
        this.user = user;
    }

    public ATM getAtm() {
        return atm;
    }

    public User getUser() {
        return user;
    }
}

// ============================================================================
// 2. CHAIN OF RESPONSIBILITY PATTERN (For Dispensing Denominations: 2000 -> 500 -> 100)
// ============================================================================

abstract class CashWithdrawProcessor {
    protected CashWithdrawProcessor nextCashWithdrawProcessor;

    public CashWithdrawProcessor(CashWithdrawProcessor nextCashWithdrawProcessor) {
        this.nextCashWithdrawProcessor = nextCashWithdrawProcessor;
    }

    public void withdraw(ATM atm, int remainingAmount) {
        if (nextCashWithdrawProcessor != null) {
            nextCashWithdrawProcessor.withdraw(atm, remainingAmount);
        } else if (remainingAmount > 0) {
            System.out.println("[CoR ERROR] Unable to dispense remaining amount: Rs. " + remainingAmount);
        }
    }
}

class TwoThousandWithdrawProcessor extends CashWithdrawProcessor {
    public TwoThousandWithdrawProcessor(CashWithdrawProcessor nextCashWithdrawProcessor) {
        super(nextCashWithdrawProcessor);
    }

    @Override
    public void withdraw(ATM atm, int remainingAmount) {
        int requiredNotes = remainingAmount / 2000;
        int balance = remainingAmount % 2000;

        if (requiredNotes <= atm.getTwoThousandNotes()) {
            if (requiredNotes > 0) {
                atm.dispenseTwoThousandNotes(requiredNotes);
                System.out.println("  [DISPENSER - CoR Step 1] Dispensed " + requiredNotes + " x Rs. 2000 note(s)");
            }
        } else {
            int available = atm.getTwoThousandNotes();
            if (available > 0) {
                atm.dispenseTwoThousandNotes(available);
                System.out.println("  [DISPENSER - CoR Step 1] Dispensed " + available + " x Rs. 2000 note(s)");
            }
            balance = remainingAmount - (available * 2000);
        }
        // send remaining to the next processor
        if (balance != 0) {
            super.withdraw(atm, balance);
        }
    }
}

class FiveHundredWithdrawProcessor extends CashWithdrawProcessor {
    public FiveHundredWithdrawProcessor(CashWithdrawProcessor nextCashWithdrawProcessor) {
        super(nextCashWithdrawProcessor);
    }

    @Override
    public void withdraw(ATM atm, int remainingAmount) {
        int requiredNotes = remainingAmount / 500;
        int balance = remainingAmount % 500;

        if (requiredNotes <= atm.getFiveHundredNotes()) {
            if (requiredNotes > 0) {
                atm.dispenseFiveHundredNotes(requiredNotes);
                System.out.println("  [DISPENSER - CoR Step 2] Dispensed " + requiredNotes + " x Rs. 500 note(s)");
            }
        } else {
            int available = atm.getFiveHundredNotes();
            if (available > 0) {
                atm.dispenseFiveHundredNotes(available);
                System.out.println("  [DISPENSER - CoR Step 2] Dispensed " + available + " x Rs. 500 note(s)");
            }
            balance = remainingAmount - (available * 500);
        }

        if (balance != 0) {
            super.withdraw(atm, balance);
        }
    }
}

class OneHundredWithdrawProcessor extends CashWithdrawProcessor {
    public OneHundredWithdrawProcessor(CashWithdrawProcessor nextCashWithdrawProcessor) {
        super(nextCashWithdrawProcessor);
    }

    @Override
    public void withdraw(ATM atm, int remainingAmount) {
        int requiredNotes = remainingAmount / 100;
        int balance = remainingAmount % 100;

        if (requiredNotes <= atm.getOneHundredNotes()) {
            if (requiredNotes > 0) {
                atm.dispenseOneHundredNotes(requiredNotes);
                System.out.println("  [DISPENSER - CoR Step 3] Dispensed " + requiredNotes + " x Rs. 100 note(s)");
            }
        } else {
            int available = atm.getOneHundredNotes();
            if (available > 0) {
                atm.dispenseOneHundredNotes(available);
                System.out.println("  [DISPENSER - CoR Step 3] Dispensed " + available + " x Rs. 100 note(s)");
            }
            balance = remainingAmount - (available * 100);
        }

        if (balance != 0) {
            super.withdraw(atm, balance);
        }
    }
}

// ============================================================================
// 3. STATE DESIGN PATTERN (For ATM Lifecycle: Idle -> HasCard -> Selection -> Withdraw/Deposit)
// ============================================================================

abstract class ATMState {
    public void insertCard(ATMRoom atmRoom) {
        System.out.println("[INVALID ACTION] Cannot insert card in current state.");
    }

    public void insertPin(ATMRoom atmRoom, int enteredPin) {
        System.out.println("[INVALID ACTION] Cannot insert PIN in current state.");
    }

    public void selectOption(ATMRoom atmRoom, String option) {
        System.out.println("[INVALID ACTION] Cannot select option in current state.");
    }

    public void withdraw(ATMRoom atmRoom, int amount) {
        System.out.println("[INVALID ACTION] Cannot withdraw in current state.");
    }

    public void deposit(ATMRoom atmRoom, int amount) {
        System.out.println("[INVALID ACTION] Cannot deposit in current state.");
    }

    public void checkBalance(ATMRoom atmRoom) {
        System.out.println("[INVALID ACTION] Cannot check balance in current state.");
    }

    public void removeCard(ATMRoom atmRoom) {
        System.out.println("[INVALID ACTION] Cannot remove card in current state.");
    }
}

// Concrete State 1: IdleState
class IdleState extends ATMState {
    public IdleState() {
        System.out.println("[STATE] ATM is now in IdleState.");
    }

    @Override
    public void insertCard(ATMRoom atmRoom) {
        System.out.println("[ACTION] Card inserted by " + atmRoom.getUser().getName());
        atmRoom.getAtm().setATMState(new HasCardState());
    }
}

// Concrete State 2: HasCardState
class HasCardState extends ATMState {
    public HasCardState() {
        System.out.println("[STATE] ATM is now in HasCardState.");
    }

    @Override
    public void insertPin(ATMRoom atmRoom, int enteredPin) {
        Card userCard = atmRoom.getUser().getCard();
        if (userCard.isCardValid(userCard.getCardNumber(), enteredPin)) {
            System.out.println("[AUTH SUCCESS] PIN verified successfully.");
            atmRoom.getAtm().setATMState(new SelectionState());
        } else {
            System.out.println("[AUTH FAILED] Invalid PIN entered!");
            removeCard(atmRoom);
        }
    }

    @Override
    public void removeCard(ATMRoom atmRoom) {
        System.out.println("[CARD EJECTED] Please take your card.");
        atmRoom.getAtm().setATMState(new IdleState());
    }
}

// Concrete State 3: SelectionState
class SelectionState extends ATMState {
    public SelectionState() {
        System.out.println("[STATE] ATM is now in SelectionState.");
    }

    @Override
    public void selectOption(ATMRoom atmRoom, String option) {
        System.out.println("[OPTION SELECTED] " + option.toUpperCase());
        if (option.equalsIgnoreCase("withdraw")) {
            atmRoom.getAtm().setATMState(new WithdrawState());
        } else if (option.equalsIgnoreCase("deposit")) {
            atmRoom.getAtm().setATMState(new DepositState());
        } else if (option.equalsIgnoreCase("checkBalance")) {
            atmRoom.getAtm().setATMState(new CheckBalanceState());
        } else {
            System.out.println("[ERROR] Unknown option selected: " + option);
            removeCard(atmRoom);
        }
    }

    @Override
    public void removeCard(ATMRoom atmRoom) {
        System.out.println("[CARD EJECTED] Transaction cancelled. Please take your card.");
        atmRoom.getAtm().setATMState(new IdleState());
    }
}

// Concrete State 4A: WithdrawState (Uses Chain of Responsibility to dispense notes)
class WithdrawState extends ATMState {
    public WithdrawState() {
        System.out.println("[STATE] ATM is now in WithdrawState.");
    }

    @Override
    public void withdraw(ATMRoom atmRoom, int amount) {
        ATM atm = atmRoom.getAtm();
        BankAccount account = atmRoom.getUser().getBankAccount();

        System.out.println("[WITHDRAW REQUEST] Requested Amount: Rs. " + amount);

        // 1. Validate multiple of 100
        if (amount <= 0 || amount % 100 != 0) {
            System.out.println("[WITHDRAW FAILED] Amount must be a positive multiple of Rs. 100!");
            removeCard(atmRoom);
            return;
        }

        // 2. Validate User's BankAccount Balance
        if (account.getBalance() < amount) {
            System.out.println("[WITHDRAW FAILED] Insufficient balance in your Bank Account (Available: Rs. "
                    + account.getBalance() + ")");
            removeCard(atmRoom);
            return;
        }

        // 3. Validate ATM Total Cash
        if (atm.getTotalAmount() < amount) {
            System.out.println("[WITHDRAW FAILED] Insufficient cash inside the ATM (Available: Rs. "
                    + atm.getTotalAmount() + ")");
            removeCard(atmRoom);
            return;
        }

        // 4. Verify exact denomination availability before mutating state
        if (!canDispenseExactAmount(atm, amount)) {
            System.out.println("[WITHDRAW FAILED] ATM does not have exact note denominations for Rs. " + amount);
            removeCard(atmRoom);
            return;
        }

        // 5. Debit User's Bank Account
        account.withdraw(amount);

        // 6. Execute Chain of Responsibility: 2000 -> 500 -> 100
        CashWithdrawProcessor withdrawProcessor = new TwoThousandWithdrawProcessor(
                new FiveHundredWithdrawProcessor(
                        new OneHundredWithdrawProcessor(null)));

        withdrawProcessor.withdraw(atm, amount);

        // 7. Eject card and return to IdleState
        removeCard(atmRoom);
    }

    private boolean canDispenseExactAmount(ATM atm, int amount) {
        int use2000 = Math.min(amount / 2000, atm.getTwoThousandNotes());
        int rem = amount - (use2000 * 2000);

        int use500 = Math.min(rem / 500, atm.getFiveHundredNotes());
        rem -= (use500 * 500);

        int use100 = Math.min(rem / 100, atm.getOneHundredNotes());
        rem -= (use100 * 100);

        return rem == 0;
    }

    @Override
    public void removeCard(ATMRoom atmRoom) {
        System.out.println("[CARD EJECTED] Please collect your cash and card.");
        atmRoom.getAtm().setATMState(new IdleState());
    }
}

// Concrete State 4B: DepositState
class DepositState extends ATMState {
    public DepositState() {
        System.out.println("[STATE] ATM is now in DepositState.");
    }

    @Override
    public void deposit(ATMRoom atmRoom, int amount) {
        atmRoom.getUser().getBankAccount().deposit(amount);
        atmRoom.getAtm().addFiveHundredNotes(amount / 500);
        removeCard(atmRoom);
    }

    @Override
    public void removeCard(ATMRoom atmRoom) {
        System.out.println("[CARD EJECTED] Deposit complete. Please take your card.");
        atmRoom.getAtm().setATMState(new IdleState());
    }
}

// Concrete State 4C: CheckBalanceState
class CheckBalanceState extends ATMState {
    public CheckBalanceState() {
        System.out.println("[STATE] ATM is now in CheckBalanceState.");
    }

    @Override
    public void checkBalance(ATMRoom atmRoom) {
        System.out.println("[BALANCE INQUIRY] Current Bank Account Balance: Rs. "
                + atmRoom.getUser().getBankAccount().getBalance());
        removeCard(atmRoom);
    }

    @Override
    public void removeCard(ATMRoom atmRoom) {
        System.out.println("[CARD EJECTED] Please take your card.");
        atmRoom.getAtm().setATMState(new IdleState());
    }
}

// ============================================================================
// 4. DRIVER CLASS: End-to-End SDE-1 LLD Interview Demonstration
// ============================================================================

public class ATM_Machine {
    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("  ATM SYSTEM DESIGN - STATE + CHAIN OF RESPONSIBILITY DEMO  ");
        System.out.println("============================================================\n");

        // 1. Initialize ATM with: 2 x Rs.2000 (4000), 3 x Rs.500 (1500), 5 x Rs.100 (500) = Rs. 6000 Total
        ATM atm = new ATM(101, "Bengaluru MG Road Branch", 2, 3, 5);
        atm.printATMStatus();

        // 2. Initialize User, BankAccount (Rs. 5000 balance), and Card (PIN: 1234)
        BankAccount account = new BankAccount(5000);
        Card card = new Card("4532-xxxx-xxxx-9876", 1234, "Om Trivedi", account);
        User user = new User("Om Trivedi", card, account);

        ATMRoom atmRoom = new ATMRoom(atm, user);

        // --------------------------------------------------------------------
        // SCENARIO 1: Withdraw Rs. 2700 (Tests State Pattern + Chain of Responsibility: 1x2000 + 1x500 + 2x100)
        // --------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: Withdraw Rs. 2700 (Happy Path) ---");
        atm.getATMState().insertCard(atmRoom);
        atm.getATMState().insertPin(atmRoom, 1234);
        atm.getATMState().selectOption(atmRoom, "withdraw");
        atm.getATMState().withdraw(atmRoom, 2700);
        atm.printATMStatus();

        // --------------------------------------------------------------------
        // SCENARIO 2: Deposit Rs. 1000 & Check Balance
        // --------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2: Deposit Rs. 1000 ---");
        atm.getATMState().insertCard(atmRoom);
        atm.getATMState().insertPin(atmRoom, 1234);
        atm.getATMState().selectOption(atmRoom, "deposit");
        atm.getATMState().deposit(atmRoom, 1000);

        // --------------------------------------------------------------------
        // SCENARIO 3: Invalid PIN Edge Case
        // --------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 3: Wrong PIN Entered ---");
        atm.getATMState().insertCard(atmRoom);
        atm.getATMState().insertPin(atmRoom, 9999);
    }
}
