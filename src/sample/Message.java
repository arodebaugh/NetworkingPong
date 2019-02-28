package sample;

import java.io.Serializable;

// Serializable means that objects of this class can be read/written over ObjectStreams
public class Message implements Serializable {
    // Message includes both sender ID and text data being sent
    private int sender;
    private double data;
    // both fields are simple Strings, so default code is used to read/write these Strings

    Message(int who, double what) {
        sender = who;
        data = what;
    }

    int sender() {
        return sender;
    }

    double data() {
        return data;
    }

    public String toString() {
        return "\"" + data + "\" from: " + sender;
    }

}
