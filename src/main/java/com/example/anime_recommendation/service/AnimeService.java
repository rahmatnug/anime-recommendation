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
    
    private HttpHeaders createJikanHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", "AnimeRecommendationApp/1.0");
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    return headers;
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
                new ParameterizedTypeReference<Map<String, Object>>() {}
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
                new ParameterizedTypeReference<Map<String, Object>>() {}
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
                new ParameterizedTypeReference<Map<String, Object>>() {}
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
    
    public Anime getAnimeDetailsJikan(int id) {
        String url = "https://api.jikan.moe/v4/anime/" + id;
        
        try {
            logger.info("Fetching anime details from Jikan API for ID: {}", id);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createJikanHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                return mapToAnime(data);
            }
        } catch (Exception e) {
            logger.error("Error fetching anime details from Jikan API", e);
        }
        return null;
    }
    
    public Map<String, Anime> getAnimeRecommendations(String search, String genre) {
        List<Anime> animeList;
        
        if (search != null && !search.isEmpty()) {
            animeList = searchAnime(search);
        } else if (genre != null && !genre.isEmpty()) {
            try {
                int genreId = Integer.parseInt(genre);
                animeList = getAnimeByGenre(genreId);
            } catch (NumberFormatException e) {
                logger.error("Invalid genre id: {}", genre, e);
                animeList = getAnimeList();
            }
        } else {
            animeList = getAnimeList();
        }
        
        return animeList.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                anime -> String.valueOf(anime.getJikanId()),
                anime -> anime,
                (existing, replacement) -> existing
            ));
    }
    
    public List<Map<String, Object>> getAnimeGenres() {
        String url = "https://api.jikan.moe/v4/genres/anime";
        List<Map<String, Object>> genreList = new ArrayList<>();
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createJikanHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                genreList = (List<Map<String, Object>>) response.getBody().get("data");
            }
        } catch (Exception e) {
            logger.error("Error fetching anime genres", e);
        }
        return genreList;
    }

    public List<Anime> getAnimeByGenre(int genreId) {
        String url = "https://api.jikan.moe/v4/anime?genres=" + genreId + "&limit=10&fields=id,title,images,synopsis,genres,external_links,mean,num_episodes,rating";
        List<Anime> filteredAnimeList = new ArrayList<>();
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createJikanHeaders()),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.debug("Raw response body from Jikan API for genre {}: {}", genreId, response.getBody());
                List<Map<String, Object>> animeData = (List<Map<String, Object>>) response.getBody().get("data");
                for (Map<String, Object> anime : animeData) {
                    filteredAnimeList.add(mapToAnime(anime));
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching anime by genre", e);
        }
        return filteredAnimeList;
    }
    
    private List<Anime> mapToAnimeList(List<Map<String, Object>> animeData) {
        if (animeData == null) return Collections.emptyList();
        
        return animeData.stream()
            .map(data -> {
                // For Jikan API, data is direct anime object, no "node" wrapper
                if (data.containsKey("node")) {
                    Map<String, Object> node = (Map<String, Object>) data.get("node");
                    return mapToAnime(node);
                } else {
                    return mapToAnime(data);
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    private Anime mapToAnime(Map<String, Object> animeData) {
        try {
            logger.info("Mapping anime data: {}", animeData);
            int jikanId = 0;
            if (animeData.get("id") != null) {
                jikanId = ((Number) animeData.get("id")).intValue();
            }
            if (jikanId == 0 && animeData.get("mal_id") != null) {
                jikanId = ((Number) animeData.get("mal_id")).intValue();
                logger.info("Using mal_id as fallback for jikanId: {}", jikanId);
            }
            if (jikanId == 0) {
                logger.warn("Anime data missing or has invalid 'id' and 'mal_id' fields: {}", animeData);
            }
            String title = (String) animeData.get("title");
            
            // Handle main picture or images (Jikan API v4)
            Map<String, Object> images = animeData.get("images") != null ?
                (Map<String, Object>) animeData.get("images") : null;
            String mediumImage = "";
            String largeImage = "";
            if (images != null) {
                Map<String, String> jpg = images.get("jpg") != null ?
                    (Map<String, String>) images.get("jpg") : null;
                if (jpg != null) {
                    mediumImage = jpg.get("image_url") != null ? jpg.get("image_url") : "";
                    largeImage = jpg.get("large_image_url") != null ? jpg.get("large_image_url") : "";
                }
            } else {
                Map<String, String> mainPicture = animeData.get("main_picture") != null ? 
                    (Map<String, String>) animeData.get("main_picture") : null;
                mediumImage = mainPicture != null ? mainPicture.get("medium") : "";
                largeImage = mainPicture != null ? mainPicture.get("large") : "";
            }
            
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
            
            // Extract MAL ID if available (from Jikan API external links)
            int malId = 0;
            if (animeData.containsKey("external_links")) {
                List<Map<String, Object>> externalLinks = (List<Map<String, Object>>) animeData.get("external_links");
                for (Map<String, Object> link : externalLinks) {
                    if ("MyAnimeList".equals(link.get("name"))) {
                        String url = (String) link.get("url");
                        if (url != null && url.matches(".*/anime/(\\d+).*")) {
                            malId = Integer.parseInt(url.replaceAll(".*/anime/(\\d+).*", "$1"));
                            break;
                        }
                    }
                }
            }
            
            // Create Anime object with jikanId and malId
            Anime anime = new Anime(jikanId, malId, title, mediumImage, largeImage, synopsis, genreString);
            
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
