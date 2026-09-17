import java.util.concurrent.*;

// Implementing Callable to calculate ETA (only this task requires a return value)
class ETACalculationTask implements Callable<String> {
    public String call() throws InterruptedException {
        Thread.sleep(5000); // Simulate 5-second delay for ETA calculation
        System.out.println("Calculation ETA using Callable.");
        return "ETA: 25 minutes"; // Return the ETA result
    }
}

// Implementing Runnable to send SMS (no return value required)
class SMSTask implements Runnable {
    public void run() {
        try {
            Thread.sleep(2000); // Simulate 2-second delay for SMS
            System.out.println("SMS Sent using Runnable.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

// Implementing Runnable to send Email (no return value required)
class EmailTask implements Runnable {
    public void run() {
        try {
            Thread.sleep(3000); // Simulate 3-second delay for Email
            System.out.println("Email Sent using Runnable.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Main {
    public static void main(String[] args) {
        // Create ExecutorService to manage threads
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        // Create Callable task for ETA calculation and Runnable tasks for SMS and Email
        SMSTask smsTask = new SMSTask();
        EmailTask emailTask = new EmailTask();
        ETACalculationTask etaTask = new ETACalculationTask();

        // Submit tasks and get Future objects for the ETA task
        Future<String> etaResult = executorService.submit(etaTask);

        // Submit the SMS and Email tasks (no result required)
        executorService.submit(smsTask);
        executorService.submit(emailTask);

        try {
            // Get the result from the Future object for ETA
            System.out.println(etaResult.get()); // Wait for ETA task to finish and print result
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // Shutdown the ExecutorService
        executorService.shutdown();
    }
}
// 
// class Main {
    // public static void main(String[] args) {
        // SMSTask smsTask = new SMSTask();
        // EmailTask emailTask = new EmailTask();
        // 
        // Wrap Callable in FutureTask so Thread can run it
        // FutureTask<String> etaTaskWrapper = new FutureTask<>(new ETACalculationTask());
// 
        // Create raw Thread objects
        // Thread smsThread = new Thread(smsTask);
        // Thread emailThread = new Thread(emailTask);
        // Thread etaThread = new Thread(etaTaskWrapper);
// 
        // Start them manually
        // smsThread.start();
        // emailThread.start();
        // etaThread.start();
// 
        // try {
            // Get the Callable result from the FutureTask (blocks until complete)
            // System.out.println(etaTaskWrapper.get());
            // 
            // Wait for other threads to finish
            // smsThread.join();
            // emailThread.join();
        // } catch (InterruptedException | ExecutionException e) {
            // e.printStackTrace();
        // }
    // }
// }
// 