# How To Build

```
./mvnw clean
./mvnw
```

# How To Run

```
java -jar target/newrelic-parser-1.0.0.jar src/test/resources/txt/1.txt src/test/resources/txt/2.txt
```

or

```
cat src/test/resources/txt/1.txt | java -jar target/newrelic-parser-1.0.0.jar
```
