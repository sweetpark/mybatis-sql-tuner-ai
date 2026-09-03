package io.github.sweetpark.sqltuner.intellij.service;

import io.github.sweetpark.sqltuner.intellij.config.AiSettingsConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * AI(Ollama / OpenAI 호환) 서버의 /chat/completions 엔드포인트로 프롬프트를 전송하고, 스트리밍(SSE) 응답을
 * 델타 단위로 콜백 전달한다.
 *
 * <p>
 * 네트워크 호출(streamChat)은 단위 테스트 대상이 아니다. SSE 한 줄을 델타 문자열로 변환하는 parseDelta()를 순수
 * static 메서드로 분리하여 검증한다.
 */
public class AiChatClient {

	private static final Logger log = LoggerFactory.getLogger(AiChatClient.class);
	private static final Gson GSON = new Gson();

	/**
	 * HttpClient는 selector 스레드와 executor 풀을 소유한다. JDK 17에서는 AutoCloseable이 아니어서
	 * 명시적으로 닫을 수 없으므로, 매 호출마다 생성하면 스레드 풀이 누수된다. 단일 인스턴스를 재사용한다.
	 */
	private final HttpClient client = HttpClient.newHttpClient();

	/**
	 * 프롬프트를 스트리밍 모드로 전송한다.
	 *
	 * <p>
	 * EDT 블로킹 방지를 위해 반드시 백그라운드 스레드에서 호출해야 한다. 델타가 도착할 때마다 onDelta 콜백이 호출되며, UI 갱신은
	 * 호출자가 EDT로 마샬링한다.
	 *
	 * @param config
	 *            baseUrl, model, apiKey
	 * @param prompt
	 *            전송할 프롬프트 (messages[].content 로 직접 전달)
	 * @param onDelta
	 *            델타 텍스트 수신 콜백
	 */
	public void streamChat(AiSettingsConfig config, String prompt, Consumer<String> onDelta)
			throws IOException, InterruptedException {
		String requestBody = buildRequestBody(config.getModel(), prompt);

		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
				.uri(URI.create(config.getBaseUrl() + "/chat/completions")).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));

		if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
			requestBuilder.header("Authorization", "Bearer " + config.getApiKey());
		}

		HttpRequest request = requestBuilder.build();

		HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());

		if (response.statusCode() != 200) {
			throw new IOException("AI 서버 응답 오류: HTTP " + response.statusCode());
		}

		try (Stream<String> lines = response.body()) {
			lines.forEach(line -> {
				String delta = parseDelta(line);

				if (delta != null) {
					onDelta.accept(delta);
				}
			});
		}
	}

	/**
	 * OpenAI chat completions 요청 본문을 생성한다. Gson을 사용하여 프롬프트 내 따옴표/줄바꿈/CDATA 등을 안전하게
	 * 이스케이프한다.
	 */
	private String buildRequestBody(String model, String prompt) {
		JsonObject message = new JsonObject();
		message.addProperty("role", "user");
		message.addProperty("content", prompt);

		JsonArray messages = new JsonArray();
		messages.add(message);

		JsonObject root = new JsonObject();
		root.addProperty("model", model);
		root.add("messages", messages);
		root.addProperty("stream", true);

		return GSON.toJson(root);
	}

	/**
	 * SSE 한 줄을 델타 텍스트로 변환한다.
	 *
	 * <p>
	 * 반환 규칙:
	 * <ul>
	 * <li>"data: " 접두사가 없는 라인(빈 줄, 주석 등) → null</li>
	 * <li>"data: [DONE]" 종료 신호 → null</li>
	 * <li>choices/delta/content가 없는 청크(역할 청크 등) → null</li>
	 * <li>정상 청크 → delta.content 문자열</li>
	 * </ul>
	 */
	public static String parseDelta(String line) {
		if (line == null || !line.startsWith("data:")) {
			return null;
		}

		String payload = line.substring("data:".length()).trim();

		if (payload.equals("[DONE]") || payload.isEmpty()) {
			return null;
		}

		try {
			JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
			JsonArray choices = obj.getAsJsonArray("choices");

			if (choices == null || choices.isEmpty()) {
				return null;
			}

			JsonObject delta = choices.get(0).getAsJsonObject().getAsJsonObject("delta");

			if (delta == null) {
				return null;
			}

			JsonElement content = delta.get("content");

			if (content == null || content.isJsonNull()) {
				return null;
			}

			return content.getAsString();
		} catch (Exception e) {
			log.warn("SSE 라인 파싱 실패: {}", line, e);
			return null;
		}
	}
}
