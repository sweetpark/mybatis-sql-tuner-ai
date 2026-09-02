package io.github.sweetpark.sqlanalyzer.intellij.service;

import com.sun.net.httpserver.HttpServer;
import io.github.sweetpark.sqlanalyzer.intellij.config.AiSettingsConfig;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiChatClientTest {

	private HttpServer server;
	private int port;

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	@DisplayName("정상 SSE 라인에서 delta.content를 추출한다")
	void parseDelta_validLine_returnsContent() {
		String line = "data: {\"choices\":[{\"index\":0,\"delta\":{\"content\":\"안녕\"},\"finish_reason\":null}]}";
		assertEquals("안녕", AiChatClient.parseDelta(line));
	}

	@Test
	@DisplayName("[DONE] 종료 라인은 null을 반환한다")
	void parseDelta_doneLine_returnsNull() {
		assertNull(AiChatClient.parseDelta("data: [DONE]"));
	}

	@Test
	@DisplayName("data 접두사가 없는 라인은 null을 반환한다")
	void parseDelta_nonDataLine_returnsNull() {
		assertNull(AiChatClient.parseDelta(""));
		assertNull(AiChatClient.parseDelta(": ping"));
		assertNull(AiChatClient.parseDelta(null));
		assertNull(AiChatClient.parseDelta("data: "));
	}

	@Test
	@DisplayName("delta에 content가 없거나 null이면 null을 반환한다")
	void parseDelta_noContent_returnsNull() {
		String line1 = "data: {\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\"},\"finish_reason\":null}]}";
		assertNull(AiChatClient.parseDelta(line1));

		String line2 = "data: {\"choices\":[{\"index\":0,\"delta\":{\"content\":null}}]}";
		assertNull(AiChatClient.parseDelta(line2));

		String line3 = "data: {\"choices\":[{\"index\":0}]}";
		assertNull(AiChatClient.parseDelta(line3));
	}

	@Test
	@DisplayName("choices가 비어있거나 없으면 null을 반환한다")
	void parseDelta_emptyChoices_returnsNull() {
		assertNull(AiChatClient.parseDelta("data: {\"choices\":[]}"));
		assertNull(AiChatClient.parseDelta("data: {}"));
	}

	@Test
	@DisplayName("malformed json 처리 시 null 반환")
	void parseDelta_malformedJson_returnsNull() {
		assertNull(AiChatClient.parseDelta("data: {invalid json}"));
	}

	@Test
	@DisplayName("streamChat - 정상 200 SSE 스트리밍 응답 수신")
	void streamChat_success() throws Exception {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();

		server.createContext("/chat/completions", exchange -> {
			byte[] responseBytes = ("data: {\"choices\":[{\"delta\":{\"content\":\"Hello \"}}]}\n\n"
					+ "data: {\"choices\":[{\"delta\":{\"content\":\"World!\"}}]}\n\n" + "data: [DONE]\n\n")
					.getBytes(StandardCharsets.UTF_8);

			exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
			exchange.sendResponseHeaders(200, responseBytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(responseBytes);
			}
		});
		server.start();

		AiChatClient client = new AiChatClient();
		AiSettingsConfig config = new AiSettingsConfig("http://localhost:" + port, "test-model", "test-key");

		List<String> received = new ArrayList<>();
		client.streamChat(config, "test prompt", received::add);

		assertEquals(List.of("Hello ", "World!"), received);
	}

	@Test
	@DisplayName("streamChat - API 키 없는 경우에도 정상 동작")
	void streamChat_withoutApiKey() throws Exception {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();

		server.createContext("/chat/completions", exchange -> {
			byte[] responseBytes = "data: {\"choices\":[{\"delta\":{\"content\":\"OK\"}}]}\n\n"
					.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, responseBytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(responseBytes);
			}
		});
		server.start();

		AiChatClient client = new AiChatClient();
		AiSettingsConfig config = new AiSettingsConfig("http://localhost:" + port, "test-model", "");

		List<String> received = new ArrayList<>();
		client.streamChat(config, "test prompt", received::add);

		assertEquals(List.of("OK"), received);
	}

	@Test
	@DisplayName("streamChat - 500 에러 시 IOException 발생")
	void streamChat_serverError() throws Exception {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();

		server.createContext("/chat/completions", exchange -> {
			exchange.sendResponseHeaders(500, 0);
			exchange.close();
		});
		server.start();

		AiChatClient client = new AiChatClient();
		AiSettingsConfig config = new AiSettingsConfig("http://localhost:" + port, "test-model", "key");

		assertThrows(IOException.class, () -> client.streamChat(config, "prompt", delta -> {
		}));
	}
}
