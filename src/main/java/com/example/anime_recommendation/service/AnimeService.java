package com.example.anime_recommendation.service;

import com.example.anime_recommendation.model.Anime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("unchecked")
public class AnimeService {
    private static final Logger logger = LoggerFactory.getLogger(AnimeService.class);
    
    private final String ANIME_RANKING_URL = "https://api.myanimelist.net/v2/anime/ranking";
    private final RestTemplate restTemplate;
    
    @Value("${myanimelist.client.id:d5a2c00a0dc443f6a44c30d5cc5ddd48}")
    private String clientId;
    
    public AnimeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    private HttpHeaders createMalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-MAL-CLIENT-ID", clientId);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "AnimeRecommendationApp/1.0");
        return headers;
    }
    
    private String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            logger.error("Error encoding value: {}", value, e);
            return value;
        }
    }

    public List<Anime> getAnimeList() {
        String url = ANIME_RANKING_URL + "?ranking_type=all&limit=10&fields=id,title,main_picture,synopsis,genres";
        
        try {
            logger.info("Fetching anime list from MAL API");
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createMalHeaders()),
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> animeData = (List<Map<String, Object>>) response.getBody().get("data");
                return mapToAnimeList(animeData);
            }
        } catch (HttpClientErrorException e) {
            logger.error("MAL API Error - Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error fetching anime list", e);
        }
        return Collections.emptyList();
    }
    
    public List<Anime> searchAnime(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String url = "https://api.myanimelist.net/v2/anime?q=" + encodeValue(query) + 
                    "&limit=10&fields=id,title,main_picture,synopsis,genres";
        
        try {
            logger.info("Searching anime with query: {}", query);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createMalHeaders()),
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> animeData = (List<Map<String, Object>>) response.getBody().get("data");
                return mapToAnimeList(animeData);
            }
        } catch (HttpClientErrorException e) {
            logger.error("MAL API Search Error - Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error searching anime", e);
        }
        return Collections.emptyList();
    }
    
    public Anime getAnimeDetails(int id) {
        String url = "https://api.myanimelist.net/v2/anime/" + id + 
                    "?fields=id,title,main_picture,synopsis,genres,mean,rank,popularity,num_episodes,rating";
        
        try {
            logger.info("Fetching anime details for ID: {}", id);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createMalHeaders()),
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return mapToAnime(response.getBody());
            }
        } catch (HttpClientErrorException e) {
            logger.error("MAL API Details Error - Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Unexpected error fetching anime details", e);
        }
        return null;
    }
    
    public Map<String, Anime> getAnimeRecommendations(String search, String genre) {
        List<Anime> animeList;
        
        if (search != null && !search.isEmpty()) {
            animeList = searchAnime(search);
        } else if (genre != null && !genre.isEmpty()) {
            animeList = getAnimeByGenre(genre);
        } else {
            animeList = getAnimeList();
        }
        
        return animeList.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                anime -> String.valueOf(anime.getId()),
                anime -> anime,
                (existing, replacement) -> existing
            ));
    }
    
    public List<Anime> getAnimeByGenre(String genre) {
    String url = "https://api.myanimelist.net/v2/anime/ranking?ranking_type=all&limit=10&fields=id,title,main_picture,synopsis,genres&genre=" + encodeValue(genre);
    try {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(createMalHeaders()),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        return mapToAnimeList((List<Map<String, Object>>) response.getBody().get("data"));
    } catch (Exception e) {
        logger.error("Error fetching anime by genre", e);
        return Collections.emptyList();
    }
    }
    
    private List<Anime> mapToAnimeList(List<Map<String, Object>> animeData) {
        if (animeData == null) return Collections.emptyList();
        
        return animeData.stream()
            .map(data -> {
                Map<String, Object> node = (Map<String, Object>) data.get("node");
                return mapToAnime(node);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private Anime mapToAnime(Map<String, Object> animeData) {
        try {
            int id = animeData.get("id") != null ? ((Number) animeData.get("id")).intValue() : 0;
            String title = (String) animeData.get("title");
            
            // Handle main picture
            Map<String, String> mainPicture = animeData.get("main_picture") != null ? 
                (Map<String, String>) animeData.get("main_picture") : null;
            String mediumImage = mainPicture != null ? mainPicture.get("medium") : "";
            String largeImage = mainPicture != null ? mainPicture.get("large") : "";
            
            // Handle synopsis
            String synopsis = (String) animeData.get("synopsis");
            if (synopsis == null) synopsis = "No synopsis available";
            
            // Handle genres
            List<Map<String, String>> genres = animeData.get("genres") != null ? 
                (List<Map<String, String>>) animeData.get("genres") : null;
            String genreString = genres != null ? 
                genres.stream()
                    .map(g -> g.get("name"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", ")) : "";
            
            // Create Anime object
            Anime anime = new Anime(id, title, mediumImage, largeImage, synopsis, genreString);
            
            // Add additional details if available
            if (animeData.containsKey("mean") && animeData.get("mean") != null) {
                anime.setScore(((Number) animeData.get("mean")).doubleValue());
            }
            if (animeData.containsKey("num_episodes") && animeData.get("num_episodes") != null) {
                anime.setEpisodes(((Number) animeData.get("num_episodes")).intValue());
            }
            if (animeData.containsKey("rating")) {
                anime.setRating((String) animeData.get("rating"));
            }
            
            return anime;
        } catch (Exception e) {
            logger.error("Error mapping anime data", e);
            return null;
        }
    }
}