/*
 * =====================================================================================
 *                     SPLITWISE - LLD (SDE-1, 45-MIN VERSION)
 * =====================================================================================
 *
 * 45-MIN PLAN
 * -------------------------------------------------------------------------------------
 *   0-5   : Clarify requirements, agree what is out of scope.
 *   5-12  : Entities + class diagram. Mention Strategy + Factory for split types.
 *   12-35 : Code: User -> Split -> SplitStrategy (Equal/Exact/Percent) -> Expense
 *           -> BalanceSheet -> SplitwiseService.
 *   35-45 : Dry run with 3-4 users, discuss follow-ups (groups, simplify debts...).
 *
 * REQUIREMENTS
 * -------------------------------------------------------------------------------------
 *   In scope:
 *     1. Add users.
 *     2. Add an expense: paid by ONE user, split among N users in 3 ways:
 *          EQUAL   -> 1200 among 4          = 300 each
 *          EXACT   -> 1000 as 500/300/200   (must sum to total)
 *          PERCENT -> 1000 as 40%/30%/30%   (must sum to 100)
 *     3. Show balances: "B owes A 300" (for everyone, or for one user).
 *     4. Settle up: B pays A some amount.
 *   Out of scope (discuss only): groups, multiple payers, currencies,
 *     notifications, persistence/DB, auth.
 *
 * ENTITIES
 * -------------------------------------------------------------------------------------
 *   User, Split, Expense, SplitType (enum), SplitStrategy (+3 impls),
 *   SplitStrategyFactory, BalanceSheet, SplitwiseService
 *
 * CLASS DIAGRAM
 * -------------------------------------------------------------------------------------
 *
 *  +----------------------------------+      +-------------------------------+
 *  |        SplitwiseService          |<>--->|         BalanceSheet          |
 *  +----------------------------------+ 1  1 +-------------------------------+
 *  | - users: Map<String, User>       |      | - balances:                   |
 *  | - expenses: List<Expense>        |      |   Map<String,Map<String,Dbl>> |
 *  +----------------------------------+      +-------------------------------+
 *  | + addUser(user)                  |      | + addExpense(expense)         |
 *  | + addExpense(desc, amount,       |      | + settle(from, to, amount)    |
 *  |     paidBy, type, users, values) |      | + show() / show(userId)       |
 *  | + settleUp(from, to, amount)     |      +-------------------------------+
 *  | + showBalances()                 |
 *  +----------------+-----------------+      +-------------------------------+
 *                   | uses                   |    SplitStrategyFactory       |
 *                   +----------------------->| + get(SplitType): Strategy    |
 *                   |                        +---------------+---------------+
 *                   | creates                                | returns
 *                   v                                        v
 *  +----------------------------------+      +-------------------------------+
 *  |            Expense               |      |  <<interface>> SplitStrategy  |
 *  +----------------------------------+      +-------------------------------+
 *  | - id, description: String        |      | + split(amount, users,        |
 *  | - amount: double                 |      |         values): List<Split>  |
 *  | - paidBy: User                   |      +-------------------------------+
 *  | - splits: List<Split>            |          ^          ^          ^
 *  +----------------+-----------------+          |          |          |
 *                   <> 1..*               EqualSplit  ExactSplit  PercentSplit
 *                   v                       Strategy    Strategy     Strategy
 *  +----------------------------------+
 *  |              Split               |      +-------------------------------+
 *  +----------------------------------+      |             User              |
 *  | - user: User                     |----->+-------------------------------+
 *  | - amount: double  (share owed)   |      | - id, name, email: String     |
 *  +----------------------------------+      +-------------------------------+
 *
 * FLOW: addExpense("Dinner", 1200, A, EQUAL, [A,B,C,D], null)
 * -------------------------------------------------------------------------------------
 *
 *   validate amount > 0, payer & users exist ---- fail --> throw exception
 *        |
 *   strategy = SplitStrategyFactory.get(EQUAL)
 *        |
 *   splits = strategy.split(1200, users, values)
 *        |   (validates: EXACT sums to amount, PERCENT sums to 100)
 *        v
 *   expense = new Expense(...) ; expenses.add(expense)
 *        |
 *   balanceSheet.addExpense(expense)
 *        |   for each split where split.user != paidBy:
 *        |       balances[paidBy][user] += share     (user owes payer)
 *        |       balances[user][paidBy] -= share     (mirror entry)
 *        v
 *   done  ->  A: +900 ;  B, C, D: each owes A 300
 *
 * BALANCE SHEET IDEA (most important part - explain this clearly!)
 * -------------------------------------------------------------------------------------
 *   balances.get(X).get(Y) =  +300  -> Y owes X 300
 *   balances.get(X).get(Y) =  -300  -> X owes Y 300
 *   Always update BOTH sides so balances[X][Y] == -balances[Y][X].
 *   Updating is O(number of splits) per expense; lookup is O(1).
 *
 * KEY POINTS INTERVIEWERS LIKE
 * -------------------------------------------------------------------------------------
 *   - STRATEGY pattern for split types: new type (e.g. SHARES "2:1:1") = new class,
 *     no if-else inside Expense/Service  -> Open/Closed Principle.
 *   - FACTORY to pick the strategy from SplitType.
 *   - VALIDATION lives inside each strategy (exact sum / percent sum = 100).
 *   - ROUNDING: 100 / 3 = 33.33 * 3 = 99.99 -> give the leftover 0.01 to the first
 *     user. (Real systems store money as long paise/cents or BigDecimal - say this!)
 *   - SRP: Strategy = how to split, BalanceSheet = who owes whom,
 *     Service = orchestration / validation (the API the client calls).
 *   - Money map keyed by userId (String), not by User object -> no equals/hashCode issues.
 *
 * FOLLOW-UPS (answer verbally, code only if time is left)
 * -------------------------------------------------------------------------------------
 *   - Groups?          Group(id, name, members, expenses) with its own BalanceSheet.
 *   - Simplify debts?  Compute NET balance per user (+ gets, - owes). Repeatedly
 *                      match the biggest debtor with the biggest creditor.
 *                      At most (N - 1) transactions.  (implemented as BONUS below)
 *   - Multiple payers? Expense has List<Payment(user, amount)> instead of one paidBy.
 *   - Concurrency?     Make BalanceSheet methods synchronized / use a lock per group.
 *   - Notifications?   Observer pattern: notify users when an expense is added.
 *   - Singleton?       SplitwiseService could be a Singleton, but dependency
 *                      injection is preferred (easier to test).
 * =====================================================================================
 */
package Splitwise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

// -------------------------------------- MODELS ---------------------------------------

class User {
  private final String id;
  private final String name;
  private final String email;

  public User(String id, String name, String email) {
    this.id = id;
    this.name = name;
    this.email = email;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }
}

enum SplitType {
  EQUAL,
  EXACT,
  PERCENT
}

/** How much ONE user owes for ONE expense. */
class Split {
  private final User user;
  private final double amount;

  public Split(User user, double amount) {
    this.user = user;
    this.amount = amount;
  }

  public User getUser() {
    return user;
  }

  public double getAmount() {
    return amount;
  }
}

class Expense {
  private final String id;
  private final String description;
  private final double amount;
  private final User paidBy;
  private final List<Split> splits;

  public Expense(String id, String description, double amount, User paidBy, List<Split> splits) {
    this.id = id;
    this.description = description;
    this.amount = amount;
    this.paidBy = paidBy;
    this.splits = splits;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public double getAmount() {
    return amount;
  }

  public User getPaidBy() {
    return paidBy;
  }

  public List<Split> getSplits() {
    return splits;
  }
}

// ---------------------------------- SPLIT STRATEGY
// -----------------------------------

interface SplitStrategy {
  /**
   * @param values extra input: null for EQUAL, exact amounts for EXACT,
   *               percentages for
   *               PERCENT (same order as users)
   */
  List<Split> split(double amount, List<User> users, List<Double> values);
}

class EqualSplitStrategy implements SplitStrategy {
  @Override
  public List<Split> split(double amount, List<User> users, List<Double> values) {
    double share = Money.round(amount / users.size());
    double leftover = Money.round(amount - share * users.size()); // e.g. 0.01 for 100/3

    List<Split> splits = new ArrayList<>();
    for (int i = 0; i < users.size(); i++) {
      double userShare = (i == 0) ? Money.round(share + leftover) : share;
      splits.add(new Split(users.get(i), userShare));
    }
    return splits;
  }
}

class ExactSplitStrategy implements SplitStrategy {
  @Override
  public List<Split> split(double amount, List<User> users, List<Double> values) {
    if (values == null || values.size() != users.size()) {
      throw new IllegalArgumentException("EXACT: give one amount per user");
    }
    double sum = 0;
    List<Split> splits = new ArrayList<>();
    for (int i = 0; i < users.size(); i++) {
      sum += values.get(i);
      splits.add(new Split(users.get(i), values.get(i)));
    }
    if (Math.abs(sum - amount) > 0.01) {
      throw new IllegalArgumentException("EXACT: amounts add up to " + sum + ", not " + amount);
    }
    return splits;
  }
}

class PercentSplitStrategy implements SplitStrategy {
  @Override
  public List<Split> split(double amount, List<User> users, List<Double> values) {
    if (values == null || values.size() != users.size()) {
      throw new IllegalArgumentException("PERCENT: give one percentage per user");
    }
    double totalPercent = 0;
    for (double p : values) {
      totalPercent += p;
    }
    if (Math.abs(totalPercent - 100) > 0.01) {
      throw new IllegalArgumentException("PERCENT: percentages add up to " + totalPercent);
    }
    List<Split> splits = new ArrayList<>();
    for (int i = 0; i < users.size(); i++) {
      splits.add(new Split(users.get(i), Money.round(amount * values.get(i) / 100)));
    }
    return splits;
  }
}

/** Factory: maps SplitType -> strategy object. */
class SplitStrategyFactory {
  public static SplitStrategy get(SplitType type) {
    switch (type) {
      case EQUAL:
        return new EqualSplitStrategy();
      case EXACT:
        return new ExactSplitStrategy();
      case PERCENT:
        return new PercentSplitStrategy();
      default:
        throw new IllegalArgumentException("Unknown split type " + type);
    }
  }
}

/** Tiny helper to keep money at 2 decimals. */
class Money {
  static double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}

// ----------------------------------- BALANCE SHEET
// -----------------------------------

/**
 * balances[X][Y] > 0 -> Y owes X. balances[X][Y] < 0 -> X owes Y. Both sides
 * are always kept
 * in sync: balances[X][Y] == -balances[Y][X].
 */
class BalanceSheet {
  private final Map<String, Map<String, Double>> balances = new HashMap<>();

  public void addExpense(Expense expense) {
    String payer = expense.getPaidBy().getId();
    for (Split split : expense.getSplits()) {
      String user = split.getUser().getId();
      if (!user.equals(payer)) {
        update(payer, user, split.getAmount()); // user owes payer
      }
    }
  }

  /** 'from' pays 'to' -> from's debt towards 'to' goes down. */
  public void settle(String from, String to, double amount) {
    update(from, to, amount);
  }

  public double getBalance(String x, String y) {
    return balances.getOrDefault(x, new HashMap<>()).getOrDefault(y, 0.0);
  }

  /** Net amount for a user: positive = others owe them, negative = they owe. */
  public Map<String, Double> getNetBalances() {
    Map<String, Double> net = new HashMap<>();
    for (String x : balances.keySet()) {
      double total = 0;
      for (double v : balances.get(x).values()) {
        total += v;
      }
      net.put(x, Money.round(total));
    }
    return net;
  }

  public void show(Map<String, User> users) {
    boolean any = false;
    for (String x : balances.keySet()) {
      for (Map.Entry<String, Double> e : balances.get(x).entrySet()) {
        if (e.getValue() > 0.009) { // print each pair once (only the positive side)
          System.out.println("  " + users.get(e.getKey()).getName() + " owes "
              + users.get(x).getName() + ": " + Money.round(e.getValue()));
          any = true;
        }
      }
    }
    if (!any) {
      System.out.println("  No balances - everyone is settled up");
    }
  }

  public void show(String userId, Map<String, User> users) {
    Map<String, Double> row = balances.getOrDefault(userId, new HashMap<>());
    String name = users.get(userId).getName();
    boolean any = false;
    for (Map.Entry<String, Double> e : row.entrySet()) {
      String other = users.get(e.getKey()).getName();
      if (e.getValue() > 0.009) {
        System.out.println("  " + other + " owes " + name + ": " + Money.round(e.getValue()));
        any = true;
      } else if (e.getValue() < -0.009) {
        System.out.println("  " + name + " owes " + other + ": " + Money.round(-e.getValue()));
        any = true;
      }
    }
    if (!any) {
      System.out.println("  " + name + " is all settled up");
    }
  }

  private void update(String creditor, String debtor, double amount) {
    balances.computeIfAbsent(creditor, k -> new HashMap<>()).merge(debtor, amount, Double::sum);
    balances.computeIfAbsent(debtor, k -> new HashMap<>()).merge(creditor, -amount, Double::sum);
  }
}

// ------------------------------------- SERVICE
// ---------------------------------------

/** Entry point for clients (like a controller/service layer). */
class SplitwiseService {
  private final Map<String, User> users = new HashMap<>();
  private final List<Expense> expenses = new ArrayList<>();
  private final BalanceSheet balanceSheet = new BalanceSheet();
  private int expenseCounter = 0;

  public void addUser(User user) {
    users.put(user.getId(), user);
  }

  public Expense addExpense(String description, double amount, String paidById,
      SplitType type, List<String> userIds, List<Double> values) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    User paidBy = getUser(paidById);
    List<User> participants = new ArrayList<>();
    for (String id : userIds) {
      participants.add(getUser(id));
    }

    List<Split> splits = SplitStrategyFactory.get(type).split(amount, participants, values);
    Expense expense = new Expense("E" + (++expenseCounter), description, amount, paidBy, splits);
    expenses.add(expense);
    balanceSheet.addExpense(expense);

    System.out.println(paidBy.getName() + " paid " + amount + " for '" + description
        + "' (" + type + ")");
    return expense;
  }

  public void settleUp(String fromId, String toId, double amount) {
    User from = getUser(fromId);
    User to = getUser(toId);
    double owed = balanceSheet.getBalance(toId, fromId); // how much 'from' owes 'to'
    if (amount <= 0 || amount > owed + 0.009) {
      throw new IllegalArgumentException(
          from.getName() + " owes " + to.getName() + " only " + Money.round(Math.max(0, owed)));
    }
    balanceSheet.settle(fromId, toId, amount);
    System.out.println(from.getName() + " paid " + to.getName() + " " + amount);
  }

  public void showBalances() {
    System.out.println("All balances:");
    balanceSheet.show(users);
  }

  public void showBalance(String userId) {
    System.out.println("Balances for " + getUser(userId).getName() + ":");
    balanceSheet.show(userId, users);
  }

  /**
   * BONUS (follow-up): minimum-ish transactions using net balances. Greedy:
   * always match the
   * biggest debtor with the biggest creditor.
   */
  public void showSimplifiedDebts() {
    PriorityQueue<Map.Entry<String, Double>> creditors = new PriorityQueue<>(
        (a, b) -> Double.compare(b.getValue(), a.getValue()));
    PriorityQueue<Map.Entry<String, Double>> debtors = new PriorityQueue<>(
        (a, b) -> Double.compare(a.getValue(), b.getValue()));
    for (Map.Entry<String, Double> e : balanceSheet.getNetBalances().entrySet()) {
      if (e.getValue() > 0.009) {
        creditors.add(e);
      } else if (e.getValue() < -0.009) {
        debtors.add(e);
      }
    }

    System.out.println("Simplified debts:");
    while (!creditors.isEmpty() && !debtors.isEmpty()) {
      Map.Entry<String, Double> c = creditors.poll();
      Map.Entry<String, Double> d = debtors.poll();
      double pay = Math.min(c.getValue(), -d.getValue());
      System.out.println("  " + users.get(d.getKey()).getName() + " pays "
          + users.get(c.getKey()).getName() + ": " + Money.round(pay));

      double cLeft = Money.round(c.getValue() - pay);
      double dLeft = Money.round(d.getValue() + pay);
      if (cLeft > 0.009) {
        creditors.add(Map.entry(c.getKey(), cLeft));
      }
      if (dLeft < -0.009) {
        debtors.add(Map.entry(d.getKey(), dLeft));
      }
    }
  }

  private User getUser(String id) {
    User user = users.get(id);
    if (user == null) {
      throw new IllegalArgumentException("Unknown user: " + id);
    }
    return user;
  }
}

// --------------------------------------- DEMO
// ----------------------------------------

public class SpliwiseApp {
  public static void main(String[] args) {
    SplitwiseService app = new SplitwiseService();
    app.addUser(new User("u1", "Alice", "alice@mail.com"));
    app.addUser(new User("u2", "Bob", "bob@mail.com"));
    app.addUser(new User("u3", "Charlie", "charlie@mail.com"));
    app.addUser(new User("u4", "David", "david@mail.com"));

    // 1. EQUAL: Alice pays 1200, split among all 4 -> 300 each
    app.addExpense("Dinner", 1200, "u1", SplitType.EQUAL,
        List.of("u1", "u2", "u3", "u4"), null);

    // 2. EXACT: Bob pays 1000 -> Alice 500, Bob 300, Charlie 200
    app.addExpense("Movie", 1000, "u2", SplitType.EXACT,
        List.of("u1", "u2", "u3"), List.of(500.0, 300.0, 200.0));

    // 3. PERCENT: Charlie pays 1000 -> Alice 40%, Charlie 30%, David 30%
    app.addExpense("Cab", 1000, "u3", SplitType.PERCENT,
        List.of("u1", "u3", "u4"), List.of(40.0, 30.0, 30.0));

    // 4. EQUAL with rounding: David pays 100 among 3 -> 33.34 / 33.33 / 33.33
    app.addExpense("Snacks", 100, "u4", SplitType.EQUAL, List.of("u4", "u1", "u2"), null);

    System.out.println();
    app.showBalances();
    System.out.println();
    app.showBalance("u1");

    System.out.println();
    app.settleUp("u1", "u3", 100); // Alice pays Charlie the 100 she owes
    app.showBalance("u1");

    System.out.println("\n--- Validation ---");
    try {
      app.addExpense("Bad", 500, "u1", SplitType.EXACT, List.of("u1", "u2"),
          List.of(100.0, 100.0));
    } catch (IllegalArgumentException e) {
      System.out.println("Error: " + e.getMessage());
    }
    try {
      app.addExpense("Bad", 500, "u1", SplitType.PERCENT, List.of("u1", "u2"),
          List.of(50.0, 20.0));
    } catch (IllegalArgumentException e) {
      System.out.println("Error: " + e.getMessage());
    }
    try {
      app.settleUp("u2", "u3", 1000);
    } catch (IllegalArgumentException e) {
      System.out.println("Error: " + e.getMessage());
    }

    System.out.println();
    app.showSimplifiedDebts();
  }
}
