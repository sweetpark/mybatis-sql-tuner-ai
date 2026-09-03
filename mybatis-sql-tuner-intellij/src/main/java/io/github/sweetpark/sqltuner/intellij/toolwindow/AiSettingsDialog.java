package io.github.sweetpark.sqltuner.intellij.toolwindow;

import io.github.sweetpark.sqltuner.intellij.config.AiSettingsConfig;
import io.github.sweetpark.sqltuner.intellij.config.SecretStore;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * AI(Ollama / OpenAI 호환) 연결 설정을 입력받아 저장하는 다이얼로그.
 *
 * <p>
 * Base URL/Model은 PropertiesComponent(프로젝트 단위)에, API Key는 OS 자격 증명 저장소를 사용하는
 * {@link io.github.sweetpark.sqltuner.intellij.config.SecretStore}(PasswordSafe)에
 * 저장한다. DbSettingsDialog와 동일한 패턴을 따른다. 설정값은 IDE 재시작 후에도 유지된다.
 */
public class AiSettingsDialog extends DialogWrapper {

	static final String KEY_BASE_URL = "sql-tuner.ai.baseUrl";
	static final String KEY_MODEL = "sql-tuner.ai.model";
	static final String KEY_API_KEY = "sql-tuner.ai.apiKey";

	// 기본 AI 엔드포인트 및 모델 기본값 (Ollama / 범용 모델 기준)
	static final String DEFAULT_BASE_URL = "http://localhost:11434/v1";
	static final String DEFAULT_MODEL = "qwen2.5-coder:7b";
	static final String DEFAULT_API_KEY = "";

	private final Project project;
	private JTextField baseUrlField;
	private JTextField modelField;
	private JPasswordField apiKeyField;

	public AiSettingsDialog(Project project) {
		super(project);
		this.project = project;
		setTitle("AI 연결 설정");
		setOKButtonText("저장");
		init();
	}

	@Override
	protected @Nullable JComponent createCenterPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(6, 6, 6, 6);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;

		PropertiesComponent props = PropertiesComponent.getInstance(project);

		// Base URL
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		panel.add(new JLabel("Base URL:"), gbc);

		baseUrlField = new JTextField(props.getValue(KEY_BASE_URL, DEFAULT_BASE_URL), 38);
		baseUrlField.setToolTipText("예: http://localhost:11434/v1 또는 https://api.openai.com/v1");
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(baseUrlField, gbc);

		// Model
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0;
		panel.add(new JLabel("Model:"), gbc);

		modelField = new JTextField(props.getValue(KEY_MODEL, DEFAULT_MODEL), 38);
		modelField.setToolTipText("예: qwen2.5-coder:7b, gpt-4o-mini");
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(modelField, gbc);

		// API Key
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 0;
		panel.add(new JLabel("API Key:"), gbc);

		apiKeyField = new JPasswordField(loadApiKey(project), 38);
		apiKeyField.setToolTipText("Ollama 등 미인증 환경은 공백 가능. OpenAI 등은 API 키 입력.");
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(apiKeyField, gbc);

		// 안내 문구
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		JLabel hint = new JLabel("<html><font color='gray'>설정은 IntelliJ 프로젝트 단위로 저장됩니다.</font></html>");
		panel.add(hint, gbc);

		return panel;
	}

	/**
	 * OK(저장) 버튼 클릭 시 저장: Base URL/Model은 PropertiesComponent, API Key는
	 * SecretStore(PasswordSafe)
	 */
	@Override
	protected void doOKAction() {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		props.setValue(KEY_BASE_URL, baseUrlField.getText().trim());
		props.setValue(KEY_MODEL, modelField.getText().trim());
		SecretStore.getInstance(project).setSecret(KEY_API_KEY, new String(apiKeyField.getPassword()).trim());
		super.doOKAction();
	}

	/**
	 * 현재 프로젝트에 저장된 AI 설정을 AiSettingsConfig 값 객체로 반환한다. 저장된 값이 없으면 기본값으로 채운다.
	 */
	public static AiSettingsConfig loadConfig(Project project) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		return new AiSettingsConfig(props.getValue(KEY_BASE_URL, DEFAULT_BASE_URL),
				props.getValue(KEY_MODEL, DEFAULT_MODEL), loadApiKey(project));
	}

	/**
	 * API Key를 SecretStore(PasswordSafe)에서 로드한다.
	 *
	 * <p>
	 * 과거 버전은 API Key를 PropertiesComponent에 평문으로 저장했다. SecretStore에 값이 없고 레거시 평문 값이
	 * 남아있으면 1회 마이그레이션 후 평문 값을 삭제한다.
	 */
	private static String loadApiKey(Project project) {
		SecretStore secretStore = SecretStore.getInstance(project);
		String apiKey = secretStore.getSecret(KEY_API_KEY);

		if (apiKey.isEmpty()) {
			PropertiesComponent props = PropertiesComponent.getInstance(project);
			String legacyApiKey = props.getValue(KEY_API_KEY, "");
			if (!legacyApiKey.isEmpty()) {
				secretStore.setSecret(KEY_API_KEY, legacyApiKey);
				props.unsetValue(KEY_API_KEY);
				return legacyApiKey;
			}
		}

		return apiKey.isEmpty() ? DEFAULT_API_KEY : apiKey;
	}
}
