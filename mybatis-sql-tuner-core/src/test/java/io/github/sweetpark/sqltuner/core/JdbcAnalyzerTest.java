package io.github.sweetpark.sqltuner.core;

import org.junit.jupiter.api.*;
import org.w3c.dom.Node;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class JdbcAnalyzerTest {

	private Connection connection;
	private static final String queryId = "findBadPerformancePayments";
	private final Path mapperPath = Path.of("src/test/resources/mapper/TestMapper.xml");
	private final Path mapperPathDir = Path.of("src/test/resources/mapper");

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
		Constructor<JdbcAnalyzer> constructor = JdbcAnalyzer.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		JdbcAnalyzer instance = constructor.newInstance();
		assertNotNull(instance);
	}

	@Test
	@DisplayName("EXPLAIN 실행")
	void doExplain() throws Exception {
		Node queryIdDetail = SqlExtractor.getQueryIdDetail(queryId, mapperPath);
		Map<String, String> sqlSnippetRegistry = SqlExtractor.getSqlSnippetRegistry(mapperPathDir);
		String namespace;
		String fakeSql = null;

		if (queryIdDetail != null) {
			namespace = queryIdDetail.getOwnerDocument().getDocumentElement().getAttribute("namespace");
			fakeSql = SqlExtractor.buildFakeSql(queryIdDetail, true, namespace, sqlSnippetRegistry);
		}

		assertNotNull(fakeSql);
		String explainInfo = JdbcAnalyzer.getExplainInfo(connection, fakeSql);
		assertNotNull(explainInfo);
	}

	@Test
	@DisplayName("getExplainInfo - 결과에 컬럼 헤더와 구분선이 포함되어야 함")
	void getExplainInfo_includesColumnHeaderAndSeparator() throws Exception {
		String fakeSql = "SELECT p.payment_id, p.amount FROM payments p WHERE p.amount > ?";
		String result = JdbcAnalyzer.getExplainInfo(connection, fakeSql);

		assertNotNull(result);
		assertFalse(result.isBlank(), "EXPLAIN 결과가 비어있으면 안 됩니다.");
		assertTrue(result.contains("-"), "컬럼 헤더와 데이터 사이의 구분선이 있어야 합니다.");
		assertTrue(result.lines().count() >= 3, "헤더 행 + 구분선 + 데이터 행 최소 3줄이어야 합니다.");
	}

	@Test
	@DisplayName("getExplainInfo - with null column values using proxy")
	void getExplainInfo_withNullValues() throws Exception {
		AtomicInteger nextCallCount = new AtomicInteger(0);

		ResultSet mockRs = createProxy(ResultSet.class, (proxy, method, args) -> {
			String name = method.getName();
			if ("next".equals(name)) {
				return nextCallCount.getAndIncrement() == 0;
			} else if ("getString".equals(name)) {
				int col = (int) args[0];
				return col == 1 ? null : "value2";
			} else if ("getMetaData".equals(name)) {
				return createProxy(ResultSetMetaData.class, (mProxy, mMethod, mArgs) -> {
					String mName = mMethod.getName();
					if ("getColumnCount".equals(mName))
						return 2;
					if ("getColumnName".equals(mName))
						return "col" + mArgs[0];
					return null;
				});
			} else if ("close".equals(name)) {
				return null;
			}
			return null;
		});

		PreparedStatement mockPstmt = createProxy(PreparedStatement.class, (proxy, method, args) -> {
			String name = method.getName();
			if ("executeQuery".equals(name))
				return mockRs;
			if ("setObject".equals(name) || "close".equals(name))
				return null;
			return null;
		});

		Connection mockConn = createProxy(Connection.class, (proxy, method, args) -> {
			String name = method.getName();
			if ("prepareStatement".equals(name))
				return mockPstmt;
			if ("close".equals(name))
				return null;
			return null;
		});

		String result = JdbcAnalyzer.getExplainInfo(mockConn, "SELECT ?");
		assertNotNull(result);
		assertTrue(result.contains("NULL"));
		assertTrue(result.contains("value2"));
	}

	@Test
	@DisplayName("Table 추출 및 null fakeSql 처리")
	void extractTables() throws Exception {
		Node queryIdDetail = SqlExtractor.getQueryIdDetail(queryId, mapperPath);
		Map<String, String> sqlSnippetRegistry = SqlExtractor.getSqlSnippetRegistry(mapperPathDir);

		String namespace = queryIdDetail.getOwnerDocument().getDocumentElement().getAttribute("namespace");
		String fakeSql = SqlExtractor.buildFakeSql(queryIdDetail, true, namespace, sqlSnippetRegistry);

		Set<String> tables = JdbcAnalyzer.extractTableMethod(fakeSql);
		assertEquals(3, tables.size());

		Set<String> nullResult = JdbcAnalyzer.extractTableMethod(null);
		assertTrue(nullResult.isEmpty());
	}

	@Test
	@DisplayName("추출된 Table에 대한 DDL 및 Index 정보 찾기")
	void extractDDLAndIndex() throws Exception {
		Node queryIdDetail = SqlExtractor.getQueryIdDetail(queryId, mapperPath);
		Map<String, String> sqlSnippetRegistry = SqlExtractor.getSqlSnippetRegistry(mapperPathDir);

		String namespace = queryIdDetail.getOwnerDocument().getDocumentElement().getAttribute("namespace");
		String fakeSql = SqlExtractor.buildFakeSql(queryIdDetail, true, namespace, sqlSnippetRegistry);

		Set<String> tables = JdbcAnalyzer.extractTableMethod(fakeSql);
		DatabaseMetaData metaData = connection.getMetaData();

		StringBuilder result = JdbcAnalyzer.getMetaDataInfo(tables, metaData);
		assertNotNull(result);
		assertTrue(result.toString().contains("[DATABASE INFO]"));
		assertTrue(result.toString().contains("[TABLE INFO]"));
	}

	@Test
	@DisplayName("MySQL / MariaDB 모드 및 null indexName 건너뛰기 테스트")
	void getMetaDataInfo_mysqlAndNullIndex() throws Exception {
		AtomicInteger showVarsNext = new AtomicInteger(0);
		AtomicInteger colsNext = new AtomicInteger(0);
		AtomicInteger idxNext = new AtomicInteger(0);

		ResultSet showVarsRs = createProxy(ResultSet.class, (proxy, method, args) -> {
			String name = method.getName();
			if ("next".equals(name))
				return showVarsNext.getAndIncrement() == 0;
			if ("getString".equals(name)) {
				if ("Variable_name".equals(args[0]))
					return "sql_mode=";
				if ("Value".equals(args[0]))
					return "STRICT_TRANS_TABLES\n";
			}
			if ("close".equals(name))
				return null;
			return null;
		});

		Statement mockStmt = createProxy(Statement.class, (proxy, method, args) -> {
			if ("executeQuery".equals(method.getName()))
				return showVarsRs;
			if ("close".equals(method.getName()))
				return null;
			return null;
		});

		ResultSet colsRs = createProxy(ResultSet.class, (proxy, method, args) -> {
			String name = method.getName();
			if ("next".equals(name))
				return colsNext.getAndIncrement() == 0;
			if ("getString".equals(name)) {
				if ("COLUMN_NAME".equals(args[0]))
					return "payment_id";
				if ("TYPE_NAME".equals(args[0]))
					return "VARCHAR";
				if ("COLUMN_SIZE".equals(args[0]))
					return "50";
			}
			if ("close".equals(name))
				return null;
			return null;
		});

		ResultSet idxRs = createProxy(ResultSet.class, (proxy, method, args) -> {
			String name = method.getName();
			if ("next".equals(name))
				return idxNext.getAndIncrement() < 2; // 2 rows
			if ("getString".equals(name)) {
				if ("INDEX_NAME".equals(args[0]))
					return idxNext.get() == 1 ? null : "PRIMARY";
				if ("COLUMN_NAME".equals(args[0]))
					return "payment_id";
			}
			if ("getBoolean".equals(name))
				return false;
			if ("close".equals(name))
				return null;
			return null;
		});

		Connection mockConn = createProxy(Connection.class, (proxy, method, args) -> {
			String name = method.getName();
			if ("createStatement".equals(name))
				return mockStmt;
			if ("getCatalog".equals(name))
				return "mydb";
			if ("close".equals(name))
				return null;
			return null;
		});

		DatabaseMetaData metaData = createProxy(DatabaseMetaData.class, (proxy, method, args) -> {
			String name = method.getName();
			if ("getDatabaseProductName".equals(name))
				return "MySQL";
			if ("getDatabaseProductVersion".equals(name))
				return "8.0.30";
			if ("getDriverName".equals(name))
				return "MySQL Connector/J";
			if ("getDriverVersion".equals(name))
				return "8.3.0";
			if ("getConnection".equals(name))
				return mockConn;
			if ("getTables".equals(name))
				return createProxy(ResultSet.class, (tProxy, tMethod, tArgs) -> {
					if ("next".equals(tMethod.getName()))
						return true;
					if ("getString".equals(tMethod.getName()) && "TABLE_NAME".equals(tArgs[0]))
						return "PAYMENTS";
					if ("close".equals(tMethod.getName()))
						return null;
					return null;
				});
			if ("getColumns".equals(name))
				return colsRs;
			if ("getIndexInfo".equals(name))
				return idxRs;
			return null;
		});

		StringBuilder result = JdbcAnalyzer.getMetaDataInfo(Set.of("PAYMENTS"), metaData);

		assertNotNull(result);
		assertTrue(result.toString().contains("MySQL"));
		assertTrue(result.toString().contains("STRICT_TRANS_TABLES"));
		assertTrue(result.toString().contains("payment_id"));
		assertTrue(result.toString().contains("PRIMARY"));
	}

	@Test
	@DisplayName("getMetaDataInfo - 테이블이 소문자로 저장된 DB(MySQL/PostgreSQL 등)에서도 컬럼/인덱스를 찾아야 함")
	void getMetaDataInfo_lowerCaseStoredTableName() throws Exception {
		ResultSet tablesRsUpper = createProxy(ResultSet.class, (proxy, method, args) -> {
			if ("next".equals(method.getName()))
				return false; // 대문자 후보: 테이블 없음 (실제로는 소문자로 저장됨)
			if ("close".equals(method.getName()))
				return null;
			return null;
		});

		ResultSet tablesRsLower = createProxy(ResultSet.class, (proxy, method, args) -> {
			if ("next".equals(method.getName()))
				return true; // 소문자 후보: 테이블 발견
			if ("getString".equals(method.getName()) && "TABLE_NAME".equals(args[0]))
				return "payments";
			if ("close".equals(method.getName()))
				return null;
			return null;
		});

		AtomicInteger colsNext = new AtomicInteger(0);
		ResultSet columnsRs = createProxy(ResultSet.class, (proxy, method, args) -> {
			String name = method.getName();
			if ("next".equals(name))
				return colsNext.getAndIncrement() == 0;
			if ("getString".equals(name)) {
				if ("COLUMN_NAME".equals(args[0]))
					return "payment_id";
				if ("TYPE_NAME".equals(args[0]))
					return "VARCHAR";
				if ("COLUMN_SIZE".equals(args[0]))
					return "50";
			}
			if ("close".equals(name))
				return null;
			return null;
		});

		ResultSet emptyRs = createProxy(ResultSet.class, (proxy, method, args) -> {
			if ("next".equals(method.getName()))
				return false;
			if ("close".equals(method.getName()))
				return null;
			return null;
		});

		Connection mockConn = createProxy(Connection.class, (proxy, method, args) -> {
			if ("getCatalog".equals(method.getName()))
				return "mydb";
			if ("close".equals(method.getName()))
				return null;
			return null;
		});

		DatabaseMetaData metaData = createProxy(DatabaseMetaData.class, (proxy, method, args) -> {
			String name = method.getName();
			if ("getDatabaseProductName".equals(name))
				return "PostgreSQL";
			if ("getDatabaseProductVersion".equals(name))
				return "16.0";
			if ("getDriverName".equals(name))
				return "PostgreSQL JDBC Driver";
			if ("getDriverVersion".equals(name))
				return "42.7.2";
			if ("getConnection".equals(name))
				return mockConn;
			if ("getTables".equals(name)) {
				String pattern = (String) args[2];
				if ("Payments".equals(pattern))
					return tablesRsUpper; // 원본 표기(첫 후보)로는 못 찾음
				if ("PAYMENTS".equals(pattern))
					return tablesRsUpper; // 대문자 후보로도 못 찾음
				return tablesRsLower; // 소문자 후보("payments")에서 발견
			}
			if ("getColumns".equals(name)) {
				String pattern = (String) args[2];
				return "payments".equals(pattern) ? columnsRs : emptyRs;
			}
			if ("getIndexInfo".equals(name))
				return emptyRs;
			return null;
		});

		StringBuilder result = JdbcAnalyzer.getMetaDataInfo(Set.of("Payments"), metaData);

		assertTrue(result.toString().contains("payment_id"), "소문자로 저장된 테이블도 컬럼 정보를 찾아야 합니다 (실제 MySQL/PostgreSQL 동작).");
	}

	@Test
	@DisplayName("getMetaDataInfo - getTables에서 테이블을 전혀 찾지 못하면 대문자 표기로 폴백해야 함")
	void getMetaDataInfo_tableNotFoundFallsBackToUpperCase() throws Exception {
		ResultSet emptyTablesRs = createProxy(ResultSet.class, (proxy, method, args) -> {
			if ("next".equals(method.getName()))
				return false;
			if ("close".equals(method.getName()))
				return null;
			return null;
		});

		ResultSet emptyRs = createProxy(ResultSet.class, (proxy, method, args) -> {
			if ("next".equals(method.getName()))
				return false;
			if ("close".equals(method.getName()))
				return null;
			return null;
		});

		Connection mockConn = createProxy(Connection.class, (proxy, method, args) -> {
			if ("getCatalog".equals(method.getName()))
				return "mydb";
			if ("close".equals(method.getName()))
				return null;
			return null;
		});

		DatabaseMetaData metaData = createProxy(DatabaseMetaData.class, (proxy, method, args) -> {
			String name = method.getName();
			if ("getDatabaseProductName".equals(name))
				return "H2";
			if ("getDatabaseProductVersion".equals(name))
				return "2.2.224";
			if ("getDriverName".equals(name))
				return "H2 JDBC Driver";
			if ("getDriverVersion".equals(name))
				return "2.2.224";
			if ("getConnection".equals(name))
				return mockConn;
			if ("getTables".equals(name))
				return emptyTablesRs;
			if ("getColumns".equals(name))
				return emptyRs;
			if ("getIndexInfo".equals(name))
				return emptyRs;
			return null;
		});

		StringBuilder result = JdbcAnalyzer.getMetaDataInfo(Set.of("ghost_table"), metaData);

		assertTrue(result.toString().contains("GHOST_TABLE"), "getTables 조회에 실패하면 기존 동작(대문자 변환)으로 폴백해야 합니다.");
	}

	@SuppressWarnings("unchecked")
	private static <T> T createProxy(Class<T> iface, InvocationHandler handler) {
		return (T) Proxy.newProxyInstance(JdbcAnalyzerTest.class.getClassLoader(), new Class<?>[]{iface}, handler);
	}
}
