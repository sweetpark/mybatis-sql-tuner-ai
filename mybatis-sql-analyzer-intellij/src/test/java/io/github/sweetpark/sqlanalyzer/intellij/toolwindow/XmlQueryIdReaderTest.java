package io.github.sweetpark.sqlanalyzer.intellij.toolwindow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class XmlQueryIdReaderTest {

	@Test
	@DisplayName("Private constructor reflection test")
	void privateConstructor() throws Exception {
		Constructor<XmlQueryIdReader> constructor = XmlQueryIdReader.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		XmlQueryIdReader instance = constructor.newInstance();
		assertNotNull(instance);
	}

	@Test
	@DisplayName("readIds - DML 태그 id 목록을 순서대로 읽기")
	void readIds_success() throws Exception {
		Path tempFile = Files.createTempFile("mapper-read-test", ".xml");
		try {
			String xml = """
					<?xml version="1.0" encoding="UTF-8"?>
					<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
					<mapper namespace="test">
					    <select id="selectUsers">SELECT 1</select>
					    <insert id="insertUser">INSERT INTO users VALUES(1)</insert>
					    <update id="updateUser">UPDATE users SET id=2</update>
					    <delete id="deleteUser">DELETE FROM users</delete>
					    <select>SELECT without id</select>
					</mapper>
					""";
			Files.writeString(tempFile, xml);

			List<String> ids = XmlQueryIdReader.readIds(tempFile);
			assertEquals(List.of("selectUsers", "insertUser", "updateUser", "deleteUser"), ids);
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	@DisplayName("readIds - DML 태그가 없는 경우 빈 리스트 반환")
	void readIds_empty() throws Exception {
		Path tempFile = Files.createTempFile("mapper-empty-test", ".xml");
		try {
			String xml = "<mapper namespace='test'></mapper>";
			Files.writeString(tempFile, xml);

			List<String> ids = XmlQueryIdReader.readIds(tempFile);
			assertTrue(ids.isEmpty());
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}
}
