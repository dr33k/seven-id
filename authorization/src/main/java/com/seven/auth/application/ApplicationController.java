package com.seven.auth.application;

import com.seven.auth.config.authorization.Authorize;
import com.seven.auth.permission.PEnum;
import com.seven.auth.util.Constants;
import com.seven.auth.util.Pagination;
import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.response.Res;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.seven.auth.response.Responder.ok;
import static com.seven.auth.response.Responder.noContent;

@RestController
@RequestMapping(Constants.PATH_PREFIX+"/applications")
public class ApplicationController {
    private final ApplicationService applicationService;
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/{applicationId}")
    @Authorize(permissions = PEnum.super_read)
    public ResponseEntity <Res> getResource(@Valid @NotNull @PathVariable(value = "applicationId") UUID id)  throws AuthorizationException {
        ApplicationDTO.Record record = applicationService.get(id);
        return ok(record);
    }

    @GetMapping
    @Authorize(permissions = PEnum.super_read)
    public ResponseEntity <Res> getResources(@ParameterObject Pagination pagination, @ParameterObject ApplicationDTO.Filter applicationFilter)  throws AuthorizationException {
        Page<ApplicationDTO.Record> applicationDTOs = applicationService.getAll(pagination, applicationFilter);
        return ok(applicationDTOs);
    }

    @PostMapping
    @Authorize(permissions = PEnum.super_create)
    public ResponseEntity <Res> createResource(@Valid @RequestBody ApplicationDTO.Create create) throws AuthorizationException {
        ApplicationDTO.Record record = applicationService.create(create);
        return ok(record);
    }
    
    @DeleteMapping("{applicationId}")
    @Authorize(permissions = PEnum.super_delete)
    public ResponseEntity <Res> deleteResource(@Valid @NotNull @PathVariable(value = "applicationId") UUID id) throws AuthorizationException {
        applicationService.delete(id);
        return noContent();
    }
}