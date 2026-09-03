package io.github.sweetpark.sqltuner.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SqlExtractor {

	private static final Logger log = LoggerFactory.getLogger(SqlExtractor.class);

	private SqlExtractor() {
	}

	// 가짜 sql 생성
	public static String buildFakeSql(Node nodeList, boolean isForExplain, String currentNamespace,
			Map<String, String> sqlSnippetRegistry) {
		NodeList childNodes = nodeList.getChildNodes();
		StringBuilder fakeSql = new StringBuilder();

		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);

			// 1. 텍스트 노드 및 CDATA 처리
			if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
				String text = child.getNodeValue();
				if (text != null) {
					// #{} 와 ${} 를 ? 로 치환
					fakeSql.append(text.replaceAll("[#$]\\{[^}]+\\}", "?")).append(" ");
				}
			}
			// 2. 엘리먼트 노드(MyBatis 태그) 처리
			else if (child.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = child.getNodeName().toLowerCase();
				switch (nodeName) {
					case "where" :
						fakeSql.append(" WHERE 1=1 ");
						String whereContent = buildFakeSql(child, isForExplain, currentNamespace, sqlSnippetRegistry);
						// 앞부분의 AND나 OR를 제거 (JSqlParser 에러 방지용)
						String cleanedWhere = whereContent.trim().replaceAll("(?i)^(and|or)\\s+", "");
						if (!cleanedWhere.isEmpty()) {
							fakeSql.append(" AND ( ").append(cleanedWhere).append(" ) ");
						}
						break;
					case "set" :
						fakeSql.append(" SET ");
						String setContent = buildFakeSql(child, isForExplain, currentNamespace, sqlSnippetRegistry)
								.trim();
						if (setContent.endsWith(",")) {
							setContent = setContent.substring(0, setContent.length() - 1);
						}
						fakeSql.append(setContent).append(" ");
						break;
					case "foreach" :
						fakeSql.append(" ( ? ) ");
						break;
					case "choose" :
						if (isForExplain) {
							Node targetNode = null;
							NodeList chooseChildren = child.getChildNodes();
							for (int j = 0; j < chooseChildren.getLength(); j++) {
								Node c = chooseChildren.item(j);
								if (c.getNodeType() == Node.ELEMENT_NODE) {
									if ("when".equalsIgnoreCase(c.getNodeName())) {
										if (targetNode == null)
											targetNode = c;
									} else if ("otherwise".equalsIgnoreCase(c.getNodeName())) {
										targetNode = c;
										break;
									}
								}
							}
							if (targetNode != null) {
								fakeSql.append(" ")
										.append(buildFakeSql(targetNode, true, currentNamespace, sqlSnippetRegistry))
										.append(" ");
							}
						} else {
							fakeSql.append(" ").append(buildFakeSql(child, false, currentNamespace, sqlSnippetRegistry))
									.append(" ");
						}
						break;
					case "if", "when", "otherwise" :
						fakeSql.append(" ")
								.append(buildFakeSql(child, isForExplain, currentNamespace, sqlSnippetRegistry))
								.append(" ");
						break;
					case "trim" :
						Element trimElem = (Element) child;
						String prefix = trimElem.getAttribute("prefix");
						String prefixOverrides = trimElem.getAttribute("prefixOverrides");

						String trimContent = buildFakeSql(child, isForExplain, currentNamespace, sqlSnippetRegistry)
								.trim();
						if (!prefixOverrides.isEmpty()) {
							String[] overrides = prefixOverrides.split("\\|");
							for (String ov : overrides) {
								trimContent = trimContent.replaceAll("(?i)^" + Pattern.quote(ov.trim()) + "\\s+", "");
							}
						}
						fakeSql.append(" ").append(prefix).append(" ").append(trimContent).append(" ");
						break;
					case "bind" :
						break;
					case "include" :
						String refid = ((Element) child).getAttribute("refid");
						String fqn = refid.contains(".") ? refid : currentNamespace + "." + refid;

						String rawSqlXml = sqlSnippetRegistry.get(fqn);
						if (rawSqlXml != null) {
							try {
								DocumentBuilder db = createSecureFactory().newDocumentBuilder();
								Document parse = db
										.parse(new ByteArrayInputStream(rawSqlXml.getBytes(StandardCharsets.UTF_8)));

								// <sql> 태그의 내용을 다시 buildFakeSql로 처리 (isForExplain 유지)
								fakeSql.append(" ").append(buildFakeSql(parse.getDocumentElement(), isForExplain,
										currentNamespace, sqlSnippetRegistry)).append(" ");
							} catch (Exception e) {
								fakeSql.append(" /* ERROR_PARSING_INCLUDE: ").append(fqn).append(" */ ");
							}
						} else {
							fakeSql.append(" /* MISSING_INCLUDE: ").append(fqn).append(" */ ");
						}
						break;
					default :
						fakeSql.append(buildFakeSql(child, isForExplain, currentNamespace, sqlSnippetRegistry));
						break;
				}
			}
		}

		return fakeSql.toString();
	}

	/**
	 * XXE(XML External Entity) 공격을 차단하는 안전한 DocumentBuilderFactory를 생성한다.
	 *
	 * <p>
	 * DTD 검증 비활성화만으로는 부족하다: 외부 엔티티 참조가 허용되면 공격자가 XML 파일을 통해 서버 파일 시스템 읽기나 SSRF 공격을
	 * 시도할 수 있다. IntelliJ 플러그인에서 외부 경로의 XML을 파싱하는 특성상 반드시 적용해야 한다.
	 *
	 * @return XXE 차단 설정이 적용된 DocumentBuilderFactory
	 * @throws ParserConfigurationException
	 *             파서 설정 실패 시
	 */
	private static DocumentBuilderFactory createSecureFactory() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		return factory;
	}

	/**
	 * 지정된 디렉토리에서 특정 queryId를 포함하는 MyBatis 매퍼 XML 파일 목록을 반환한다.
	 *
	 * <p>
	 * queryId는 select, insert, update, delete 태그의 id 속성으로 탐색한다. 동일한 queryId가 여러 파일에
	 * 분산될 수 있으므로 List로 반환한다. IntelliJ 플러그인의 SqlTunerService에서 매퍼 파일 선택 UI를 구성할 때도
	 * 사용한다.
	 *
	 * @param mapperBaseDir
	 *            매퍼 XML 파일들이 위치한 루트 디렉토리
	 * @param queryId
	 *            탐색할 MyBatis 쿼리 ID (예: "findBadPerformancePayments")
	 * @return queryId를 포함하는 XML 파일 경로 목록 (없으면 빈 List 반환, null 반환 없음)
	 * @throws IOException
	 *             파일 탐색 중 IO 오류 발생 시
	 * @throws ParserConfigurationException
	 *             XML 파서 설정 오류 시
	 * @throws SAXException
	 *             XML 파싱 오류 시
	 */
	public static List<Path> findMapperFiles(Path mapperBaseDir, String queryId)
			throws IOException, ParserConfigurationException, SAXException {

		List<Path> matchedPaths = new ArrayList<>();

		if (!Files.isDirectory(mapperBaseDir)) {
			// 디렉토리가 없으면 조용히 빈 리스트를 반환 (예외 대신 경고 로그)
			log.warn("매퍼 디렉토리가 존재하지 않습니다: {}", mapperBaseDir);
			return matchedPaths;
		}

		// XML 파서 사전 설정: XXE 차단 및 오프라인 환경에서 DTD 네트워크 요청 방지
		DocumentBuilder builder = createSecureFactory().newDocumentBuilder();

		String[] targetTags = {"select", "insert", "update", "delete"};

		// .xml 확장자를 가진 파일만 탐색 (하위 디렉토리 포함)
		List<File> xmlFiles;
		try (Stream<Path> paths = Files.walk(mapperBaseDir)) {
			xmlFiles = paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".xml")).map(Path::toFile)
					.toList();
		}

		for (File xmlFile : xmlFiles) {
			try {
				Document document = builder.parse(xmlFile);
				document.getDocumentElement().normalize();

				// MyBatis mapper 파일이 아닌 XML(Spring 설정, Ant 빌드 등)은 오탐 방지를 위해 스킵
				// getSqlSnippetRegistry()와 동일한 기준: 루트 태그가 <mapper>이고 namespace 속성이 있어야 함
				Element rootElement = document.getDocumentElement();
				if (!"mapper".equalsIgnoreCase(rootElement.getTagName())
						|| rootElement.getAttribute("namespace").trim().isEmpty()) {
					continue;
				}

				// 파일 하나에서 queryId를 찾으면 즉시 다음 파일로 이동 (중복 추가 방지)
				boolean found = false;
				for (String tagName : targetTags) {
					NodeList nodeList = document.getElementsByTagName(tagName);

					for (int i = 0; i < nodeList.getLength(); i++) {
						Node idAttr = nodeList.item(i).getAttributes().getNamedItem("id");
						if (idAttr != null && queryId.equals(idAttr.getNodeValue())) {
							matchedPaths.add(xmlFile.toPath());
							found = true;
							break;
						}
					}

					if (found) {
						break;
					}
				}
			} catch (SAXException e) {
				// 단일 파일 파싱 오류는 스킵하되 경고 로그로 추적 가능하게 유지
				log.warn("XML 파싱 실패, 스킵합니다: {} - {}", xmlFile.getName(), e.getMessage());
			}
		}

		return matchedPaths;
	}

	/**
	 * queryId에 해당하는 MyBatis 매퍼 XML 태그 블록의 DOM Node를 반환한다.
	 *
	 * <p>
	 * select, delete, update, insert 순서로 탐색하며, 가장 먼저 일치하는 Node를 반환한다. 일치하는 queryId가
	 * 없을 경우 null을 반환하므로 호출자는 null 체크를 해야 한다.
	 *
	 * @param queryId
	 *            분석 대상 쿼리 ID
	 * @param mapperPath
	 *            매퍼 XML 파일 경로 (String 타입)
	 * @return 해당 queryId의 DOM Node (없으면 null)
	 * @throws ParserConfigurationException
	 *             XML 파서 설정 오류 시
	 * @throws SAXException
	 *             XML 파싱 오류 시
	 * @throws IOException
	 *             파일 읽기 실패 시
	 */
	public static Node getQueryIdDetail(String queryId, String mapperPath)
			throws ParserConfigurationException, SAXException, IOException {

		// XXE 차단 및 DTD 검증 비활성화가 적용된 안전한 파서 사용
		DocumentBuilder builder = createSecureFactory().newDocumentBuilder();
		Document document = builder.parse(mapperPath);

		String[] targetTags = {"select", "delete", "update", "insert"};
		for (String targetTag : targetTags) {
			NodeList nodeList = document.getElementsByTagName(targetTag);

			for (int i = 0; i < nodeList.getLength(); i++) {
				if (queryId.equals(nodeList.item(i).getAttributes().getNamedItem("id").getNodeValue())) {
					return nodeList.item(i);
				}
			}

		}

		return null;
	}

	/**
	 * {@link #getQueryIdDetail(String, String)}의 Path 타입 오버로드.
	 *
	 * <p>
	 * IntelliJ 플러그인 등 Path 기반 코드에서 {@code toString()} 변환 없이 바로 호출할 수 있도록 편의 메서드로
	 * 제공한다. 내부적으로 기존 String 버전에 위임하므로 로직 중복이 없다.
	 *
	 * @param queryId
	 *            분석 대상 쿼리 ID
	 * @param mapperPath
	 *            매퍼 XML 파일 경로 (Path 타입)
	 * @return 해당 queryId의 DOM Node (없으면 null)
	 * @throws ParserConfigurationException
	 *             XML 파서 설정 오류 시
	 * @throws SAXException
	 *             XML 파싱 오류 시
	 * @throws IOException
	 *             파일 읽기 실패 시
	 */
	public static Node getQueryIdDetail(String queryId, Path mapperPath)
			throws ParserConfigurationException, SAXException, IOException {
		// Path를 String으로 변환하여 기존 구현 재사용 (로직 중복 방지)
		return getQueryIdDetail(queryId, mapperPath.toString());
	}

	// <sql> 태그 캐싱처리
	public static Map<String, String> getSqlSnippetRegistry(Path mapperBaseDir)
			throws IOException, ParserConfigurationException, SAXException, TransformerException {
		Map<String, String> sqlSnippetRegistry = new HashMap<>();
		// 1. 매퍼 디렉토리 존재 확인
		if (!Files.isDirectory(mapperBaseDir)) {
			log.info("매퍼 디렉토리가 존재하지 않습니다.");
			return sqlSnippetRegistry;
		}

		// 2. xml 파일 리스트 찾기
		List<File> xmlFileList;
		try (Stream<Path> paths = Files.walk(mapperBaseDir)) {
			xmlFileList = paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith("xml"))
					.map(Path::toFile).toList();
		}

		// 3. <sql> 찾기 — XXE 차단 설정이 포함된 안전한 파서 사용
		DocumentBuilder builder = createSecureFactory().newDocumentBuilder();
		for (File xmlFile : xmlFileList) {
			Document doc = builder.parse(xmlFile);
			doc.getDocumentElement().normalize();

			// [핵심 1] 루트 태그(<mapper>)에서 namespace 속성 추출
			Element mapperElement = doc.getDocumentElement();
			String namespace = mapperElement.getAttribute("namespace");

			// namespace가 없는 xml(예: 설정파일 등)은 스킵
			if (namespace.trim().isEmpty())
				continue;

			// <sql> 태그 찾기
			NodeList sqlNodes = doc.getElementsByTagName("sql");
			for (int i = 0; i < sqlNodes.getLength(); i++) {
				Element sqlElement = (Element) sqlNodes.item(i);

				// [핵심 2] sql 태그의 id 추출하여 풀네임(FQN) Key 생성
				String id = sqlElement.getAttribute("id");
				String fqn = namespace + "." + id;

				// [핵심 3] 내부 태그 유지를 위해 Node를 순수 XML 문자열로 변환 (메모리 효율을 위해 String 저장)
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

				StringWriter stringWriter = new StringWriter();
				transformer.transform(new DOMSource(sqlElement), new StreamResult(stringWriter));

				String rawSqlXml = stringWriter.toString();

				sqlSnippetRegistry.put(fqn, rawSqlXml);
			}

		}
		return sqlSnippetRegistry;
	}

	public static String nodeToString(Node node) {
		StringWriter stringWriter = new StringWriter();
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();

			// XML 선언부 제외
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

			// 출력시 정렬
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			transformer.transform(new DOMSource(node), new StreamResult(stringWriter));
		} catch (TransformerException e) {
			log.error(e.getMessage(), e);
		}
		return stringWriter.toString();
	}
}
