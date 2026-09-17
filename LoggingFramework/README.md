# Logging Framework - SDE-1 LLD / Machine Coding Guide

This repository contains the complete, production-grade, minimal implementation of a **Logging Framework** (inspired by Log4j / SLF4J) tailored for **SDE-1 Low-Level Design (LLD)** and **Machine Coding** interviews.

---

## 1. The SDE-1 Interview Strategy (45-Minute Playbook)

| Time | Phase | What You Should Do / Say |
|---|---|---|
| **0 - 5 min** | **Clarify Requirements** | Confirm log levels (DEBUG, INFO, WARN, ERROR, FATAL), sinks (Console, File), format structure, and thread-safety expectations. |
| **5 - 12 min** | **Define Core Entities** | Sketch the 5 core classes on the board/editor (`LogLevel`, `LogMessage`, `LogAppender`, `Logger`, `LogManager`). |
| **12 - 32 min** | **Implement Code** | Write clean, working code in [LoggingFramework.java](file:///usr/local/google/home/omtrivedi/Personal/System%20Design/LoggingFramework/LoggingFramework.java). |
| **32 - 40 min** | **Concurrency & Patterns** | Explain `CopyOnWriteArrayList`, `ConcurrentHashMap`, and Strategy/Observer pattern for appenders. |
| **40 - 45 min** | **Run Driver Code** | Execute `main()` to verify filtering, multi-appenders, dynamic level changing, and multi-threaded logging. |

---

## 2. Core Design & Mental Model

```
       ┌────────────────────────┐
       │   LogManager (Singleton)│
       └───────────┬────────────┘
                   │ creates / caches
                   ▼
               Logger (holds currentLevel: e.g. INFO)
                   │
         log(level, message)
                   │ (checks level >= currentLevel)
                   ▼
              LogMessage (immutable: timestamp, level, thread, text)
                   │
         notifies all Appenders (Observer / Strategy Pattern)
         ┌─────────┴──────────┐
         ▼                    ▼
   ConsoleAppender       FileAppender
   (formats & writes)   (formats & writes)
```

---

## 3. Design Patterns Applied

1. **Observer / Strategy Pattern (`LogAppender`)**:
   - Decouples message emission from log storage/output.
   - Allows dynamically registering new sinks (`ConsoleAppender`, `FileAppender`, `KafkaAppender`) without modifying `Logger`.
2. **Singleton Pattern (`LogManager`)**:
   - Ensures a single global configuration and registry for loggers across the application.
3. **Factory Pattern (`LogManager.getLogger(name)`)**:
   - Manages and caches logger instances by name.
4. **Template / Formatter Pattern (`LogFormatter`)**:
   - Separates message layout (timestamps, thread name, padding) from destination sink.

---

## 4. How to Compile & Run

```bash
cd "/usr/local/google/home/omtrivedi/Personal/System Design/LoggingFramework"
javac LoggingFramework.java && java LoggingFramework
```

---

## 5. Top Follow-up Interview Questions & Answers

### Q1: "Some interviewers ask for Chain of Responsibility pattern for Logger. How does that look?"
> **Answer:** In the **Chain of Responsibility** approach, each LogLevel is a handler node that either processes the message or passes it down the chain:
> ```java
> abstract class LoggerHandler {
>     protected LogLevel level;
>     protected LoggerHandler nextHandler;
>
>     public void setNext(LoggerHandler next) { this.nextHandler = next; }
>     public void logMessage(LogLevel level, String msg) {
>         if (this.level == level) { write(msg); }
>         if (nextHandler != null) { nextHandler.logMessage(level, msg); }
>     }
>     abstract protected void write(String msg);
> }
> ```
> *Note for SDE-1:* The **Appender / Sink (Observer)** pattern used in [LoggingFramework.java](file:///usr/local/google/home/omtrivedi/Personal/System%20Design/LoggingFramework/LoggingFramework.java) is how real-world frameworks (Log4j2, Logback) work because a log level is a filter threshold, not an independent chain processor. However, knowing both shows deep design pattern maturity!

### Q2: "How would you make this Asynchronous (Async Logger)?"
> **Answer:** In synchronous logging, the calling thread blocks on file I/O. For high-throughput systems:
> 1. Wrap the appender with a `BlockingQueue<LogMessage>`:
> 2. The main thread calls `queue.offer(logMessage)` (non-blocking).
> 3. A dedicated background worker thread polls the queue and writes batches to the actual sinks.
> *(Mentioning this is a massive bonus point in FAANG interviews!)*

### Q3: "How is thread-safety achieved?"
> **Answer:**
> - `ConcurrentHashMap` ensures safe thread-concurrent retrieval of loggers in `LogManager`.
> - `CopyOnWriteArrayList` allows safely iterating over appenders while new appenders might be registered.
> - `synchronized` on appender write methods prevents interleaved or corrupted output when multiple threads write to the same sink simultaneously.
