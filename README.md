# SEVEN ID
Schema-per-tenant modular-monolithic authentication solution. 

Plug-and-play adapters for microservices. 

Supports JWT, OAuth2 and Kerberos. 

OAuth2 implementation currently in progress.

## Requirements

Java version: 21

Maven version: 3.9.6

Postgres version: 14+

Required DB name: auth_db

Env:

    PG_USER, PG_PASSWORD, PG_PORT, JWT_SIGNING_KEY


## Run JWT application

In project root folder

    $ mvn clean install
    $ mvn -pl jwt-auth spring-boot:run

OR for truly native pseudo-random number generation by BCryptPasswordEncoder on Linux machines
    
    $ mvn clean install
    $ java -Djava.security.egd=file:///dev/random -jar jwt-auth/target/jwt-auth-1.0-SNAPSHOT-exec.jar

Visit http://localhost:8080/swagger