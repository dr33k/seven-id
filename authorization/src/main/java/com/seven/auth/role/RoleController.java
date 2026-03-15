package com.seven.auth.role;

import com.seven.auth.config.authorization.Authorize;
import com.seven.auth.permission.PEnum;
import com.seven.auth.util.Constants;
import com.seven.auth.util.Pagination;
import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.dto.response.Response;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.seven.auth.dto.response.Responder.noContent;
import static com.seven.auth.dto.response.Responder.ok;

@RestController
@RequestMapping(Constants.PATH_PREFIX+"/roles")
public class RoleController {
    private final RoleService roleService;
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("{roleId}")
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.read_role, PEnum.super_read})
    public ResponseEntity <Response> getResource(@Valid @NotNull @PathVariable(value = "roleId") UUID id)  throws AuthorizationException {
        RoleDTO.Record roleRecord = roleService.get(id);
        return ok(roleRecord);
    }

    @GetMapping
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.read_role, PEnum.super_read})
    public ResponseEntity <Response> getResources(@ParameterObject Pagination pagination, @ParameterObject RoleDTO.Filter roleFilter)  throws AuthorizationException {
        Page<RoleDTO.Record> roleRecords = roleService.getAll(pagination, roleFilter);
        return ok(roleRecords);
    }

    @PostMapping
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.create_role, PEnum.super_create})
    public ResponseEntity <Response> createResource(@Valid @RequestBody RoleDTO.Create request) throws AuthorizationException {
        RoleDTO.Record roleRecord = roleService.create(request);
        return ok(roleRecord);
    }

//    @PutMapping("/{roleId}")
//    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
//    public ResponseEntity <Response> updateResource(@Valid @NotNull @PathVariable(value = "roleId") UUID id, @Valid @RequestBody RoleDTO.Update request) {
//       RoleDTO.Record roleRecord = roleService.update(id, request);
//        return ok(roleRecord);
//    }
    
    @DeleteMapping("{roleId}")
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.delete_role, PEnum.super_delete})
    public ResponseEntity <Response> deleteResource(@Valid @NotNull @PathVariable(value = "roleId") UUID id) throws AuthorizationException {
        roleService.delete(id);
        return noContent();
    }
}