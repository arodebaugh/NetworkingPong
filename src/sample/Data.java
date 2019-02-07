package sample;

import java.util.ArrayList;

public class Data {
    private ArrayList dataList = new ArrayList();
    private int x;
    private int numObjects;
    private int arrayCounter;
    private boolean queue;

    Data(boolean q) {
        queue = q;
        arrayCounter = -1;
        if (queue) {
            x = -1;
        } else {
            x = numObjects;
        }
    }

    public void put(Object data) {
        arrayCounter++;
        dataList.add(data);
    }

    synchronized boolean get() {
        if (queue) {
            x++;
        } else {
            x--;
        }

        try {
            System.out.println("Order Stack " + x + ": " + dataList.get(x));
        } catch (ArrayIndexOutOfBoundsException exception) {
            return false;
        }

        if (x < 0) {
            System.out.println("ERROR: Data cannot output less than index of 0.");
            return false;
        } else if (x > dataList.size()) {
            System.out.println("ERROR: Data cannot output more than index of size.");
            return false;
        }

        return true;
    }
}