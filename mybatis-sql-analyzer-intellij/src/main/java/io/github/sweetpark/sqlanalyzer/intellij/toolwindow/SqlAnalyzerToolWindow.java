package io.github.sweetpark.sqlanalyzer.intellij.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * IntelliJ Tool Window 팩토리.
 *
 * <p>
 * plugin.xml의 toolWindow 확장 포인트에 등록되어, IDE 시작 시 또는 사용자가 Tool Window를 처음 열 때
 * 호출된다. 실제 UI 컴포넌트 생성은 SqlAnalyzerPanel에 위임한다.
 *
 * <p>
 * Tool Window ID "MyBatis SQL Analyzer" 는 plugin.xml 및 AnalyzeSqlAction과 반드시
 * 일치해야 한다.
 */
public class SqlAnalyzerToolWindow implements ToolWindowFactory {

	/**
	 * Tool Window 콘텐츠를 생성한다. IDE가 Tool Window를 처음 열 때 한 번 호출된다.
	 *
	 * @param project
	 *            현재 IntelliJ 프로젝트
	 * @param toolWindow
	 *            Tool Window 컨테이너 (탭, 제목 등 관리)
	 */
	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		SqlAnalyzerPanel panel = new SqlAnalyzerPanel(project);

		ContentFactory contentFactory = ContentFactory.getInstance();
		Content content = contentFactory.createContent(panel, "", false);
		toolWindow.getContentManager().addContent(content);
	}
}
