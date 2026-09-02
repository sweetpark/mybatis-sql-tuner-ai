package io.github.sweetpark.sqlanalyzer.intellij.toolwindow;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 단일 MyBatis 매퍼 XML 파일에서 DML 태그의 id 목록을 읽어온다.
 *
 * <p>
 * Tool Window의 queryId 드롭다운을 자동으로 구성할 때 사용한다. SqlAnalyzerPanel에서만 사용되는 패키지-프라이빗
 * 유틸리티 클래스다.
 *
 * <p>
 * 대상 태그: select, insert, update, delete (MyBatis DML 태그 전체)
 */
class XmlQueryIdReader {

	private static final String[] DML_TAGS = {"select", "insert", "update", "delete"};

	private XmlQueryIdReader() {
	}

	/**
	 * 지정된 XML 파일에서 DML 태그의 id 속성 목록을 반환한다.
	 *
	 * <p>
	 * MyBatis DTD 검증을 비활성화하여 오프라인 환경에서도 파싱이 가능하다.
	 *
	 * @param mapperFilePath
	 *            매퍼 XML 파일 경로
	 * @return id 목록 (태그 출현 순서대로 반환, 없으면 빈 리스트)
	 * @throws ParserConfigurationException
	 *             XML 파서 설정 오류 시
	 * @throws IOException
	 *             파일 읽기 실패 시
	 * @throws SAXException
	 *             XML 파싱 오류 시
	 */
	static List<String> readIds(Path mapperFilePath) throws ParserConfigurationException, IOException, SAXException {

		List<String> ids = new ArrayList<>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(mapperFilePath.toFile());

		for (String tag : DML_TAGS) {
			NodeList nodeList = doc.getElementsByTagName(tag);

			for (int i = 0; i < nodeList.getLength(); i++) {
				Node idAttr = nodeList.item(i).getAttributes().getNamedItem("id");

				if (idAttr != null) {
					ids.add(idAttr.getNodeValue());
				}
			}
		}

		return ids;
	}
}
