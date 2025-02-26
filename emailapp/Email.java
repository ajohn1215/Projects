package main.java.com.emailapp;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * Represents an email with recipients, subject, body, and timestamp.
 */
public class Email implements Serializable {
    private static final long serialVersionUID = 1L;
    private String to, cc, bcc, subject, body;
    private GregorianCalendar timestamp;

    public Email(String to, String cc, String bcc, String subject, String body) {
        this.to = to;
        this.cc = cc;
        this.bcc = bcc;
        this.subject = subject;
        this.body = body;
        this.timestamp = new GregorianCalendar();
    }

    public String getTo() { return to; }
    public String getCc() { return cc; }
    public String getBcc() { return bcc; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public GregorianCalendar getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a MM/dd/yyyy");
        return "[" + sdf.format(timestamp.getTime()) + "] " + subject;
    }
}
