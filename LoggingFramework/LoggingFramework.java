import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * ============================================================================================================
 *                          LOGGING FRAMEWORK (LOG4J / SLF4J) LLD - UML & ARCHITECTURE
 * ============================================================================================================
 *
 * 1. UML CLASS DIAGRAM:
 * ---------------------
 *   +------------------------------------+          +------------------------------------+
 *   |     LoggingFramework (Singleton)   |          |               Logger               |
 *   +------------------------------------+          +------------------------------------+
 *   | - instance : LoggingFramework      |          | - name : String                    |
 *   | - loggers  : Map<String, Logger>   | *------> | - currentLevel : LogLevel          |
 *   | - rootLevel: LogLevel              |  caches  | - appenders : List<LogAppender>    |
 *   +------------------------------------+          +------------------------------------+
 *   | + getInstance() : LoggingFramework |          | + log(level, message) : void       |
 *   | + getLogger(name) : Logger         |          | + addAppender(appender) : void     |
 *   +------------------------------------+          | + debug/info/warn/error/fatal()    |
 *                                                   +------------------------------------+
 *                                                                     o
 *                                                                     | notifies (Observer Pattern)
 *                                                                     v
 *   +------------------------------------+          +------------------------------------+
 *   |             LogMessage             |          |      <<interface>>                 |
 *   +------------------------------------+          |          LogAppender               |
 *   | - level : LogLevel                 | <------- | + append(msg: LogMessage) : void   |
 *   | - message : String                 |  passes  +------------------------------------+
 *   | - threadName : String              |                     ^              ^
 *   | - timestamp : LocalDateTime        |                     |              |
 *   +------------------------------------+          +----------+----+    +----+----------+
 *                     ^                             |ConsoleAppender|    | FileAppender  |
 *                     | formats                     +---------------+    +---------------+
 *   +-----------------+------------------+                  |                    |
 *   |      <<interface>>                 | <----------------+--------------------+
 *   |          LogFormatter              |     uses (Strategy Pattern)
 *   +------------------------------------+
 *   | + format(msg: LogMessage) : String |
 *   +------------------------------------+
 *                     ^
 *                     |
 *           [DefaultLogFormatter]
 *
 *
 * 2. LOGGING PIPELINE EXECUTION FLOW:
 * -----------------------------------
 *   [Caller: logger.info("Order created")]
 *         |
 *         v
 *   [1. Level Filter Check]: Is `LogLevel.INFO.priority >= logger.currentLevel.priority`?
 *         |-- NO  --> Discard immediately (Zero overhead!)
 *         |-- YES --> Proceed to Step 2
 *         v
 *   [2. Event Creation]: Construct immutable `LogMessage(level, msg, threadName, timestamp)`
 *         |
 *         v
 *   [3. Observer Broadcast]: Loop through all registered `LogAppender` sinks (`ConsoleAppender`, `FileAppender`)
 *         |
 *         v
 *   [4. Strategy Formatting]: Each `LogAppender` delegates to its `LogFormatter.format(logMessage)`
 *         |
 *         v
 *   [5. Output Sink Write]: Writes formatted string to Console (`System.out`) or Disk (`app.log`)
 * ============================================================================================================
 */

// =====================================================================
// 1. ENUM: Log Levels with Priority Ordering
// =====================================================================
enum LogLevel {
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    FATAL(5);

    private final int priority;

    LogLevel(int priority) {
        this.priority = priority;
    }

    public boolean isGreaterOrEqual(LogLevel other) {
        return this.priority >= other.priority;
    }
}

// =====================================================================
// 2. MODEL: Immutable Log Message Event
// =====================================================================
class LogMessage {
    private final LogLevel level;
    private final String message;
    private final String threadName;
    private final LocalDateTime timestamp;

    public LogMessage(LogLevel level, String message) {
        this.level = level;
        this.message = message;
        this.threadName = Thread.currentThread().getName();
        this.timestamp = LocalDateTime.now();
    }

    public LogLevel getLevel() { return level; }
    public String getMessage() { return message; }
    public String getThreadName() { return threadName; }
    public LocalDateTime getTimestamp() { return timestamp; }
}

// =====================================================================
// 3. FORMATTER: Transforms LogMessage into formatted String
// =====================================================================
interface LogFormatter {
    String format(LogMessage message);
}

class DefaultLogFormatter implements LogFormatter {
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String format(LogMessage log) {
        return String.format("[%s] [%-5s] [%s] - %s",
                log.getTimestamp().format(FORMATTER),
                log.getLevel(),
                log.getThreadName(),
                log.getMessage());
    }
}

// =====================================================================
// 4. APPENDER / SINK: Destination for logs (Observer / Strategy Pattern)
// =====================================================================
interface LogAppender {
    void append(LogMessage message);
}

class ConsoleAppender implements LogAppender {
    private final LogFormatter formatter;

    public ConsoleAppender(LogFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public synchronized void append(LogMessage message) {
        System.out.println(formatter.format(message));
    }
}

class FileAppender implements LogAppender {
    private final LogFormatter formatter;
    private final String destinationFile;

    public FileAppender(LogFormatter formatter, String destinationFile) {
        this.formatter = formatter;
        this.destinationFile = destinationFile;
    }

    @Override
    public synchronized void append(LogMessage message) {
        // In an interview, printing the target file destination simulates disk write
        System.out.println("[FILE: " + destinationFile + "] " + formatter.format(message));
    }
}

// =====================================================================
// 5. LOGGER: Dispatches logs to appenders based on configured level
// =====================================================================
class Logger {
    private final String name;
    private LogLevel currentLevel;
    private final List<LogAppender> appenders = new CopyOnWriteArrayList<>();

    public Logger(String name, LogLevel initialLevel) {
        this.name = name;
        this.currentLevel = initialLevel;
    }

    public void setLevel(LogLevel level) {
        this.currentLevel = level;
    }

    public void addAppender(LogAppender appender) {
        appenders.add(appender);
    }

    // Core logging logic: checks level and notifies all registered appenders
    public void log(LogLevel level, String message) {
        if (level.isGreaterOrEqual(currentLevel)) {
            LogMessage logMessage = new LogMessage(level, message);
            for (LogAppender appender : appenders) {
                appender.append(logMessage);
            }
        }
    }

    // Convenience helper methods
    public void debug(String message) { log(LogLevel.DEBUG, message); }
    public void info(String message)  { log(LogLevel.INFO, message);  }
    public void warn(String message)  { log(LogLevel.WARN, message);  }
    public void error(String message) { log(LogLevel.ERROR, message); }
    public void fatal(String message) { log(LogLevel.FATAL, message); }

    public String getName() { return name; }
    public LogLevel getLevel() { return currentLevel; }
}

// =====================================================================
// 6. LOG MANAGER: Central Registry / Factory (Singleton Pattern)
// =====================================================================
public class LoggingFramework {
    private static LoggingFramework instance;
    private final Map<String, Logger> loggers = new ConcurrentHashMap<>();
    private LogLevel rootLevel = LogLevel.INFO;
    private final LogFormatter defaultFormatter = new DefaultLogFormatter();
    private final List<LogAppender> defaultAppenders = new ArrayList<>();

    private LoggingFramework() {
        defaultAppenders.add(new ConsoleAppender(defaultFormatter));
    }

    public static synchronized LoggingFramework getInstance() {
        if (instance == null) {
            instance = new LoggingFramework();
        }
        return instance;
    }

    public Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, key -> {
            Logger logger = new Logger(key, rootLevel);
            for (LogAppender appender : defaultAppenders) {
                logger.addAppender(appender);
            }
            return logger;
        });
    }

    public void setRootLevel(LogLevel level) {
        this.rootLevel = level;
    }

    // =====================================================================
    // 7. MAIN DEMO: Interview Execution Driver
    // =====================================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("==============================================");
        System.out.println("          LOGGING FRAMEWORK DEMO              ");
        System.out.println("==============================================");

        LoggingFramework logManager = LoggingFramework.getInstance();
        Logger logger = logManager.getLogger("OrderService");

        // 1. Level Filtering (Root is INFO by default, so DEBUG is suppressed)
        System.out.println("\n--- Step 1: Default Level Filtering (Level = INFO) ---");
        logger.debug("This DEBUG log should be IGNORED (below INFO)");
        logger.info("Order 1001 created successfully");
        logger.warn("Payment retry attempt 1 for Order 1001");
        logger.error("Payment gateway timeout for Order 1001");
        logger.fatal("Database connection lost! System shutting down");

        // 2. Multiple Appenders (Observer Pattern)
        System.out.println("\n--- Step 2: Adding FileAppender (Console + File) ---");
        logger.addAppender(new FileAppender(new DefaultLogFormatter(), "app.log"));
        logger.info("New User registered: user_99");

        // 3. Dynamic Level Change at Runtime
        System.out.println("\n--- Step 3: Changing Level to DEBUG ---");
        logger.setLevel(LogLevel.DEBUG);
        logger.debug("Executing SQL: SELECT * FROM orders WHERE id = 1001");

        // 4. Thread-Safety / Concurrency Test
        System.out.println("\n--- Step 4: Multi-Threaded Logging Test ---");
        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (int i = 1; i <= 3; i++) {
            final int workerId = i;
            executor.submit(() -> {
                logger.info("Worker thread #" + workerId + " completed job");
            });
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);

        System.out.println("\n--- Logging Demo Completed Successfully ---");
    }
}
