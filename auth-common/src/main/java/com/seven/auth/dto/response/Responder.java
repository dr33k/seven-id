package com.seven.auth.dto.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.LocalDateTime;

public final class Responder {
    public static ResponseEntity <Res> ok(Object records) {
        return ResponseEntity.ok(Res.builder()
                .data(records)
                .isError(false)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build());
    }
    public static ResponseEntity <Res> ok(Object records, String token) {
        return ResponseEntity.ok(Res.builder()
                .token(token)
                .data(records)
                .isError(false)
                .status(HttpStatus.OK)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public static ResponseEntity <Res> badRequest(String message) {
        return ResponseEntity.status(400).body(Res.builder()
                .message(message)
                .isError(true)
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(LocalDateTime.now())
                .build());
    }
    public static ResponseEntity <Res> badRequest(Object data) {
        return ResponseEntity.status(400).body(Res.builder()
                .data(data)
                .isError(true)
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public static ResponseEntity <Res> notFound(String message) {
        return ResponseEntity.notFound().build();
    }
    public static ResponseEntity <Res> noContent() {return ResponseEntity.noContent().build();}
    public static ResponseEntity <Res> forbidden(String message) {
        return ResponseEntity.status(403).body(Res.builder()
                .message(message)
                .isError(true)
                .status(HttpStatus.FORBIDDEN)
                .timestamp(LocalDateTime.now())
                .build());
    }
    public static ResponseEntity <Res> unauthorized(String message) {
        return ResponseEntity.status(401).body(Res.builder()
                .message(message)
                .isError(true)
                .status(HttpStatus.UNAUTHORIZED)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public static ResponseEntity <Res> conflict(String message) {
        return ResponseEntity.status(409).body(Res.builder()
                .message(message)
                .isError(true)
                .status(HttpStatus.CONFLICT)
                .timestamp(LocalDateTime.now())
                .build());
    }
    public static ResponseEntity <Res> internalServerError(String message) {
        return ResponseEntity.internalServerError().body(Res.builder()
                .message(message)
                .isError(true)
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public static ResponseEntity<Res> created(Object records, String token, String location) {
        return ResponseEntity.status(201).location(URI.create(location)).body(
                Res.builder()
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
