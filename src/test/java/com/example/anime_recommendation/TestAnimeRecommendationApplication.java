package com.example.anime_recommendation;

import org.springframework.boot.SpringApplication;

public class TestAnimeRecommendationApplication {

	public static void main(String[] args) {
		SpringApplication.from(AnimeRecommendationApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
