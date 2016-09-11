import java.util.Random;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;

class UpdaterThread extends Thread {
    private int id;

    public int inserts;
    public int removals;
    public int contains;

    public UpdaterThread(int i) {
        this.id = i;
        inserts = 0;
        removals = 0;
        contains = 0;
    }

    public void run() {
        SetInterface set = Bench.set;
        List<String> ops = Bench.ops.get(id);
        int n = 0;
        int num_ops = ops.size();

        while (!Bench.begin);

        while (!Bench.stop) {
            String line = ops.get(n);

            String[] tokens = line.split("\t");
            String op = tokens[0];
            int key = Integer.parseInt(tokens[1]);

            if (op.equals("ins")) {
                set.insert(key, id);
                inserts += 1;
            }
            else if (op.equals("del")) {
                set.delete(key, id);
                removals += 1;
            }
            else {
                set.search(key, id);
                contains += 1;
            }
            n = (n + 1) % num_ops; 
        }
    }
}
