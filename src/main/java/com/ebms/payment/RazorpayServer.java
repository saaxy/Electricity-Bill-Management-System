package com.ebms.payment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.ebms.dao.DBConnection;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Tiny HTTP server to create Razorpay orders and receive webhooks (dev/demo only).
 * - Endpoints:
 *   GET /create-order?billId=123&amountPaise=10000   -> creates order and assigns order_id to bill
 *   GET /checkout?order_id=order_xxx                 -> returns a minimal HTML with Razorpay Checkout JS that uses the order id
 *   POST /webhook                                    -> webhook receiver; verifies signature and updates bill status if possible
 *
 * Set env vars: RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET
 */
public class RazorpayServer {
    private final RazorpayClient client = new RazorpayClient();
    private final String webhookSecret = System.getenv("RAZORPAY_WEBHOOK_SECRET");

    public void start(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/create-order", new CreateOrderHandler());
    server.createContext("/create-payment-link", new CreatePaymentLinkHandler());
        server.createContext("/checkout", new CheckoutHandler());
        server.createContext("/webhook", new WebhookHandler());
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("RazorpayServer listening on http://localhost:" + port);
    }

    class CreateOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                Map<String, String> q = parseQuery(exchange.getRequestURI());
                String billIdStr = q.get("billId");
                String amountStr = q.get("amountPaise");
                if (billIdStr == null || amountStr == null) {
                    writeJson(exchange, 400, "{\"error\":\"billId and amountPaise required\"}");
                    return;
                }
                long amount = Long.parseLong(amountStr);
                String receipt = "bill_" + billIdStr + "_" + Instant.now().toEpochMilli();
                String resp = client.createOrder(amount, receipt);
                String orderId = RazorpayClient.extractOrderId(resp);
                // Try to persist order id to bill record if DB column exists
                try (Connection con = DBConnection.getConnection()) {
                    if (con != null && orderId != null) {
                        try (PreparedStatement ps = con.prepareStatement("UPDATE bill SET razorpay_order_id = ? WHERE id = ?")) {
                            ps.setString(1, orderId);
                            ps.setInt(2, Integer.parseInt(billIdStr));
                            ps.executeUpdate();
                        } catch (Exception ex) {
                            System.err.println("Failed to persist razorpay_order_id to bill: " + ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("DB update skipped: " + ex.getMessage());
                }
                String out = String.format("{\"order_id\":\"%s\",\"order_resp\":%s,\"checkout_page\":\"/checkout?order_id=%s\"}", orderId, resp, orderId);
                writeJson(exchange, 200, out);
            } catch (Exception e) {
                e.printStackTrace();
                writeJson(exchange, 500, "{\"error\":\"" + e.getMessage().replace("\"","\\\"") + "\"}");
            }
        }
    }

    class CreatePaymentLinkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                Map<String, String> q = parseQuery(exchange.getRequestURI());
                String billIdStr = q.get("billId");
                String amountStr = q.get("amountPaise");
                String email = q.get("email");
                String contact = q.get("contact");
                if (billIdStr == null || amountStr == null) {
                    writeJson(exchange, 400, "{\"error\":\"billId and amountPaise required\"}");
                    return;
                }
                long amount = Long.parseLong(amountStr);
                String description = "Bill payment: " + billIdStr;
                String resp = client.createPaymentLink(amount, description, null, email, contact);
                String shortUrl = RazorpayClient.extractShortUrl(resp);
                String linkId = RazorpayClient.extractOrderId(resp); // extract id field (naive)
                // persist payment_link_id if possible
                try (Connection con = DBConnection.getConnection()) {
                    if (con != null && linkId != null) {
                        try (PreparedStatement ps = con.prepareStatement("UPDATE bill SET payment_link_id = ? WHERE id = ?")) {
                            ps.setString(1, linkId);
                            ps.setInt(2, Integer.parseInt(billIdStr));
                            ps.executeUpdate();
                        } catch (Exception ex) {
                            System.err.println("Failed to persist payment_link_id to bill: " + ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("DB update skipped: " + ex.getMessage());
                }
                String out = String.format("{\"payment_link_id\":\"%s\",\"short_url\":\"%s\",\"resp\":%s}", linkId==null?"":linkId, shortUrl==null?"":shortUrl, resp);
                writeJson(exchange, 200, out);
            } catch (Exception e) {
                e.printStackTrace();
                writeJson(exchange, 500, "{\"error\":\"" + e.getMessage().replace("\"","\\\"") + "\"}");
            }
        }
    }

    class CheckoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI u = exchange.getRequestURI();
            Map<String, String> q = parseQuery(u);
            String orderId = q.get("order_id");
            if (orderId == null) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            String keyId = System.getenv("RAZORPAY_KEY_ID");
        String html = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Pay</title></head><body>"
            + "<h3>Razorpay Checkout (demo)</h3>"
            + "<script src=\"https://checkout.razorpay.com/v1/checkout.js\"></script>"
            + "<button id=\"payBtn\">Pay Now</button>"
            + "<script>"
            + "document.getElementById('payBtn').onclick = function(){"
            + " var options = {"
            + "  key: '" + (keyId!=null?keyId:"") + "',"
            + "  order_id: '" + orderId + "',"
            + "  handler: function(response){ alert(\"Payment success: \" + response.razorpay_payment_id); }"
            + " }; var rzp = new Razorpay(options); rzp.open();" 
            + "};"
            + "</script>"
            + "</body></html>";
            byte[] b = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, b.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
        }
    }

    class WebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
            Headers headers = exchange.getRequestHeaders();
            String sig = headers.getFirst("X-Razorpay-Signature");
            byte[] body = readAll(exchange.getRequestBody());
            String payload = new String(body, StandardCharsets.UTF_8);
            boolean ok = false;
            try {
                ok = verifySignature(payload, sig, webhookSecret);
            } catch (Exception ex) {
                System.err.println("Webhook signature verification error: " + ex.getMessage());
            }
            if (!ok) {
                System.err.println("Invalid webhook signature");
                exchange.sendResponseHeaders(401, -1);
                return;
            }
            System.out.println("Received valid webhook: " + payload);
            // Try to extract payment and order id and update DB mapping if possible (naive parsing)
            String orderId = extractJsonField(payload, "order_id");
            String paymentId = extractJsonField(payload, "payment_id");
            String paymentLinkId = extractJsonField(payload, "payment_link_id");
            // If payload contains order/payment details, try update bill by order id
            if (orderId != null) {
                try (Connection con = DBConnection.getConnection()) {
                    if (con != null) {
                        try (PreparedStatement ps = con.prepareStatement("UPDATE bill SET payment_status = ?, razorpay_payment_id = ?, payment_at = NOW() WHERE razorpay_order_id = ?")) {
                            ps.setString(1, "SUCCESS");
                            ps.setString(2, paymentId);
                            ps.setString(3, orderId);
                            int rows = ps.executeUpdate();
                            System.out.println("Webhook DB update rows=" + rows);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to update bill from webhook: " + ex.getMessage());
                }
            }
            // If payload contains payment_link_id (for payment link payments), update by payment_link_id
            if (paymentLinkId != null) {
                try (Connection con = DBConnection.getConnection()) {
                    if (con != null) {
                        try (PreparedStatement ps = con.prepareStatement("UPDATE bill SET payment_status = ?, razorpay_payment_id = ?, payment_at = NOW() WHERE payment_link_id = ?")) {
                            ps.setString(1, "SUCCESS");
                            ps.setString(2, paymentId);
                            ps.setString(3, paymentLinkId);
                            int rows = ps.executeUpdate();
                            System.out.println("Webhook DB update rows (link)=" + rows);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to update bill from webhook (link): " + ex.getMessage());
                }
            }
            writeJson(exchange, 200, "{\"ok\":true}\n");
        }
    }

    // Helpers
    private static void writeJson(HttpExchange exchange, int code, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, b.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        return in.readAllBytes();
    }

    private static Map<String, String> parseQuery(URI u) {
        String q = u.getRawQuery();
        if (q == null || q.isBlank()) return Map.of();
        return java.util.Arrays.stream(q.split("&"))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(a -> urlDecode(a[0]), a -> a.length>1?urlDecode(a[1]):""));
    }

    private static String urlDecode(String s) {
        try { return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8); } catch (Exception e) { return s; }
    }

    private static boolean verifySignature(String payload, String headerSig, String secret) throws Exception {
        if (secret == null) throw new IllegalStateException("RAZORPAY_WEBHOOK_SECRET not set");
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String computed = bytesToHex(hash);
        return computed.equals(headerSig);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static String extractJsonField(String json, String field) {
        if (json == null || field == null) return null;
        String key = '"' + field + '"';
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int startQuote = json.indexOf('"', colon);
        if (startQuote < 0) return null;
        int endQuote = json.indexOf('"', startQuote + 1);
        if (endQuote < 0) return null;
        return json.substring(startQuote + 1, endQuote);
    }
}
