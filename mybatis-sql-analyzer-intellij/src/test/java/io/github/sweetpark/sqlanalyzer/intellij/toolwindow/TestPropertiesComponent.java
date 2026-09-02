package io.github.sweetpark.sqlanalyzer.intellij.toolwindow;

import com.intellij.ide.util.PropertiesComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TestPropertiesComponent extends PropertiesComponent {

	private final Map<String, String> map = new HashMap<>();

	@Override
	public void unsetValue(String name) {
		map.remove(name);
	}

	@Override
	public boolean isValueSet(String name) {
		return map.containsKey(name);
	}

	@Override
	public @Nullable String getValue(String name) {
		return map.get(name);
	}

	@Override
	public void setValue(@NotNull String name, @Nullable String value) {
		if (value == null) {
			map.remove(name);
		} else {
			map.put(name, value);
		}
	}

	@Override
	public void setValue(@NotNull String name, @Nullable String value, @Nullable String defaultValue) {
		setValue(name, value);
	}

	@Override
	public void setValue(@NotNull String name, float value, float defaultValue) {
		map.put(name, String.valueOf(value));
	}

	@Override
	public void setValue(@NotNull String name, int value, int defaultValue) {
		map.put(name, String.valueOf(value));
	}

	@Override
	public void setValue(@NotNull String name, boolean value, boolean defaultValue) {
		map.put(name, String.valueOf(value));
	}

	@Override
	public @Nullable String getValue(String name, @NotNull String defaultValue) {
		return map.getOrDefault(name, defaultValue);
	}

	@Override
	public @Nullable String[] getValues(String name) {
		return new String[0];
	}

	@Override
	public void setValues(String name, String[] values) {
	}

	@Override
	public @Nullable List<String> getList(String name) {
		return new ArrayList<>();
	}

	@Override
	public void setList(String name, @Nullable Collection<String> values) {
	}

	@Override
	public boolean updateValue(@NotNull String name, boolean value) {
		map.put(name, String.valueOf(value));
		return true;
	}
}
