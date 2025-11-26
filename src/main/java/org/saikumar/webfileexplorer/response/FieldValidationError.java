package org.saikumar.webfileexplorer.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FieldValidationError {
    private String fileName;
    private String message;
    private String rejectedValue;
}
