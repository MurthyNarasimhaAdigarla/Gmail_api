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
        System.out.println("Config loaded: recipients=" + ConfigReader.getNullable("recipients"));// throws if missing
        String[] recipients = Arrays.stream(recipientsRaw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        MimeBodyPart textPart = new MimeBodyPart();

        String from = ConfigReader.get("gmail.from");
        String subject = ConfigReader.getOrDefault("mail.subject", "No subject");
        String raw = ConfigReader.get("mail.body");
        if (raw == null) raw = "";
        String body = raw.contains("\\n") ? raw.replace("\\n", System.lineSeparator()) : raw;
        textPart.setText(body);
        String resumePath = ConfigReader.getNullable("resume.path");
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