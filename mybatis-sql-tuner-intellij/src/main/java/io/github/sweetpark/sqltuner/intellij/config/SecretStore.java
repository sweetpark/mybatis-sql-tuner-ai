package io.github.sweetpark.sqltuner.intellij.config;

import com.intellij.openapi.project.Project;

/**
 * DB 비밀번호, AI API Key 등 민감 정보를 저장/조회하는 인터페이스.
 *
 * <p>
 * IntelliJ {@code PropertiesComponent}는 값을 프로젝트 workspace 설정 파일에 평문으로 저장하므로
 * 비밀번호, API Key 등 민감 정보 저장에는 부적합하다. 실제 구현체({@link PasswordSafeSecretStore})는 OS
 * 자격 증명 저장소(Windows Credential Manager, macOS Keychain 등)를 사용하는 IntelliJ
 * {@code PasswordSafe}를 통해 안전하게 저장한다.
 *
 * <p>
 * 테스트에서는 {@code Project.getService(SecretStore.class)}를 가로채는 방식으로 인메모리 가짜 구현체를
 * 주입한다 (PropertiesComponent를 테스트할 때 사용하는 패턴과 동일).
 */
public interface SecretStore {

	/** 지정된 키에 저장된 비밀 값을 반환한다. 저장된 값이 없으면 빈 문자열을 반환한다. */
	String getSecret(String key);

	/** 지정된 키에 비밀 값을 저장한다. value가 null이거나 공백이면 저장된 값을 삭제한다. */
	void setSecret(String key, String value);

	static SecretStore getInstance(Project project) {
		return project.getService(SecretStore.class);
	}
}
