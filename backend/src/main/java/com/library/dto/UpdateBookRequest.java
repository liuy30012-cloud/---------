package com.library.dto;

import com.library.model.Book;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateBookRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200字符")
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 100, message = "作者长度不能超过100字符")
    private String author;

    @NotBlank(message = "ISBN不能为空")
    @Size(max = 50, message = "ISBN长度不能超过50字符")
    private String isbn;

    @NotBlank(message = "位置不能为空")
    @Size(max = 100, message = "位置长度不能超过100字符")
    private String location;

    @Size(max = 500, message = "封面URL长度不能超过500字符")
    private String coverUrl;

    @Size(max = 50, message = "状态长度不能超过50字符")
    private String status;

    @Size(max = 20, message = "出版年份长度不能超过20字符")
    private String year;

    private String description;

    @Size(max = 10, message = "语言代码长度不能超过10字符")
    private String languageCode;

    @Size(max = 50, message = "可用性长度不能超过50字符")
    private String availability;

    @Size(max = 100, message = "分类长度不能超过100字符")
    private String category;

    private Book.CirculationPolicy circulationPolicy;

    @Min(value = 1, message = "总副本数必须至少为1")
    private Integer totalCopies;
}
