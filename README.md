# SEVEN ID
Schema-per-tenant modular-monolithic authentication solution. 

Supports self-signed JWT and OAuth2 OIDC Authentication.

Supports Google and Apple OIDC

Uses a public facing REST API

Each tenant corresponds to an application/solution in a microservice ecosystem that requires user management

Provision a new tenant using the API (/api/applications) and the credentials for an elevated user will be 

created in the user.home directory of the OS

Plug-and-play adapters for microservices. 


## Requirements

Java version: 21

Maven version: 3.9.6

Postgres version: 14+

Required DB name: auth_db

## Run application

### DOCKER IMAGE
Build image
    
    $ sudo mvn clean package com.google.cloud.tools:jib-maven-plugin:dockerBuild -pl oauth2

Run image
    
    $ sudo docker-compose up

### OR MANUALLY

Using Env (defaults are already configured):

    PG_USER, PG_PASSWORD, PG_PORT, JWT_SECRET_KEY
    OIDC_GOOGLE_CLIENT_ID, OIDC_GOOGLE_CLIENT_SECRET,
    OIDC_APPLE_CLIENT_ID

Make sure to have postgres 14+ server running.

Then in project root folder:

    $ sudo mvn clean install
    $ sudo mvn -pl oauth2 spring-boot:run

OR for truly native pseudo-random number generation by BCryptPasswordEncoder on Linux machines
    
    $ sudo mvn clean install
    $ sudo java -Djava.security.egd=file:///dev/random -jar jwt-auth/target/oauth2-1.0-SNAPSHOT-exec.jar



## Visit http://localhost:8082/swagger


## Todo
* Pay $99 for an Apple Developer account and .p8 file
* Integrate WSO2 for Kerberos
* Implement MFA for self-signed JWT authentication
* Implement Refresh tokens

