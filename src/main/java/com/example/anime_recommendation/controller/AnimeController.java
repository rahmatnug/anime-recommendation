package com.example.anime_recommendation.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.anime_recommendation.model.Anime;
import com.example.anime_recommendation.service.AnimeService;

@Controller
public class AnimeController {

    private final AnimeService animeService;

    public AnimeController(AnimeService animeService) {
        this.animeService = animeService;
    }

    @GetMapping("/")
public String index(Model model,
                    @RequestParam(required = false) String search,
                    @RequestParam(required = false) String genre) {
    List<Anime> animeList;
    if (search != null && !search.isEmpty()) {
        animeList = animeService.searchAnime(search);
    } else if (genre != null && !genre.isEmpty()) {
        animeList = animeService.getAnimeByGenre(genre);
    } else {
        animeList = animeService.getAnimeList();
    }
    model.addAttribute("animeList", animeList);
    return "index";
}

    @GetMapping("/genre/{genreId}")
    public String animeByGenre(@PathVariable String genreId, Model model) {
        Object animeList = animeService.getAnimeByGenre(genreId);
        model.addAttribute("animeList", animeList);
        model.addAttribute("genreId", genreId);
        return "genre";
    }

    @GetMapping("/search")
    public String search(@RequestParam String query, Model model) {
    List<Anime> results = animeService.searchAnime(query);
    model.addAttribute("animeList", results);
    model.addAttribute("query", query);
    return "search";
    }

    @GetMapping("/anime/{id}")
    public String animeDetail(@PathVariable int id, Model model) {
    if (id <= 0) {
        model.addAttribute("errorMessage", "ID anime tidak valid.");
        return "error";
    }
    
    Anime anime = animeService.getAnimeDetails(id);
    if (anime == null) {
        model.addAttribute("errorMessage", "Anime tidak ditemukan.");
        return "error";
    }
    model.addAttribute("anime", anime);
    return "anime-detail";
    }
}



    
