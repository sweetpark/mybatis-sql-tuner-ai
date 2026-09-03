package io.github.sweetpark.sqlanalyzer.intellij.toolwindow;

import io.github.sweetpark.sqlanalyzer.intellij.config.SecretStore;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link SecretStore}의 인메모리 가짜 구현체. 테스트에서 PasswordSafe(OS 자격 증명 저장소) 접근을 대체한다.
 */
public class TestSecretStore implements SecretStore {

	private final Map<String, String> map = new HashMap<>();

	@Override
	public String getSecret(String key) {
		return map.getOrDefault(key, "");
	}

	@Override
	public void setSecret(String key, String value) {
		if (value == null || value.isBlank()) {
			map.remove(key);
		} else {
			map.put(key, value);
		}
	}
}
