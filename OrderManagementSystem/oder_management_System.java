/*
 * =====================================================================================
 *                ORDER MANAGEMENT SYSTEM - LLD (SDE-1, 45-60 MIN VERSION)
 * =====================================================================================
 *
 * 1. REQUIREMENTS (clarify first)
 * -------------------------------------------------------------------------------------
 *   In scope:
 *     1. Products with price; Inventory keeps stock per product.
 *     2. User adds/removes products in a Cart.
 *     3. Place order from cart -> check & reserve stock -> pay -> order confirmed.
 *     4. Payment methods: UPI, Card (easy to add more).
 *     5. Order lifecycle: PLACED -> CONFIRMED -> SHIPPED -> DELIVERED
 *                          |           |
 *                          v           v
 *                   PAYMENT_FAILED  CANCELLED (only before shipping, stock restored)
 *     6. View a user's order history.
 *   Out of scope (mention only): discounts/coupons, delivery partner assignment,
 *     returns/refunds flow, multiple warehouses, DB, notifications.
 *
 * 2. ENTITIES
 * -------------------------------------------------------------------------------------
 *   User, Product, Cart, CartItem, Order, OrderItem, OrderStatus (enum),
 *   Inventory, PaymentStrategy (+ UpiPayment, CardPayment), OrderService
 *
 * 3. UML CLASS DIAGRAM
 * -------------------------------------------------------------------------------------
 *
 *   +------------------------------+          +---------------------------+
 *   |         OrderService         |--------->|         Inventory         |
 *   +------------------------------+   uses   +---------------------------+
 *   | - inventory: Inventory       |          | - stock: Map<pid, Integer>|
 *   | - orders: Map<id, Order>     |          +---------------------------+
 *   +------------------------------+          | + addStock(pid, qty)      |
 *   | + placeOrder(user, payment)  |          | + reserve(items): boolean |
 *   | + cancelOrder(orderId)       |          | + release(items)          |
 *   | + updateStatus(id, status)   |          +---------------------------+
 *   | + getOrders(user)            |
 *   +------+---------------+-------+          +---------------------------+
 *          |               |  uses            | <<interface>>             |
 *          | creates       +----------------->| PaymentStrategy           |
 *          v                                  +---------------------------+
 *   +------------------------------+          | + pay(amount): boolean    |
 *   |            Order             |          +---------------------------+
 *   +------------------------------+                ^              ^
 *   | - id: String                 |                |              |
 *   | - user: User                 |          UpiPayment      CardPayment
 *   | - items: List<OrderItem>     |
 *   | - totalAmount: double        |          +---------------------------+
 *   | - status: OrderStatus        |--------->|  <<enum>> OrderStatus     |
 *   +------------------------------+          +---------------------------+
 *   | + updateStatus(newStatus)    |          | PLACED, CONFIRMED,        |
 *   +--------------+---------------+          | PAYMENT_FAILED, SHIPPED,  |
 *                  <> 1..*                    | DELIVERED, CANCELLED      |
 *                  v                          | + canMoveTo(next): boolean|
 *   +------------------------------+          +---------------------------+
 *   |          OrderItem           |
 *   +------------------------------+          +---------------------------+
 *   | - product: Product           |--------->|         Product           |
 *   | - quantity: int              |          +---------------------------+
 *   | - priceAtOrder: double       |          | - id, name: String        |
 *   +------------------------------+          | - price: double           |
 *                                             +---------------------------+
 *   +------------------------------+                       ^
 *   |            User              |<>---+                 |
 *   +------------------------------+     |  1   +---------------------------+
 *   | - id, name: String           |     +----->|  Cart                     |
 *   | - cart: Cart                 |            +---------------------------+
 *   +------------------------------+            | - items: Map<pid,CartItem>|
 *                                               | + add / remove / clear    |
 *                                               | + getTotal()              |
 *                                               +---------------------------+
 *                                                 (CartItem = product + qty)
 *
 *   Relationships:
 *     User   <>-- Cart       composition: every user owns one cart
 *     Order  <>-- OrderItem  composition: items exist only inside an order
 *     OrderItem --> Product  association: product exists independently
 *     OrderService --> Inventory, PaymentStrategy : uses (dependency)
 *     UpiPayment / CardPayment implement PaymentStrategy : STRATEGY pattern
 *
 *   Explanation:
 *     - Cart vs Order: Cart is temporary & editable. Order is a permanent record,
 *       created from the cart at checkout, then the cart is cleared.
 *     - CartItem vs OrderItem: OrderItem stores priceAtOrder (a SNAPSHOT). If the
 *       product price changes tomorrow, old orders must still show what user paid.
 *     - Inventory is separate from Product: stock changes very often and needs
 *       locking; Product details (name, price) rarely change.
 *     - OrderService is the "brain": it coordinates cart, inventory, payment, order.
 *
 * 4. FLOW: placeOrder(user, paymentMethod)
 * -------------------------------------------------------------------------------------
 *
 *     cart empty? ------------------------------- yes --> reject
 *        | no
 *     convert CartItems -> OrderItems (snapshot price)
 *        |
 *     inventory.reserve(items)  (all-or-nothing) - fail --> reject "out of stock"
 *        | ok (stock deducted)
 *     order = new Order(...)  status = PLACED
 *        |
 *     payment.pay(total) ------------------------ fail --> inventory.release(items)
 *        | success                                         status = PAYMENT_FAILED
 *     status = CONFIRMED
 *        |
 *     cart.clear(), save order, return order
 *
 *   Why reserve BEFORE payment? So two users can't buy the last item at the same
 *   time and both get charged. If payment fails, we release (roll back) the stock.
 *
 * 5. KEY POINTS INTERVIEWERS LIKE
 * -------------------------------------------------------------------------------------
 *   - STRATEGY for payment: add WalletPayment = new class, no change in OrderService.
 *   - STATE TRANSITIONS validated in the enum (canMoveTo) - no illegal jumps like
 *     DELIVERED -> PLACED or cancelling a SHIPPED order.
 *   - Inventory.reserve() is 'synchronized' + all-or-nothing -> no overselling.
 *   - Price snapshot in OrderItem.
 *   - Rollback: payment failure / cancellation releases stock.
 *   - SRP: Cart = selection, Inventory = stock, Payment = money, Order = record,
 *     OrderService = orchestration.
 *   - Money: double here for simplicity; say "BigDecimal / paise as long in real code".
 *
 * 6. FOLLOW-UPS (answer verbally)
 * -------------------------------------------------------------------------------------
 *   - Notifications on status change?  OBSERVER: OrderObserver.onStatusChange(order).
 *   - Discounts/coupons?  Strategy/Decorator on price: DiscountStrategy.apply(total).
 *   - More complex lifecycle?  STATE pattern (class per state) instead of enum.
 *   - Refunds?  On cancel of a CONFIRMED order call payment.refund(amount).
 *   - Scale / concurrency?  Lock per product (ConcurrentHashMap + per-key lock) or
 *     DB row lock / optimistic locking with a version column.
 *   - Creating Order with many fields?  BUILDER pattern.
 * =====================================================================================
 */
package OrderManagementSystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// -------------------------------------- MODELS ---------------------------------------

class Product {
  private final String id;
  private final String name;
  private final double price;

  public Product(String id, String name, double price) {
    this.id = id;
    this.name = name;
    this.price = price;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public double getPrice() {
    return price;
  }
}

class CartItem {
  private final Product product;
  private int quantity;

  public CartItem(Product product, int quantity) {
    this.product = product;
    this.quantity = quantity;
  }

  public Product getProduct() {
    return product;
  }

  public int getQuantity() {
    return quantity;
  }

  public void addQuantity(int qty) {
    this.quantity += qty;
  }
}

class Cart {
  private final Map<String, CartItem> items = new LinkedHashMap<>(); // productId -> item

  public void addItem(Product product, int qty) {
    if (qty <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }
    CartItem existing = items.get(product.getId());
    if (existing != null) {
      existing.addQuantity(qty);
    } else {
      items.put(product.getId(), new CartItem(product, qty));
    }
  }

  public void removeItem(String productId) {
    items.remove(productId);
  }

  public Collection<CartItem> getItems() {
    return items.values();
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  public void clear() {
    items.clear();
  }
}

class User {
  private final String id;
  private final String name;
  private final Cart cart = new Cart(); // composition: user owns one cart

  public User(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Cart getCart() {
    return cart;
  }
}

/** Snapshot of a product inside an order (price frozen at order time). */
class OrderItem {
  private final Product product;
  private final int quantity;
  private final double priceAtOrder;

  public OrderItem(Product product, int quantity) {
    this.product = product;
    this.quantity = quantity;
    this.priceAtOrder = product.getPrice();
  }

  public Product getProduct() {
    return product;
  }

  public int getQuantity() {
    return quantity;
  }

  public double getSubtotal() {
    return priceAtOrder * quantity;
  }
}

// ---------------------------------- ORDER STATUS -------------------------------------

enum OrderStatus {
  PLACED,
  CONFIRMED,
  PAYMENT_FAILED,
  SHIPPED,
  DELIVERED,
  CANCELLED;

  /** Allowed transitions - the order "state machine". */
  public boolean canMoveTo(OrderStatus next) {
    switch (this) {
      case PLACED:
        return next == CONFIRMED || next == PAYMENT_FAILED || next == CANCELLED;
      case CONFIRMED:
        return next == SHIPPED || next == CANCELLED;
      case SHIPPED:
        return next == DELIVERED;
      default:
        return false; // DELIVERED, CANCELLED, PAYMENT_FAILED are final
    }
  }
}

class Order {
  private final String id;
  private final User user;
  private final List<OrderItem> items;
  private final double totalAmount;
  private OrderStatus status;

  public Order(String id, User user, List<OrderItem> items) {
    this.id = id;
    this.user = user;
    this.items = List.copyOf(items);
    this.totalAmount = items.stream().mapToDouble(OrderItem::getSubtotal).sum();
    this.status = OrderStatus.PLACED;
  }

  public void updateStatus(OrderStatus next) {
    if (!status.canMoveTo(next)) {
      throw new IllegalStateException("Order " + id + ": cannot move " + status + " -> " + next);
    }
    System.out.println("  Order " + id + ": " + status + " -> " + next);
    status = next;
  }

  public String getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public List<OrderItem> getItems() {
    return items;
  }

  public double getTotalAmount() {
    return totalAmount;
  }

  public OrderStatus getStatus() {
    return status;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(id + " [" + status + "] Rs." + totalAmount + " :");
    for (OrderItem item : items) {
      sb.append(" ").append(item.getProduct().getName()).append(" x").append(item.getQuantity());
    }
    return sb.toString();
  }
}

// ------------------------------------ INVENTORY --------------------------------------

class Inventory {
  private final Map<String, Integer> stock = new HashMap<>(); // productId -> quantity

  public synchronized void addStock(String productId, int qty) {
    stock.merge(productId, qty, Integer::sum);
  }

  public synchronized int getStock(String productId) {
    return stock.getOrDefault(productId, 0);
  }

  /** All-or-nothing: either every item is reserved, or none is. */
  public synchronized boolean reserve(List<OrderItem> items) {
    for (OrderItem item : items) { // 1. check everything first
      if (getStock(item.getProduct().getId()) < item.getQuantity()) {
        return false;
      }
    }
    for (OrderItem item : items) { // 2. then deduct
      stock.merge(item.getProduct().getId(), -item.getQuantity(), Integer::sum);
    }
    return true;
  }

  public synchronized void release(List<OrderItem> items) {
    for (OrderItem item : items) {
      stock.merge(item.getProduct().getId(), item.getQuantity(), Integer::sum);
    }
  }
}

// ------------------------------------- PAYMENT ---------------------------------------

interface PaymentStrategy {
  boolean pay(double amount);
}

class UpiPayment implements PaymentStrategy {
  private final String upiId;

  public UpiPayment(String upiId) {
    this.upiId = upiId;
  }

  @Override
  public boolean pay(double amount) {
    System.out.println("  Paid Rs." + amount + " via UPI (" + upiId + ")");
    return true;
  }
}

class CardPayment implements PaymentStrategy {
  private final String cardNumber;
  private final double limit; // used to simulate a failure

  public CardPayment(String cardNumber, double limit) {
    this.cardNumber = cardNumber;
    this.limit = limit;
  }

  @Override
  public boolean pay(double amount) {
    String masked = "****" + cardNumber.substring(cardNumber.length() - 4);
    if (amount > limit) {
      System.out.println("  Card " + masked + " declined for Rs." + amount);
      return false;
    }
    System.out.println("  Paid Rs." + amount + " via Card " + masked);
    return true;
  }
}

// ---------------------------------- ORDER SERVICE ------------------------------------

class OrderService {
  private final Inventory inventory;
  private final Map<String, Order> orders = new LinkedHashMap<>();
  private int orderCounter = 0;

  public OrderService(Inventory inventory) {
    this.inventory = inventory;
  }

  public Order placeOrder(User user, PaymentStrategy payment) {
    Cart cart = user.getCart();
    if (cart.isEmpty()) {
      throw new IllegalStateException("Cart is empty");
    }

    // 1. Cart -> OrderItems (price snapshot)
    List<OrderItem> items = new ArrayList<>();
    for (CartItem ci : cart.getItems()) {
      items.add(new OrderItem(ci.getProduct(), ci.getQuantity()));
    }

    // 2. Reserve stock (all-or-nothing)
    if (!inventory.reserve(items)) {
      throw new IllegalStateException("Out of stock for one or more items");
    }

    // 3. Create order
    Order order = new Order("ORD-" + (++orderCounter), user, items);
    orders.put(order.getId(), order);
    System.out.println("  Order " + order.getId() + " PLACED for " + user.getName()
        + ", total Rs." + order.getTotalAmount());

    // 4. Pay -> confirm, or roll back stock
    if (payment.pay(order.getTotalAmount())) {
      order.updateStatus(OrderStatus.CONFIRMED);
      cart.clear(); // only clear cart when the order succeeds
    } else {
      inventory.release(items);
      order.updateStatus(OrderStatus.PAYMENT_FAILED);
    }
    return order;
  }

  public void cancelOrder(String orderId) {
    Order order = getOrder(orderId);
    order.updateStatus(OrderStatus.CANCELLED); // throws if already shipped/delivered
    inventory.release(order.getItems());
    System.out.println("  Stock restored for " + orderId + " (refund would be triggered here)");
  }

  public void updateStatus(String orderId, OrderStatus status) {
    getOrder(orderId).updateStatus(status);
  }

  public List<Order> getOrders(User user) {
    List<Order> result = new ArrayList<>();
    for (Order o : orders.values()) {
      if (o.getUser().getId().equals(user.getId())) {
        result.add(o);
      }
    }
    return result;
  }

  private Order getOrder(String orderId) {
    Order order = orders.get(orderId);
    if (order == null) {
      throw new IllegalArgumentException("Order not found: " + orderId);
    }
    return order;
  }
}

// --------------------------------------- DEMO ----------------------------------------

public class oder_management_System {
  public static void main(String[] args) {
    Product phone = new Product("P1", "Phone", 20000);
    Product earbuds = new Product("P2", "Earbuds", 2000);
    Product charger = new Product("P3", "Charger", 500);

    Inventory inventory = new Inventory();
    inventory.addStock("P1", 2);
    inventory.addStock("P2", 5);
    inventory.addStock("P3", 10);

    OrderService service = new OrderService(inventory);
    User alice = new User("U1", "Alice");
    User bob = new User("U2", "Bob");

    System.out.println("1) Alice orders Phone + 2 Earbuds via UPI");
    alice.getCart().addItem(phone, 1);
    alice.getCart().addItem(earbuds, 2);
    Order o1 = service.placeOrder(alice, new UpiPayment("alice@upi"));
    service.updateStatus(o1.getId(), OrderStatus.SHIPPED);
    service.updateStatus(o1.getId(), OrderStatus.DELIVERED);

    System.out.println("\n2) Try to cancel a DELIVERED order");
    try {
      service.cancelOrder(o1.getId());
    } catch (IllegalStateException e) {
      System.out.println("  Error: " + e.getMessage());
    }

    System.out.println("\n3) Bob's card is declined -> stock rolled back");
    bob.getCart().addItem(phone, 1);
    service.placeOrder(bob, new CardPayment("1234567812345678", 5000));
    System.out.println("  Phone stock now: " + inventory.getStock("P1") + " (unchanged)");

    System.out.println("\n4) Bob retries with UPI, then cancels before shipping");
    Order o3 = service.placeOrder(bob, new UpiPayment("bob@upi"));
    System.out.println("  Phone stock now: " + inventory.getStock("P1"));
    service.cancelOrder(o3.getId());
    System.out.println("  Phone stock now: " + inventory.getStock("P1"));

    System.out.println("\n5) Out of stock");
    alice.getCart().addItem(phone, 5);
    try {
      service.placeOrder(alice, new UpiPayment("alice@upi"));
    } catch (IllegalStateException e) {
      System.out.println("  Error: " + e.getMessage());
    }

    System.out.println("\n6) Empty cart");
    try {
      service.placeOrder(new User("U3", "Carol"), new UpiPayment("carol@upi"));
    } catch (IllegalStateException e) {
      System.out.println("  Error: " + e.getMessage());
    }

    System.out.println("\nOrder history:");
    for (Order o : service.getOrders(alice)) {
      System.out.println("  Alice: " + o);
    }
    for (Order o : service.getOrders(bob)) {
      System.out.println("  Bob  : " + o);
    }
  }
}
