package io.github.sweetpark.sqltuner.intellij.config;

/**
 * AI(Ollama / OpenAI 호환) 연결 정보를 담는 값 객체. 저장/로드는 AiSettingsDialog가 담당한다 (Base
 * URL/Model은 PropertiesComponent, API Key는 {@link SecretStore}를 통해).
 */
public class AiSettingsConfig {

	private final String baseUrl;
	private final String model;
	private final String apiKey;

	public AiSettingsConfig(String baseUrl, String model, String apiKey) {
		this.baseUrl = baseUrl;
		this.model = model;
		this.apiKey = apiKey;
	}

	/**
	 * AI 설정이 입력된 상태인지 확인. baseUrl과 model이 모두 채워져 있으면 configured로 판단한다. apiKey는
	 * 선택적이거나 기본값이 허용되므로 검증 대상에서 제외한다.
	 */
	public boolean isConfigured() {
		return baseUrl != null && !baseUrl.isBlank() && model != null && !model.isBlank();
	}

	public String getBaseUrl() {
		return baseUrl;
	}
	public String getModel() {
		return model;
	}
	public String getApiKey() {
		return apiKey;
	}
}
