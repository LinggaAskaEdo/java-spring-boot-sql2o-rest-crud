package com.otis.util;

import java.security.SecureRandom;
import java.util.Random;

public final class RandomUtils {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final Random RANDOM = new Random();

	private static final String[] COMPANY_PREFIXES = { "Tech", "Digital", "Cloud", "Data", "AI", "Software",
			"Innovation", "Systems", "Solutions", "Labs" };
	private static final String[] COMPANY_SUFFIXES = { "Corp", "Solutions", "Systems", "Labs", "Inc", "Group",
			"Technologies", "Services", "Factory", "Hub" };
	private static final String[] CITIES = { "Lviv", "Kyiv", "Kharkiv", "Madrid", "Milan", "Seoul", "Tokyo",
			"Singapore", "Berlin", "Paris", "London", "New York", "Sydney", "Toronto", "Dubai" };

	private static final String[] PRODUCT_CATEGORIES = { "ERP", "CRM", "SCM", "HRIS", "BI", "Cloud", "E-Commerce",
			"Marketing", "Security", "IoT", "ML", "Blockchain", "DevOps", "API", "Analytics" };
	private static final String[] PRODUCT_TYPES = { "Platform", "System", "Suite", "Dashboard", "Management",
			"Services", "Tools", "Gateway", "Solution", "Hub" };

	private static final String[] TUTORIAL_TOPICS = { "Spring Boot", "RESTful APIs", "Microservices", "Database",
			"Security", "Testing", "Docker", "Kubernetes", "GraphQL", "Performance", "Reactive", "Cloud Native",
			"CI/CD", "Monitoring", "Logging" };
	private static final String[] TUTORIAL_LEVELS = { "Fundamentals", "Advanced", "Masterclass", "Complete Guide",
			"Beginner Course", "Deep Dive", "Best Practices", "Hands-on" };
	private static final String[] TUTORIAL_PREFIXES = { "Mastering", "Building", "Learning", "Implementing",
			"Developing", "Creating", "Deploying", "Optimizing" };

	private RandomUtils() {
	}

	public static SecureRandom getSecureRandom() {
		return SECURE_RANDOM;
	}

	public static Random getRandom() {
		return RANDOM;
	}

	public static String randomCompanyName() {
		String city = CITIES[RANDOM.nextInt(CITIES.length)];
		String prefix = COMPANY_PREFIXES[RANDOM.nextInt(COMPANY_PREFIXES.length)];
		String suffix = COMPANY_SUFFIXES[RANDOM.nextInt(COMPANY_SUFFIXES.length)];
		return city + " " + prefix + " " + suffix;
	}

	public static String randomProductName() {
		String category = PRODUCT_CATEGORIES[RANDOM.nextInt(PRODUCT_CATEGORIES.length)];
		String type = PRODUCT_TYPES[RANDOM.nextInt(PRODUCT_TYPES.length)];
		return category + " " + type;
	}

	public static String randomTutorialTitle() {
		if (RANDOM.nextBoolean()) {
			String prefix = TUTORIAL_PREFIXES[RANDOM.nextInt(TUTORIAL_PREFIXES.length)];
			String topic = TUTORIAL_TOPICS[RANDOM.nextInt(TUTORIAL_TOPICS.length)];
			return prefix + " " + topic;
		} else {
			String topic = TUTORIAL_TOPICS[RANDOM.nextInt(TUTORIAL_TOPICS.length)];
			String level = TUTORIAL_LEVELS[RANDOM.nextInt(TUTORIAL_LEVELS.length)];
			return topic + " " + level;
		}
	}

	public static String randomTutorialDescription() {
		return "Comprehensive guide covering essential concepts, best practices, and real-world examples for " +
				"building modern applications. Includes hands-on exercises and practical demonstrations.";
	}

	public static boolean randomBoolean() {
		return RANDOM.nextBoolean();
	}

	public static int randomInt(int bound) {
		return RANDOM.nextInt(bound);
	}
}
