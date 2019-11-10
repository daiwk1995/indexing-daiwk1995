import java.util.*;
import java.lang.*;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;
import java.lang.Math;
import java.util.Collections;

public class Benchmarks {
    Random generator;
    Integer iterations;
    Integer dataSize;
    HashMap<String, Integer> maxValues;
    HashMap<String, Integer> minValues;
    Long startTime;
    String banner;
    LocalDateTime currentTime;
    static PrintWriter writer;
    static PrintWriter queries;
    static {
        try {
            writer = new PrintWriter("benchmarks/results/benchmark_results.sql", "UTF-8");
            //queries = new PrintWriter("benchmark_queries.sql", "UTF-8");
        } catch(Exception e){
            System.out.println(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public Benchmarks() {
        generator = new Random();
        generator.setSeed(12345);
        iterations = 100;
        dataSize = 10000;
        maxValues = new HashMap<String, Integer>();
        minValues = new HashMap<String, Integer>();
        banner = new Main().bannerid;
        currentTime = LocalDateTime.now(ZoneId.of("America/New_York"));
    }

    void startTimer() {
        startTime = System.nanoTime();
    }

    Long endTimer() {
        Long endTime = System.nanoTime();
        return (endTime - startTime); // convert to millis because 64bit?
    }

    void printDuration(String label, Float nanos) {
        System.out.println(
                label + " performance: "
                + nanos
                + "ns per iteration, "
                + iterations
                + " iterations");

        writer.println(
                "INSERT INTO BENCHMARKS VALUES ('"
                + banner + "','"
                + currentTime + "',"
                + nanos + "," + "'"
                + label + "');");
    }

    Table setupTable() {
        HashSet<String> a = new HashSet<String>();
        a.add("A");
        a.add("B");
        a.add("C");
        // reset max min data because new table
        maxValues = new HashMap<String, Integer>();
        minValues = new HashMap<String, Integer>();
        Table t = new Table("test_table", a);
        return t;
    }

    void updateMaxMin(String attr, Integer val) {
        Integer max = maxValues.get(attr);
        Integer min = minValues.get(attr);
        if (max == null) {
            maxValues.put(attr, val);
            minValues.put(attr, val);
        } else if (max < val) {
            maxValues.put(attr, val);
        } else if (min > val) {
            minValues.put(attr, val);
        }
    }

    Vector<Integer> setupColumn() {
        Vector<Integer> v = new Vector<Integer>();
        for (Integer i = 0; i < dataSize; i++) {
            v.add(i);
        }
        Collections.shuffle(v);
        return v;
    }

    Vector<Tuple> setupTuples() {
        Vector<Tuple> v = new Vector<Tuple>();
        Vector<Integer> A = setupColumn();
        Vector<Integer> B = setupColumn();
        Vector<Integer> C = setupColumn();

        for (Integer i = 0; i < dataSize; i++) {
            Tuple t = new Tuple();
            t.put("A", A.get(i));
            t.put("B", B.get(i));
            t.put("C", C.get(i));
            v.add(t);
        }

        return v;
    }

    public void insertBenchmark() {

        Long sum = Long.valueOf(0);
        for (Integer i = 0; i < iterations; ++i) {
            Table x = setupTable();
            Vector<Tuple> v = setupTuples();
            startTimer();
            for (Tuple t : v) {
                x.insert(t);
            }
            Long end = endTimer();
            sum += end;
        }
        Float s = Float.valueOf(sum);
        Float iter = Float.valueOf(iterations);
        printDuration("insert", s/iter);
    }

    public void bulkLoadBenchmark() {
        Long sum = Long.valueOf(0);
        for (Integer i = 0; i < iterations; ++i) {
            Table x = setupTable();
            Vector<Tuple> v = setupTuples();
            startTimer();
            x.load(v);
            sum += endTimer();
        }
        Float s = Float.valueOf(sum);
        Float iter = Float.valueOf(iterations);
        printDuration("bulkload", s/iter);
    }

    void filterWarmup(Table t) throws Exception {
        System.gc();
        Set<String> cols = new HashSet<String>();
        cols.add("A");
        cols.add("B");
        cols.add("C");

        for (Integer i = 0; i < iterations; ++i) {
            Integer x = generator.nextInt();
            Integer y = generator.nextInt();

            Integer low = x;
            Integer high = y;
            if (low > high) {
                low = y;
                high = x;
            }

            Filter f = new Filter("B", low, high);
            TupleIDSet pl = t.filter(f);
            t.materialize(cols, pl);
        }
    }

    public void filterBenchmark() throws Exception {
        Float avg = Float.valueOf(0);

        for (Integer c = 1; c < 30; c++) {
            Long sum = Long.valueOf(0);
            Table t = setupTable();

            Vector<Tuple> v = setupTuples();
            t.load(v);

            Set<String> cols = new HashSet<String>();
            cols.add("A");
            cols.add("B");
            cols.add("C");

            filterWarmup(t);
            for (Integer i = 0; i < iterations; ++i) {
                Integer low = getRandomNumberInRange(0, dataSize);
                int off = (int)Math.round(dataSize * (c * .01));
                Integer high = low + Integer.valueOf(off);

                Filter f = new Filter("B", low, high);
                startTimer();
                TupleIDSet pl = t.filter(f);
                t.materialize(cols, pl);
                sum += endTimer();
            }
            Float s = Float.valueOf(sum);
            Float iter = Float.valueOf(iterations);
            avg += s/iter;
        }
        printDuration("filter no index", avg/Float.valueOf(30));
    }

    private static Integer getRandomNumberInRange(Integer min, Integer max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public void filterSecondaryIndexBenchmark() throws Exception {
        Float avg = Float.valueOf(0);

        for (Integer c = 1; c < 30; c++) {
            Long sum = Long.valueOf(0);
            Table t = setupTable();

            Vector<Tuple> v = setupTuples();

            t.load(v);
            t.setClusteredIndex("A");
            t.setSecondaryIndex("B");

            Set<String> cols = new HashSet<String>();
            cols.add("A");
            cols.add("B");
            cols.add("C");

            filterWarmup(t);
            for (Integer i = 0; i < iterations; ++i) {
                Integer low = getRandomNumberInRange(0, dataSize);
                int off = (int)Math.round(dataSize * (c * .01));
                Integer high = low + Integer.valueOf(off);

                Filter f = new Filter("B", low, high);
                startTimer();
                TupleIDSet pl = t.filter(f);
                t.materialize(cols, pl);
                sum += endTimer();
            }

            Float s = Float.valueOf(sum);
            Float iter = Float.valueOf(iterations);
            avg += s/iter;
        }
        printDuration("filter secondary index", avg/Float.valueOf(30));
    }

    public void filterClusteredIndexBenchmark() throws Exception {
        Float avg = Float.valueOf(0);

        for (Integer c = 0; c < 30; c++) {
            Long sum = Long.valueOf(0);
            Table t = setupTable();

            Vector<Tuple> v = setupTuples();
            t.load(v);
            t.setClusteredIndex("A");

            Set<String> cols = new HashSet<String>();
            cols.add("A");
            cols.add("B");
            cols.add("C");

            filterWarmup(t);
            for (Integer i = 0; i < iterations; ++i) {
                Integer low = getRandomNumberInRange(0, dataSize);
                int off = (int)Math.round(dataSize * (c * .01));
                Integer high = low + Integer.valueOf(off);

                Filter f = new Filter("A", low, high);
                startTimer();
                TupleIDSet pl = t.filter(f);
                t.materialize(cols, pl);
                sum += endTimer();
            }

            Float s = Float.valueOf(sum);
            Float iter = Float.valueOf(iterations);
            avg += s/iter;
        }
        printDuration("filter clustered index", avg/Float.valueOf(30));
    }

    public void deleteBenchmark() {

        TupleIDSet tuples = allIds();

        Long sum = Long.valueOf(0);
        for (Integer i = 0; i < iterations; ++i) {
            Table x = setupTable();
            Vector<Tuple> v = setupTuples();
            x.load(v);
            startTimer();
            x.delete(tuples);
            sum += endTimer();
        }
        Float s = Float.valueOf(sum);
        Float iter = Float.valueOf(iterations);
        printDuration("delete", s/iter);
    }

    public void updateWarmup(String col) {
        Table x = setupTable();
        Vector<Tuple> v = setupTuples();
        x.load(v);
        for (Integer i = 0; i < iterations; ++i) {

            // update a random half of the tuples
            TupleIDSet tuples = randomIds(dataSize/2);
            Integer value = generator.nextInt();

            x.update(col, tuples, value);
        }
    }

    public void updateBenchmark(String col) {
        // control which column is updated using arguments

        updateWarmup(col);
        Long sum = Long.valueOf(0);

        Table x = setupTable();
        Vector<Tuple> v = setupTuples();
        x.load(v);
        for (Integer i = 0; i < iterations; ++i) {

            // update a random half of the tuples
            TupleIDSet tuples = randomIds(dataSize/2);
            Integer value = generator.nextInt();

            startTimer();
            x.update(col, tuples, value);
            sum += endTimer();
        }

        Float s = Float.valueOf(sum);
        Float iter = Float.valueOf(iterations);

        String label = "clustered update";
        if (col == "B") {
            label = "secondary update";
        } else if (col == "C") {
            label = "unindexed update";
        }
        printDuration(label, s/iter);
    }

    TupleIDSet allIds() {
        TupleIDSet tuples = new TupleIDSet();
        for (Integer i = 0; i < dataSize; i++) {
            tuples.add(i);
        }
        return tuples;
    }

    TupleIDSet randomIds(Integer n) {
        TupleIDSet tuples = new TupleIDSet();
        while (tuples.size() < n) {
            tuples.add(Math.abs(generator.nextInt()) % dataSize);
        }
        return tuples;
    }

    void finish() {
        writer.close();
    }
}
