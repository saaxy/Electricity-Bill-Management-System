




package com.ebms.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

// Jakarta Mail will be used via reflection at runtime (no compile-time dependency required)

/**
 * Email service API (skeleton).
 *
 * Add your SMTP implementation here (e.g., using Jakarta Mail).
 * Keep secrets in environment variables, do not hardcode them.
 */
public class EmailService {
    // Environment variable names (expected configuration)
    public static final String ENV_SMTP_HOST = "SMTP_HOST";         // e.g., smtp.gmail.com
    public static final String ENV_SMTP_PORT = "SMTP_PORT";         // e.g., 587 or 465
    public static final String ENV_SMTP_USERNAME = "SMTP_USERNAME"; // SMTP account/user
    public static final String ENV_SMTP_PASSWORD = "SMTP_PASSWORD"; // SMTP password or app password
    public static final String ENV_SMTP_FROM = "SMTP_FROM";         // From address (e.g., no-reply@yourdomain)
    public static final String ENV_EMAIL_ENABLED = "EMAIL_ENABLED"; // true/false to toggle sending
    public static final String ENV_EMAIL_DEBUG = "EMAIL_DEBUG";     // true to print debug info
    // Gmail-only implementation; redundant providers removed

    public EmailService() {}

    /**
     * Sends a bill email with an optional PDF attachment.
     * Priority: SMTP if SMTP_HOST is configured, otherwise Mailgun HTTP API.
     */
    public boolean sendBill(String toAddress, String subject, String bodyText, File pdfAttachment) {
        String enabled = System.getenv(ENV_EMAIL_ENABLED);
        boolean debug = "true".equalsIgnoreCase(getenvTrim(ENV_EMAIL_DEBUG));
        if (enabled == null || !"true".equalsIgnoreCase(enabled.trim())) {
            if (debug) {
                System.out.println("[EmailDebug] EMAIL_ENABLED is not 'true' (value='" + enabled + "'). Skipping send.");
            }
            return false;
        }
        return sendViaSmtp(toAddress, subject, bodyText, pdfAttachment);
    }

    /**
     * SMTP sender using Jakarta Mail with STARTTLS (587) or SSL (465).
     * Required env vars for SMTP:
     *  - SMTP_HOST (e.g., smtp.mailgun.org)
     *  - SMTP_PORT (e.g., 587 or 465)
     *  - SMTP_USERNAME (e.g., postmaster@your-domain)
     *  - SMTP_PASSWORD
     *  - SMTP_FROM (e.g., "Your App <postmaster@your-domain>")
     */
    public boolean sendViaSmtp(String to, String subject, String text, File pdfAttachment) {
        if (to == null || to.isBlank()) { return false; }
        boolean debug = "true".equalsIgnoreCase(getenvTrim(ENV_EMAIL_DEBUG));

        String host = getenvTrim(ENV_SMTP_HOST);
        String portStr = getenvTrim(ENV_SMTP_PORT);
        String username = getenvTrim(ENV_SMTP_USERNAME);
        String password = getenvTrim(ENV_SMTP_PASSWORD);
        String fromRaw = getenvTrim(ENV_SMTP_FROM);

        if (host == null || portStr == null || username == null || password == null) {
            if (debug) {
                System.out.println("[EmailDebug] Missing required SMTP env vars:" +
                        (host == null ? " SMTP_HOST" : "") +
                        (portStr == null ? " SMTP_PORT" : "") +
                        (username == null ? " SMTP_USERNAME" : "") +
                        (password == null ? " SMTP_PASSWORD" : ""));
            }
            return false;
        }

        int port = 587; // default STARTTLS
        try { port = Integer.parseInt(portStr.trim()); } catch (NumberFormatException ignored) { }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", "true");
        if (port == 465) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        // Gmail-friendly defaults
    boolean isGmail = host.toLowerCase().contains("gmail")
        || (username.toLowerCase().endsWith("@gmail.com") || username.toLowerCase().endsWith("@googlemail.com"));
        if (isGmail) {
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.auth.mechanisms", "LOGIN PLAIN");
            props.put("mail.smtp.user", username);
            props.put("mail.smtp.ssl.trust", host);
            // Gmail often ignores differing From; enforce username as sender
            fromRaw = username;
        }
        // Helpful defaults
        props.put("mail.smtp.connectiontimeout", "15000"); // 15s
        props.put("mail.smtp.timeout", "20000"); // 20s
        props.put("mail.smtp.writetimeout", "20000"); // 20s
        props.put("mail.smtp.ssl.trust", host); // trust this host for TLS
        // Explicitly control Jakarta Mail debug for clean output
        props.put("mail.debug", debug ? "true" : "false");

        try {
            ClassLoader mailCl = resolveMailClassLoader();
            Class<?> sessionCls = Class.forName("jakarta.mail.Session", false, mailCl);
            Class<?> messageCls = Class.forName("jakarta.mail.Message", false, mailCl);
            Class<?> addressCls = Class.forName("jakarta.mail.Address", false, mailCl);
            Class<?> mimeMessageCls = Class.forName("jakarta.mail.internet.MimeMessage", false, mailCl);
            Class<?> internetAddressCls = Class.forName("jakarta.mail.internet.InternetAddress", false, mailCl);
            Class<?> mimeBodyPartCls = Class.forName("jakarta.mail.internet.MimeBodyPart", false, mailCl);
            Class<?> mimeMultipartCls = Class.forName("jakarta.mail.internet.MimeMultipart", false, mailCl);
            Class<?> transportCls = Class.forName("jakarta.mail.Transport", false, mailCl);
            Class<?> recipientTypeCls = Class.forName("jakarta.mail.Message$RecipientType", false, mailCl);
            Class<?> authenticatorCls = Class.forName("jakarta.mail.Authenticator", false, mailCl);

            // Session session = Session.getInstance(props, null);
            java.lang.reflect.Method getInstance = sessionCls.getMethod("getInstance", Properties.class, authenticatorCls);
            Object session = getInstance.invoke(null, props, null);
            // Force session debug on/off according to EMAIL_DEBUG (overrides any JVM defaults)
            try {
                sessionCls.getMethod("setDebug", boolean.class).invoke(session, debug);
            } catch (ReflectiveOperationException ignore) {}

            // MimeMessage message = new MimeMessage(session);
            java.lang.reflect.Constructor<?> msgCtor = mimeMessageCls.getConstructor(sessionCls);
            Object message = msgCtor.newInstance(session);

            // From
            String fromEmail = extractEmail(fromRaw != null ? fromRaw : username);
            if (fromEmail == null || fromEmail.isBlank()) fromEmail = username;
            props.put("mail.smtp.from", fromEmail);
            Object fromAddr = internetAddressCls.getConstructor(String.class).newInstance(fromEmail);
            mimeMessageCls.getMethod("setFrom", addressCls).invoke(message, fromAddr);

            // Gmail path only; no sandbox guards

            if (debug) {
                System.out.println("[EmailDebug] SMTP config:" +
                        " host=" + host +
                        " port=" + port +
                        " username=" + username +
                        " from=" + fromEmail +
                        " to=" + to);
            }

            // To
            java.lang.reflect.Method parseMethod = internetAddressCls.getMethod("parse", String.class, boolean.class);
            Object toAddrs = parseMethod.invoke(null, to, false);
            Object recipientTypeTo = recipientTypeCls.getField("TO").get(null);
            mimeMessageCls.getMethod(
                    "setRecipients",
                    recipientTypeCls,
                    java.lang.reflect.Array.newInstance(addressCls, 0).getClass()
            ).invoke(message, recipientTypeTo, toAddrs);

            // Subject
            mimeMessageCls.getMethod("setSubject", String.class, String.class)
                    .invoke(message, subject, StandardCharsets.UTF_8.name());

            // Text part
            Object textPart = mimeBodyPartCls.getConstructor().newInstance();
            mimeBodyPartCls.getMethod("setText", String.class, String.class)
                    .invoke(textPart, text, StandardCharsets.UTF_8.name());

            Object multipart = mimeMultipartCls.getConstructor().newInstance();
            // multipart.addBodyPart(textPart);
            Class<?> bodyPartCls = Class.forName("jakarta.mail.BodyPart", false, mailCl);
            mimeMultipartCls.getMethod("addBodyPart", bodyPartCls).invoke(multipart, textPart);

            // Attachment
            if (pdfAttachment != null && pdfAttachment.exists()) {
                Object attachPart = mimeBodyPartCls.getConstructor().newInstance();
                Class<?> dataSourceCls = Class.forName("jakarta.activation.DataSource", false, mailCl);
                Class<?> fileDataSourceCls = Class.forName("jakarta.activation.FileDataSource", false, mailCl);
                Object dataSource = fileDataSourceCls.getConstructor(File.class).newInstance(pdfAttachment);
                Class<?> dataHandlerCls = Class.forName("jakarta.activation.DataHandler", false, mailCl);
                Object dataHandler = dataHandlerCls.getConstructor(dataSourceCls).newInstance(dataSource);
                mimeBodyPartCls.getMethod("setDataHandler", dataHandlerCls).invoke(attachPart, dataHandler);
                mimeBodyPartCls.getMethod("setFileName", String.class).invoke(attachPart, pdfAttachment.getName());
                mimeBodyPartCls.getMethod("setHeader", String.class, String.class)
                        .invoke(attachPart, "Content-Type", "application/pdf");
                mimeMultipartCls.getMethod("addBodyPart", bodyPartCls).invoke(multipart, attachPart);
            }

            // message.setContent(multipart);
            Class<?> multipartCls = Class.forName("jakarta.mail.Multipart", false, mailCl);
            mimeMessageCls.getMethod("setContent", multipartCls).invoke(message, multipart);

            // Send
            Object transport = sessionCls.getMethod("getTransport", String.class).invoke(session, "smtp");
            transportCls.getMethod("connect", String.class, int.class, String.class, String.class)
                    .invoke(transport, host, port, username, password);
        Object recipients = mimeMessageCls.getMethod("getAllRecipients").invoke(message);
        Class<?> addressArrayCls = java.lang.reflect.Array.newInstance(addressCls, 0).getClass();
        transportCls.getMethod("sendMessage", messageCls, addressArrayCls)
            .invoke(transport, message, recipients);
            transportCls.getMethod("close").invoke(transport);

            System.out.println("SMTP: email sent to " + to);
            return true;
        } catch (ClassNotFoundException e) { if (debug) System.out.println("[EmailDebug] ClassNotFound: " + e.getMessage()); return false; }
        catch (NoSuchMethodException e) { if (debug) System.out.println("[EmailDebug] NoSuchMethod: " + e.getMessage()); return false; }
        catch (NoSuchFieldException e) { if (debug) System.out.println("[EmailDebug] NoSuchField: " + e.getMessage()); return false; }
        catch (IllegalAccessException e) { if (debug) System.out.println("[EmailDebug] IllegalAccess: " + e.getMessage()); return false; }
        catch (InstantiationException e) { if (debug) System.out.println("[EmailDebug] Instantiation: " + e.getMessage()); return false; }
        catch (java.lang.reflect.InvocationTargetException e) {
            if (debug) {
                System.out.println("[EmailDebug] InvocationTarget: " + e.toString());
            }
            return false;
        }
        catch (java.net.MalformedURLException e) { if (debug) System.out.println("[EmailDebug] MalformedURL: " + e.getMessage()); return false; }
        catch (RuntimeException e) { if (debug) System.out.println("[EmailDebug] Runtime: " + e.getMessage()); return false; }
    }

    private static String getenvTrim(String key) {
        String v = System.getenv(key);
        return v == null ? null : v.trim();
    }

    private static String extractEmail(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        int lt = s.indexOf('<');
        int gt = s.indexOf('>');
        if (lt >= 0 && gt > lt) return s.substring(lt + 1, gt).trim();
        return s;
    }

    private static ClassLoader resolveMailClassLoader() throws ClassNotFoundException, java.net.MalformedURLException {
        // If Jakarta Mail is already on the classpath, use the current TCCL
        try {
            Class.forName("jakarta.mail.Session");
            return Thread.currentThread().getContextClassLoader();
        } catch (ClassNotFoundException e) {
            // Try to load from local libs/*.jar at runtime
            File libsDir = new File("libs");
            File libDir = new File("lib");
            if (!libsDir.exists()) libsDir = new File("./libs");
            if (!libDir.exists()) libDir = new File("./lib");
            java.util.List<File> jarList = new java.util.ArrayList<>();
            if (libsDir.exists()) {
                File[] arr = libsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
                if (arr != null) jarList.addAll(java.util.Arrays.asList(arr));
            }
            if (libDir.exists()) {
                File[] arr = libDir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
                if (arr != null) jarList.addAll(java.util.Arrays.asList(arr));
            }
            int jarCount = jarList.size();
            if (jarCount == 0) throw e;
            java.net.URL[] urls = new java.net.URL[jarCount];
            for (int i = 0; i < jarCount; i++) {
                urls[i] = jarList.get(i).toURI().toURL();
            }
            java.net.URLClassLoader ucl = new java.net.URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
            Thread.currentThread().setContextClassLoader(ucl);
            // Verify
            ucl.loadClass("jakarta.mail.Session");
            return ucl;
        }
    }

    // Removed Mailgun sandbox helpers
}
