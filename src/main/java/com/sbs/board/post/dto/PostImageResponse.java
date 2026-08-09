package com.sbs.board.post.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostImageResponse {
    private Long id;
    private String url;
    private String originalName;
    private int sortOrder;
}
