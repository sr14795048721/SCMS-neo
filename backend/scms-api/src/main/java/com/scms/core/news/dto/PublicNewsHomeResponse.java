package com.scms.core.news.dto;

import java.util.List;

public record PublicNewsHomeResponse(
        List<PublicNewsCardResponse> carousel,
        List<PublicNewsCardResponse> textList
) {
}
