package vn.edu.hcmuaf.fit.ttltw_nhom6.utils;

//import javax.mail.*;
//import javax.mail.internet.*;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailUtils {
    public static void sendEmail(String to, String subject, String content) {
        final String username = "doghealthapp.app@gmail.com";
        final String password = "fhuhzpuzjqqilzqm";
//        final String username = "comicstore365@gmail.com";
//        final String password = "ddnaxksbhrfimxux";
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

             String form =
                    "<html>" +
                            "<head><meta charset='UTF-8'></head>" +
                            "<body>" +
                            "  <h3>Comic Store gửi mã code cho bạn: </h3>" + content +
                            "</body>" +
                            "</html>";

            message.setContent(form, "text/html; charset=UTF-8");



            //message.setText(content);

            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
