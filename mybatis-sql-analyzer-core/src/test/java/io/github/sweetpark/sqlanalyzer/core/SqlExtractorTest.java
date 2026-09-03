package io.github.sweetpark.sqlanalyzer.core;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.junit.jupiter.api.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SqlExtractorTest {

	private Connection connection;
	private static final String queryId = "findBadPerformancePayments";
	private static final String mapperPath = "src/test/resources/mapper/TestMapper.xml";
	private final Path mapperBaseDir = Path.of("src/test/resources/mapper");

	@BeforeEach
	void set() throws Exception {
		connection = DriverManager.getConnection("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");

		try (Statement stmt = connection.createStatement()) {
			stmt.execute(
					"CREATE TABLE payments (payment_id VARCHAR(50) PRIMARY KEY, order_id VARCHAR(50) NOT NULL, amount DECIMAL(10, 2) NOT NULL, status VARCHAR(20) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
			stmt.execute(
					"CREATE TABLE orders (order_id VARCHAR(50) PRIMARY KEY, user_id VARCHAR(50) NOT NULL, mid VARCHAR(10) NOT NULL)");
			stmt.execute(
					"CREATE TABLE merchants (mid VARCHAR(10) PRIMARY KEY, merchant_name VARCHAR(100) NOT NULL, status VARCHAR(20) DEFAULT 'ACTIVE', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
			stmt.execute(
					"CREATE TABLE refunds (refund_id VARCHAR(50) PRIMARY KEY, payment_id VARCHAR(50) NOT NULL, refund_amount DECIMAL(10, 2) NOT NULL, reason VARCHAR(255), refunded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

			stmt.execute("CREATE INDEX payments_amount_idx ON payments(amount)");
			stmt.execute("CREATE INDEX orders_user_id_idx ON orders(user_id)");
			stmt.execute("CREATE INDEX idx_merchants_status ON merchants(status)");
			stmt.execute("CREATE INDEX idx_refunds_payment_id ON refunds(payment_id)");
		}
	}

	@AfterEach
	void remove() throws Exception {
		if (connection != null && !connection.isClosed()) {
			try (Statement stmt = connection.createStatement()) {
				stmt.execute("DROP ALL OBJECTS");
			}
			connection.close();
		}
	}

	@Test
	@DisplayName("Private constructor invocation via reflection")
	void privateConstructor() throws Exception {
		Constructor<SqlExtractor> constructor = SqlExtractor.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		SqlExtractor instance = constructor.newInstance();
		assertNotNull(instance);
	}

	@Test
	@DisplayName("Dynamic Query 추출")
	void findDynamicQuery() throws Exception {
		Node queryIdNode = SqlExtractor.getQueryIdDetail(queryId, mapperPath);
		assertNotNull(queryIdNode);
		assertNotNull(queryIdNode.getTextContent());

		// Path overload
		Node queryIdNodePath = SqlExtractor.getQueryIdDetail(queryId, Path.of(mapperPath));
		assertNotNull(queryIdNodePath);
	}

	@Test
	@DisplayName("DML 태그별 getQueryIdDetail (select, insert, update, delete)")
	void findVariousDmlTags() throws Exception {
		String fullPath = "src/test/resources/mapper/FullFeaturesMapper.xml";
		assertNotNull(SqlExtractor.getQueryIdDetail("selectWithAllTags", fullPath));
		assertNotNull(SqlExtractor.getQueryIdDetail("insertPayment", fullPath));
		assertNotNull(SqlExtractor.getQueryIdDetail("updatePayment", fullPath));
		assertNotNull(SqlExtractor.getQueryIdDetail("deletePayment", fullPath));
		assertNull(SqlExtractor.getQueryIdDetail("unknownQuery", fullPath));
	}

	@Test
	@DisplayName("동적 쿼리 >> SQL 변경")
	void dynamicQueryChangeToSql() throws Exception {
		Node nodeList = SqlExtractor.getQueryIdDetail(queryId, mapperPath);
		Map<String, String> sqlSnippetRegistry = SqlExtractor.getSqlSnippetRegistry(mapperBaseDir);

		String namespace = nodeList.getOwnerDocument().getDocumentElement().getAttribute("namespace");
		String fakeSql = SqlExtractor.buildFakeSql(nodeList, true, namespace, sqlSnippetRegistry);

		net.sf.jsqlparser.statement.Statement statement = CCJSqlParserUtil.parse(fakeSql);
		TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
		Set<String> tableNames = new HashSet<>(tablesNamesFinder.getTableList(statement));

		assertEquals(3, tableNames.size());
	}

	@Test
	@DisplayName("buildFakeSql - 모든 태그(where, set, trim, choose, foreach, bind, include, cdata, default) 검증")
	void buildFakeSql_allTags() throws Exception {
		String fullPath = "src/test/resources/mapper/FullFeaturesMapper.xml";
		Map<String, String> sqlSnippetRegistry = SqlExtractor.getSqlSnippetRegistry(mapperBaseDir);

		// 1. selectWithAllTags with isForExplain = true
		Node node1 = SqlExtractor.getQueryIdDetail("selectWithAllTags", fullPath);
		String namespace = "io.github.sweetpark.sqlanalyzer.core.mapper.FullFeaturesMapper";
		String fakeSql1 = SqlExtractor.buildFakeSql(node1, true, namespace, sqlSnippetRegistry);
		assertNotNull(fakeSql1);
		assertTrue(fakeSql1.contains("WHERE 1=1"));
		assertTrue(fakeSql1.contains("payment_id"));
		assertTrue(fakeSql1.contains(
				"/* MISSING_INCLUDE: io.github.sweetpark.sqlanalyzer.core.mapper.FullFeaturesMapper.missingColumns */"));

		// 2. selectWithAllTags with isForExplain = false
		String fakeSql1NotExplain = SqlExtractor.buildFakeSql(node1, false, namespace, sqlSnippetRegistry);
		assertNotNull(fakeSql1NotExplain);

		// 3. selectWithTrimAndOtherwise
		Node node2 = SqlExtractor.getQueryIdDetail("selectWithTrimAndOtherwise", fullPath);
		String fakeSql2 = SqlExtractor.buildFakeSql(node2, true, namespace, sqlSnippetRegistry);
		assertNotNull(fakeSql2);
		assertTrue(fakeSql2.contains("WHERE status = 'ACTIVE'"));

		// 4. updatePayment (set tag)
		Node nodeUpdate = SqlExtractor.getQueryIdDetail("updatePayment", fullPath);
		String fakeSqlUpdate = SqlExtractor.buildFakeSql(nodeUpdate, true, namespace, sqlSnippetRegistry);
		assertNotNull(fakeSqlUpdate);
		assertTrue(fakeSqlUpdate.contains("SET"));
		assertTrue(fakeSqlUpdate.contains("amount = ?"));

		// 5. emptyWhereQuery
		Node nodeEmptyWhere = SqlExtractor.getQueryIdDetail("emptyWhereQuery", fullPath);
		String fakeSqlEmptyWhere = SqlExtractor.buildFakeSql(nodeEmptyWhere, true, namespace, sqlSnippetRegistry);
		assertNotNull(fakeSqlEmptyWhere);
		assertTrue(fakeSqlEmptyWhere.contains("WHERE 1=1"));

		// 6. Custom XML node with corrupted include XML and unknown/custom tag
		String customXml = """
				<select id="custom">
				    <unknownTag>custom content</unknownTag>
				    <include refid="corruptedSnippet" />
				</select>
				""";
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document customDoc = dbf.newDocumentBuilder()
				.parse(new ByteArrayInputStream(customXml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		Map<String, String> customRegistry = new HashMap<>(sqlSnippetRegistry);
		customRegistry.put("io.github.sweetpark.sqlanalyzer.core.mapper.FullFeaturesMapper.corruptedSnippet",
				"NOT_VALID_XML <<<");

		String fakeSqlCustom = SqlExtractor.buildFakeSql(customDoc.getDocumentElement(), true, namespace,
				customRegistry);
		assertNotNull(fakeSqlCustom);
		assertTrue(fakeSqlCustom.contains("/* ERROR_PARSING_INCLUDE:"));
		assertTrue(fakeSqlCustom.contains("custom content"));
	}

	@Test
	@DisplayName("refId <sql> 캐싱하기")
	void getRefIdCache() throws Exception {
		Map<String, String> sqlSnippetRegistry = SqlExtractor.getSqlSnippetRegistry(mapperBaseDir);
		assertFalse(sqlSnippetRegistry.isEmpty());
	}

	@Test
	@DisplayName("getSqlSnippetRegistry - 존재하지 않는 디렉토리 및 namespace 없는 XML")
	void getSqlSnippetRegistry_edgeCases() throws Exception {
		Path tempDir = Files.createTempDirectory("snippet-test");
		try {
			// Non-directory
			Map<String, String> nonDirResult = SqlExtractor.getSqlSnippetRegistry(tempDir.resolve("non_existent"));
			assertTrue(nonDirResult.isEmpty());

			// XML without namespace
			Path nonMapperXml = tempDir.resolve("not-mapper.xml");
			Files.writeString(nonMapperXml, "<configuration><settings/></configuration>");
			Map<String, String> result = SqlExtractor.getSqlSnippetRegistry(tempDir);
			assertTrue(result.isEmpty());
		} finally {
			try (Stream<Path> stream = Files.walk(tempDir)) {
				stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
		}
	}

	@Test
	@DisplayName("findMapperFiles - 여러 파일에 걸쳐 queryId 검색 성공")
	void findMapperFiles_multipleFiles() throws Exception {
		List<Path> result = SqlExtractor.findMapperFiles(mapperBaseDir, queryId);

		List<String> fileNames = result.stream().map(p -> p.getFileName().toString()).sorted().toList();

		assertEquals(4, result.size());
		assertEquals(List.of("SampleMapper.xml", "TestMapper.xml", "TestMapper2.xml", "TestMapper3.xml"), fileNames);
	}

	@Test
	@DisplayName("findMapperFiles - 존재하지 않는 queryId 는 빈 리스트 반환")
	void findMapperFiles_notFound() throws Exception {
		List<Path> result = SqlExtractor.findMapperFiles(mapperBaseDir, "nonExistentQueryId");
		assertTrue(result.isEmpty());
	}

	@Test
	@DisplayName("findMapperFiles - 존재하지 않는 디렉토리, 비매퍼 XML, 깨진 XML 스킵 검증")
	void findMapperFiles_edgeCases() throws Exception {
		List<Path> emptyResult = SqlExtractor.findMapperFiles(Path.of("non/existent/path"), queryId);
		assertTrue(emptyResult.isEmpty());

		Path tempDir = Files.createTempDirectory("mapper-edge-cases");
		try {
			// Non-mapper XML
			Files.writeString(tempDir.resolve("app-config.xml"), "<beans></beans>");
			// Corrupted XML
			Files.writeString(tempDir.resolve("corrupted.xml"), "<mapper namespace='test'><unclosed>");
			// Mapper with empty namespace
			Files.writeString(tempDir.resolve("empty-ns.xml"),
					"<mapper namespace=' '><select id='findBadPerformancePayments'/></mapper>");

			List<Path> result = SqlExtractor.findMapperFiles(tempDir, queryId);
			assertTrue(result.isEmpty());
		} finally {
			try (Stream<Path> stream = Files.walk(tempDir)) {
				stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			}
		}
	}

	@Test
	@DisplayName("nodeToString - 정상 및 예외 처리")
	void nodeToString_tests() throws Exception {
		Node node = SqlExtractor.getQueryIdDetail(queryId, mapperPath);
		String xmlString = SqlExtractor.nodeToString(node);
		assertNotNull(xmlString);
		assertTrue(xmlString.contains("findBadPerformancePayments"));

		Node brokenNode = (Node) Proxy.newProxyInstance(SqlExtractorTest.class.getClassLoader(),
				new Class<?>[]{Node.class}, (proxy, method, args) -> {
					throw new RuntimeException("Forced transformer error");
				});
		String errorString = SqlExtractor.nodeToString(brokenNode);
		assertEquals("", errorString);
	}
}
