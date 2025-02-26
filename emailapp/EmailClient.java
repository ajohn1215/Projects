package main.java.com.emailapp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import main.java.com.emailapp.exceptions.MailboxException;

/**
 * JavaFX GUI for the email system.
 */
public class EmailClient extends Application {
    private Mailbox mailbox;

    @Override
    public void start(Stage stage) {
        try {
            mailbox = Mailbox.loadMailbox();
        } catch (MailboxException e) {
            System.out.println(e.getMessage());
            mailbox = new Mailbox();
        }

        Button composeBtn = new Button("Compose Email");
        Button viewInboxBtn = new Button("View Inbox");

        composeBtn.setOnAction(e -> System.out.println("Composing email..."));
        viewInboxBtn.setOnAction(e -> System.out.println("Opening Inbox..."));

        VBox layout = new VBox(10, composeBtn, viewInboxBtn);
        Scene scene = new Scene(layout, 300, 200);
        stage.setScene(scene);
        stage.setTitle("Email Client");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
