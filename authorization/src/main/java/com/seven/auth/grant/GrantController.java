package com.seven.auth.grant;

import com.seven.auth.config.authorization.Authorize;
import com.seven.auth.permission.PEnum;
import com.seven.auth.util.Constants;
import com.seven.auth.util.Pagination;
import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.dto.response.Res;
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
@RequestMapping(Constants.PATH_PREFIX+"/grants")
public class GrantController {
    private final GrantService grantService;
    public GrantController(GrantService grantService) {
        this.grantService = grantService;
    }

    @GetMapping("{grantId}")
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.read_grant, PEnum.super_read, PEnum.elev_read_authorization})
    public ResponseEntity <Res> getResource(@Valid @NotNull @PathVariable(value = "grantId") UUID id)  throws AuthorizationException {
        GrantDTO.Record grantRecord = grantService.get(id);
        return ok(grantRecord);
    }

    @GetMapping
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.read_grant, PEnum.super_read, PEnum.elev_read_authorization})
    public ResponseEntity <Res> getResources(@ParameterObject Pagination pagination, @ParameterObject GrantDTO.Filter grantFilter)  throws AuthorizationException {
        Page<GrantDTO.Record> grantRecords = grantService.getAll(pagination, grantFilter);
        return ok(grantRecords);
    }

    @PostMapping
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.create_grant, PEnum.super_create, PEnum.elev_create_authorization})
    public ResponseEntity <Res> createResource(@Valid @RequestBody GrantDTO.Create request) throws AuthorizationException {
        GrantDTO.Record grantRecord = grantService.create(request);
        return ok(grantRecord);
    }

//    @PutMapping("/{grantId}")
//    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
//    public ResponseEntity <Response> updateResource(@Valid @NotNull @PathVariable(value = "grantId") UUID id, @Valid @RequestBody GrantDTO.Update request) {
//       GrantDTO.Record grantRecord = grantService.update(id, request);
//        return ok(grantRecord);
//    }
    
    @DeleteMapping("{grantId}")
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.delete_grant, PEnum.super_delete, PEnum.elev_delete_authorization})
    public ResponseEntity <Res> deleteResource(@Valid @NotNull @PathVariable(value = "grantId") UUID id) throws AuthorizationException {
        grantService.delete(id);
        return noContent();
    }
}