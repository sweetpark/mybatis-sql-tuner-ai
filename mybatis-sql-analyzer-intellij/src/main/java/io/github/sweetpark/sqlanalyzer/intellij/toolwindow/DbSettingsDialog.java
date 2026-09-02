package io.github.sweetpark.sqlanalyzer.intellij.toolwindow;

import io.github.sweetpark.sqlanalyzer.intellij.config.SqlAnalyzerConfig;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * DB 연결 설정을 입력받아 IntelliJ PropertiesComponent(프로젝트 단위)에 저장하는 다이얼로그.
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

	static final String KEY_JDBC_URL = "sql-analyzer.jdbc.url";
	static final String KEY_JDBC_USER = "sql-analyzer.jdbc.user";
	static final String KEY_JDBC_PASSWORD = "sql-analyzer.jdbc.password";

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

		passwordField = new JPasswordField(props.getValue(KEY_JDBC_PASSWORD, ""), 38);
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

	/** OK(저장) 버튼 클릭 시 PropertiesComponent에 저장 */
	@Override
	protected void doOKAction() {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		props.setValue(KEY_JDBC_URL, urlField.getText().trim());
		props.setValue(KEY_JDBC_USER, userField.getText().trim());
		props.setValue(KEY_JDBC_PASSWORD, new String(passwordField.getPassword()));
		super.doOKAction();
	}

	/**
	 * 현재 프로젝트에 저장된 DB 설정을 SqlAnalyzerConfig 값 객체로 반환한다. 설정이 없으면 빈 문자열로 채워진 객체를
	 * 반환한다.
	 */
	public static SqlAnalyzerConfig loadConfig(Project project) {
		PropertiesComponent props = PropertiesComponent.getInstance(project);
		return new SqlAnalyzerConfig(props.getValue(KEY_JDBC_URL, ""), props.getValue(KEY_JDBC_USER, ""),
				props.getValue(KEY_JDBC_PASSWORD, ""));
	}
}
