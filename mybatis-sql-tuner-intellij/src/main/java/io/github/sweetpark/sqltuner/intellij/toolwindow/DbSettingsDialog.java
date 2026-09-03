package io.github.sweetpark.sqltuner.intellij.toolwindow;

import io.github.sweetpark.sqltuner.intellij.config.SecretStore;
import io.github.sweetpark.sqltuner.intellij.config.SqlTunerConfig;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * DB 연결 설정을 입력받아 저장하는 다이얼로그.
 *
 * <p>
 * URL/User는 IntelliJ PropertiesComponent(프로젝트 단위)에, 비밀번호는 OS 자격 증명 저장소를 사용하는
 * {@link io.github.sweetpark.sqltuner.intellij.config.SecretStore}(PasswordSafe)에
 * 저장한다.
 *
 * <p>
 * 설정값은 IDE 재시작 후에도 유지되며, .gitignore에 파일을 추가할 필요가 없다.
 *
 * <p>
 * 사용:
 *
 * <pre>{@code
 * DbSettingsDialog dialog = new DbSettingsDialog(project);
 * dialog.show();
 * }</pre>
 */
public class DbSettingsDialog extends DialogWrapper {

	static final String KEY_JDBC_URL = "sql-tuner.jdbc.url";
	static final String KEY_JDBC_USER = "sql-tuner.jdbc.user";
	static final String KEY_JDBC_PASSWORD = "sql-tuner.jdbc.password";

	private final Project project;
	private JTextField urlField;
	private JTextField userField;
	private JPasswordField passwordField;

	public DbSettingsDialog(Project project) {
		super(project);
		this.project = project;
		setTitle("DB 연결 설정");
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

		// JDBC URL
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		panel.add(new JLabel("JDBC URL:"), gbc);

		urlField = new JTextField(props.getValue(KEY_JDBC_URL, ""), 38);
		urlField.setToolTipText("예: jdbc:mariadb://localhost:3306/mydb 또는 jdbc:mysql://localhost:3306/mydb");
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(urlField, gbc);

		// User
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0;
		panel.add(new JLabel("User:"), gbc);

		userField = new JTextField(props.getValue(KEY_JDBC_USER, ""), 38);
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(userField, gbc);

		// Password
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 0;
		panel.add(new JLabel("Password:"), gbc);

		passwordField = new JPasswordField(loadPassword(project), 38);
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(passwordField, gbc);

		// 안내 문구
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.weightx = 1.0;
		JLabel hint = new JLabel("<html><font color='gray'>설정은 IntelliJ 프로젝트 단위로 저장됩니다. (파일 생성 불필요)</font></html>");
		panel.add(hint, gbc);

		return panel;
	}

	/**
	 * OK(저장) 버튼 클릭 시 저장: URL/User는 PropertiesComponent, 비밀번호는
	 * SecretStore(PasswordSafe)
	 */
	@Override
	protected void doOKAction() {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		props.setValue(KEY_JDBC_URL, urlField.getText().trim());
		props.setValue(KEY_JDBC_USER, userField.getText().trim());
		SecretStore.getInstance(project).setSecret(KEY_JDBC_PASSWORD, new String(passwordField.getPassword()));
		super.doOKAction();
	}

	/**
	 * 현재 프로젝트에 저장된 DB 설정을 SqlTunerConfig 값 객체로 반환한다. 설정이 없으면 빈 문자열로 채워진 객체를 반환한다.
	 */
	public static SqlTunerConfig loadConfig(Project project) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		return new SqlTunerConfig(props.getValue(KEY_JDBC_URL, ""), props.getValue(KEY_JDBC_USER, ""),
				loadPassword(project));
	}

	/**
	 * 비밀번호를 SecretStore(PasswordSafe)에서 로드한다.
	 *
	 * <p>
	 * 과거 버전은 비밀번호를 PropertiesComponent에 평문으로 저장했다. SecretStore에 값이 없고 레거시 평문 값이
	 * 남아있으면 1회 마이그레이션 후 평문 값을 삭제한다.
	 */
	private static String loadPassword(Project project) {
		SecretStore secretStore = SecretStore.getInstance(project);
		String password = secretStore.getSecret(KEY_JDBC_PASSWORD);

		if (password.isEmpty()) {
			PropertiesComponent props = PropertiesComponent.getInstance(project);
			String legacyPassword = props.getValue(KEY_JDBC_PASSWORD, "");
			if (!legacyPassword.isEmpty()) {
				secretStore.setSecret(KEY_JDBC_PASSWORD, legacyPassword);
				props.unsetValue(KEY_JDBC_PASSWORD);
				return legacyPassword;
			}
		}

		return password;
	}
}
