# SMTP Jars (No Maven)

To enable SMTP (Jakarta Mail) without Maven, download these two jars and place them in this `libs/` folder:

1. Jakarta Mail (2.x)
   - Group: com.sun.mail
   - Artifact: jakarta.mail
   - Version: 2.0.1 (or latest 2.x)
   - Download: https://repo1.maven.org/maven2/com/sun/mail/jakarta.mail/2.0.1/jakarta.mail-2.0.1.jar

2. Jakarta Activation (2.x)
   - Group: com.sun.activation
   - Artifact: jakarta.activation
   - Version: 2.0.1 (or latest 2.x)
   - Download: https://repo1.maven.org/maven2/com/sun/activation/jakarta.activation/2.0.1/jakarta.activation-2.0.1.jar

After downloading, this folder should look like:

- libs/
  - jakarta.mail-2.0.1.jar
  - jakarta.activation-2.0.1.jar

The VS Code Java extension is configured to include all jars under `libs/` automatically (see `.vscode/settings.json`).

Once jars are in place, tell me and I will switch `EmailService.sendBill` to use SMTP (STARTTLS on port 587) with PDF attachment support.
