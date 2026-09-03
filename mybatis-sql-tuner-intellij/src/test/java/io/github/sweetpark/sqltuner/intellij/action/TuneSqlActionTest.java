package io.github.sweetpark.sqltuner.intellij.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TuneSqlActionTest {

	static class TestVirtualFile extends LightVirtualFile {
		private final String content;

		public TestVirtualFile(String name, String content) {
			super(name);
			this.content = content;
		}

		@Override
		public String getExtension() {
			int dot = getName().lastIndexOf('.');
			return dot >= 0 ? getName().substring(dot + 1) : "";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
		}
	}

	static class ErrorVirtualFile extends TestVirtualFile {
		public ErrorVirtualFile(String name) {
			super(name, "");
		}

		@Override
		public InputStream getInputStream() throws IOException {
			throw new IOException("Forced test disk error");
		}
	}

	@Test
	@DisplayName("getActionUpdateThread - BGT 반환")
	void getActionUpdateThread_returnsBgt() {
		TuneSqlAction action = new TuneSqlAction();
		assertEquals(ActionUpdateThread.BGT, action.getActionUpdateThread());
	}

	@Test
	@DisplayName("update - 다양한 파일 조건에 따른 enabled/visible 검증")
	void update_conditions() {
		TuneSqlAction action = new TuneSqlAction();

		// 1. file is null
		Presentation p1 = new Presentation();
		AnActionEvent eventNullFile = createActionEvent(null, p1, null);
		action.update(eventNullFile);
		assertFalse(p1.isEnabledAndVisible());

		// 2. non-xml file
		Presentation p2 = new Presentation();
		VirtualFile txtFile = new TestVirtualFile("test.txt", "<mapper namespace='test'>");
		AnActionEvent eventTxt = createActionEvent(null, p2, txtFile);
		action.update(eventTxt);
		assertFalse(p2.isEnabledAndVisible());

		// 3. xml file without <mapper
		Presentation p3 = new Presentation();
		VirtualFile nonMapperXml = new TestVirtualFile("pom.xml", "<project></project>");
		AnActionEvent eventNonMapper = createActionEvent(null, p3, nonMapperXml);
		action.update(eventNonMapper);
		assertFalse(p3.isEnabledAndVisible());

		// 4. valid mapper xml
		Presentation p4 = new Presentation();
		VirtualFile mapperXml = new TestVirtualFile("UserMapper.xml", "<?xml><mapper namespace='test'>");
		AnActionEvent eventMapper = createActionEvent(null, p4, mapperXml);
		action.update(eventMapper);
		assertTrue(p4.isEnabledAndVisible());

		// 5. xml file causing IOException
		Presentation p5 = new Presentation();
		VirtualFile errorXml = new ErrorVirtualFile("ErrorMapper.xml");
		AnActionEvent eventError = createActionEvent(null, p5, errorXml);
		action.update(eventError);
		assertFalse(p5.isEnabledAndVisible());
	}

	@Test
	@DisplayName("actionPerformed - null project / null file 처리")
	void actionPerformed_nullHandling() {
		TuneSqlAction action = new TuneSqlAction();

		// 1. null project
		VirtualFile mapperXml = new TestVirtualFile("UserMapper.xml", "<mapper/>");
		AnActionEvent event1 = createActionEvent(null, new Presentation(), mapperXml);
		assertDoesNotThrow(() -> action.actionPerformed(event1));

		// 2. null file
		Project mockProject = (Project) Proxy.newProxyInstance(TuneSqlActionTest.class.getClassLoader(),
				new Class<?>[]{Project.class}, (p, m, a) -> null);
		AnActionEvent event2 = createActionEvent(mockProject, new Presentation(), null);
		assertDoesNotThrow(() -> action.actionPerformed(event2));
	}

	private static AnActionEvent createActionEvent(Project project, Presentation presentation, VirtualFile file) {
		DataContext dataContext = dataId -> {
			if (dataId != null) {
				if (CommonDataKeys.VIRTUAL_FILE.is(dataId))
					return file;
				if (CommonDataKeys.PROJECT.is(dataId))
					return project;
			}
			return null;
		};
		return new AnActionEvent(null, dataContext, ActionPlaces.UNKNOWN, presentation, new TestActionManager(), 0);
	}
}
