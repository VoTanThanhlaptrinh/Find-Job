package com.nlu.content.api.dto;

public record BlogResponse(long id, String title, String description
    , int amountLike, String authorName, String createDate) {
} 