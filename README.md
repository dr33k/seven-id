# SEVEN ID
Schema-per-tenant modular-monolithic authentication solution. 

Supports self-signed JWT and OAuth2 OIDC Authentication.

Supports Google and Apple OIDC

Each tenant corresponds to an application/solution in a microservice ecosystem that requires user management

Plug-and-play adapters for microservices. 


## Requirements

Java version: 21

Maven version: 3.9.6

Postgres version: 14+

Required DB name: auth_db

Env:

    PG_USER, PG_PASSWORD, PG_PORT, JWT_SECRET_KEY
    OIDC_GOOGLE_CLIENT_ID, OIDC_GOOGLE_CLIENT_SECRET,
    OIDC_APPLE_CLIENT_ID


## Run application

In project root folder

    $ mvn clean install
    $ mvn -pl oauth2 spring-boot:run

OR for truly native pseudo-random number generation by BCryptPasswordEncoder on Linux machines
    
    $ mvn clean install
    $ java -Djava.security.egd=file:///dev/random -jar jwt-auth/target/oauth2-1.0-SNAPSHOT-exec.jar


Visit http://localhost:8080/swagger


## Todo
* Pay $99 for an Apple Developer account and .p8 file
* Integrate WSO2 for Kerberos
* Implement MFA for self-signed JWT authentication
* Implement Refresh tokens

