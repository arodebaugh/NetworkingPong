package sample;

import java.io.Serializable;

// Serializable means that objects of this class can be read/written over ObjectStreams
public class Message implements Serializable {
    // Message includes both sender ID and text data being sent
    private int sender;
    private double data;
    private double x;
    private double y;
    private int player0Score;
    private int player1Score;
    // both fields are simple Strings, so default code is used to read/write these Strings

    Message(int who, double paddlePosition, double ballX, double ballY, int zero, int one) {
        sender = who;
        data = paddlePosition;
        x = ballX;
        y = ballY;
        player0Score = zero;
        player1Score = one;
    }

    int sender() {
        return sender;
    }

    double data() {
        return data;
    }

    double ballX() { return x; }

    double ballY() { return y; }

    int player0Score() { return player0Score; }

    int player1Score() { return player1Score; }

    public String toString() {
        return "paddle \"" + data + "\" Ball XY \"" + x + ", " + y + "\" from: " + sender;
    }

}
