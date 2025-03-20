package com.example.socialapp;

public class comment {
    private String postId;
    private String author;
    private String content;
    private String authorPhotoUrl;

    public comment(String postId, String author, String content, String authorPhotoUrl) {
        this.postId = postId;
        this.author = author;
        this.content = content;
        this.authorPhotoUrl = authorPhotoUrl;
    }

    public String getPostId() {
        return postId;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public String getAuthorPhotoUrl() {
        return authorPhotoUrl;
    }
}