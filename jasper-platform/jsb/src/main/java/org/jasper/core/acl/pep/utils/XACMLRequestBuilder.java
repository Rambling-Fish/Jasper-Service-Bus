package org.jasper.core.acl.pep.utils;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jasper.core.constants.JasperPEPConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
                    subjectElement.appendChild(createRequestSubElement(attribute.getValue(), data, JasperPEPConstants.SUBJECT_ID_DEFAULT, doc, FALSE));
                    
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