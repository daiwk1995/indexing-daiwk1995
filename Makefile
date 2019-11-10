JFLAGS =
JC = javac
HOST=$(shell hostname)

.SUFFIXES: .java .class

.java.class:
	@echo Building $*.java
	@$(JC) $(JFLAGS) $*.java

CLASSES = $(wildcard *.java)
default: classes

classes: $(CLASSES:.java=.class)

testtable: default
	@java -ea Main -testtable
	@(cd data_validation; python3 compare_csv.py) || data_validation/compare.sh

testtree: default
	@java -ea Main -testtree

bench: default
	@java -ea Main -bench

run: default
	@java -ea Main -c

register:
ifeq ($(HOST), dontbuildbplustrees.please)
	@sqlite3 /home/benchmarks/benchmarks.db < benchmarks/results/benchmark_results.sql
	@echo Registered into sqlitedb
else
	@echo "You aren't logged into the Linode machine so you can't register this benchmark"
endif

query:
ifeq ($(HOST), dontbuildbplustrees.please)
	@sqlite3 /home/benchmarks/benchmarks.db < benchmarks/query.sql
else
	@echo "You aren't logged into the Linode machine so you can't query the benchmark db"
endif

data:
	cd data_validation ; ./generate_data.py ; ./range_filter.py ; ./delete.py; ./update.py

clean:
	@rm -rf *.class
	@rm -rf benchmarks/results/benchmark_results.sql
	@rm -rf data_validation/results/*.csv
	@echo "Done cleaning"

