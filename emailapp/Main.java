package main.java.com.emailapp;

public class Main {
    public static void main(String[] args) {
        // Uncomment one of the following lines depending on which mode you want:
        
        // To run the JavaFX GUI client:
        // EmailClient.main(args);
        
        // To run the socket-based client (console):
        // EmailClientSocket.main(args);
        
        // To run the socket server (hosting multi-user connections):
        // EmailServer.main(args);
        
        // For demonstration, you might start the server in one terminal
        // and the client in another.
        System.out.println("Please run either EmailServer, EmailClientSocket, or EmailClient separately.");
    }
}
