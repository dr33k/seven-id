package com.seven.auth.util;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class Pagination {
    private int limit;
    private int offset;
    private String sortField;
    private String sortOrder;

    public Pagination(){
        this.limit = 10;
        this.offset = 0;
    }
}
