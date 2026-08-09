package com.sbs.board.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {
    @NotBlank
    @Size(max = 1000)
    private String content;     // 글
    private Long parentId;      // 부모 id
}
