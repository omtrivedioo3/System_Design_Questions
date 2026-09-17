package Patterns.DecoratorPattern;

/**
 * SDE-1 LLD Interview Practice: Decorator Design Pattern
 * ------------------------------------------------------
 * Problem: Domino's / Pizza Hut Custom Pizza Billing System
 *
 * Why Decorator Pattern?
 * Without Decorator, creating subclasses for every topping combination
 * (`MargheritaWithCheese`, `MargheritaWithCheeseAndMushroom`, etc.) causes
 * a "Class Explosion" (2^N classes). Decorator allows wrapping toppings
 * dynamically at runtime!
 *
 * Requirements:
 * 1. Create a Component Interface `Pizza`:
 * String getDescription();
 * double getCost();
 *
 * 2. Create 2 Concrete Base Pizzas implementing `Pizza`:
 * - `MargheritaPizza`: Description = "Margherita Pizza", Cost = 200.0
 * - `FarmhousePizza`: Description = "Farmhouse Pizza", Cost = 300.0
 *
 * 3. Create an Abstract Decorator Class `ToppingDecorator` that implements
 * `Pizza`:
 * - MUST hold a reference to a `Pizza` object (HAS-A relationship).
 * - Think: Should `protected final Pizza pizza;` (or `private final`) be
 * `final`?
 *
 * 4. Create at least 2 Concrete Toppings extending `ToppingDecorator`:
 * - `ExtraCheese`: adds ", Extra Cheese" to description, adds 50.0 to cost.
 * - `Mushroom`: adds ", Mushroom" to description, adds 40.0 to cost.
 * - `Jalapeno`: adds ", Jalapeno" to description, adds 30.0 to cost.
 *
 * 5. In `main()`:
 * - Order 1: Plain `MargheritaPizza` -> print description & cost.
 * - Order 2: `FarmhousePizza` wrapped with `ExtraCheese` AND `Mushroom` AND
 * `Jalapeno`
 * -> print final description & total cost.
 * - Order 3: `MargheritaPizza` with DOUBLE `ExtraCheese` (wrap `ExtraCheese`
 * twice!)
 * -> print final description & total cost.
 * 
 */

interface Pizza {
  String getDescription();

  double getCost();
}

/**
 * Margherita
 */
class Margherita implements Pizza {
  @Override
  public String getDescription() {
    return "Margherita Pizza";
  }

  @Override
  public double getCost() {
    return 200.0;
  }

}

class Farmhouse implements Pizza {
  @Override
  public String getDescription() {
    return "Farmhouse Pizza";
  }

  @Override
  public double getCost() {
    return 300.0;
  }

}

abstract class ToppingDecorator implements Pizza {
  protected final Pizza pizza;

  public ToppingDecorator(Pizza pizza) {
    if (pizza == null) {
      throw new IllegalArgumentException("Base pizza cannot be null");
    }
    this.pizza = pizza;
  }

  @Override
  public String getDescription() {
    return pizza.getDescription();
  }

  @Override
  public double getCost() {
    return pizza.getCost();
  }
}

class ExtraCheese extends ToppingDecorator {
  public ExtraCheese(Pizza pizza) {
    super(pizza);
  }

  @Override
  public String getDescription() {
    return super.getDescription() + ", Extra Cheese";
  }

  @Override
  public double getCost() {
    return super.getCost() + 50.0;
  }
}

class Mushroom extends ToppingDecorator {
  public Mushroom(Pizza pizza) {
    super(pizza);
  }

  @Override
  public String getDescription() {
    return super.getDescription() + ", Mushroom";
  }

  @Override
  public double getCost() {
    return super.getCost() + 40.0;
  }
}

class Jalapeno extends ToppingDecorator {
  public Jalapeno(Pizza pizza) {
    super(pizza);
  }

  @Override
  public String getDescription() {
    return super.getDescription() + ", Jalapeno";
  }

  @Override
  public double getCost() {
    return super.getCost() + 30.0;
  }
}

public class Decorator {
  public static void main(String[] args) {
    // Order 1: Plain Margherita
    Pizza order1 = new Margherita();
    System.out.println("Order 1: " + order1.getDescription() + " | Cost: Rs. " + order1.getCost());

    // Order 2: Farmhouse + Extra Cheese + Mushroom + Jalapeno
    Pizza order2 = new Farmhouse();
    order2 = new ExtraCheese(order2);
    order2 = new Mushroom(order2);
    order2 = new Jalapeno(order2);
    System.out.println("Order 2: " + order2.getDescription() + " | Cost: Rs. " + order2.getCost());

    // Order 3: Margherita with DOUBLE Extra Cheese
    Pizza order3 = new ExtraCheese(new ExtraCheese(new Margherita()));
    System.out.println("Order 3: " + order3.getDescription() + " | Cost: Rs. " + order3.getCost());
  }
}
