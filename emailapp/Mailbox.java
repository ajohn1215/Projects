package main.java.com.emailapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.java.com.emailapp.exceptions.MailboxException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Manages folders and emails with persistence using JSON.
 */
public class Mailbox implements Serializable {
    private static final long serialVersionUID = 1L;
    private Folder inbox = new Folder("Inbox");
    private Folder trash = new Folder("Trash");
    private ArrayList<Folder> folders = new ArrayList<>();

    public Mailbox() {
        folders.add(inbox);
        folders.add(trash);
    }

    public void addFolder(String name) throws MailboxException {
        if (getFolder(name) != null)
            throw new MailboxException("Folder exists.");
        folders.add(new Folder(name));
    }

    public void removeFolder(String name) throws MailboxException {
        Folder folder = getFolder(name);
        if (folder == null || folder.getName().equals("Inbox") || folder.getName().equals("Trash"))
            throw new MailboxException("Cannot remove this folder.");
        folders.remove(folder);
    }

    public Folder getFolder(String name) {
        return folders.stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);
    }

    public void saveMailbox() throws MailboxException {
        try {
            new ObjectMapper().writeValue(new File("src/main/resources/mailbox.json"), this);
        } catch (IOException e) {
            throw new MailboxException("Failed to save mailbox.");
        }
    }

    public static Mailbox loadMailbox() throws MailboxException {
        File file = new File("src/main/resources/mailbox.json");
        if (!file.exists())
            return new Mailbox();
        try {
            return new ObjectMapper().readValue(file, Mailbox.class);
        } catch (IOException e) {
            throw new MailboxException("Failed to load mailbox.");
        }
    }
}
