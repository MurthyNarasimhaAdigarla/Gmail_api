package mail;

import auth.GmailServiceBuilder;
import com.google.api.services.gmail.Gmail;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.commons.codec.binary.Base64;
import utils.ConfigReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Properties;

public class GmailApiSend {

    // Return type uses fully-qualified Gmail model Message to avoid name clash
    private static com.google.api.services.gmail.model.Message createMessageWithEmail(MimeMessage emailContent) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);

        com.google.api.services.gmail.model.Message message = new com.google.api.services.gmail.model.Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private static MimeMessage createEmailWithAttachment(String to, String from, String subject, String bodyText, File file) throws Exception {

        // No SMTP props required for Gmail API; Session only used to build MimeMessage
        Session session = Session.getDefaultInstance(new Properties(), null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        // This Message refers to jakarta.mail.Message because of the import above
        email.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(bodyText);

        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.attachFile(file);

        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(attachmentPart);

        email.setContent(multipart);
        return email;
    }


    public static void sendEmails() throws Exception {
        Gmail service = GmailServiceBuilder.getGmailService();
        // System.out.println("Config loaded: recipients=" + ConfigReader.getNullable("recipients"));

        String recipientsRaw = ConfigReader.get("recipients");
        System.out.println("Config loaded: recipientss=" + ConfigReader.getNullable("recipients"));// throws if missing
        //String[] recipients = Arrays.stream(recipientsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        //MimeBodyPart textPart = new MimeBodyPart();

        // load config values (safe accessors)
        String from = ConfigReader.getOrDefault("gmail.from", "<missing-from>");
        String subject = ConfigReader.getOrDefault("mail.subject", "No subject");
        String raw = ConfigReader.getNullable("mail.body");
        if (raw == null) raw = "";

// handle both literal "\n" sequences and already-converted newlines
        String body = raw.contains("\\n") ? raw.replace("\\n", System.lineSeparator()) : raw;

// set the email body part
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body, "utf-8");

// resume path (may be null)
        String resumePath = ConfigReader.getNullable("resume.path");

// recipients (safe split and trim)
        //String recipientsRaw = ConfigReader.getNullable("recipients");
        String[] recipients = new String[0];
        if (recipientsRaw != null && !recipientsRaw.isBlank()) {
            recipients = Arrays.stream(recipientsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        }

// Print everything to console in a clear format
        System.out.println("=== Email configuration ===");
        System.out.println("From       : " + from);
        System.out.println("Subject    : " + subject);
        System.out.println("Resume path: " + (resumePath == null ? "<none>" : resumePath));
        System.out.println("Recipients : " + (recipients.length == 0 ? "<none>" : String.join(", ", recipients)));
        System.out.println("Body (preview, first 300 chars):");
        if (body.length() <= 300) {
            System.out.println(body);
        } else {
            System.out.println(body.substring(0, 300) + "...");
        }
        System.out.println("===========================");
        File resume = (resumePath == null || resumePath.isBlank()) ? null : new File(resumePath);
        if (resume != null && (!resume.exists() || !resume.isFile())) {
            throw new IllegalStateException("Attachment file not found: " + resumePath);
        }


        for (String to : recipients) {
            MimeMessage email = createEmailWithAttachment(to, from, subject, body, resume);
            com.google.api.services.gmail.model.Message message = createMessageWithEmail(email);
            service.users().messages().send("me", message).execute();
            System.out.println("Email sent to: " + to);
        }
    }
}