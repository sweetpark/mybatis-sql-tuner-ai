package io.github.sweetpark.sqltuner.intellij.toolwindow;

import io.github.sweetpark.sqltuner.intellij.config.AiSettingsConfig;
import io.github.sweetpark.sqltuner.intellij.config.SqlTunerConfig;
import io.github.sweetpark.sqltuner.intellij.service.AiChatClient;
import io.github.sweetpark.sqltuner.intellij.service.SqlTunerService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * MyBatis SQL Tuner Tool Window의 메인 UI 패널.
 *
 * <p>
 * 레이아웃 구조:
 *
 * <pre>
 * ┌─ MyBatis SQL Tuner ──────────────────────────────────┐
 * │  [DB Settings] [AI Settings]                            │  ← 설정 버튼
 * ├─────────────────────────────────────────────────────────┤
 * │  Mapper Dir : [경로 입력창              ] [...]          │
 * │  Mapper File: [상대경로/파일명 드롭다운  ]                │  ← 재귀 탐색, 상대경로 표시
 * │  Query ID   : [쿼리ID 드롭다운         ]                 │
 * │                                    [Analyze]              │
 * ├─────────────────────────────────────────────────────────┤
 * │  (결과 텍스트 영역)                                       │
 * ├─────────────────────────────────────────────────────────┤
 * │  [AI 분석 실행]                      [Copy to Clipboard]  │
 * └─────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>
 * 캐스케이드 로직:
 * <ul>
 * <li>browse 버튼(또는 mapperDirField Enter/포커스 이탈) → {@code reloadMapperFiles()} —
 * mapper base dir 하위 XML 파일을 재귀 탐색, 상대 경로로 표시</li>
 * <li>mapperFileCombo 선택 변경 → {@code reloadQueryIds()} — 선택된 XML 파일의 DML id 목록을
 * queryIdCombo에 로드</li>
 * </ul>
 */
public class SqlTunerPanel extends JPanel {

	private static final Logger log = LoggerFactory.getLogger(SqlTunerPanel.class);

	private final Project project;
	private final SqlTunerService service;

	private JTextField mapperDirField;
	private JComboBox<String> mapperFileCombo;
	private JComboBox<String> queryIdCombo;
	private JButton analyzeButton;
	private JTextArea resultArea;
	private JButton copyButton;
	private JButton aiAnalyzeButton;

	/** AI 스트리밍 호출 클라이언트 (네트워크 I/O는 백그라운드 태스크에서 수행) */
	private final AiChatClient aiChatClient = new AiChatClient();

	/** mapperFileCombo 검색 필터링을 위한 전체 파일 목록 캐시 */
	private List<String> allMapperFiles = new ArrayList<>();

	/**
	 * Mapper File 필터링 도중 removeAllItems()/addItem() 이 유발하는 ActionEvent의 cascade를 막기
	 * 위한 가드 플래그.
	 */
	private boolean isUpdatingFilter = false;

	/** queryIdCombo 검색 필터링을 위한 전체 Query ID 목록 캐시 */
	private List<String> allQueryIds = new ArrayList<>();

	/**
	 * Query ID 필터링 도중 removeAllItems()/addItem() 이 유발하는 이벤트 cascade를 막는 플래그.
	 */
	private boolean isUpdatingQueryFilter = false;

	public SqlTunerPanel(Project project) {
		this.project = project;
		this.service = new SqlTunerService();
		initComponents();
	}

	/**
	 * 외부(TuneSqlAction)에서 XML 파일 경로를 받아 UI를 초기화한다.
	 *
	 * @param mapperFilePath
	 *            에디터에서 우클릭한 XML 파일의 절대 경로
	 */
	public void setMapperFile(String mapperFilePath) {
		Path mapperPath = Path.of(mapperFilePath);
		Path parentDir = mapperPath.getParent();
		Path fileName = mapperPath.getFileName();

		if (parentDir == null || fileName == null) {
			return;
		}

		mapperDirField.setText(parentDir.toString());
		reloadMapperFiles();

		// 파일명으로 Mapper File 콤보에서 선택 (상대 경로가 "." 기준이므로 파일명만 일치)
		mapperFileCombo.setSelectedItem(fileName.toString());
		reloadQueryIds();
	}

	// ──────────────────────────────────────────────────────────────────────────
	// UI 초기화
	// ──────────────────────────────────────────────────────────────────────────

	private void initComponents() {
		setLayout(new BorderLayout(8, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		add(buildTopPanel(), BorderLayout.NORTH);
		add(buildResultPanel(), BorderLayout.CENTER);
		add(buildBottomPanel(), BorderLayout.SOUTH);
	}

	/** DB Settings & AI Settings 버튼(최상단) + 입력 폼 */
	private JPanel buildTopPanel() {
		JPanel wrapper = new JPanel(new BorderLayout(4, 4));

		JButton dbSettingsButton = new JButton("DB Settings");
		dbSettingsButton.setToolTipText("JDBC 연결 정보를 설정합니다.");
		dbSettingsButton.addActionListener(e -> new DbSettingsDialog(project).show());

		JButton aiSettingsButton = new JButton("AI Settings");
		aiSettingsButton.setToolTipText("AI 서버 연결 정보를 설정합니다.");
		aiSettingsButton.addActionListener(e -> new AiSettingsDialog(project).show());

		JPanel settingsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		settingsRow.add(dbSettingsButton);
		settingsRow.add(aiSettingsButton);
		wrapper.add(settingsRow, BorderLayout.NORTH);
		wrapper.add(buildInputPanel(), BorderLayout.CENTER);

		return wrapper;
	}

	/**
	 * 입력 폼: Mapper Dir, Mapper File(상대 경로), Query ID, Analyze 버튼.
	 */
	private JPanel buildInputPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// 행 0 — Mapper Dir
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		panel.add(new JLabel("Mapper Dir:"), gbc);

		mapperDirField = new JTextField();
		// Enter 키 → 즉시 파일 목록 갱신
		mapperDirField.addActionListener(e -> reloadMapperFiles());
		// 포커스 이탈 → 다른 필드로 이동 시에도 갱신
		mapperDirField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				reloadMapperFiles();
			}
		});
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(mapperDirField, gbc);

		JButton browseButton = new JButton("...");
		browseButton.setToolTipText("매퍼 XML 루트 디렉토리를 선택합니다.");
		browseButton.addActionListener(e -> browseMapperDir());
		gbc.gridx = 2;
		gbc.weightx = 0;
		panel.add(browseButton, gbc);

		// 행 1 — Mapper File (재귀 탐색, 상대 경로 표시)
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0;
		panel.add(new JLabel("Mapper File:"), gbc);

		mapperFileCombo = new JComboBox<>();
		mapperFileCombo.setEditable(true);
		mapperFileCombo.setToolTipText("파일명을 입력하면 실시간으로 필터링됩니다. 목록에서 선택하면 Query ID가 자동으로 로드됩니다.");

		mapperFileCombo.addActionListener(e -> {
			if (!isUpdatingFilter) {
				reloadQueryIds();
			}
		});

		JTextField mapperFileEditor = (JTextField) mapperFileCombo.getEditor().getEditorComponent();
		mapperFileEditor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				int keyCode = e.getKeyCode();

				if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
					return;
				}

				if (keyCode == KeyEvent.VK_ENTER) {
					String editorText = mapperFileEditor.getText();

					if (!allMapperFiles.contains(editorText) && mapperFileCombo.getItemCount() > 0) {
						int idx = mapperFileCombo.getSelectedIndex();
						String itemToSelect = (idx >= 0 && idx < mapperFileCombo.getItemCount())
								? mapperFileCombo.getItemAt(idx)
								: mapperFileCombo.getItemAt(0);
						isUpdatingFilter = true;
						try {
							mapperFileCombo.setSelectedItem(itemToSelect);
							mapperFileEditor.setText(itemToSelect);
							mapperFileCombo.hidePopup();
						} finally {
							isUpdatingFilter = false;
						}
						reloadQueryIds();
					}
					return;
				}

				filterMapperFiles();
			}
		});

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		gbc.gridwidth = 2;
		panel.add(mapperFileCombo, gbc);
		gbc.gridwidth = 1;

		// 행 2 — Query ID + Analyze 버튼
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 0;
		panel.add(new JLabel("Query ID:"), gbc);

		queryIdCombo = new JComboBox<>();
		queryIdCombo.setEditable(true);
		queryIdCombo.setToolTipText("Query ID를 입력하면 실시간으로 필터링됩니다.");

		JTextField queryIdEditor = (JTextField) queryIdCombo.getEditor().getEditorComponent();
		queryIdEditor.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				int keyCode = e.getKeyCode();

				if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
					return;
				}

				if (keyCode == KeyEvent.VK_ENTER) {
					String editorText = queryIdEditor.getText();

					if (!allQueryIds.contains(editorText) && queryIdCombo.getItemCount() > 0) {
						int idx = queryIdCombo.getSelectedIndex();
						String itemToSelect = (idx >= 0 && idx < queryIdCombo.getItemCount())
								? queryIdCombo.getItemAt(idx)
								: queryIdCombo.getItemAt(0);
						isUpdatingQueryFilter = true;
						try {
							queryIdCombo.setSelectedItem(itemToSelect);
							queryIdEditor.setText(itemToSelect);
							queryIdCombo.hidePopup();
						} finally {
							isUpdatingQueryFilter = false;
						}
					}
					return;
				}

				filterQueryIds();
			}
		});

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		panel.add(queryIdCombo, gbc);

		analyzeButton = new JButton("Analyze");
		analyzeButton.addActionListener(e -> runAnalysis());
		gbc.gridx = 2;
		gbc.weightx = 0;
		panel.add(analyzeButton, gbc);

		return panel;
	}

	/** 중앙 결과 영역: 스크롤 가능한 읽기 전용 텍스트 영역 */
	private JScrollPane buildResultPanel() {
		resultArea = new JTextArea();
		resultArea.setEditable(false);
		resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		resultArea.setLineWrap(true);
		resultArea.setWrapStyleWord(true);
		return new JScrollPane(resultArea);
	}

	/** 하단: AI 분석 실행(좌) + 클립보드 복사(우) 버튼 */
	private JPanel buildBottomPanel() {
		JPanel panel = new JPanel(new BorderLayout());

		JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		aiAnalyzeButton = new JButton("AI 분석 실행");
		aiAnalyzeButton.setToolTipText("프롬프트를 생성하여 AI 서버에 전송하고 응답을 스트리밍으로 표시합니다.");
		aiAnalyzeButton.addActionListener(e -> runAiAnalysis());
		leftPanel.add(aiAnalyzeButton);

		JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		copyButton = new JButton("Copy to Clipboard");
		copyButton.setEnabled(false);
		copyButton.addActionListener(e -> copyToClipboard());
		rightPanel.add(copyButton);

		panel.add(leftPanel, BorderLayout.WEST);
		panel.add(rightPanel, BorderLayout.EAST);
		return panel;
	}

	// ──────────────────────────────────────────────────────────────────────────
	// 캐스케이드 갱신 메서드
	// ──────────────────────────────────────────────────────────────────────────

	private void reloadMapperFiles() {
		String baseDirStr = mapperDirField.getText().trim();

		allMapperFiles.clear();
		isUpdatingFilter = true;
		try {
			mapperFileCombo.removeAllItems();
			JTextField editor = (JTextField) mapperFileCombo.getEditor().getEditorComponent();
			editor.setText("");
		} finally {
			isUpdatingFilter = false;
		}

		if (baseDirStr.isBlank()) {
			return;
		}

		try {
			List<String> files = service.listXmlFiles(Path.of(baseDirStr));
			allMapperFiles.addAll(files);

			isUpdatingFilter = true;
			try {
				files.forEach(mapperFileCombo::addItem);
			} finally {
				isUpdatingFilter = false;
			}
		} catch (Exception e) {
			log.warn("XML 파일 목록 로드 실패: {}", baseDirStr, e);
		}

		reloadQueryIds();
	}

	private void reloadQueryIds() {
		allQueryIds.clear();
		isUpdatingQueryFilter = true;
		try {
			queryIdCombo.removeAllItems();
			JTextField editor = (JTextField) queryIdCombo.getEditor().getEditorComponent();
			editor.setText("");
		} finally {
			isUpdatingQueryFilter = false;
		}

		Path mapperFilePath = getSelectedMapperFilePath();

		if (mapperFilePath == null) {
			return;
		}

		try {
			List<String> ids = XmlQueryIdReader.readIds(mapperFilePath);
			allQueryIds.addAll(ids);

			isUpdatingQueryFilter = true;
			try {
				ids.forEach(queryIdCombo::addItem);
			} finally {
				isUpdatingQueryFilter = false;
			}
		} catch (Exception e) {
			log.warn("queryId 목록 로드 실패: {}", mapperFilePath, e);
		}
	}

	private void filterMapperFiles() {
		if (isUpdatingFilter) {
			return;
		}

		JTextField editor = (JTextField) mapperFileCombo.getEditor().getEditorComponent();
		String keyword = editor.getText();
		String lower = keyword.toLowerCase();

		isUpdatingFilter = true;
		try {
			mapperFileCombo.removeAllItems();

			List<String> filtered = lower.isBlank()
					? allMapperFiles
					: allMapperFiles.stream().filter(f -> f.toLowerCase().contains(lower))
							.collect(java.util.stream.Collectors.toList());

			filtered.forEach(mapperFileCombo::addItem);

			editor.setText(keyword);
			editor.setCaretPosition(keyword.length());

			if (!lower.isBlank() && !filtered.isEmpty()) {
				mapperFileCombo.showPopup();
			}
		} finally {
			isUpdatingFilter = false;
		}
	}

	private void filterQueryIds() {
		if (isUpdatingQueryFilter) {
			return;
		}

		JTextField editor = (JTextField) queryIdCombo.getEditor().getEditorComponent();
		String keyword = editor.getText();
		String lower = keyword.toLowerCase();

		isUpdatingQueryFilter = true;
		try {
			queryIdCombo.removeAllItems();

			List<String> filtered = lower.isBlank()
					? allQueryIds
					: allQueryIds.stream().filter(id -> id.toLowerCase().contains(lower))
							.collect(java.util.stream.Collectors.toList());

			filtered.forEach(queryIdCombo::addItem);

			editor.setText(keyword);
			editor.setCaretPosition(keyword.length());

			if (!lower.isBlank() && !filtered.isEmpty()) {
				queryIdCombo.showPopup();
			}
		} finally {
			isUpdatingQueryFilter = false;
		}
	}

	// ──────────────────────────────────────────────────────────────────────────
	// 헬퍼
	// ──────────────────────────────────────────────────────────────────────────

	private void browseMapperDir() {
		VirtualFile initialDir = resolveInitialBrowseDir();

		VirtualFile chosen = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(),
				project, initialDir);

		if (chosen != null) {
			mapperDirField.setText(chosen.getPath());
			reloadMapperFiles();
		}
	}

	private VirtualFile resolveInitialBrowseDir() {
		String currentText = mapperDirField.getText().trim();
		if (!currentText.isBlank()) {
			VirtualFile existing = LocalFileSystem.getInstance().findFileByPath(currentText);
			if (existing != null && existing.isDirectory()) {
				return existing;
			}
		}

		String basePath = project.getBasePath();
		if (basePath != null) {
			return LocalFileSystem.getInstance().findFileByPath(basePath);
		}

		return null;
	}

	private Path getSelectedMapperFilePath() {
		String baseDirStr = mapperDirField.getText().trim();
		String relativePath = (String) mapperFileCombo.getSelectedItem();

		if (baseDirStr.isBlank() || relativePath == null || relativePath.isBlank()) {
			return null;
		}

		return Path.of(baseDirStr).resolve(relativePath);
	}

	// ──────────────────────────────────────────────────────────────────────────
	// 분석 실행
	// ──────────────────────────────────────────────────────────────────────────

	private void runAnalysis() {
		String queryId = (String) queryIdCombo.getSelectedItem();
		String mapperDirStr = mapperDirField.getText().trim();

		if (queryId == null || queryId.isBlank()) {
			Messages.showWarningDialog(project, "Query ID를 입력하세요.", "입력 오류");
			return;
		}

		if (mapperDirStr.isBlank()) {
			Messages.showWarningDialog(project, "Mapper Dir을 지정하세요.", "입력 오류");
			return;
		}

		SqlTunerConfig config = DbSettingsDialog.loadConfig(project);

		if (!config.isConfigured()) {
			int answer = Messages.showYesNoDialog(project, "DB 연결 정보가 설정되지 않았습니다.\n지금 설정하시겠습니까?", "DB 설정 필요",
					Messages.getQuestionIcon());

			if (answer == Messages.YES) {
				new DbSettingsDialog(project).show();
			}

			return;
		}

		Path selectedMapperFile = getSelectedMapperFilePath();

		if (selectedMapperFile == null) {
			Messages.showWarningDialog(project, "분석할 매퍼 파일을 선택하세요.", "입력 오류");
			return;
		}

		Path mapperBaseDir = Path.of(mapperDirStr);

		analyzeButton.setEnabled(false);
		aiAnalyzeButton.setEnabled(false);
		copyButton.setEnabled(false);
		resultArea.setText("분석 중...");

		ProgressManager.getInstance().run(new Task.Backgroundable(project, "MyBatis SQL 분석 중...") {
			private String result;
			private Exception error;

			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				indicator.setIndeterminate(true);
				indicator.setText("DB 분석 중...");

				try {
					result = service.analyze(config, selectedMapperFile, mapperBaseDir, queryId);
				} catch (Exception e) {
					error = e;
				}
			}

			@Override
			public void onSuccess() {
				analyzeButton.setEnabled(true);
				aiAnalyzeButton.setEnabled(true);

				if (error != null) {
					resultArea.setText("오류 발생:\n" + error.getMessage());
					log.error("SQL 분석 실패 - queryId: {}", queryId, error);
				} else if (result != null) {
					resultArea.setText(result);
					resultArea.setCaretPosition(0);
					copyButton.setEnabled(true);
				}
			}

			@Override
			public void onCancel() {
				analyzeButton.setEnabled(true);
				aiAnalyzeButton.setEnabled(true);
				resultArea.setText("분석이 취소되었습니다.");
			}
		});
	}

	private void runAiAnalysis() {
		String queryId = (String) queryIdCombo.getSelectedItem();
		String mapperDirStr = mapperDirField.getText().trim();

		if (queryId == null || queryId.isBlank()) {
			Messages.showWarningDialog(project, "Query ID를 입력하세요.", "입력 오류");
			return;
		}

		if (mapperDirStr.isBlank()) {
			Messages.showWarningDialog(project, "Mapper Dir을 지정하세요.", "입력 오류");
			return;
		}

		SqlTunerConfig dbConfig = DbSettingsDialog.loadConfig(project);

		if (!dbConfig.isConfigured()) {
			int answer = Messages.showYesNoDialog(project, "DB 연결 정보가 설정되지 않았습니다.\n지금 설정하시겠습니까?", "DB 설정 필요",
					Messages.getQuestionIcon());

			if (answer == Messages.YES) {
				new DbSettingsDialog(project).show();
			}

			return;
		}

		AiSettingsConfig aiConfig = AiSettingsDialog.loadConfig(project);

		if (!aiConfig.isConfigured()) {
			int answer = Messages.showYesNoDialog(project, "AI 연결 정보가 설정되지 않았습니다.\n지금 설정하시겠습니까?", "AI 설정 필요",
					Messages.getQuestionIcon());

			if (answer == Messages.YES) {
				new AiSettingsDialog(project).show();
			}

			return;
		}

		Path selectedMapperFile = getSelectedMapperFilePath();

		if (selectedMapperFile == null) {
			Messages.showWarningDialog(project, "분석할 매퍼 파일을 선택하세요.", "입력 오류");
			return;
		}

		Path mapperBaseDir = Path.of(mapperDirStr);

		aiAnalyzeButton.setEnabled(false);
		analyzeButton.setEnabled(false);
		copyButton.setEnabled(false);
		resultArea.setText("");

		ProgressManager.getInstance().run(new Task.Backgroundable(project, "AI 분석 중...") {
			private Exception error;

			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				indicator.setIndeterminate(true);
				indicator.setText("프롬프트 생성 및 AI 응답 수신 중...");

				try {
					String prompt = service.analyze(dbConfig, selectedMapperFile, mapperBaseDir, queryId);
					aiChatClient.streamChat(aiConfig, prompt, SqlTunerPanel.this::appendDelta);
				} catch (Exception e) {
					error = e;
				}
			}

			@Override
			public void onSuccess() {
				aiAnalyzeButton.setEnabled(true);
				analyzeButton.setEnabled(true);

				if (error != null) {
					appendDelta("\n\n[오류 발생] " + error.getMessage());
					log.error("AI 분석 실패 - queryId: {}", queryId, error);
				} else {
					copyButton.setEnabled(true);
				}
			}

			@Override
			public void onCancel() {
				aiAnalyzeButton.setEnabled(true);
				analyzeButton.setEnabled(true);
			}
		});
	}

	private void appendDelta(String delta) {
		ApplicationManager.getApplication().invokeLater(() -> {
			resultArea.append(delta);
			resultArea.setCaretPosition(resultArea.getDocument().getLength());
		}, ModalityState.any());
	}

	private void copyToClipboard() {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(resultArea.getText()), null);

		copyButton.setText("Copied!");
		Timer timer = new Timer(2000, e -> copyButton.setText("Copy to Clipboard"));
		timer.setRepeats(false);
		timer.start();
	}
}
