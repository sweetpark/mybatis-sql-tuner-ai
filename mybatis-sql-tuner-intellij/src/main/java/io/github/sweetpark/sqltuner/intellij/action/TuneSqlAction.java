package io.github.sweetpark.sqltuner.intellij.action;

import io.github.sweetpark.sqltuner.intellij.toolwindow.SqlTunerPanel;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * XML 에디터 우클릭 메뉴에 "Analyze SQL with AI" 항목을 추가하는 IntelliJ 액션.
 *
 * <p>
 * 활성화 조건 (update() 메서드): - 현재 에디터 파일이 .xml 확장자를 가져야 함 - 파일 내용에 {@code <mapper}
 * 문자열이 포함되어야 함 (MyBatis 매퍼 XML 판별)
 *
 * <p>
 * 실행 흐름 (actionPerformed() 메서드): 1. "MyBatis SQL Tuner" Tool Window를 열거나 포커스를
 * 가져옴 2. Tool Window가 열린 후 SqlTunerPanel에 현재 파일 경로 전달 3. SqlTunerPanel이 매퍼
 * 디렉토리와 queryId 드롭다운을 자동 세팅
 */
public class TuneSqlAction extends AnAction {

	// plugin.xml의 toolWindow id와 반드시 일치해야 함
	private static final String TOOL_WINDOW_ID = "MyBatis SQL Tuner";

	@Override
	public void actionPerformed(@NotNull AnActionEvent event) {
		Project project = event.getProject();
		VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);

		if (project == null || file == null) {
			return;
		}

		ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);

		if (toolWindow == null) {
			return;
		}

		// Tool Window를 열고, 열림이 완료된 후 콜백으로 파일 경로를 패널에 전달
		toolWindow.show(() -> {
			Content content = toolWindow.getContentManager().getSelectedContent();

			// Java 16+ pattern matching instanceof: SqlTunerPanel 타입 확인 및 캐스팅 동시 처리
			if (content != null && content.getComponent() instanceof SqlTunerPanel panel) {
				panel.setMapperFile(file.getPath());
			}
		});
	}

	/**
	 * 메뉴 항목의 활성화 여부를 결정한다.
	 *
	 * <p>
	 * getActionUpdateThread()가 BGT를 반환하므로 이 메서드는 백그라운드 스레드에서 실행된다. 파일 I/O(내용 읽기)가
	 * 포함되어 있으므로 EDT에서 실행하면 UI 블로킹이 발생한다.
	 */
	@Override
	public void update(@NotNull AnActionEvent event) {
		VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);

		boolean isMapperXml = file != null && "xml".equalsIgnoreCase(file.getExtension()) && isMapperXmlContent(file);

		event.getPresentation().setEnabledAndVisible(isMapperXml);
	}

	/**
	 * IntelliJ 2022.3+에서 update()를 실행할 스레드를 지정한다.
	 *
	 * <p>
	 * BGT(백그라운드 스레드) 선택 이유: update()에서 파일 내용을 읽는 I/O가 발생하므로 EDT에서 실행하면 UI 응답성이
	 * 떨어진다. BGT에서 실행하면 IDE 성능 경고도 피할 수 있다.
	 */
	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		return ActionUpdateThread.BGT;
	}

	/**
	 * 파일 내용에 {@code <mapper} 문자열이 포함되어 있는지 확인한다.
	 *
	 * <p>
	 * 파일 전체를 읽지 않고 라인 단위로 스트리밍하여 {@code <mapper}를 찾는 즉시 반환한다. 대부분의 매퍼 XML은 앞부분에
	 * {@code <mapper} 태그가 있으므로 성능 영향이 최소화된다.
	 *
	 * @param file
	 *            검사할 VirtualFile
	 * @return true이면 MyBatis 매퍼 XML로 판단
	 */
	private boolean isMapperXmlContent(VirtualFile file) {
		try (InputStream input = file.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

			return reader.lines().anyMatch(line -> line.contains("<mapper"));

		} catch (IOException e) {
			// 파일을 읽을 수 없는 경우 안전하게 false 반환 (메뉴 비활성화)
			return false;
		}
	}
}
