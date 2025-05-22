package com.example.anime_recommendation.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AnimeController.class);

    @GetMapping("/jikan/anime/{id}")
    public String animeDetailJikan(@PathVariable int id, Model model) {
        logger.info("Received request for anime detail with ID: {}", id);
        if (id <= 0) {
            model.addAttribute("errorMessage", "ID anime tidak valid.");
            return "error";
        }

        Anime anime = animeService.getAnimeDetailsJikan(id);
        logger.info("Anime object returned from service: {}", anime);
        if (anime == null) {
            model.addAttribute("errorMessage", "Anime tidak ditemukan.");
            return "error";
        }
        model.addAttribute("anime", anime);
        return "anime-detail";
    }

    @GetMapping("/")
    public String index(Model model,
                    @RequestParam(required = false) String search,
                    @RequestParam(required = false) String genre) {
    List<Anime> animeList;
    if (search != null && !search.isEmpty()) {
        animeList = animeService.searchAnime(search);
    } else if (genre != null && !genre.isEmpty()) {
        try {
            int genreId = Integer.parseInt(genre);
            animeList = animeService.getAnimeByGenre(genreId);
        } catch (NumberFormatException e) {
            animeList = animeService.getAnimeList();
        }
    } else {
        animeList = animeService.getAnimeList();
    }
    model.addAttribute("animeList", animeList);

    // Add genres list to model for genre dropdown
    model.addAttribute("genres", animeService.getAnimeGenres());

    return "index";
    }

    @GetMapping("/genres")
    public String getGenres(Model model) {
        List<Map<String, Object>> genres = animeService.getAnimeGenres();
        model.addAttribute("genres", genres);
        return "genres"; // Create a new view for genres
    }
    
     @GetMapping("/searchByGenre")
    public ResponseEntity<List<Anime>> searchByGenre(@RequestParam String genreId, Model model) {
        try {
            int genreIdInt = Integer.parseInt(genreId); // Convert String to int
            List<Anime> animeList = animeService.getAnimeByGenre(genreIdInt);
            if (animeList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(animeList); // Return 404 if no anime found
            }
            model.addAttribute("animeList", animeList);
            return ResponseEntity.ok(animeList); // Return 200 OK with anime list
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Return 400 Bad Request for invalid genreId
        }
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



    
