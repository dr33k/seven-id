package com.seven.auth.assignment;

import com.seven.auth.config.authorization.Authorize;
import com.seven.auth.permission.PEnum;
import com.seven.auth.util.Constants;
import com.seven.auth.util.Pagination;
import com.seven.auth.exception.AuthorizationException;
import com.seven.auth.response.Res;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.seven.auth.response.Responder.noContent;
import static com.seven.auth.response.Responder.ok;

@RestController
@RequestMapping(Constants.PATH_PREFIX+"/assignments")
public class AssignmentController {
    private final AssignmentService assignmentService;
    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.read_assignment, PEnum.super_read, PEnum.elev_read_authorization})
    public ResponseEntity <Res> getResources(@ParameterObject Pagination pagination, @ParameterObject AssignmentDTO.Filter assignmentFilter)  throws AuthorizationException {
        Page<AssignmentDTO.Record> assignmentRecords = assignmentService.getAll(pagination, assignmentFilter);
        return ok(assignmentRecords);
    }

    @PostMapping
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.create_assignment, PEnum.super_create, PEnum.elev_create_authorization})
    public ResponseEntity <Res> createResource(@Valid @RequestBody AssignmentDTO.Create request) throws AuthorizationException {
        AssignmentDTO.Record assignmentRecord = assignmentService.create(request);
        return ok(assignmentRecord);
    }

    @DeleteMapping("{assignmentId}")
    @Parameter(name = Constants.TENANT_ID_KEY, in = ParameterIn.HEADER)
    @Authorize(permissions = {PEnum.delete_assignment, PEnum.super_delete, PEnum.elev_delete_authorization})
    public ResponseEntity <Res> deleteResource(@Valid @NotNull @PathVariable(value = "assignmentId") UUID id) throws AuthorizationException {
        assignmentService.delete(id);
        return noContent();
    }
}