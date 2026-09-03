package io.github.sweetpark.sqltuner.intellij.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.ActionCallback;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;

public class TestActionManager extends ActionManager {

	@Override
	public ActionPopupMenu createActionPopupMenu(String place, ActionGroup group) {
		return null;
	}

	@Override
	public ActionToolbar createActionToolbar(String place, ActionGroup group, boolean horizontal) {
		return null;
	}

	@Override
	public AnAction getAction(String actionId) {
		return null;
	}

	@Override
	public String getId(AnAction action) {
		return "";
	}

	@Override
	public void registerAction(String actionId, AnAction action) {
	}

	@Override
	public void registerAction(String actionId, AnAction action, PluginId pluginId) {
	}

	@Override
	public void unregisterAction(String actionId) {
	}

	@Override
	public void replaceAction(String actionId, AnAction newAction) {
	}

	@Override
	public String[] getActionIds(String idPrefix) {
		return new String[0];
	}

	@Override
	public List<String> getActionIdList(String idPrefix) {
		return List.of();
	}

	@Override
	public boolean isGroup(String actionId) {
		return false;
	}

	@Override
	public AnAction getActionOrStub(String actionId) {
		return null;
	}

	@Override
	public void addTimerListener(TimerListener listener) {
	}

	@Override
	public void removeTimerListener(TimerListener listener) {
	}

	@Override
	public ActionCallback tryToExecute(AnAction action, InputEvent inputEvent, Component contextComponent, String place,
			boolean now) {
		return null;
	}

	@Override
	public void addAnActionListener(AnActionListener listener) {
	}

	@Override
	public KeyboardShortcut getKeyboardShortcut(String actionId) {
		return null;
	}
}
