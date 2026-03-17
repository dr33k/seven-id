package com.seven.auth.permission;

import com.seven.auth.config.authorization.Authorize;
import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.util.Constants;
import com.seven.auth.util.Pagination;
import com.seven.auth.dto.response.Response;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.seven.auth.dto.response.Responder.noContent;
import static com.seven.auth.dto.response.Responder.ok;

@RestController
@RequestMapping(Constants.PATH_PREFIX+"/permissions")
public class PermissionController {
    private final PermissionService permissionService;
    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("{permissionId}")
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.read_permission, PEnum.super_read, PEnum.elev_read_authorization})
    public ResponseEntity <Response> getResource(@Valid @NotNull @PathVariable(value = "permissionId") UUID id)  throws AuthorizationException {
        PermissionDTO.Record permissionRecord = permissionService.get(id);
        return ok(permissionRecord);
    }

    @GetMapping
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.read_permission, PEnum.super_read, PEnum.elev_read_authorization})
    public ResponseEntity <Response> getResources(@ParameterObject Pagination pagination,  @ParameterObject PermissionDTO.Filter permissionFilter)  throws AuthorizationException {
        Page<PermissionDTO.Record> permissionRecords = permissionService.getAll(pagination, permissionFilter);
        return ok(permissionRecords);
    }

    @PostMapping
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.create_permission, PEnum.super_create, PEnum.elev_create_authorization})
    public ResponseEntity <Response> createResource(@Valid @RequestBody PermissionDTO.Create request) throws AuthorizationException {
        PermissionDTO.Record permissionRecord = permissionService.create(request);
        return ok(permissionRecord);
    }

//    @PutMapping("/{permissionId}")
//    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
//    public ResponseEntity <Response> updateResource(@Valid @NotNull @PathVariable(value = "permissionId") UUID id, @Valid @RequestBody PermissionDTO.Update request) {
//       PermissionDTO.Record permissionRecord = permissionService.update(id, request);
//        return ok(permissionRecord);
//    }
    
    @DeleteMapping("{permissionId}")
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.delete_permission, PEnum.super_delete, PEnum.elev_delete_authorization})
    public ResponseEntity <Response> deleteResource(@Valid @NotNull @PathVariable(value = "permissionId") UUID id) throws AuthorizationException {
        permissionService.delete(id);
        return noContent();
    }
}