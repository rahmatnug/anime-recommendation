package com.example.anime_recommendation.response;

import com.example.anime_recommendation.model.Anime;
import java.util.List;

public class AnimeResponse {
    private List<Anime> data; // Daftar anime yang diterima dari API

    // Getter untuk data
    public List<Anime> getData() {
        return data;
    }

    // Setter untuk data
    public void setData(List<Anime> data) {
        this.data = data;
    }
}
