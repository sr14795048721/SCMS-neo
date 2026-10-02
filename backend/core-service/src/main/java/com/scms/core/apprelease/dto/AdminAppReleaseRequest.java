package com.scms.core.apprelease.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminAppReleaseRequest(
        @NotBlank
        @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$")
        String versionName,
        @NotNull
        @Min(1)
        Integer buildNumber,
        @NotBlank
        @Size(max = 4000)
        String releaseNotes,
        @NotNull
        Boolean forceUpdate,
        @Size(max = 500)
        String androidUrl,
        @Size(max = 500)
        String harmonyUrl
) {
}
