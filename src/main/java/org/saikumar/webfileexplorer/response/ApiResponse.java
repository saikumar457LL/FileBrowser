package org.saikumar.webfileexplorer.response;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiResponse<T> {
    private String message;
    private int statusCode;
    public boolean success;
    private T data;
    private LocalDateTime timestamp;
}
