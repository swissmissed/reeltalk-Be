package com.alom.reeltalkbe.review.dto.response.summary;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryDto {

    private Long reviewId;
    private Long userId;


    private String imageUrl;
    private String createAt;
    private String updateAt;
}
