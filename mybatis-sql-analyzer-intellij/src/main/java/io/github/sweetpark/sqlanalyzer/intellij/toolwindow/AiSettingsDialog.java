package io.github.sweetpark.sqlanalyzer.intellij.toolwindow;

import io.github.sweetpark.sqlanalyzer.intellij.config.AiSettingsConfig;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * AI(Ollama / OpenAI 호환) 연결 설정을 입력받아 PropertiesComponent(프로젝트 단위)에 저장하는 다이얼로그.
 *
 * <p>
 * DbSettingsDialog와 동일한 패턴을 따른다. 설정값은 IDE 재시작 후에도 유지된다.
 */
public class AiSettingsDialog extends DialogWrapper {

	static final String KEY_BASE_URL = "sql-analyzer.ai.baseUrl";
	static final String KEY_MODEL = "sql-analyzer.ai.model";
	static final String KEY_API_KEY = "sql-analyzer.ai.apiKey";

	// 기본 AI 엔드포인트 및 모델 기본값 (Ollama / 범용 모델 기준)
	static final String DEFAULT_BASE_URL = "http://localhost:11434/v1";
	static final String DEFAULT_MODEL = "qwen2.5-coder:7b";
	static final String DEFAULT_API_KEY = "";

	private final Project project;
	private JTextField baseUrlField;
	private JTextField modelField;
	private JTextField apiKeyField;

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

		apiKeyField = new JTextField(props.getValue(KEY_API_KEY, DEFAULT_API_KEY), 38);
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

	/** OK(저장) 버튼 클릭 시 PropertiesComponent에 저장 */
	@Override
	protected void doOKAction() {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		props.setValue(KEY_BASE_URL, baseUrlField.getText().trim());
		props.setValue(KEY_MODEL, modelField.getText().trim());
		props.setValue(KEY_API_KEY, apiKeyField.getText().trim());
		super.doOKAction();
	}

	/**
	 * 현재 프로젝트에 저장된 AI 설정을 AiSettingsConfig 값 객체로 반환한다. 저장된 값이 없으면 기본값으로 채운다.
	 */
	public static AiSettingsConfig loadConfig(Project project) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		return new AiSettingsConfig(props.getValue(KEY_BASE_URL, DEFAULT_BASE_URL),
				props.getValue(KEY_MODEL, DEFAULT_MODEL), props.getValue(KEY_API_KEY, DEFAULT_API_KEY));
	}
}
