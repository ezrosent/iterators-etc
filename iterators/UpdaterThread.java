import java.util.Random;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileNotFoundException;

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
        BufferedReader br = null;
        try { br = new BufferedReader(new FileReader(Bench.OP_FILE)); }
        catch (FileNotFoundException e) { e.printStackTrace(); System.exit(1); }
        SetInterface set = Bench.set;

        while (!Bench.begin);

        // Offset
        try { for (int j = 0; j < id; j++) br.readLine(); }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(1); }

        while (!Bench.stop) {
            String line = null;
            try { 
                for (int j = 0; j < Bench.UPDATERS_NUM; j++)
                    line = br.readLine();
            }
            catch (IOException e) {
                e.printStackTrace();
                System.out.println("error");
                System.exit(1); }
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
        }

        try { br.close(); }
        catch (IOException e) {
                e.printStackTrace();
                System.exit(1); }
    }
}
