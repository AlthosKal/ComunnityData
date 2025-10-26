package com.senasoft.comunidataapi.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.web.multipart.MultipartFile;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatMultipartDTO extends ChatDTO {

    @NotNull(message = "You need one file")
    private MultipartFile file;
}
