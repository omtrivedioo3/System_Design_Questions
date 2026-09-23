import java.util.*;
import java.util.concurrent.*;

// Implementing the Runnable interface for sending SMS
class SMSTask implements Runnable {
    public void run() {
        try {
            Thread.sleep(2000); // 2-second delay for SMS
            System.out.println("SMS Sent using Runnable.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

// Implementing the Runnable interface for sending Email
class EmailTask implements Runnable {
    public void run() {
        try {
            Thread.sleep(3000); // 3-second delay for Email
            System.out.println("Email Sent using Runnable.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

// Implementing the Runnable interface for calculating ETA
class ETATask implements Runnable {
    public void run() {
        try {
            Thread.sleep(5000); // 5-second delay for ETA calculation
            System.out.println("ETA Calculated using Runnable. Estimated Time: 25 minutes.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Main {
    public static void main(String[] args) {
        // Create Runnable objects for SMS, Email, and ETA calculation
        SMSTask smsTask = new SMSTask();
        EmailTask emailTask = new EmailTask();
        ETATask etaTask = new ETATask();
        
        // another way to create thread instead of using thread class 
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.submit(smsTask);
        executorService.submit(emailTask);
        executorService.submit(etaTask);

        executorService.shutdown();

        // Create Thread objects and pass the Runnable tasks to them
        Thread smsThread = new Thread(smsTask);
        Thread emailThread = new Thread(emailTask);
        Thread etaThread = new Thread(etaTask);

        System.out.println("Task Started.\n");

        // Start all threads
        smsThread.start();
        System.out.println("Task 1 ongoing...");

        emailThread.start();
        System.out.println("Task 2 ongoing...");

        etaThread.start();
        System.out.println("Task 3 ongoing...");

        // Wait for all threads to finish
        try {
            smsThread.join();
            emailThread.join();
            etaThread.join();
            System.out.println("All tasks completed.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
