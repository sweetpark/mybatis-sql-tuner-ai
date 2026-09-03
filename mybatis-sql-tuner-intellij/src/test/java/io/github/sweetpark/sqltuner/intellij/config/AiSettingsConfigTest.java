package io.github.sweetpark.sqltuner.intellij.config;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class AiSettingsConfigTest {

	@Test
	@DisplayName("baseUrl과 model이 있으면 isConfigured()가 true를 반환한다")
	void isConfigured_withBaseUrlAndModel_returnsTrue() {
		AiSettingsConfig config = new AiSettingsConfig("http://localhost:11434/v1", "qwen2.5-coder:7b", "test-key");
		assertTrue(config.isConfigured());
	}

	@Test
	@DisplayName("baseUrl이 null이거나 비어있으면 isConfigured()가 false를 반환한다")
	void isConfigured_withBlankOrNullBaseUrl_returnsFalse() {
		AiSettingsConfig config1 = new AiSettingsConfig("   ", "qwen2.5-coder:7b", "test-key");
		assertFalse(config1.isConfigured());

		AiSettingsConfig config2 = new AiSettingsConfig(null, "qwen2.5-coder:7b", "test-key");
		assertFalse(config2.isConfigured());
	}

	@Test
	@DisplayName("model이 null이거나 비어있으면 isConfigured()가 false를 반환한다")
	void isConfigured_withNullOrBlankModel_returnsFalse() {
		AiSettingsConfig config1 = new AiSettingsConfig("http://localhost:11434/v1", null, "test-key");
		assertFalse(config1.isConfigured());

		AiSettingsConfig config2 = new AiSettingsConfig("http://localhost:11434/v1", "  ", "test-key");
		assertFalse(config2.isConfigured());
	}

	@Test
	@DisplayName("생성자로 전달한 값이 getter를 통해 그대로 반환된다")
	void getters_returnConstructorValues() {
		AiSettingsConfig config = new AiSettingsConfig("http://localhost:11434/v1", "my-model", "my-key");

		assertEquals("http://localhost:11434/v1", config.getBaseUrl());
		assertEquals("my-model", config.getModel());
		assertEquals("my-key", config.getApiKey());
	}
}
