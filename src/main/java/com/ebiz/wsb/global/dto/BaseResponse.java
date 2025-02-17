package com.ebiz.wsb.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import net.minidev.json.annotate.JsonIgnore;

@Getter
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse {
    protected String message;
    private Object data;
}
