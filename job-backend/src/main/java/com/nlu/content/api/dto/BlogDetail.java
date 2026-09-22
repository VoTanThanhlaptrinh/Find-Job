package com.nlu.content.api.dto;

public record BlogDetail(long id, String title, String description, String content, int amountLike, String authorName,
        String createDate) {
}