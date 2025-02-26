package main.java.com.emailapp;

import main.java.com.emailapp.exceptions.MailboxException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A socket server that supports multiple client connections.
 * Clients log in with a username and can use commands like:
 *   LOGIN <username>
 *   COMPOSE
 *   INBOX
 *   VIEW <index>
 *   QUIT
 */
public class EmailServer {
    private static final int PORT = 5000;
    // Map username to Mailbox (using a thread-safe map)
    private Map<String, Mailbox> mailboxes = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new EmailServer().startServer();
    }

    public void startServer() {
        System.out.println("Email server starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket, mailboxes)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private Map<String, Mailbox> mailboxes;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private Mailbox mailbox;

        public ClientHandler(Socket socket, Map<String, Mailbox> mailboxes) {
            this.socket = socket;
            this.mailboxes = mailboxes;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("Welcome to the Email Server. Please log in with: LOGIN <username>");
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty())
                        continue;

                    if (username == null) {
                        if (line.toUpperCase().startsWith("LOGIN")) {
                            String[] tokens = line.split("\\s+");
                            if (tokens.length >= 2) {
                                username = tokens[1];
                                // Get or create mailbox for this user
                                mailbox = mailboxes.computeIfAbsent(username, k -> new Mailbox());
                                out.println("Logged in as " + username);
                                out.println("Commands: COMPOSE, INBOX, VIEW <index>, QUIT");
                            } else {
                                out.println("Invalid login command. Usage: LOGIN <username>");
                            }
                        } else {
                            out.println("Please log in first with: LOGIN <username>");
                        }
                    } else {
                        // Process commands for a logged-in user
                        if (line.equalsIgnoreCase("QUIT")) {
                            out.println("Goodbye!");
                            break;
                        } else if (line.equalsIgnoreCase("INBOX")) {
                            listInbox();
                        } else if (line.toUpperCase().startsWith("VIEW")) {
                            viewEmail(line);
                        } else if (line.equalsIgnoreCase("COMPOSE")) {
                            composeEmail();
                        } else {
                            out.println("Unknown command. Available commands: COMPOSE, INBOX, VIEW <index>, QUIT");
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Client connection error: " + e.getMessage());
            } finally {
                try { socket.close(); } catch(IOException e) { /* Ignore */ }
            }
        }

        private void listInbox() {
            Folder inbox = mailbox.getFolder("Inbox");
            if (inbox == null || inbox.getEmails().isEmpty()) {
                out.println("Inbox is empty.");
                return;
            }
            for (int i = 0; i < inbox.getEmails().size(); i++) {
                out.println((i + 1) + ": " + inbox.getEmails().get(i).toString());
            }
        }

        private void viewEmail(String command) {
            String[] tokens = command.split("\\s+");
            if (tokens.length < 2) {
                out.println("Usage: VIEW <index>");
                return;
            }
            try {
                int index = Integer.parseInt(tokens[1]) - 1;
                Folder inbox = mailbox.getFolder("Inbox");
                if (inbox == null || index < 0 || index >= inbox.getEmails().size()) {
                    out.println("Invalid email index.");
                } else {
                    Email email = inbox.getEmails().get(index);
                    out.println("To: " + email.getTo());
                    out.println("CC: " + email.getCc());
                    out.println("BCC: " + email.getBcc());
                    out.println("Subject: " + email.getSubject());
                    out.println("Body: " + email.getBody());
                    out.println("Timestamp: " + email.getTimestamp().getTime());
                }
            } catch (NumberFormatException e) {
                out.println("Invalid index format.");
            }
        }

        private void composeEmail() throws IOException {
            out.println("Composing a new email.");
            out.println("Enter recipient (TO):");
            String to = in.readLine();
            out.println("Enter CC (optional):");
            String cc = in.readLine();
            out.println("Enter BCC (optional):");
            String bcc = in.readLine();
            out.println("Enter subject:");
            String subject = in.readLine();
            out.println("Enter body:");
            String body = in.readLine();

            Email email = new Email(to, cc, bcc, subject, body);
            Folder inbox = mailbox.getFolder("Inbox");
            inbox.addEmail(email);
            out.println("Email composed and added to Inbox.");
        }
    }
}
