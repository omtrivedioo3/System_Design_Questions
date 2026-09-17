package Patterns.StrategyPattern;

/**
 * SDE-1 LLD Interview Practice: Strategy Design Pattern
 * -----------------------------------------------------
 * Problem: E-Commerce Checkout Payment System
 *
 * Requirements:
 * 1. Create a Strategy Interface `PaymentStrategy` with a method:
 * void pay(double amount);
 *
 * 2. Implement at least 3 Concrete Strategies:
 * - `CreditCardPayment`: stores `cardNumber` and `cardHolderName`.
 * - `UpiPayment`: stores `upiId`.
 * - `CryptoPayment` (or `PayPalPayment`): stores `walletAddress` (or `email`).
 * (Think carefully: which fields should be `private final`?)
 *
 * 3. Create the Context Class `ShoppingCart` (or `CheckoutService`):
 * - Holds the current `PaymentStrategy`.
 * - Has a method `setPaymentStrategy(PaymentStrategy strategy)` so the user can
 * switch payment methods at runtime before paying!
 * (Think: Should the `paymentStrategy` field in `ShoppingCart` be `final` or
 * NOT?)
 * - Has a method `checkout(double amount)` that delegates payment to the
 * selected strategy
 * (and handles the edge case where no payment strategy is selected yet!).
 *
 * 4. In `main()`:
 * - Create a `ShoppingCart`.
 * - Pay Rs. 1500 using UPI.
 * - Change/switch the payment method on the SAME cart to Credit Card.
 * - Pay Rs. 5000 using Credit Card.
 */

interface PaymentStrategy {
  void pay(double amount);
}

class CreditCardPayment implements PaymentStrategy {
  private final String cardNumber;
  private final String cardHolderName;

  public CreditCardPayment(String cardNumber, String cardHolderName) {
    this.cardNumber = cardNumber;
    this.cardHolderName = cardHolderName;
  }

  @Override
  public void pay(double amount) {
    System.out.println("Paid " + amount + " using Credit Card (" + cardHolderName + ", " + cardNumber + ")");
  }
}

class UpiPayment implements PaymentStrategy {
  private final String upiId;

  public UpiPayment(String upiId) {
    this.upiId = upiId;
  }

  @Override
  public void pay(double amount) {
    System.out.println("Paid " + amount + " using UPI (" + upiId + ")");
  }
}

class CryptoPayment implements PaymentStrategy {
  private final String walletAddress;

  public CryptoPayment(String walletAddress) {
    this.walletAddress = walletAddress;
  }

  @Override
  public void pay(double amount) {
    System.out.println("Paid " + amount + " using Crypto (" + walletAddress + ")");
  }
}

class CheckoutService {
  private PaymentStrategy paymentStrategy;

  public CheckoutService(PaymentStrategy paymentStrategy) {
    this.paymentStrategy = paymentStrategy;
  }

  public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
    this.paymentStrategy = paymentStrategy;
  }

  public void checkout(double amount) {
    if (paymentStrategy == null) {
      throw new IllegalStateException("Please select a valid payment method before checkout.");
    }
    paymentStrategy.pay(amount);
  }
}

public class Strategy {
  public static void main(String[] args) {
    // Write your test code here

    CheckoutService checkoutService = new CheckoutService(new UpiPayment("email protected"));
    checkoutService.checkout(1500);

    checkoutService.setPaymentStrategy(new CreditCardPayment("1234-5678-9012-3456", "om"));
    checkoutService.checkout(5000);
  }
}
