package com.ebms.payment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Minimal Razorpay client using java.net.http for dev/test usage.
 * - Uses env vars RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET
 * - createOrder returns the raw JSON response from Razorpay
 */
public class RazorpayClient {
    private final String keyId = System.getenv("RAZORPAY_KEY_ID");
    private final String keySecret = System.getenv("RAZORPAY_KEY_SECRET");
    private final HttpClient client = HttpClient.newHttpClient();

    public RazorpayClient() {
        if (keyId == null || keySecret == null) {
            System.err.println("Razorpay keys are not set in environment variables (RAZORPAY_KEY_ID/RAZORPAY_KEY_SECRET)");
        }
    }

    public String createOrder(long amountPaise, String receipt) throws Exception {
        String auth = keyId + ":" + keySecret;
        String basic = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String json = String.format("{\"amount\":%d,\"currency\":\"INR\",\"receipt\":\"%s\",\"payment_capture\":1}", amountPaise, escapeJson(receipt));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI("https://api.razorpay.com/v1/orders"))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("Razorpay create order failed: HTTP " + resp.statusCode() + " - " + resp.body());
        }
        return resp.body();
    }

    /**
     * Create a payment link for a given amount. Returns the raw JSON response.
     * Minimal payload: amount (paise), currency, description, customer (optional name/email/contact)
     */
    public String createPaymentLink(long amountPaise, String description, String customerName, String customerEmail, String customerContact) throws Exception {
        String auth = keyId + ":" + keySecret;
        String basic = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append(String.format("\"amount\":%d,", amountPaise));
        json.append("\"currency\":\"INR\",");
        json.append(String.format("\"description\":\"%s\",", escapeJson(description)));
        json.append("\"accept_partial\":false,");
        // minimal customer object
        json.append("\"customer\":{");
        boolean added = false;
        if (customerName != null && !customerName.isBlank()) {
            json.append(String.format("\"name\":\"%s\"", escapeJson(customerName)));
            added = true;
        }
        if (customerEmail != null && !customerEmail.isBlank()) {
            if (added) json.append(',');
            json.append(String.format("\"email\":\"%s\"", escapeJson(customerEmail)));
            added = true;
        }
        if (customerContact != null && !customerContact.isBlank()) {
            if (added) json.append(',');
            json.append(String.format("\"contact\":\"%s\"", escapeJson(customerContact)));
        }
        json.append("}");
        json.append("}");

        HttpRequest req = HttpRequest.newBuilder()
                .uri(new URI("https://api.razorpay.com/v1/payment_links"))
                .header("Authorization", "Basic " + basic)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new RuntimeException("Razorpay create payment link failed: HTTP " + resp.statusCode() + " - " + resp.body());
        }
        return resp.body();
    }

    // Very small helper to pull the order id from a Razorpay JSON response.
    // This is a naive extraction suitable for development. For production, use a JSON parser.
    public static String extractOrderId(String razorpayOrderJson) {
        if (razorpayOrderJson == null) return null;
        String marker = "\"id\":\"";
        int idx = razorpayOrderJson.indexOf(marker);
        if (idx < 0) return null;
        int start = idx + marker.length();
        int end = razorpayOrderJson.indexOf('"', start);
        if (end < 0) return null;
        return razorpayOrderJson.substring(start, end);
    }

    // Very small helper to extract the short_url (payment link URL) from a payment link response
    public static String extractShortUrl(String paymentLinkJson) {
        if (paymentLinkJson == null) return null;
        String marker = "\"short_url\":\"";
        int idx = paymentLinkJson.indexOf(marker);
        if (idx < 0) return null;
        int start = idx + marker.length();
        int end = paymentLinkJson.indexOf('"', start);
        if (end < 0) return null;
        return paymentLinkJson.substring(start, end);
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}
