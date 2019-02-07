package sample;

public class Get implements Runnable {
    private Data data;
    private int numObjects;
    private int id;

    Get(Data d, int n, int i) {
        data = d;
        numObjects = n;
        id = i;
    }

    @Override
    public void run() {
        while (true) {
            boolean dataSuccesful = data.get();
            while (!dataSuccesful) {
                // let other threads make some progress; hopefully another thread will decrement below 100
                Thread.currentThread().yield();
                // try incrementing again
                dataSuccesful = data.get();
            }
        }
    }
}