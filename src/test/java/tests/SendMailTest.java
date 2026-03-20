package tests;

import mail.GmailApiSend;
import org.testng.annotations.Test;

public class SendMailTest {

    @Test
    public void sendEmails() throws Exception {
        GmailApiSend.sendEmails();
    }
}