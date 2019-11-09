import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

public class TableTests {

    Table table;
    HashMap<Integer, Tuple> tuples;
    HashSet<String> attributes;
    CSVReader reader;
    Random generator;

    public TableTests() {
    }

    void resetTests() throws Exception {
        HashSet<String> a = new HashSet<String>();
        a.add("A");
        a.add("B");
        a.add("C");

        table = new Table("test_table", a);
        reader = new CSVReader("data_validation/data");
        Vector<Tuple> tup = reader.read();
        table.load(tup);
        table.setClusteredIndex("A");
        table.setSecondaryIndex("B");
        this.attributes = a;
        this.generator = new Random();
    }

    public void testLoad() throws Exception {
        resetTests();
    }

    TupleCollection filterHelper(Filter filter) {
        TupleIDSet intermediate = table.filter(filter);
        HashSet<String> attributes = new HashSet<String>();
        attributes.add("A");
        attributes.add("B");
        attributes.add("C");
        MaterializedResults results = table.materialize(attributes, intermediate);
        TupleCollection tuples = new TupleCollection();
        for (Tuple t : results) {
            tuples.addTuple(t);
        };
        return tuples;
    }
    public void testBetween() throws Exception {
        resetTests();
        Filter filter = new Filter("A", 500, 600);
        TupleCollection tuples = filterHelper(filter);
        tuples.toCSV("data_validation/results/between.csv");
    }

    public void testLT() throws Exception {
        resetTests();
        Filter filter = new Filter("A", null, 600);
        TupleCollection tuples = filterHelper(filter);
        tuples.toCSV("data_validation/results/lt.csv");
    }

    public void testGT() throws Exception {
        resetTests();
        Filter filter = new Filter("A", 500, null);
        TupleCollection tuples = filterHelper(filter);
        tuples.toCSV("data_validation/results/gt.csv");
    }

    public void testComposite() throws Exception {
        resetTests();
        Filter filterA = new Filter("A", 500, 600);
        Filter filterB = new Filter("B", 100, 200);
        Filter composite = new Filter(filterA, filterB, FilterOp.AND);
        TupleCollection tuples = filterHelper(composite);
        tuples.toCSV("data_validation/results/composite.csv");
    }

    public void testUpdate() throws Exception {
        resetTests();
        Filter filter = new Filter("A", 500, 600);
        TupleIDSet intermediate = table.filter(filter);
        table.update("B", intermediate, 2000);

        // need to recreate the indexes because we don't require students to
        // update the index
        table.setClusteredIndex("A");
        table.setSecondaryIndex("B");

        MaterializedResults results = table.materialize(attributes, null);
        TupleCollection tuples = new TupleCollection();
        for (Tuple t : results) {
            tuples.addTuple(t);
        };
        tuples.toCSV("data_validation/results/update.csv");
    }

    public void testDelete() throws Exception {
        resetTests();
        Filter filter = new Filter("A", 500, 600);
        TupleIDSet intermediate = table.filter(filter);

        table.delete(intermediate);

        // need to recreate the indexes because we don't require students to
        // update the index
        table.setClusteredIndex("A");
        table.setSecondaryIndex("B");

        HashSet<String> attributes = new HashSet<String>();
        attributes.add("A");
        attributes.add("B");
        attributes.add("C");
        MaterializedResults results = table.materialize(attributes, null);
        TupleCollection tuples = new TupleCollection();
        for (Tuple t : results) {
            tuples.addTuple(t);
        };
        tuples.toCSV("data_validation/results/delete.csv");
    }

}

class TupleCollection {
    HashMap<Tuple, Integer> map;
    HashSet<String> cols;

    public TupleCollection() {
        map = new HashMap<Tuple, Integer>();
        cols = new HashSet<String>();
    }

    public void addTuple(Tuple t) {
        for (String name : t.keySet()) {
            cols.add(name);
        }

        Integer v = map.putIfAbsent(t, 1);
        if (!(v==null)) {
            map.replace(t, v+1);
        }
    }

    public void toCSV(String file) throws Exception {
        Vector<String> temp = new Vector<String>();

        for (String c : cols) {
            temp.add(c);
        }

        String[] csv_cols = Arrays.copyOf(temp.toArray(),
                                          temp.size(),
                                          String[].class);

        String lines = "";
        Arrays.sort(csv_cols);
        for (Tuple t: map.keySet()) {
            Integer n = map.get(t);
            for (Integer i = 0; i < n; i++) {
                String line = "";
                for (String col : csv_cols) {
                    line = line + t.get(col) + ",";
                }
                // remove last comma and add new line
                line = line.substring(0, line.length() - 1);
                lines = lines + line + "\n";
            }
        }

        // remove last new line
        lines = lines.substring(0, lines.length() - 1);

        // write to csv
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.println(lines);
        writer.close();
    }
}

class CSVReader {
    String path;
    public CSVReader(String p) {
        path = p;
    }

    public Vector<Tuple> read() throws Exception {
        Vector<Tuple> vec = new Vector<Tuple>();

        BufferedReader br = new BufferedReader(new FileReader(path)); 
        String line = "";
        while ((line = br.readLine()) != null) {
            String[] data = line.split(",");
            Tuple t = new Tuple();
            char col = 'A';
            for (String s : data) {
                Integer val = Integer.parseInt(s);
                String colname = "" + col;
                t.put(colname, val);
                col += 1;
            }
            vec.add(t);
        }
        return vec;
    }
}

