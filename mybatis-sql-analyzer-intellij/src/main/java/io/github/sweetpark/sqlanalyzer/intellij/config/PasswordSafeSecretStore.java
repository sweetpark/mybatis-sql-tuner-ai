package io.github.sweetpark.sqlanalyzer.intellij.config;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.project.Project;

/**
 * {@link SecretStore}의 실제 구현체. IntelliJ {@code PasswordSafe}(OS 자격 증명 저장소)에 값을
 * 위임하여 저장한다.
 *
 * <p>
 * 프로젝트마다 독립된 DB/AI 연결 정보를 유지해야 하므로, 서비스명에 프로젝트를 식별하는 {@code locationHash}를 포함시켜
 * 키를 프로젝트 단위로 분리한다.
 *
 * <p>
 * 실제 OS 자격 증명 저장소(Windows Credential Manager 등)에 접근하는 코드이므로 실행 중인 IntelliJ
 * Application이 필요하다. 단위 테스트 대상이 아니다 — DbSettingsDialog/AiSettingsDialog는
 * {@link SecretStore} 인터페이스를 통해 이 클래스를 대체할 수 있는 가짜 구현체로 테스트한다.
 */
public class PasswordSafeSecretStore implements SecretStore {

	private static final String SUBSYSTEM = "MyBatis SQL Analyzer";

	private final Project project;

	public PasswordSafeSecretStore(Project project) {
		this.project = project;
	}

	@Override
	public String getSecret(String key) {
		String value = PasswordSafe.getInstance().getPassword(attributesFor(key));
		return value == null ? "" : value;
	}

	@Override
	public void setSecret(String key, String value) {
		PasswordSafe.getInstance().setPassword(attributesFor(key), (value == null || value.isBlank()) ? null : value);
	}

	private CredentialAttributes attributesFor(String key) {
		String serviceName = CredentialAttributesKt.generateServiceName(SUBSYSTEM,
				project.getLocationHash() + ":" + key);
		return new CredentialAttributes(serviceName);
	}
}
