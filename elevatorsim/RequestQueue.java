package elevatorsim;

import java.util.ArrayList;
import java.util.NoSuchElementException;

public class RequestQueue extends ArrayList<Request> {
    public void enqueue(Request request) {
        this.add(request);
    }

    public Request dequeue() {
        if (!this.isEmpty()) {
            return this.remove(0);
        } else {
            throw new NoSuchElementException("Queue is empty");
        }
    }
}

