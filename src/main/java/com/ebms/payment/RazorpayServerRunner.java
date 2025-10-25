package com.ebms.payment;

import java.util.concurrent.CountDownLatch;

/**
 * Simple runner for the RazorpayServer. Starts the server on the given port (default 8080)
 * and blocks indefinitely so it can receive webhooks from ngrok or Razorpay.
 *
 * Usage:
 *   java -cp "out;libs/*" com.ebms.payment.RazorpayServerRunner 8080
 */
public class RazorpayServerRunner {
    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException e) { /* ignore, use default */ }
        }
        RazorpayServer server = new RazorpayServer();
        try {
            server.start(port);
            System.out.println("Razorpay server started on port " + port);
            System.out.println("Tip: run `ngrok http " + port + "` and configure that https URL in Razorpay webhooks (use payment.captured)");
            // block forever
            new CountDownLatch(1).await();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
