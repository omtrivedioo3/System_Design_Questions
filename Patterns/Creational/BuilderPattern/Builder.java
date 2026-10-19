/*
 * =====================================================================================
 *                            BUILDER PATTERN (CREATIONAL) - NOTES
 * =====================================================================================
 *
 * 1. WHAT IS IT?
 * -------------------------------------------------------------------------------------
 *   Builder lets you CREATE a complex object STEP BY STEP, then call build() once
 *   to get the final (usually IMMUTABLE) object.
 *
 *     Pizza p = new Pizza.Builder("LARGE", "THIN")   // required fields
 *                   .cheese(true)                    // optional
 *                   .addTopping("Mushroom")          // optional
 *                   .build();                        // validate + create
 *
 * 2. WHAT PROBLEM DOES IT SOLVE?
 * -------------------------------------------------------------------------------------
 *   An object with MANY fields, most of them OPTIONAL.
 *
 *   (a) Telescoping constructors - one constructor per combination:
 *         new Pizza("LARGE", "THIN");
 *         new Pizza("LARGE", "THIN", true);
 *         new Pizza("LARGE", "THIN", true, false, List.of("Onion"));  // what is 'false'??
 *       -> hard to read, easy to swap arguments, explodes as fields grow.
 *
 *   (b) Empty constructor + setters:
 *         Pizza p = new Pizza(); p.setSize("LARGE"); ...
 *       -> object can be used half-built, it is MUTABLE, no place to validate.
 *
 *   Builder fixes both: readable named steps, validation in build(), immutable result.
 *
 * 3. UML
 * -------------------------------------------------------------------------------------
 *
 *     +-------------------------------+   creates   +--------------------------------+
 *     |        Pizza.Builder          |------------>|             Pizza              |
 *     |   (static nested class)       |             |      (immutable product)       |
 *     +-------------------------------+             +--------------------------------+
 *     | - size, crust   (required)    |             | - final size, crust            |
 *     | - cheese, extraSauce          |             | - final cheese, extraSauce     |
 *     | - toppings: List<String>      |             | - final toppings: List<String> |
 *     +-------------------------------+             +--------------------------------+
 *     | + Builder(size, crust)        |             | - Pizza(Builder b)  (PRIVATE)  |
 *     | + cheese(bool): Builder       |             | + getters only, NO setters     |
 *     | + extraSauce(bool): Builder   |             | + getPrice()                   |
 *     | + addTopping(t): Builder      |             +--------------------------------+
 *     | + build(): Pizza  (validates) |
 *     +-------------------------------+
 *
 *   Every setter-like method returns 'this' (the Builder) -> method chaining.
 *   Pizza's constructor is PRIVATE -> the ONLY way to create a Pizza is via Builder.
 *
 * 4. FLOW
 * -------------------------------------------------------------------------------------
 *
 *     new Builder(size, crust) --> .cheese() --> .addTopping() --> ... --> .build()
 *            |                                                               |
 *     required fields                                         validate (throw if bad)
 *     set up front                                                           |
 *                                                          new Pizza(this) -> immutable
 *
 * 5. BUILDER vs DECORATOR  (they look similar - both "add things to a pizza"!)
 * -------------------------------------------------------------------------------------
 *
 *   | Point          | BUILDER                          | DECORATOR                          |
 *   |----------------|----------------------------------|------------------------------------|
 *   | Type           | Creational (how to CREATE)       | Structural (how to EXTEND/WRAP)    |
 *   | Goal           | Build ONE object with many       | Add BEHAVIOUR to an existing       |
 *   |                | optional fields, readably        | object at runtime, layer by layer  |
 *   | Result         | ONE final object (immutable)     | A CHAIN of wrapper objects         |
 *   | Adds           | DATA / configuration (fields)    | BEHAVIOUR (each wrapper changes    |
 *   |                |                                  | getCost(), getDescription()...)    |
 *   | When it acts   | Once, at creation (build())      | Every method call goes through     |
 *   |                |                                  | all the wrappers                   |
 *   | New option =   | New field + new builder method   | New decorator CLASS (Open/Closed)  |
 *   | Relationship   | Builder CREATES product          | Decorator IS-A + HAS-A component   |
 *   | Java examples  | StringBuilder, Lombok @Builder,  | java.io: new BufferedReader(       |
 *   |                | HttpRequest.newBuilder()         |   new InputStreamReader(in))       |
 *
 *   Same pizza, two views:
 *     Builder   -> "Give me a LARGE THIN pizza with cheese and onion."
 *                  Toppings are just DATA in a list; Pizza.getPrice() calculates once.
 *     Decorator -> new Jalapeno(new Mushroom(new Farmhouse()))
 *                  Each topping is a CLASS with its OWN pricing logic, wrapped around
 *                  the pizza. Adding "Paneer" = new class, no existing code changes.
 *
 *   WHEN TO USE WHICH?
 *     Use BUILDER when:
 *       - constructor has 4+ params / many optional ones (User, HttpRequest, Config)
 *       - you want an IMMUTABLE object and validation before it exists
 *     Use DECORATOR when:
 *       - you need to add/stack responsibilities at RUNTIME (toppings with own logic,
 *         logging/compression/encryption on a stream, notification channels)
 *       - subclassing every combination would cause class explosion (2^N classes)
 *
 *   One-liner for interview:
 *     "Builder is about CONSTRUCTING an object cleanly; Decorator is about
 *      ENHANCING an object's behaviour by wrapping it."
 *
 * 6. INTERVIEW POINTS
 * -------------------------------------------------------------------------------------
 *   - Make product fields 'final' + no setters -> immutable & thread-safe.
 *   - Required params go in Builder's constructor; optional ones are methods.
 *   - Validate in build() (e.g. max 5 toppings) - object never exists in a bad state.
 *   - Builder is a 'static' nested class so it can be created without a Pizza.
 *   - Copy mutable collections (List.copyOf) so the caller can't modify Pizza later.
 *   - Optional "Director" class can hold common recipes (e.g. makeMargherita(builder)).
 *   - Downside: more code (fields written twice). Not worth it for 2-3 fields.
 * =====================================================================================
 */
package Patterns.Creational.BuilderPattern;

import java.util.ArrayList;
import java.util.List;

/** Product: immutable - all fields final, no setters, private constructor. */
class Pizza {
  private final String size; // required
  private final String crust; // required
  private final boolean cheese; // optional
  private final boolean extraSauce; // optional
  private final List<String> toppings; // optional

  private Pizza(Builder builder) {
    this.size = builder.size;
    this.crust = builder.crust;
    this.cheese = builder.cheese;
    this.extraSauce = builder.extraSauce;
    this.toppings = List.copyOf(builder.toppings); // defensive, unmodifiable copy
  }

  public String getSize() {
    return size;
  }

  public String getCrust() {
    return crust;
  }

  public boolean hasCheese() {
    return cheese;
  }

  public boolean hasExtraSauce() {
    return extraSauce;
  }

  public List<String> getToppings() {
    return toppings;
  }

  public double getPrice() {
    double price = size.equals("LARGE") ? 400 : size.equals("MEDIUM") ? 300 : 200;
    if (cheese) {
      price += 50;
    }
    if (extraSauce) {
      price += 20;
    }
    price += toppings.size() * 30;
    return price;
  }

  @Override
  public String toString() {
    return size + " " + crust + " pizza"
        + (cheese ? ", cheese" : "")
        + (extraSauce ? ", extra sauce" : "")
        + (toppings.isEmpty() ? "" : ", toppings=" + toppings)
        + " | Rs. " + getPrice();
  }

  /** Builder: collects values step by step, validates, then creates Pizza. */
  public static class Builder {
    private final String size;
    private final String crust;
    private boolean cheese = false; // sensible defaults
    private boolean extraSauce = false;
    private final List<String> toppings = new ArrayList<>();

    public Builder(String size, String crust) { // required fields up front
      this.size = size;
      this.crust = crust;
    }

    public Builder cheese(boolean cheese) {
      this.cheese = cheese;
      return this; // returning 'this' enables chaining
    }

    public Builder extraSauce(boolean extraSauce) {
      this.extraSauce = extraSauce;
      return this;
    }

    public Builder addTopping(String topping) {
      this.toppings.add(topping);
      return this;
    }

    public Pizza build() {
      if (!List.of("SMALL", "MEDIUM", "LARGE").contains(size)) {
        throw new IllegalStateException("Invalid size: " + size);
      }
      if (crust == null || crust.isBlank()) {
        throw new IllegalStateException("Crust is required");
      }
      if (toppings.size() > 5) {
        throw new IllegalStateException("Max 5 toppings allowed");
      }
      return new Pizza(this);
    }
  }
}

/** Optional Director: knows common "recipes" so clients don't repeat build steps. */
class PizzaMenu {
  static Pizza margherita(String size) {
    return new Pizza.Builder(size, "THIN").cheese(true).build();
  }

  static Pizza farmhouse(String size) {
    return new Pizza.Builder(size, "PAN")
        .cheese(true)
        .addTopping("Onion")
        .addTopping("Capsicum")
        .addTopping("Tomato")
        .addTopping("Mushroom")
        .build();
  }
}

public class Builder {
  public static void main(String[] args) {
    // 1. Only required fields
    Pizza basic = new Pizza.Builder("SMALL", "THIN").build();
    System.out.println("Basic     : " + basic);

    // 2. Pick only the options you want, in any order - readable, no 'true, false, null'
    Pizza custom = new Pizza.Builder("LARGE", "CHEESE_BURST")
        .addTopping("Jalapeno")
        .cheese(true)
        .addTopping("Paneer")
        .extraSauce(true)
        .build();
    System.out.println("Custom    : " + custom);

    // 3. Director recipes
    System.out.println("Margherita: " + PizzaMenu.margherita("MEDIUM"));
    System.out.println("Farmhouse : " + PizzaMenu.farmhouse("LARGE"));

    // 4. Validation happens in build() -> invalid Pizza never exists
    try {
      new Pizza.Builder("XXL", "THIN").build();
    } catch (IllegalStateException e) {
      System.out.println("Error     : " + e.getMessage());
    }

    // 5. Immutability: toppings list can't be changed from outside
    try {
      custom.getToppings().add("Pineapple");
    } catch (UnsupportedOperationException e) {
      System.out.println("Error     : cannot modify toppings of a built pizza (immutable)");
    }
  }
}
