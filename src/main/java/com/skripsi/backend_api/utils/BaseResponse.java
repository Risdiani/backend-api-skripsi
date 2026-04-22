package com.skripsi.backend_api.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> implements Serializable {
    private static final long serialVersionUID = 6788150879742170453L;

    private int status;
    private boolean success;
    private String message;

    @JsonIgnoreProperties({"pageable", "sort"})
    private T data;

    public static BaseResponse<String> info(String message) {
        return BaseResponse.<String>builder()
                .status(200)
                .success(true)
                .message(message)
                .build();
    }

    public static <T> BaseResponse<Object> ok(T data) {
        return BaseResponse.<Object>builder()
                .status(200)
                .data(data)
                .success(true)
                .build();
    }

    public static <T> BaseResponse<Object> ok(List<Object> data) {
        return BaseResponse.<Object>builder()
                .status(200)
                .data(data)
                .success(true)
                .build();
    }

    public static <T> BaseResponse<Object> ok(String message, T data) {
        return BaseResponse.<Object>builder()
                .status(200)
                .data(data)
                .message(message)
                .success(true)
                .build();
    }

    public static BaseResponse<String> error(String message) {
        return BaseResponse.<String>builder()
                .status(500)
                .success(false)
                .message(message)
                .build();
    }

    public static <T> BaseResponse<Object> error(String message, T data) {
        return BaseResponse.<Object>builder()
                .status(500)
                .data(data)
                .success(false)
                .message(message)
                .build();
    }

    public static BaseResponse<String> notFound(String message) {
        return BaseResponse.<String>builder()
                .status(404)
                .success(false)
                .message(message)
                .build();
    }

    public static BaseResponse<String> tidakKetmu(String message) {
        return BaseResponse.<String>builder()
                .status(200)
                .success(false)
                .message(message)
                .build();
    }

    public static BaseResponse<String> unauthorized(String message) {
        return BaseResponse.<String>builder()
                .status(401)
                .success(false)
                .message(message)
                .build();
    }

    public static <T> BaseResponse<Object> unauthorized(String message, T data) {
        return BaseResponse.<Object>builder()
                .status(401)
                .data(data)
                .message(message)
                .success(false)
                .build();
    }

    public static <T> BaseResponse<Object> create(int status, boolean success, String message, T data) {
        return BaseResponse.<Object>builder()
                .status(status)
                .success(success)
                .message(message)
                .data(data)
                .build();
    }
}