package com.seven.auth.domain;

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
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.seven.auth.dto.response.Responder.noContent;
import static com.seven.auth.dto.response.Responder.ok;

@RestController
@RequestMapping(Constants.PATH_PREFIX+"/domains")
public class DomainController {
    private final DomainService domainService;
    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping("{domainId}")
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.read_domain, PEnum.super_read, PEnum.elev_read_authorization})
    public ResponseEntity <Response> getResource(@Valid @NotNull @PathVariable(value = "domainId") UUID id)  throws AuthorizationException {
        DomainDTO.Record domainRecord = domainService.get(id);
        return ok(domainRecord);
    }

    @GetMapping
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.read_domain, PEnum.super_read, PEnum.elev_read_authorization})
    public ResponseEntity <Response> getResources(@ParameterObject Pagination pagination,  @ParameterObject DomainDTO.Filter domainFilter)  throws AuthorizationException {
        Page<DomainDTO.Record> domainRecords = domainService.getAll(pagination, domainFilter);
        return ok(domainRecords);
    }

    @PostMapping
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.create_domain, PEnum.super_create, PEnum.elev_create_authorization})
    public ResponseEntity <Response> createResource(@Valid @RequestBody DomainDTO.Create request) throws AuthorizationException {
        DomainDTO.Record domainRecord = domainService.create(request);
        return ok(domainRecord);
    }

//    @PutMapping("/{domainId}")
//    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
//    public ResponseEntity <Response> updateResource(@Valid @NotNull @PathVariable(value = "domainId") UUID id, @Valid @RequestBody DomainDTO.Update request) {
//        DomainDTO.Record domainRecord = domainService.update(id, request);
//        return ok(domainRecord);
//    }
    
    @DeleteMapping("{domainId}")
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.delete_domain, PEnum.super_delete, PEnum.elev_delete_authorization})
    public ResponseEntity <Response> deleteResource(@Valid @NotNull @PathVariable(value = "domainId") UUID id) throws AuthorizationException {
        domainService.delete(id);
        return noContent();
    }
}