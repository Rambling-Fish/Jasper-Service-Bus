package org.jasper.jLib.idar.video.codec;

import java.io.StringReader;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;



import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class IDARVideoDecoder {
    
	/*
	 * Each message has an extra 28 bytes, which is broken down as follows:
	 *   - Start token - 4 bytes
	 *   - Body Length - 4 bytes
	 *   - Sequence #  - 4 bytes
	 *   - TimeStamp   - 8 bytes
	 *   - Checksum    - 4 bytes
	 *   - End token   - 4 bytes
	 */  


	public IDARVideoMessage doDecode(byte[] byteArray) {
		if(byteArray == null) return null;
				
		byte[] jpegImage = setImage(byteArray);
		int imageSize = getImageSize(byteArray);
		String xmlMessage = completeXMLFormat(byteArray);	
		
		// For Debug purposes  
		// prints the content of the XML and compares the image size
		boolean debug = false;
		if (debug){
			System.out.println("Image's calculated size: " + jpegImage.length + " Image size from msg: " + imageSize);
			try {
				parseXMLFromString(xmlMessage);
			} catch (Exception e) {
				
				e.printStackTrace();
			}
		}
		
		//if (jpegImage != null && xmlMessage != null && imageSize == jpegImage.length){
		// TODO enable previous and delete the following line (Show all the frames)
		if (jpegImage != null && xmlMessage != null){	
			IDARVideoMessage ivm = new IDARVideoMessage();
			ivm.setIDARJpeg(jpegImage);
			ivm.setIntelliDARinfo(xmlMessage);
			return ivm;
		}
		
		
		return null;
	}
	
	private String completeXMLFormat(byte[] message) {
		
		/*
		 * Extracts the content of <IntelliDAR> </IntelliDAR>
		 *   And creates an XML envelope
		 *   <?xml version="1.0"?>
		 *   	<IntelliDAR> </IntelliDAR>
		 *   
		 */  
		
		String messageString = new String(message);
		String startIntelliDAR = "<IntelliDAR>";
		String endIntelliDAR   = "</IntelliDAR>";
		
		int msgIntelliDARstart = messageString.indexOf(startIntelliDAR);
		int msgIntelliDARend = messageString.indexOf(endIntelliDAR) + endIntelliDAR.length(); 
		
		
		String messageToComplete = messageString.substring(msgIntelliDARstart, msgIntelliDARend);
		
		return "<?xml version=\"1.0\"?>" + messageToComplete;
		
	}
	
	private static byte[] setImage(byte[] message){
		String messageString = new String(message);
		
		int start = messageString.indexOf(">") + 1;
		int end = messageString.indexOf("</JP>");
		return messageString.substring(start, end).getBytes();

	}
	
	private static int getImageSize(byte[] message){
		String messageString = new String(message);
		Pattern regex = Pattern.compile("<JP\\s+L=\"(\\d+)\">");
		Matcher regexMatcher = regex.matcher(messageString);
		
		if (regexMatcher.find()){
			return Integer.parseInt(regexMatcher.group(1));
		} else {
			System.out.println("No Match");
			return 0;
		}
	}
	
	private static void parseXMLFromString(String xml) throws Exception {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        Document doc = builder.parse(is);
        NodeList targetsList = doc.getElementsByTagName("TARGET");      
        Element targetElement = null;
        
        for (int i = 0; i < targetsList.getLength(); i++){
        	targetElement = (Element) targetsList.item(i);
        	NamedNodeMap targetElementAttributes = targetElement.getAttributes();
        	
        	for (int j = 0; j < targetElementAttributes.getLength(); j++){
        		Node attribute = targetElementAttributes.item(j);
        		System.out.println(attribute.getNodeName() + " = " + attribute.getNodeValue());
        		
        	}
        	
        	NodeList innerElementsList = targetElement.getChildNodes();
        	Element innerElement = null;
        	for (int j=0; j < innerElementsList.getLength(); j++){
        		
        		if (innerElementsList.item(j) instanceof Element){
        			innerElement = (Element) innerElementsList.item(j);
        			// Get MISC or BBOX
        			NamedNodeMap innerElementAttr = innerElement.getAttributes();
        			System.out.println("Tag Name: " + innerElement.getTagName());
        			for (int k = 0; k < innerElementAttr.getLength(); ++k){
                		// Get attributes and values
        				Node attr = innerElementAttr.item(k);
                		System.out.println(attr.getNodeName() + " = " + attr.getNodeValue());
                	}
        		}      		
        	}
        	
        }
         
    }


}
