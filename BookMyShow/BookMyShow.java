package BookMyShow;

import java.util.*;

/**
 * ============================================================================================================
 *                          BOOKMYSHOW LLD - UML CLASS DIAGRAM & ARCHITECTURE (SDE-1)
 * ============================================================================================================
 *
 * 1. UML CLASS DIAGRAM:
 * ---------------------
 *   +------------------------------------+
 *   |            BookMyShow              |
 *   +------------------------------------+
 *   | - movieController   : MovieCtrl    | *--------------------+
 *   | - theaterController : TheaterCtrl  | *---+                |
 *   +------------------------------------+     |                |
 *   | + setup() : void                   |     |                |
 *   | + bookTickets(...) : Booking       |     |                |
 *   +------------------------------------+     |                |
 *                                              v                v
 *   +------------------------------------+     |     +------------------------------------+
 *   |         TheaterController          | <---+     |          MovieController           |
 *   +------------------------------------+           +------------------------------------+
 *   | - cityVsTheaters : Map<City,List>  |           | - cityVsMovies : Map<City,List>    |
 *   | - allTheaters    : List<Theater>   |           | - allMovies    : List<Movie>       |
 *   +------------------------------------+           +------------------------------------+
 *   | + addTheater(Theater, City) : void |           | + addMovie(Movie, City) : void     |
 *   | + getAllShow(Movie, City) : Map    |           | + getMovieByName(String) : Movie   |
 *   +------------------------------------+           | + getMoviesByCity(City) : List     |
 *                     |                              +------------------------------------+
 *                     | manages (1..*)                                  |
 *                     v                                                 | manages (1..*)
 *   +------------------------------------+                              v
 *   |              Theater               |           +------------------------------------+
 *   +------------------------------------+           |               Movie                |
 *   | - id      : String                 |           +------------------------------------+
 *   | - name    : String                 |           | - movieId     : String             |
 *   | - address : String                 |           | - title       : String             |
 *   | - city    : City                   |           | - description : String             |
 *   | - screens : List<Screen>           |           | - duration    : String             |
 *   | - shows   : List<Show>             |           +------------------------------------+
 *   +------------------------------------+
 *        |                        |
 *        | has (1..*)             | schedules (1..*)
 *        v                        v
 *   +--------------------+   +------------------------------------+      +--------------------+
 *   |       Screen       |   |                Show                |      |      Booking       |
 *   +--------------------+   +------------------------------------+      +--------------------+
 *   | - id       : String|   | - showId        : String           | <--- | - id     : String  |
 *   | - name     : String|   | - movie         : Movie            |      | - show   : Show    |
 *   | - capacity : int   |   | - screen        : Screen           |      | - seats  : List    |
 *   | - seats    : List  |   | - startTime     : int              |      | - payment: Payment |
 *   +--------------------+   | - endTime       : int              |      +--------------------+
 *            |               | - bookedSeatIds : Set<String>      |
 *            | contains      +------------------------------------+
 *            v
 *   +--------------------+   +--------------------+
 *   |        Seat        |   |  <<enumeration>>   |
 *   +--------------------+   |    SeatCategory    |
 *   | - id       : String|   +--------------------+
 *   | - name     : String|-->| SILVER             |
 *   | - price    : int   |   | GOLD               |
 *   | - category : Enum  |   | PLATINUM           |
 *   +--------------------+   +--------------------+
 *
 *
 * 2. TICKET BOOKING EXECUTION FLOW (Inside BookMyShow.bookTickets()):
 * -------------------------------------------------------------------
 *   [USER REQUEST] ---> bookTickets(user, movieName, cityName, showTime, requestedSeats)
 *         |
 *         v
 *   1. Validate City & Fetch Movies  ---> movieController.getMoviesByCity(city)
 *         |
 *         v
 *   2. Match Target Movie            ---> Verify `movieName` is currently playing in `city`
 *         |
 *         v
 *   3. Fetch Running Shows in City   ---> theaterController.getAllShow(targetMovie, city)
 *         |
 *         v
 *   4. Match Target Show & Screen    ---> Filter show by `showTime` (e.g., 10:00 -> 10)
 *         |
 *         v
 *   5. Thread-Safe Seat Lock Check   ---> synchronized(targetShow) {
 *                                           Check if ANY requestedSeat.id is in `show.bookedSeatIds`
 *                                           |-- YES --> Throw / Print "Seat Already Booked!" & Abort
 *                                           |-- NO  --> Add all seat IDs to `show.bookedSeatIds`
 *                                         }
 *         |
 *         v
 *   6. Generate Payment & Booking    ---> Calculate total price -> Complete Payment -> Return Booking Receipt
 *
 *
 * 3. KEY SDE-1 INTERVIEW TALKING POINTS:
 * --------------------------------------
 *   • Why separate `Screen` (physical Seat list) from `Show` (`bookedSeatIds`)?
 *     -> A physical `Screen` has 100 fixed `Seat` objects. The same screen hosts multiple `Show`s per day
 *        (e.g., 10 AM, 2 PM, 6 PM). Storing `bookedSeatIds` inside `Show` ensures booking Seat A1 for the
 *        10 AM show does NOT block Seat A1 for the 2 PM show!
 *   • How is Concurrency (Race Condition) handled when 2 users book Seat A1 at the exact same millisecond?
 *     -> We synchronize on the specific `Show` instance (`synchronized (targetShow)`) while checking and
 *        marking `bookedSeatIds`. This locks only that single show without blocking bookings for other shows.
 * ============================================================================================================
 */

enum SeatCategory {
  SILVER,
  GOLD,
  PLATINUM
}

class Movie {
  private final String movieId;
  private final String title;
  private final String description;
  private final String duration;

  public Movie(String movieId, String title, String description, String duration) {
    this.movieId = movieId;
    this.title = title;
    this.description = description;
    this.duration = duration;
  }

  public String getMovieId() {
    return movieId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getDuration() {
    return duration;
  }
}

class City {
  private final String cityId;
  private final String name;

  public City(String cityId, String name) {
    this.cityId = cityId;
    this.name = name;
  }

  public String getCityId() {
    return cityId;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof City)) return false;
    City city = (City) o;
    return Objects.equals(cityId, city.cityId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cityId);
  }
}

class MovieController {
  private final Map<City, List<Movie>> cityVsMovies;
  private final List<Movie> allMovies;
  private final List<City> allCities;

  public MovieController() {
    this.cityVsMovies = new HashMap<>();
    this.allMovies = new ArrayList<>();
    this.allCities = new ArrayList<>();
  }

  public void addCity(City city) {
    if (!allCities.contains(city)) {
      allCities.add(city);
    }
  }

  public City getCityByName(String cityName) {
    for (City city : allCities) {
      if (city.getName().equalsIgnoreCase(cityName)) {
        return city;
      }
    }
    return null;
  }

  public void addMovie(Movie movie, City city) {
    allMovies.add(movie);
    cityVsMovies.computeIfAbsent(city, k -> new ArrayList<>()).add(movie);
  }

  public List<Movie> getMoviesInCity(City city) {
    return cityVsMovies.getOrDefault(city, Collections.emptyList());
  }

  public Movie getMovieByName(String movieName) {
    for (Movie movie : allMovies) {
      if (movie.getTitle().equalsIgnoreCase(movieName)) {
        return movie;
      }
    }
    return null;
  }
}

class Seat {
  private final String id;
  private final String name;
  private final int price;
  private final SeatCategory category;

  public Seat(String id, String name, int price, SeatCategory category) {
    this.id = id;
    this.name = name;
    this.price = price;
    this.category = category;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getPrice() {
    return price;
  }

  public SeatCategory getCategory() {
    return category;
  }
}

class Screen {
  private final String id;
  private final String name;
  private final int capacity;
  private final List<Seat> seats;

  public Screen(String id, String name, int capacity) {
    this.id = id;
    this.name = name;
    this.capacity = capacity;
    this.seats = new ArrayList<>();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getCapacity() {
    return capacity;
  }

  public void addSeat(Seat seat) {
    seats.add(seat);
  }

  public List<Seat> getSeats() {
    return seats;
  }
}

class Show {
  private final String showId;
  private final Movie movie;
  private final Screen screen;
  private final int startTime; // e.g., 10 for 10:00
  private final int endTime;   // e.g., 12 for 12:00
  private final Set<String> bookedSeatIds;

  public Show(String showId, Movie movie, Screen screen, int startTime, int endTime) {
    this.showId = showId;
    this.movie = movie;
    this.screen = screen;
    this.startTime = startTime;
    this.endTime = endTime;
    this.bookedSeatIds = new HashSet<>();
  }

  public String getShowId() {
    return showId;
  }

  public Movie getMovie() {
    return movie;
  }

  public Screen getScreen() {
    return screen;
  }

  public int getStartTime() {
    return startTime;
  }

  public int getEndTime() {
    return endTime;
  }

  public Set<String> getBookedSeatIds() {
    return bookedSeatIds;
  }
}

class Theater {
  private final String id;
  private final String name;
  private final String address;
  private final City city;
  private final List<Screen> screens;
  private final List<Show> shows;

  public Theater(String id, String name, String address, City city) {
    this.id = id;
    this.name = name;
    this.address = address;
    this.city = city;
    this.screens = new ArrayList<>();
    this.shows = new ArrayList<>();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getAddress() {
    return address;
  }

  public City getCity() {
    return city;
  }

  public void addScreen(Screen screen) {
    screens.add(screen);
  }

  public List<Screen> getScreens() {
    return screens;
  }

  public void addShow(Show show) {
    shows.add(show);
  }

  public List<Show> getShows() {
    return shows;
  }
}

class TheaterController {
  private final Map<City, List<Theater>> cityVsTheaters;
  private final List<Theater> allTheaters;

  public TheaterController() {
    this.cityVsTheaters = new HashMap<>();
    this.allTheaters = new ArrayList<>();
  }

  public void addTheater(Theater theater, City city) {
    allTheaters.add(theater);
    cityVsTheaters.computeIfAbsent(city, k -> new ArrayList<>()).add(theater);
  }

  // Returns all running shows for a given Movie in a City grouped by Theater
  public Map<Theater, List<Show>> getAllShows(Movie movie, City city) {
    Map<Theater, List<Show>> theaterVsShows = new HashMap<>();
    List<Theater> theaters = cityVsTheaters.getOrDefault(city, Collections.emptyList());

    for (Theater theater : theaters) {
      List<Show> matchingShows = new ArrayList<>();
      for (Show show : theater.getShows()) {
        if (show.getMovie().getMovieId().equals(movie.getMovieId())) {
          matchingShows.add(show);
        }
      }
      if (!matchingShows.isEmpty()) {
        theaterVsShows.put(theater, matchingShows);
      }
    }
    return theaterVsShows;
  }
}

class Payment {
  private final String paymentId;
  private final int amount;
  private final String status;

  public Payment(String paymentId, int amount, String status) {
    this.paymentId = paymentId;
    this.amount = amount;
    this.status = status;
  }

  public String getPaymentId() {
    return paymentId;
  }

  public int getAmount() {
    return amount;
  }

  public String getStatus() {
    return status;
  }
}

class Booking {
  private final String bookingId;
  private final Show show;
  private final Theater theater;
  private final List<Seat> bookedSeats;
  private final Payment payment;

  public Booking(String bookingId, Show show, Theater theater, List<Seat> bookedSeats, Payment payment) {
    this.bookingId = bookingId;
    this.show = show;
    this.theater = theater;
    this.bookedSeats = bookedSeats;
    this.payment = payment;
  }

  public void printReceipt() {
    System.out.println("==================================================");
    System.out.println("            🎟️ BOOKING CONFIRMED!                 ");
    System.out.println("==================================================");
    System.out.println("Booking ID : " + bookingId);
    System.out.println("Movie      : " + show.getMovie().getTitle() + " (" + show.getMovie().getDuration() + ")");
    System.out.println("Theater    : " + theater.getName() + ", " + theater.getCity().getName());
    System.out.println("Screen     : " + show.getScreen().getName());
    System.out.println("Show Time  : " + show.getStartTime() + ":00 - " + show.getEndTime() + ":00");
    System.out.print("Seats      : ");
    for (Seat s : bookedSeats) {
      System.out.print(s.getName() + " (" + s.getCategory() + ") ");
    }
    System.out.println();
    System.out.println("Total Paid : Rs. " + payment.getAmount() + " [" + payment.getStatus() + "]");
    System.out.println("==================================================\n");
  }
}

public class BookMyShow {
  private final MovieController movieController;
  private final TheaterController theaterController;

  public BookMyShow() {
    this.movieController = new MovieController();
    this.theaterController = new TheaterController();
  }

  public void setup() {
    // 1. Create Cities
    City hyderabad = new City("1", "Hyderabad");
    City bangalore = new City("2", "Bangalore");
    movieController.addCity(hyderabad);
    movieController.addCity(bangalore);

    // 2. Create Movies
    Movie rrr = new Movie("1", "RRR", "Action / Period Drama", "3h 2m");
    Movie bahubali = new Movie("2", "Bahubali", "Epic Fantasy Action", "2h 39m");

    // Add Movies to Cities
    movieController.addMovie(rrr, hyderabad);
    movieController.addMovie(rrr, bangalore);
    movieController.addMovie(bahubali, hyderabad);
    movieController.addMovie(bahubali, bangalore);

    // 3. Create Screens & Seats
    Screen screen1 = createScreenWithSeats("1", "Screen1", 10);
    Screen screen2 = createScreenWithSeats("2", "Screen2", 10);
    Screen screen3 = createScreenWithSeats("3", "Screen3", 10);
    Screen screen4 = createScreenWithSeats("4", "Screen4", 10);

    // 4. Create Theaters
    Theater pvrHyderabad = new Theater("1", "PVR Nexus Mall", "Kukatpally, Hyderabad", hyderabad);
    pvrHyderabad.addScreen(screen1);
    pvrHyderabad.addScreen(screen2);

    Theater inoxBangalore = new Theater("2", "INOX Mantri Square", "Malleswaram, Bangalore", bangalore);
    inoxBangalore.addScreen(screen3);
    inoxBangalore.addScreen(screen4);

    // 5. Create Shows & Assign to Theaters
    Show show1 = new Show("SH101", rrr, screen1, 10, 13);
    Show show2 = new Show("SH102", bahubali, screen2, 14, 17);
    pvrHyderabad.addShow(show1);
    pvrHyderabad.addShow(show2);

    Show show3 = new Show("SH201", rrr, screen3, 12, 15);
    Show show4 = new Show("SH202", bahubali, screen4, 18, 21);
    inoxBangalore.addShow(show3);
    inoxBangalore.addShow(show4);

    // Register Theaters with TheaterController
    theaterController.addTheater(pvrHyderabad, hyderabad);
    theaterController.addTheater(inoxBangalore, bangalore);
  }

  private Screen createScreenWithSeats(String screenId, String screenName, int totalSeats) {
    Screen screen = new Screen(screenId, screenName, totalSeats);
    for (int i = 1; i <= totalSeats; i++) {
      SeatCategory category;
      int price;
      if (i <= 4) {
        category = SeatCategory.SILVER;
        price = 150;
      } else if (i <= 8) {
        category = SeatCategory.GOLD;
        price = 250;
      } else {
        category = SeatCategory.PLATINUM;
        price = 400;
      }
      screen.addSeat(new Seat(String.valueOf(i), "Seat" + i, price, category));
    }
    return screen;
  }

  // Thread-safe ticket booking method
  public Booking bookTickets(String movieName, String cityName, String time, List<String> requestedSeatIds) {
    System.out.println("-> Processing Booking Request for '" + movieName + "' in " + cityName
        + " at " + time + " | Requested Seats: " + requestedSeatIds);

    // Step 1: Validate City
    City city = movieController.getCityByName(cityName);
    if (city == null) {
      System.out.println("   ❌ Error: Service not available in city: " + cityName + "\n");
      return null;
    }

    // Step 2: Find Movie in City
    Movie interestedMovie = null;
    for (Movie movie : movieController.getMoviesInCity(city)) {
      if (movie.getTitle().equalsIgnoreCase(movieName)) {
        interestedMovie = movie;
        break;
      }
    }
    if (interestedMovie == null) {
      System.out.println("   ❌ Error: Movie '" + movieName + "' is not currently playing in " + cityName + "\n");
      return null;
    }

    // Step 3: Get all shows for this Movie in the City
    int requestedStartHour = Integer.parseInt(time.split(":")[0]);
    Map<Theater, List<Show>> showsMap = theaterController.getAllShows(interestedMovie, city);

    Theater selectedTheater = null;
    Show selectedShow = null;

    for (Map.Entry<Theater, List<Show>> entry : showsMap.entrySet()) {
      for (Show show : entry.getValue()) {
        if (show.getStartTime() == requestedStartHour) {
          selectedTheater = entry.getKey();
          selectedShow = show;
          break;
        }
      }
      if (selectedShow != null) break;
    }

    if (selectedShow == null) {
      System.out.println("   ❌ Error: No show found for '" + movieName + "' at " + time + "\n");
      return null;
    }

    // Step 4: Thread-Safe Seat Availability Check & Locking
    List<Seat> seatsToBook = new ArrayList<>();
    int totalAmount = 0;

    synchronized (selectedShow) {
      Set<String> alreadyBooked = selectedShow.getBookedSeatIds();
      for (String seatId : requestedSeatIds) {
        if (alreadyBooked.contains(seatId)) {
          System.out.println("   ❌ BOOKING FAILED: Seat ID '" + seatId
              + "' is already booked! Please select different seats.\n");
          return null;
        }
      }

      // Match physical Seat objects from the Screen
      for (Seat screenSeat : selectedShow.getScreen().getSeats()) {
        if (requestedSeatIds.contains(screenSeat.getId())) {
          seatsToBook.add(screenSeat);
          totalAmount += screenSeat.getPrice();
        }
      }

      // Lock the seats for this show
      alreadyBooked.addAll(requestedSeatIds);
    }

    // Step 5: Complete Payment & Create Booking
    Payment payment = new Payment("PAY-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
        totalAmount, "SUCCESS");
    Booking booking = new Booking("BKG-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
        selectedShow, selectedTheater, seatsToBook, payment);
    booking.printReceipt();
    return booking;
  }

  public static void main(String[] args) {
    BookMyShow bookMyShow = new BookMyShow();
    bookMyShow.setup();

    // Test Case 1: Successful Booking of Seat 1 & Seat 2 for RRR in Hyderabad at 10:00
    bookMyShow.bookTickets("RRR", "Hyderabad", "10:00", List.of("1", "2"));

    // Test Case 2: Duplicate Booking Attempt on Seat 2 (Proves concurrency / double-booking prevention)
    bookMyShow.bookTickets("RRR", "Hyderabad", "10:00", List.of("2", "3"));

    // Test Case 3: Booking Available Seats (Seat 3 & Seat 9 Platinum) for RRR in Hyderabad at 10:00
    bookMyShow.bookTickets("RRR", "Hyderabad", "10:00", List.of("3", "9"));
  }
}
