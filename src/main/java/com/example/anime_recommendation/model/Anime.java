package com.example.anime_recommendation.model;

public class Anime {
    private int jikanId;
    private int malId;
    private String title;
    private String mainPictureMedium;
    private String mainPictureLarge;
    private String synopsis;
    private String genre;
    private Double score;
    private Integer episodes;
    private String rating;

    // Constructor
    public Anime(int jikanId, int malId, String title, String mainPictureMedium, String mainPictureLarge, 
                String synopsis, String genre) {
        this.jikanId = jikanId;
        this.malId = malId;
        this.title = title;
        this.mainPictureMedium = mainPictureMedium;
        this.mainPictureLarge = mainPictureLarge;
        this.synopsis = synopsis;
        this.genre = genre;
    }

    // Getters and Setters
    public int getJikanId() {
        return jikanId;
    }

    public void setJikanId(int jikanId) {
        this.jikanId = jikanId;
    }

    public int getMalId() {
        return malId;
    }

    public void setMalId(int malId) {
        this.malId = malId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMainPictureMedium() {
        return mainPictureMedium;
    }

    public void setMainPictureMedium(String mainPictureMedium) {
        this.mainPictureMedium = mainPictureMedium;
    }

    public String getMainPictureLarge() {
        return mainPictureLarge;
    }

    public void setMainPictureLarge(String mainPictureLarge) {
        this.mainPictureLarge = mainPictureLarge;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getEpisodes() {
        return episodes;
    }

    public void setEpisodes(Integer episodes) {
        this.episodes = episodes;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }
}
