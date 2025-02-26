package main.java.com.emailapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Represents a folder that contains emails and supports sorting.
 */
public class Folder implements Serializable {
    private static final long serialVersionUID = 1L;
    private ArrayList<Email> emails;
    private String name;
    private String sortingMethod = "dateDesc";

    public Folder(String name) {
        this.emails = new ArrayList<>();
        this.name = name;
    }

    public ArrayList<Email> getEmails() { return emails; }
    public String getName() { return name; }

    public void addEmail(Email email) {
        emails.add(email);
        sortEmails();
    }

    public Email removeEmail(int index) {
        return emails.remove(index);
    }

    public void sortEmails() {
        switch (sortingMethod) {
            case "dateAsc":
                emails.sort(Comparator.comparing(Email::getTimestamp));
                break;
            case "dateDesc":
                emails.sort(Comparator.comparing(Email::getTimestamp).reversed());
                break;
            case "subjectAsc":
                emails.sort(Comparator.comparing(Email::getSubject));
                break;
            case "subjectDesc":
                emails.sort(Comparator.comparing(Email::getSubject).reversed());
                break;
        }
    }

    public void setSortingMethod(String method) {
        this.sortingMethod = method;
        sortEmails();
    }
}
