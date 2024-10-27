package org.yyc.ignite.operator.api.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.yyc.ignite.operator.api.spec.IgniteConfigMapSpec;
import org.yyc.ignite.operator.api.spec.PersistenceSpec;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Objects;

import static org.yyc.ignite.operator.api.type.XmlTagEnum.*;

@Slf4j
public class XmlUpdateUtils {
    public static final String IGNITE_CONFIGURATION = "org.apache.ignite.configuration.IgniteConfiguration";
    public static final String IGNITE_DATA_STORAGE_CONFIGURATION = "org.apache.ignite.configuration.DataStorageConfiguration";
    private static final String IGNITE_DATA_REGION_CONFIG = "org.apache.ignite.configuration.DataRegionConfiguration";
    private static final String IGNITE_K8S_IP_FINDER = "org.apache.ignite.spi.discovery.tcp.ipfinder.kubernetes.TcpDiscoveryKubernetesIpFinder";
    
    public static String getDataRegionSizeFromXml(String data) {
        Document doc = parseStringToXmlDoc(data);
        doc.getDocumentElement().normalize();
        NodeList beanList = doc.getElementsByTagName(BEAN.tagValue());
        for (int i = 0; i < beanList.getLength(); i++) {
            Element bean = (Element) beanList.item(i);
            if (isRelationalDataRegion(bean)) {
                Element dataRegionProperty = getDataRegionProperty(bean);
                Objects.requireNonNull(dataRegionProperty, "dataRegionProperty cannot be null in the configmap data");
                return dataRegionProperty.getAttribute(VALUE.tagValue());
            }
        }
        return StringUtils.EMPTY;
    }
    
    // TODO: investigate a better way to manipulate xml file
    public static String updateConfigMapXmlData(String data,
                                                IgniteConfigMapSpec igniteConfigMapSpec,
                                                PersistenceSpec persistenceSpec,
                                                String igniteServiceName, String namespace) {
        if (Objects.nonNull(igniteConfigMapSpec.getConfigXmlOverride())
                && !igniteConfigMapSpec.getConfigXmlOverride().isBlank()) {
            // TODO: validate user input xml
            return igniteConfigMapSpec.getConfigXmlOverride();
        }
        
        Document doc = parseStringToXmlDoc(data);
        doc.getDocumentElement().normalize();
        NodeList beanList = doc.getElementsByTagName(BEAN.tagValue());
        for (int i = 0; i < beanList.getLength(); i++) {
            Element bean = (Element) beanList.item(i);
            if (isRelationalDataRegion(bean)) {
                updateDataRegionSize(igniteConfigMapSpec.getRelationalDataRegionSize(), bean);
            } else if (isDefaultDataRegion(bean)) {
                updateDataRegionSize(igniteConfigMapSpec.getDefaultDataRegionSize(), bean);
            } else if (isIgniteK8sIpFinder(bean)) {
                updateIgniteK8sServiceConfig(namespace, igniteServiceName, bean);
            }
        }
        
        if (persistenceSpec.isPersistenceEnabled()) {
            setIgniteDataPersistencePath(doc, persistenceSpec, beanList);
        }
        return parseXmlDocToString(doc);
    }
    
    public static Document parseStringToXmlDoc(String data) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(data)));
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Exception during parsing String to XML doc", e);
        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String parseXmlDocToString(Document doc) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource domSource = new DOMSource(doc);
            
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(domSource, result);
            
            return writer.toString();
        } catch (TransformerException e) {
            throw new RuntimeException("Exception during parsing XML doc to String", e);
        }
    }
    
    private static boolean isRelationalDataRegion(Element bean) {
        return IGNITE_DATA_REGION_CONFIG.equals(bean.getAttribute(CLASS.tagValue()))
                && "RELATIONAL_DATA_REGION".equals(getChildPropertyValue(bean, NAME.tagValue()));
    }
    
    private static boolean isDefaultDataRegion(Element bean) {
        return IGNITE_DATA_REGION_CONFIG.equals(bean.getAttribute(CLASS.tagValue()))
                && "DEFAULT_REGION".equals(getChildPropertyValue(bean, NAME.tagValue()));
    }
    
    private static boolean isIgniteK8sIpFinder(Element bean) {
        return IGNITE_K8S_IP_FINDER.equals(bean.getAttribute(CLASS.tagValue()));
    }
    
    private static void updateIgniteK8sServiceConfig(String namespace, String igniteServiceName, Element bean) {
        NodeList propertyList = bean.getElementsByTagName(PROPERTY.tagValue());
        for (int j = 0; j < propertyList.getLength(); j++) {
            Element property = (Element) propertyList.item(j);
            if ("namespace".equals(property.getAttribute(NAME.tagValue()))) {
                property.setAttribute(VALUE.tagValue(), namespace);
            } else if ("serviceName".equals(property.getAttribute(NAME.tagValue()))) {
                property.setAttribute(VALUE.tagValue(), igniteServiceName);
            }
        }
    }
    
    private static void updateDataRegionSize(String size, Element bean) {
        NodeList propertyList = bean.getElementsByTagName(PROPERTY.tagValue());
        for (int j = 0; j < propertyList.getLength(); j++) {
            Element property = (Element) propertyList.item(j);
            if ("initialSize".equals(property.getAttribute(NAME.tagValue()))
                    || "maxSize".equals(property.getAttribute(NAME.tagValue()))) {
                property.setAttribute(VALUE.tagValue(), String.format("#{%s}", size));
            }
        }
    }
    
    private static Element getDataRegionProperty(Element bean) {
        NodeList propertyList = bean.getElementsByTagName(PROPERTY.tagValue());
        for (int j = 0; j < propertyList.getLength(); j++) {
            Element property = (Element) propertyList.item(j);
            if ("initialSize".equals(property.getAttribute(NAME.tagValue()))
                    || "maxSize".equals(property.getAttribute(NAME.tagValue()))) {
                return property;
            }
        }
        return null;
    }
    
    private static String getChildPropertyValue(Element bean, String propertyName) {
        NodeList propertyList = bean.getElementsByTagName(PROPERTY.tagValue());
        for (int i = 0; i < propertyList.getLength(); i++) {
            Element property = (Element) propertyList.item(i);
            if (propertyName.equals(property.getAttribute(NAME.tagValue()))) {
                return property.getAttribute(VALUE.tagValue());
            }
        }
        return null;
    }
    
    private static void setIgniteDataPersistencePath(Document doc, PersistenceSpec persistenceSpec, NodeList beanList) {
        for (int i = 0; i < beanList.getLength(); i++) {
            Element bean = (Element) beanList.item(i);
            if (bean.getAttribute(CLASS.tagValue()).equals(IGNITE_CONFIGURATION)
                    && isChildPropertyAbsent("workDirectory", bean)) {
                appendProperty(doc, "workDirectory", persistenceSpec.getDataVolumeSpec().getMountPath(), bean);
            } else if (bean.getAttribute(CLASS.tagValue()).equals(IGNITE_DATA_STORAGE_CONFIGURATION)
                    && isChildPropertyAbsent("walPath", bean)
                    && isChildPropertyAbsent("walArchivePath", bean)) {
                appendProperty(doc, "walPath", persistenceSpec.getWalVolumeSpec().getMountPath(), bean);
                appendProperty(doc, "walArchivePath", persistenceSpec.getWalArchiveVolumeSpec().getMountPath(), bean);
            } else if (isRelationalDataRegion(bean)) {
                // TODO: for now only relational data region is allowed for persistence.
                turnOnRelationalDataRegionPersistence(bean);
            }
        }
    }
    
    private static boolean isChildPropertyAbsent(String propertyName, Element bean) {
        NodeList propertyList = bean.getElementsByTagName(PROPERTY.tagValue());
        for (int j = 0; j < propertyList.getLength(); j++) {
            Element property = (Element) propertyList.item(j);
            if (propertyName.equals(property.getAttribute(NAME.tagValue()))) {
                return false;
            }
        }
        return true;
    }
    
    private static void appendProperty(Document doc, String directoryName, String mountPath, Element bean) {
        Element workPathProperty = doc.createElement(PROPERTY.tagValue());
        workPathProperty.setAttribute(NAME.tagValue(), directoryName);
        workPathProperty.setAttribute(VALUE.tagValue(), mountPath);
        bean.appendChild(workPathProperty);
    }
    
    private static void turnOnRelationalDataRegionPersistence(Element bean) {
        NodeList propertyList = bean.getElementsByTagName(PROPERTY.tagValue());
        for (int j = 0; j < propertyList.getLength(); j++) {
            Element property = (Element) propertyList.item(j);
            if ("persistenceEnabled".equals(property.getAttribute(NAME.tagValue()))) {
                property.setAttribute(VALUE.tagValue(), "true");
            }
        }
    }
}
