package org.yyc.ignite.operator.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;

import static org.yyc.ignite.operator.api.utils.XmlUpdateUtils.parseXmlDocToString;

public final class TemplateFileLoadUtils {
    private static final ObjectMapper yamlObjectMapper;
    private static final DocumentBuilder documentBuilder;
    
    static {
        yamlObjectMapper = new ObjectMapper(new YAMLFactory());
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
    
    private TemplateFileLoadUtils() {
    }
    
    public static <T> T loadYamlTemplate(Class<T> clazz, String resource) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = TemplateFileLoadUtils.class.getClassLoader();
        }
        
        try (InputStream is = cl.getResourceAsStream(resource)) {
            return objectMapYamlTemplate(clazz, is);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load classpath resource '" + resource + "': " + ioe.getMessage());
        }
    }
    
    public static <T> T objectMapYamlTemplate(Class<T> clazz, InputStream is) throws IOException {
        return yamlObjectMapper.readValue(is, clazz);
    }
    
    public static String loadXmlTemplate(String resource) {
        ClassLoader cl = TemplateFileLoadUtils.class.getClassLoader();
        try (InputStream is = cl.getResourceAsStream(resource)) {
            Document document = documentBuilder.parse(is);
            return parseXmlDocToString(document);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to load classpath resource '" + resource + "': " + ioe.getMessage());
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
