/*
 * =====================================================================================
 *                     MEDIATOR DESIGN PATTERN (BEHAVIORAL) - NOTES
 * =====================================================================================
 *
 * WHAT
 *   Instead of objects talking directly to each other (creating a tangled mess),
 *   they ONLY talk to a central "Mediator" object, which coordinates everything.
 *
 *   Real-life analogy:
 *     - Air Traffic Control (ATC): Pilots don't call every other plane to ask
 *       "Can I land?". They talk ONLY to the ATC tower, and the tower coordinates.
 *     - Auction House: Bidders don't shout prices directly to each other. They submit
 *       bids to the Auctioneer (Mediator), who validates the bid and notifies others.
 *
 * WITHOUT vs WITH MEDIATOR
 *   Without (Mesh / Many-to-Many)       With (Star Topology via Mediator)
 *     Bidder1 <---> Bidder2                  Bidder1 \
 *        ^    \   /    ^                              \
 *        |     \ /     |                     Bidder2 ---> [ AuctionMediator ]
 *        |      X      |                              /
 *        v     / \     v                     Bidder3 /
 *     Bidder3 <---> Bidder4
 *   Every bidder holds a list of all      Every bidder ONLY knows the Mediator.
 *   other bidders -> N*(N-1) links!       Mediator knows all bidders -> N links!
 *
 * 4 PARTS OF THE PATTERN
 *   1. Mediator Interface    -> AuctionMediator : addBidder(b), placeBid(b, amount)
 *   2. Concrete Mediator     -> Auction         : holds List<Colleague>, tracks highest bid,
 *                                                 notifies other bidders when a new bid arrives
 *   3. Colleague Interface   -> Colleague       : placeBid(amount), receiveBidNotification(b, amt)
 *   4. Concrete Colleague    -> Bidder          : holds reference to AuctionMediator
 *
 * UML
 *
 *   +-------------------------------------+           +------------------------------------+
 *   |   <<interface>> AuctionMediator     |<----------|       <<interface>> Colleague      |
 *   +-------------------------------------+   has-a   +------------------------------------+
 *   | + addBidder(bidder: Colleague)      |           | + placeBid(amount: int)            |
 *   | + placeBid(bidder: Colleague, int)  |           | + receiveBidNotification(String,int)|
 *   +------------------^------------------+           | + getName(): String                |
 *                      | implements                   +-----------------^------------------+
 *                      |                                                | implements
 *   +------------------+------------------+           +-----------------+------------------+
 *   |              Auction                |---------->|               Bidder               |
 *   +-------------------------------------+  notifies +------------------------------------+
 *   | - bidders: List<Colleague>          |  (1 to *) | - name: String                     |
 *   | - highestBid: int                   |           | - mediator: AuctionMediator        |
 *   | - highestBidder: String             |           +------------------------------------+
 *   +-------------------------------------+
 *
 * FLOW: om.placeBid(55000)
 *
 *     Om calls mediator.placeBid(Om, 55000)
 *          |
 *     Auction checks: Is 55000 > current highestBid?
 *          |-- No  --> reject bid (tell Om it's too low)
 *          |-- Yes --> update highestBid = 55000
 *                      loop over all bidders EXCEPT Om:
 *                        call bidder.receiveBidNotification("Om", 55000)
 *
 * =====================================================================================
 *        CONFUSION BUSTER: MEDIATOR vs OBSERVER vs PROXY (INTERVIEW FAVORITE!)
 * =====================================================================================
 *
 * Why do they feel similar?
 *   All three sit "in between" objects or forward calls. Here is how to tell them apart:
 *
 * 1. MEDIATOR (Behavioral) -> "Many-to-Many Coordinator / Traffic Police"
 *    - Intent    : Stop N colleague objects from referencing each other directly.
 *    - Direction : Bi-directional! Any Bidder can send a bid TO the Mediator, AND the
 *                  Mediator sends notifications back TO all other Bidders.
 *    - Example   : Auction House, Chat Room (WhatsApp group), Air Traffic Control.
 *
 *    MEDIATOR UML / STRUCTURE (Two-Way Hub & Spoke):
 *
 *          placeBid()                        receiveBidNotification()
 *      +----------------+                  +----------------+
 *      |   Bidder 1     |----------------->|                |----------------->|   Bidder 2   |
 *      +----------------+                  |    Auction     |                  +--------------+
 *                                          |   (Mediator)   |
 *      +----------------+  <-------------- |                |----------------->|   Bidder 3   |
 *      |   Bidder 2     |   notifies back  +----------------+                  +--------------+
 *      +----------------+                    ^            |
 *         (Every Colleague BOTH sends        +------------+
 *          actions & receives updates)     holds List<Colleague>
 *
 * -------------------------------------------------------------------------------------
 *
 * 2. OBSERVER (Behavioral) -> "One-to-Many Broadcast / YouTube Subscription"
 *    - Intent    : When ONE object (Subject) changes state, automatically notify all
 *                  dependents (Observers).
 *    - Direction : Strictly One-Way broadcast (Subject ---> Observers). Observers just
 *                  listen; they don't coordinate with each other through the Subject.
 *    - Example   : Stock price alert, YouTube channel notifying subscribers, Weather station.
 *
 *    OBSERVER UML (One-Way Broadcast):
 *
 *      +----------------------------------+           +----------------------------------+
 *      |     <<interface>> Observable     |---------->|     <<interface>> Observer       |
 *      |           (Subject)              |  notifies |          (Subscriber)            |
 *      +----------------------------------+  (1 to *) +----------------------------------+
 *      | - observers: List<Observer>      |           | + update()                       |
 *      | + add(obs) / remove(obs)         |           +-----------------^----------------+
 *      | + notifyObservers()              |                             | implements
 *      | + setData(newPrice)              |              +--------------+---------------+
 *      +-----------------^----------------+              |                              |
 *                        | implements         +----------+-----------+       +----------+-----------+
 *      +-----------------+----------------+   |  EmailAlertObserver  |       |  MobileAlertObserver |
 *      |       IphoneStockObservable      |   +----------------------+       +----------------------+
 *      +----------------------------------+   (Observers ONLY listen; EmailAlert never sends messages
 *      (State changes HERE only)               to MobileAlert through the Subject!)
 *
 * -------------------------------------------------------------------------------------
 *
 * 3. PROXY (Structural) -> "One-to-One Gatekeeper / Bodyguard"
 *    - Intent    : Control access to a SINGLE real object (lazy loading, auth, caching,
 *                  logging) by implementing the EXACT SAME interface as the real object.
 *    - Direction : Client ---> Proxy ---> RealService (1-to-1 wrapper).
 *    - Example   : EmployeeDaoProxy checking if user == "ADMIN" before calling EmployeeDaoImpl.
 *
 *    PROXY UML (1-to-1 Same-Interface Wrapper):
 *
 *                         +------------------------------------+
 *                         |     <<interface>> EmployeeDao      |
 *                         +------------------------------------+
 *                         | + create(role: String, emp: Emp)   |
 *                         +-----------------^------------------+
 *                                           | implements (BOTH implement same interface!)
 *                            +--------------+---------------+
 *                            |                              |
 *             +--------------+---------------+   +----------+-------------------+
 *      Client |       EmployeeDaoProxy       |   |       EmployeeDaoImpl        |
 *      ------>|         (Gatekeeper)         |-->|        (Real Object)         |
 *             +------------------------------+   +------------------------------+
 *             | - realObj: EmployeeDaoImpl   |   | + create(role, emp) {        |
 *             +------------------------------+   |     // actual DB insert      |
 *             | + create(role, emp) {        |   |   }                          |
 *             |     if (role == "ADMIN")     |   +------------------------------+
 *             |       realObj.create(...);   |
 *             |     else throw AccessDenied; |
 *             |   }                          |
 *             +------------------------------+
 *
 * -------------------------------------------------------------------------------------
 *
 * QUICK CHEAT SHEET TABLE:
 * +-----------+--------------+------------------------+----------------------------------+
 * | Pattern   | Category     | Relationship           | Key Question to Ask Yourself     |
 * +-----------+--------------+------------------------+----------------------------------+
 * | Mediator  | Behavioral   | Many <--> 1 <--> Many  | Are many peers talking to each   |
 * |           |              | (Bi-directional Hub)   | other in a messy web?            |
 * +-----------+--------------+------------------------+----------------------------------+
 * | Observer  | Behavioral   | 1 Subject ---> Many    | Do many listeners just want      |
 * |           |              | Subscribers (One-way)  | updates when 1 object changes?   |
 * +-----------+--------------+------------------------+----------------------------------+
 * | Proxy     | Structural   | 1 Client -> 1 Proxy    | Do I need a gatekeeper/security  |
 * |           |              | -> 1 Real Object       | guard in front of 1 object?      |
 * +-----------+--------------+------------------------+----------------------------------+
 *
 * KEY POINTS FOR INTERVIEW
 *   - Colleagues NEVER hold references to other Colleagues—only to the Mediator.
 *   - Pros: Decouples colleagues, centralizes complex coordination rules in one place.
 *   - Cons: The Mediator can become a "God Object" if too much logic is dumped into it.
 * =====================================================================================
 */
package Patterns.Behavioral.MediatorPattern;

import java.util.ArrayList;
import java.util.List;

/** 1. Mediator Interface: defines how colleagues communicate with the mediator. */
interface AuctionMediator {
  void addBidder(Colleague bidder);

  void placeBid(Colleague bidder, int bidAmount);
}

/** 2. Colleague Interface: represents participants in the auction. */
interface Colleague {
  void placeBid(int bidAmount);

  void receiveBidNotification(String bidderName, int bidAmount);

  String getName();
}

/** 3. Concrete Mediator: coordinates bidding and notifies all other bidders. */
class Auction implements AuctionMediator {
  private final List<Colleague> bidders = new ArrayList<>();
  private final String itemName;
  private int highestBid = 0;
  private String highestBidder = "None";

  public Auction(String itemName, int startingPrice) {
    this.itemName = itemName;
    this.highestBid = startingPrice;
    System.out.println("Auction started for '" + itemName + "' (Base Price: Rs. " + startingPrice + ")");
  }

  @Override
  public void addBidder(Colleague bidder) {
    bidders.add(bidder);
    System.out.println("  [+] " + bidder.getName() + " joined the auction.");
  }

  @Override
  public void placeBid(Colleague bidder, int bidAmount) {
    System.out.println("\n--> " + bidder.getName() + " attempts to bid Rs. " + bidAmount);

    // Central business rule inside Mediator
    if (bidAmount <= highestBid) {
      System.out.println("  [Rejected] Bid of Rs. " + bidAmount + " by " + bidder.getName()
          + " must be higher than current highest bid (Rs. " + highestBid + ").");
      return;
    }

    // Update highest bid
    highestBid = bidAmount;
    highestBidder = bidder.getName();
    System.out.println("  [Accepted] New highest bid is Rs. " + highestBid + " by " + highestBidder);

    // Notify all OTHER bidders
    for (Colleague b : bidders) {
      if (b != bidder) {
        b.receiveBidNotification(bidder.getName(), bidAmount);
      }
    }
  }

  public void closeAuction() {
    System.out.println("\n==========================================");
    System.out.println("Auction Closed! '" + itemName + "' sold to " + highestBidder + " for Rs. " + highestBid);
    System.out.println("==========================================");
  }
}

/** 4. Concrete Colleague: talks ONLY to the AuctionMediator, never to other Bidders directly. */
class Bidder implements Colleague {
  private final String name;
  private final AuctionMediator auctionMediator;

  public Bidder(String name, AuctionMediator auctionMediator) {
    this.name = name;
    this.auctionMediator = auctionMediator;
    this.auctionMediator.addBidder(this); // Auto-register with the mediator
  }

  @Override
  public void placeBid(int bidAmount) {
    auctionMediator.placeBid(this, bidAmount);
  }

  @Override
  public void receiveBidNotification(String bidderName, int bidAmount) {
    System.out.println("  [Notification to " + this.name + "] " + bidderName
        + " has placed a higher bid of Rs. " + bidAmount);
  }

  @Override
  public String getName() {
    return name;
  }
}

/** Client: demonstrates the Auction Mediator pattern. */
public class AuctionDesign {
  public static void main(String[] args) {
    // 1. Create the Mediator (Auction House for an iPhone 16 Pro)
    Auction iphoneAuction = new Auction("iPhone 16 Pro", 50000);

    // 2. Create Colleagues (Bidders) connected ONLY to the Mediator
    Colleague om = new Bidder("Om", iphoneAuction);
    Colleague rahul = new Bidder("Rahul", iphoneAuction);
    Colleague priya = new Bidder("Priya", iphoneAuction);

    // 3. Bidders place bids through the Mediator
    om.placeBid(55000);      // Accepted -> Rahul & Priya get notified
    rahul.placeBid(60000);   // Accepted -> Om & Priya get notified
    priya.placeBid(58000);   // Rejected! (Lower than 60000) -> Nobody gets spammed
    priya.placeBid(65000);   // Accepted -> Om & Rahul get notified

    // 4. Close the auction
    iphoneAuction.closeAuction();
  }
}
