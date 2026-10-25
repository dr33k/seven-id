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

Java version: 25

Maven version: 3.9.6 +

Postgres version: 14 +

Required DB name: auth_db

## Run application

### DOCKER IMAGE

Build project

    sudo mvn clean install
    
Build image
    
    sudo mvn clean package com.google.cloud.tools:jib-maven-plugin:dockerBuild -pl oauth2

Run image
    
    sudo docker-compose up -d
    

## Visit http://localhost:8082/swagger


## Todo
* Pay $99 for an Apple Developer account and .p8 file
* Integrate WSO2 for Kerberos
* Implement MFA for self-signed JWT authentication
* Implement Refresh tokens

