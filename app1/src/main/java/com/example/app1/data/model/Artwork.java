package com.example.app1.data.model;

public class Artwork {
    private String title;
    private String artist;
    private String imageUrl;

    public Artwork(String title, String artist, String imageUrl) {
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
