# SampleGraphApp

A demo application that uses JanusGraph to load and explore the Air Routes dataset.
Supports in-memory, BerkeleyDB, and FoundationDB storage backends.

---

## Setup Instructions

### Prerequisites
- Java 8 (Zulu preferred)
- Maven 3+
- IntelliJ IDEA (or any IDE with Maven support)

### Clone and Build
```bash
git clone https://github.com/danieljsong/SampleGraphApp.git
cd SampleGraphApp
mvn clean install
```

---

## Running the Application

### In-Memory Backend (Default)
No external setup needed. Just run the `InMemoryTest` class.
```java
JanusGraphFactory.build()
    .set("storage.backend", "inmemory")
    .open();
```

### BerkeleyDB Backend
Make sure the `db/berkeley` directory is writable.
```java
JanusGraphFactory.build()
    .set("storage.backend", "berkeleyje")
    .set("storage.directory", "db/berkeley")
    .open();
```
Add cleanup logic before starting if needed:
```java
Files.walk(Paths.get("db/berkeley"))
    .sorted(Comparator.reverseOrder())
    .map(Path::toFile).forEach(File::delete);
```

### FoundationDB Backend (Experimental)
Make sure FoundationDB 6.2.30 is installed and running.
Also install the custom `janusgraph-foundationdb` backend (version `1.0.0-nugraph-1.9.4-SNAPSHOT`) locally:
```bash
cd /path/to/janusgraph-foundationdb
mvn clean install
```

Update `pom.xml`:
```xml
<dependency>
  <groupId>org.janusgraph</groupId>
  <artifactId>janusgraph-foundationdb</artifactId>
  <version>1.0.0-nugraph-1.9.4-SNAPSHOT</version>
</dependency>
```

Run with VM option set:
```
-Djava.library.path=/usr/local/lib
```
Make sure `libfdb_c.dylib` is present at that location.

---

## Dataset

Air Routes Dataset:
- Airports: https://github.com/krlawrence/graph/blob/master/sample-data/air-routes-latest-nodes.csv
- Routes: https://github.com/krlawrence/graph/blob/master/sample-data/air-routes-latest-edges.csv

---

## Output Metrics
- In-memory: ~6 seconds for 3,504 vertices and 50,637 edges
- BerkeleyDB: ~16 seconds, ~1.8MB disk usage

---

## Author
Daniel Song

---

## License


