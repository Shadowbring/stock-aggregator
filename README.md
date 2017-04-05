# stock-aggregator
Vert.x-based stock aggregator that receives orders via UDP multicast channel, aggregates them and emits aggregated results via UDP multicast channel

## Building and Running
Make sure that Java 8 is installed.
Project uses Maven as a build tool so make sure that it's installed too. Also, JAVA_HOME and M2_HOME environment variables must be set.
Running solution is as easy as running the following shell command in the root directory of the solution:

    mvn spring-boot:run
    
Or you can build an executable JAR with command

    mvn package
    
and run it as follows:

    java -jar target/stock-aggregator-1.0.0-SNAPSHOT.jar
