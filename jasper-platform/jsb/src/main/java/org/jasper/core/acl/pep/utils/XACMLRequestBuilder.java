package org.jasper.core.acl.pep.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.jasper.core.constants.JasperPEPConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Build XACML request
 */
public class XACMLRequestBuilder {
	private static final String TRUE  = "true";
	private static final String FALSE = "false";

    public Document createRequestElement(Set<AttributeValueDTO> attributeValues) throws Exception {

        Document doc = createNewDocument();
        Element requestElement = doc.createElement(JasperPEPConstants.REQUEST_ELEMENT);
        requestElement.setAttribute("xmlns", JasperPEPConstants.REQ_RES_CONTEXT);
        requestElement.setAttribute("CombinedDecision", "false");
        requestElement.setAttribute("ReturnPolicyIdList", "false");
       
        Element resourceElement = null;
        Element subjectElement = null;
        Element actionElement = null;
        String data = JasperPEPConstants.STRING_DATA_TYPE;

        if(attributeValues != null){            
            for(AttributeValueDTO attribute : attributeValues){
                if(JasperPEPConstants.CATEGORY_RESOURCE.equals(attribute.getCategory())){
                	resourceElement = doc.createElement(JasperPEPConstants.ATTRIBUTES);
                	resourceElement.setAttribute(JasperPEPConstants.CATEGORY, JasperPEPConstants.CATEGORY_RESOURCE);
                    resourceElement.appendChild(createRequestSubElement(attribute.getValue(), data, JasperPEPConstants.RESOURCE_ID, doc,TRUE));
                    
                    requestElement.appendChild(resourceElement); 
                }

                if(JasperPEPConstants.CATEGORY_ACTION.equals(attribute.getCategory())){
                	actionElement = doc.createElement(JasperPEPConstants.ATTRIBUTES);
                	actionElement.setAttribute(JasperPEPConstants.CATEGORY, JasperPEPConstants.CATEGORY_ACTION);
                    actionElement.appendChild(createRequestSubElement(attribute.getValue(), data, JasperPEPConstants.ACTION_ID,doc, FALSE));
                    
                    requestElement.appendChild(actionElement);
                }

                if(JasperPEPConstants.CATEGORY_SUBJECT.equals(attribute.getCategory())){
                	subjectElement =  doc.createElement(JasperPEPConstants.ATTRIBUTES);
                	subjectElement.setAttribute(JasperPEPConstants.CATEGORY, JasperPEPConstants.CATEGORY_SUBJECT);
                    subjectElement.appendChild(createRequestSubElement(attribute.getValue(), data, JasperPEPConstants.SUBJECT_ID, doc, FALSE));
                    
                    requestElement.appendChild(subjectElement);
                }
            }
        }

        if(resourceElement == null){
            resourceElement = doc.createElement(JasperPEPConstants.ATTRIBUTES);
            resourceElement.setAttribute(JasperPEPConstants.CATEGORY, JasperPEPConstants.CATEGORY_RESOURCE);
            requestElement.appendChild(resourceElement);
        }

        if(subjectElement == null){
            subjectElement = doc.createElement(JasperPEPConstants.ATTRIBUTES);
            subjectElement.setAttribute(JasperPEPConstants.CATEGORY, JasperPEPConstants.CATEGORY_SUBJECT);
            requestElement.appendChild(subjectElement);
        }

        if(actionElement == null){
            actionElement = doc.createElement(JasperPEPConstants.ATTRIBUTES);
            actionElement.setAttribute(JasperPEPConstants.CATEGORY, JasperPEPConstants.CATEGORY_ACTION);
            requestElement.appendChild(actionElement);
        }
         
        doc.appendChild(requestElement);

        return doc;
    }
    
	 /**
     * Builds an String representation of an XACML request to be sent to PDP server
     * @param String subject comma delineated list of 0 or more subjects
     * @param String resource comma delineated list of 0 or more resource URIs
     * @param String action comma delineated list of 0 or more actions
     * 
     * @return String representation of an XACML request or null on any error
     */
	public static String buildRequest(String subject, String resource, String action) {
		Set<AttributeValueDTO> valueDTOs = new HashSet<AttributeValueDTO>();
		XACMLRequestBuilder reqBuilder = new XACMLRequestBuilder();
		Set<AttributeValueDTO> subjectAttributes = reqBuilder.getSubjectAttributeValues(subject);
		Set<AttributeValueDTO> actionAttributes = reqBuilder.getActionAttributeValues(action);
        Set<AttributeValueDTO> resourceAttributes = reqBuilder.getResourceAttributeValues(resource);
        
        if(resourceAttributes.size() > 0){
        	valueDTOs.addAll(resourceAttributes);
        }
        if(subjectAttributes.size() > 0){
        	valueDTOs.addAll(subjectAttributes);
        }
        if(actionAttributes.size() > 0){
        	valueDTOs.addAll(actionAttributes);
        }
        
        try{
        	Document document = reqBuilder.createRequestElement(valueDTOs);
        	String request = reqBuilder.getStringFromDocument(document);
        	return request;
        }catch(Exception e){
        	return null;
        }

	}
	
	/* This method parses the XACML decision into a HashMap. The key to the map is the
    URI of the resource and the value is the XACML decision (e.g. Permit). This
    allows us the ability in the future to remove or anonymize data that the
    requester does not have permission to view
	 */
	public static Map<String,String> parseDecision(String decision) throws Exception{
		Map<String, String> result = new HashMap<String, String>();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse( new InputSource( new StringReader( decision ) ) );
		doc.getDocumentElement().normalize();
	
		NodeList decList = doc.getElementsByTagName("Decision");
		NodeList attList = doc.getElementsByTagName("AttributeValue");

		for (int temp = 0; temp < decList.getLength(); temp++) {
			Node dNode = decList.item(temp);
			Node aNode = attList.item(temp);
			result.put(aNode.getFirstChild().getTextContent(), dNode.getFirstChild().getTextContent());
		}
	
		return result;
	}
    
    public static String buildXACML3Request(Attribute[] attributes) {

        OMFactory factory = OMAbstractFactory.getOMFactory();

        OMElement requestXML = factory.createOMElement("Request", null);
        requestXML.addAttribute("xlmns", "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17", null);
        requestXML.addAttribute("CombinedDecision", "false", null);
        requestXML.addAttribute("ReturnPolicyIdList", "false", null);

        HashSet<String> catagorySet = new HashSet<String>();
        for (Attribute attribute : attributes) {
            if (!catagorySet.contains(attribute.getCategory())) {
                catagorySet.add(attribute.getCategory());
                OMElement attributesXML = factory.createOMElement("Attributes", null);
                attributesXML.addAttribute("Category", attribute.getCategory(), null);

                HashSet<String> attributeSet = new HashSet<String>();
                if (!attributeSet.contains(attribute.getId())) {
                    attributeSet.add(attribute.getId());
                    OMElement attributeXML = factory.createOMElement("Attribute", null);
                    attributeXML.addAttribute("AttributeId", attribute.getId(), null);
                    attributeXML.addAttribute("IncludeInResult", "true", null);

                    OMElement attributeValueXML = factory.createOMElement("AttributeValue", null);
                    attributeValueXML.addAttribute("DataType", "http://www.w3.org/2001/XMLSchema#" + attribute.getType(), null);
                    attributeValueXML.setText(attribute.getValue());
                    attributeXML.addChild(attributeValueXML);
                    attributesXML.addChild(attributeXML);
                } else {
                    Iterator itr = attributesXML.getChildElements();
                    while (itr.hasNext()) {
                        OMElement attributeXML = (OMElement) itr.next();
                        if (attribute.getId().equals(attributeXML.getAttributeValue(new QName("AttributeId")))) {
                            OMElement attributeValueXML = factory.createOMElement("AttributeValue", null);
                            attributeValueXML.addAttribute("DataType", "http://www.w3.org/2001/XMLSchema#" + attribute.getType(), null);
                            attributeValueXML.setText(attribute.getValue());
                            attributeXML.addChild(attributeValueXML);
                            break;
                        }
                    }
                }
                requestXML.addChild(attributesXML);
            } else {
                Iterator itr = requestXML.getChildElements();
                while (itr.hasNext()) {
                    OMElement attributesXML = (OMElement) itr.next();
                    if (attribute.getCategory().equals(attributesXML.getAttributeValue(new QName("Category")))) {
                        HashSet<String> attributeSet = new HashSet<String>();
                        Iterator itr1 = attributesXML.getChildElements();
                        while (itr1.hasNext()) {
                            attributeSet.add(((OMElement) itr1.next()).getAttributeValue(new QName("AttributeId")));
                        }

                        if (!attributeSet.contains(attribute.getId())) {
                            attributeSet.add(attribute.getId());
                            OMElement attributeXML = factory.createOMElement("Attribute", null);
                            attributeXML.addAttribute("AttributeId", attribute.getId(), null);
                            attributeXML.addAttribute("IncludeInResult", "true", null);

                            OMElement attributeValueXML = factory.createOMElement("AttributeValue", null);
                            attributeValueXML.addAttribute("DataType", "http://www.w3.org/2001/XMLSchema#" + attribute.getType(), null);
                            attributeValueXML.setText(attribute.getValue());
                            attributeXML.addChild(attributeValueXML);
                            attributesXML.addChild(attributeXML);
                        } else {
                            Iterator itr2 = attributesXML.getChildElements();
                            while (itr2.hasNext()) {
                                OMElement attributeXML = (OMElement) itr2.next();
                                if (attribute.getId().equals(attributeXML.getAttributeValue(new QName("AttributeId")))) {
                                    OMElement attributeValueXML = factory.createOMElement("AttributeValue", null);
                                    attributeValueXML.addAttribute("DataType", "http://www.w3.org/2001/XMLSchema#" + attribute.getType(), null);
                                    attributeValueXML.setText(attribute.getValue());
                                    attributeXML.addChild(attributeValueXML);
                                    break;
                                }
                            }
                        }
                        break;
                    }

                }
            }

        }
        return requestXML.toString();
    }


    public Element createRequestSubElement(String attributeValue, String data, String id, Document doc, String includeInResult){

        if(attributeValue != null) {
            Element attributeElement = doc.createElement(JasperPEPConstants.ATTRIBUTE);
            attributeElement.setAttribute(JasperPEPConstants.ATTRIBUTE_ID,
                                         id);
            attributeElement.setAttribute("IncludeInResult", includeInResult);            
            Element attributeValueElement = doc.createElement(JasperPEPConstants.ATTRIBUTE_VALUE);
            attributeValueElement.setAttribute(JasperPEPConstants.DATA_TYPE,
                 data);
            attributeValueElement.setTextContent(attributeValue.trim());
            attributeElement.appendChild(attributeValueElement);
            return attributeElement;
        }

        return null;
    }


    private Document createNewDocument() throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        Document doc = null;

        try {
            docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new Exception("Exception occurred while creating Document Object", e);
        }

        return doc;
    }
    
    /**
     * Convert comma separated String value to a Set<AttributeValueDTOA> for subject elements
     * @param String subjectValues
     * @return Set of AttributeValueDTOs containing 0 or more subjects
     */
    public Set<AttributeValueDTO> getSubjectAttributeValues(String subjectValues){
        Set<AttributeValueDTO> set = new HashSet<AttributeValueDTO>();
        if(subjectValues != null){
            String[] values = subjectValues.split(",");
            for(String value : values){
                AttributeValueDTO valueDTO = new AttributeValueDTO();
                valueDTO.setCategory(JasperPEPConstants.CATEGORY_SUBJECT);
                valueDTO.setValue(value.trim());
                set.add(valueDTO);
            }
        }

        return set;
    }
    
    /**
     * Convert comma separated String value to a Set<AttributeValueDTOA> for resource elements
     * @param String resourceValues
     * @return Set of AttributeValueDTOs containing 0 or more resources
     */
	public Set<AttributeValueDTO> getResourceAttributeValues(String resourceValues){

        Set<AttributeValueDTO> set = new HashSet<AttributeValueDTO>();
        if(resourceValues != null){
            String[] values = resourceValues.split(",");
            for(String value : values){
                AttributeValueDTO valueDTO = new AttributeValueDTO();
                valueDTO.setCategory(JasperPEPConstants.CATEGORY_RESOURCE);
                valueDTO.setValue(value.trim());
                set.add(valueDTO);
            }
        }

        return set;
    }
	
	/**
     * Convert comma separated String value to a Set<AttributeValueDTOA> for action elements
     * @param String actionValues
     * @return Set of AttributeValueDTOs containing 0 or more actions
     */
    public Set<AttributeValueDTO> getActionAttributeValues(String actionValues){

        Set<AttributeValueDTO> set = new HashSet<AttributeValueDTO>();
        if(actionValues != null){
            String[] values = actionValues.split(",");
            for(String value : values){
                AttributeValueDTO valueDTO = new AttributeValueDTO();
                valueDTO.setCategory(JasperPEPConstants.CATEGORY_ACTION);
                valueDTO.setValue(value.trim());
                set.add(valueDTO);
            }
        }

        return set;
    }

    /**
     * Convert XACML policy Document element to a String object
     * @param doc Document element
     * @return String XACML policy
     * @throws Exception
     */
    public String getStringFromDocument(Document doc) throws Exception {
        try {
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString().substring(writer.toString().indexOf('>') + 1);

        } catch(TransformerException e){
            throw new Exception("Exception occurred while transforming XACML request to String", e);
        }
    }
    
}