package CasaCulturaAPI.notification;

public interface EmailNotificationPort {
    void send(String recipient, String subject, String body);
}
