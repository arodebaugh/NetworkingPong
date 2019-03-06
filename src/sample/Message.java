package sample;

import java.io.Serializable;

// Serializable means that objects of this class can be read/written over ObjectStreams
public class Message implements Serializable {
    // Message includes both sender ID and text data being sent
    private int sender;
    private double data;
    private double x;
    private double y;
    // both fields are simple Strings, so default code is used to read/write these Strings

    Message(int who, double paddlePosition, double ballX, double ballY) {
        sender = who;
        data = paddlePosition;
        x = ballX;
        y = ballY;
    }

    int sender() {
        return sender;
    }

    double data() {
        return data;
    }

    double ballX() { return x; }

    double ballY() { return y; }

    public String toString() {
        return "\"" + data + "\" from: " + sender;
    }

}
