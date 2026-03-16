package com.seven.auth.dto.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

public final class Responder {
    public static ResponseEntity <Response> ok(Object records) {
        return ResponseEntity.ok(Response.builder()
                .data(records)
                .isError(false)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build());
    }
    public static ResponseEntity <Response> ok(Object records, String token) {
        return ResponseEntity.ok(Response.builder()
                .token(token)
                .data(records)
                .isError(false)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public static ResponseEntity <Response> badRequest(String message) {
        return ResponseEntity.status(400).body(Response.builder()
                .message(message)
                .isError(true)
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(LocalDateTime.now())
                .build());
    }
    public static ResponseEntity <Response> badRequest(Object data) {
        return ResponseEntity.status(400).body(Response.builder()
                .data(data)
                .isError(true)
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public static ResponseEntity <Response> notFound(String message) {
        return ResponseEntity.notFound().build();
    }
    public static ResponseEntity <Response> noContent() {return ResponseEntity.noContent().build();}
    public static ResponseEntity <Response> forbidden(String message) {
        return ResponseEntity.status(403).body(Response.builder()
                .message(message)
                .isError(true)
                .status(HttpStatus.FORBIDDEN)
                .timestamp(LocalDateTime.now())
                .build());
    }
    public static ResponseEntity <Response> unauthorized(String message) {
        return ResponseEntity.status(401).body(Response.builder()
                .message(message)
                .isError(true)
                .status(HttpStatus.UNAUTHORIZED)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public static ResponseEntity <Response> conflict(String message) {
        return ResponseEntity.status(409).body(Response.builder()
                .message(message)
                .isError(true)
                .status(HttpStatus.CONFLICT)
                .timestamp(LocalDateTime.now())
                .build());
    }
    public static ResponseEntity <Response> internalServerError(String message) {
        return ResponseEntity.internalServerError().body(Response.builder()
                .message(message)
                .isError(true)
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public static ResponseEntity<Response> created(Object records, String token, String location) {
        URI uri = ServletUriComponentsBuilder.fromUri(URI.create(location)).buildAndExpand().toUri();
        return ResponseEntity.status(201).location(uri).body(
                Response.builder()
                .data(records)
                .token(token)
                .isError(false)
                .status(HttpStatus.CREATED)
                .timestamp(LocalDateTime.now())
                .build());
    }
//    public static EntityModel<ResponseEntity<Response>> okHal(Object userData){
//        EntityModel<ResponseEntity<Response>> entityModel = EntityModel.of(
//                ResponseEntity.ok(
//                        Response.builder()
//                                .data(userData)
//                                .isError(false)
//                                .status(HttpStatus.OK)
//                                .timestamp(LocalDateTime.now())
//                                .build()
//                )
//        );
//        return entityModel;
//    }
//    public static EntityModel<ResponseEntity<Response>> createdHal(Object records, String location){
//        URI uri = ServletUriComponentsBuilder.fromCurrentRequestUri().path(location).buildAndExpand().toUri();
//
//        EntityModel<ResponseEntity<Response>> entityModel = EntityModel.of(
//                ResponseEntity.status(201).location(uri).body(
//                Response.builder()
//                        .data(records)
//                        .isError(false)
//                        .status(HttpStatus.CREATED)
//                        .timestamp(LocalDateTime.now())
//                        .build()
//                )
//        );
//        return entityModel;
//    }
//    public static void createAndIncludeLinks(Map <String,Object> refToInvocationObjectMap ,EntityModel<ResponseEntity<Response>> entityModel) {
//        //The key of an entryset in refToInvocationObjectMap is the "ref" property of the link
//        //The value of an entrySet in refToInvocationObjectMap is the invocation object of type Object
//
//        Set <Link> links = refToInvocationObjectMap.entrySet().stream().map(
//                entry -> linkTo(entry.getValue()).withRel(entry.getKey())
//        ).collect(Collectors.toSet());
//        entityModel.add(links);
//    }
}
