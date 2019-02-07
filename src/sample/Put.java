package sample;

public class Put implements Runnable {
    private Data data;
    private int numObjects;
    private int id;
    private int x;

    Put(Data d, int n, int i) {
        data = d;
        numObjects = n;
        id = i;
        x = 1;
    }

    @Override
    public void run() {
        while (true) {
            boolean dataSuccesful = data.put(x);
            while (!dataSuccesful) {
                // let other threads make some progress; hopefully another thread will decrement below 100
                Thread.currentThread().yield();
                // try incrementing again
                dataSuccesful = data.put(x);
            }
            x++;
        }
    }
}