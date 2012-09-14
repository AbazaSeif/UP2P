package up2p.xml.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Captures and filters attachment links that begin with the given prefix and
 * stores them as key/value pairs where the key is the attachment name and the
 * value is the complete attachment URL without the prefix. A 'path' attribute
 * associated with an element will be stored for lookup using the attachment
 * name, if it is found.
 * 
 * @author Neal Arthorne
 * @version 1.0
 */
public class AttachmentListFilter extends BaseResourceFilter {

    /**
     * Holds an element with its attributes for use in the element stack.
     * 
     * @author Neal Arthorne
     * @version 1.0
     */
    protected class ElementWithAtts {
        /** Element attributes. */
        public Attributes atts;

        /** Local name of the element. */
        public String localName;

        /** Namespace of the element. */
        public String namespaceURI;

        /** Qualified name of the element. */
        public String qName;

        /**
         * Creates an element holder with all its properties.
         * 
         * @param ns namespace
         * @param ln localname
         * @param qn qualified name
         * @param a attributes
         */
        public ElementWithAtts(String ns, String ln, String qn, Attributes a) {
            namespaceURI = ns;
            localName = ln;
            qName = qn;
            atts = a;
        }
    }

    /**
     * Property name where the <code>Map</code> containing the attachment list
     * is stored in the filter. Alternatively, the
     * <code>getAttachmentNames</code> method can be used to iterate over
     * attachment names.
     *  
     */
    public static final String ATTACH_LIST_PROPERTY = "attachmentListMap";

    /**
     * The default prefix used for attachments URIs.
     */
    public static final String DEFAULT_ATTACH_PREFIX = "file:";

    /** Attribute for path. */
    public static final String PATH_ATTRIBUTE = "path";

    /**
     * List of attachments links maintained in the order in which they appear in
     * the document. Duplicate attachment names are included but lookup of
     * attachment link by using its name will only return the first entry for
     * that name.
     * 
     * TODO: this seems to be not the pure attachment link but generated by a URL path
     */
    protected List<String> attachmentLinkList;

    /** Current attachment prefix to look for in XML data. */
    private String attachPrefix;

    /** Buffer used to hold character data. */
    private StringBuffer charBuffer;

    /** Stack used to track which element is currently open. */
    private Stack<ElementWithAtts> elementStack;

    /** A map of attachment names to attachment links. */
    protected Map<String,String> nameToLinkMap;

    /**
     * A map of attachment names to attachments paths. Only attachments found in
     * elements with an attribute 'path' are recorded in this map along with the
     * value of the 'path' attribute. Attachment links within attributes and
     * attachment links without a 'path' attribute are not recorded.
     */
    protected Map<String,String> nameToPathMap;

    /**
     * Constructs the filter with an empty attachment list.
     */
    public AttachmentListFilter() {
        this(DEFAULT_ATTACH_PREFIX);
    }

    /**
     * Constructs the filter with an empty attachment list.
     * 
     * @param attachmentPrefix prefix for attachments
     */
    public AttachmentListFilter(String attachmentPrefix) {
        super();
        attachmentLinkList = new ArrayList<String>();
        nameToLinkMap = new HashMap<String,String>();
        nameToPathMap = new HashMap<String,String>();
        elementStack = new Stack<ElementWithAtts>();
        charBuffer = new StringBuffer();
        attachPrefix = attachmentPrefix;
    }

    /*
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        // append characters to the character buffer
        charBuffer.append(ch, start, length);
        super.characters(ch, start, length);
    }

    /*
     * @see org.xml.sax.ContentHandler#endElement(String, String, String)
     */
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        // check character buffer to see if there is a prefix match
        String charData = charBuffer.toString().trim();
        if (charData.startsWith(attachPrefix)) {

            // attachment found in character data for the current element
            String attachName = processAttachment(charData);
            // check if current element has a path attribute
            String pathAtt = getCurrentPath();
            if (pathAtt != null) {
                nameToPathMap.put(attachName, pathAtt);
            }
        }
        // clear the character buffer
        charBuffer.setLength(0);
        // pop element
        elementStack.pop();
        super.endElement(namespaceURI, localName, qName);
    }

    /**
     * Returns the number of attachment found by the filter, including duplicate
     * attachment names.
     * 
     * @return the number of attachments
     */
    public int getAttachmentCount() {
        return attachmentLinkList.size();
    }

    /**
     * Returns the attachment URL or link with prefix intact.
     * 
     * @param attachName name of the attachment
     * @return attachment URL with prefix intact
     */
    public String getAttachmentLink(String attachName) {
        Object o = nameToLinkMap.get(attachName);
        if (o != null)
            return (String) o;
        return null;
    }

    /**
     * Returns a list of attachment links in the order in which they appear in
     * the document. Duplicate attachment links are allowed in this list.
     * 
     *NOTE sept 29th, 2008, Alan: seems that the list is in reverse order.
     * 
     * @return list of <code>String</code> attachment links
     */
    public List<String> getAttachmentList() {
        return attachmentLinkList;
    }

    /**
     * Returns an iterator for the list of attachment file names held in the
     * attachment map in no particular order.
     * 
     * @return a list of String attachment names
     */
    public Iterator<String> getAttachmentNames() {
        return nameToLinkMap.keySet().iterator();
    }

    /**
     * Returns the value of the 'path' attribute associated with an attachment
     * found in an element.
     * 
     * @param attachName file name of the attachment
     * @return the path or <code>null</code> if not found
     */
    public String getAttachmentPath(String attachName) {
        Object o = nameToPathMap.get(attachName);
        if (o != null)
            return (String) o;
        return null;
    }

    /**
     * Returns the prefix used to look for attachments.
     * 
     * @return attachment prefix
     */
    public String getAttachPrefix() {
        return attachPrefix;
    }

    /**
     * Returns the value of the 'path' attribute of the current element in the
     * element stack.
     * 
     * @return value of the 'path' attribute or <code>null</code> if not found
     */
    private String getCurrentPath() {
        ElementWithAtts top = (ElementWithAtts) elementStack.peek();
        String attsValue = top.atts.getValue(PATH_ATTRIBUTE);
        // return null for zero length string
        if (attsValue != null && attsValue.length() == 0)
            return null;
        return attsValue;
    }

    /**
     * Returns the map that connect attachment names to their links.
     * 
     * @return name to link attachment map
     */
    public Map<String,String> getNameToLinkMap() {
        return nameToLinkMap;
    }

    /**
     * Returns the number of unique attachment names. Attachment names should be
     * unique within a resource but duplicate names are permitted and recorded
     * in the list of attachment links. This method returns the number of
     * attachment names if those duplicates were omitted and only unique names
     * are considered.
     * 
     * @return number of unique attachment names.
     */
    public int getUniqueAttachmentCount() {
        return nameToLinkMap.size();
    }

    /**
     * Processes the full attachment link now that it has been buffered from one
     * or more calls to <code>characters</code>.
     * 
     * @param attachmentText the text containing the full link to the attachment
     * including the prefix
     * @return attachment name
     */
    protected String processAttachment(String attachmentText) {
    	// store the attachment name and text without the prefix!
        String attachName = attachmentText.substring(attachmentText
                .lastIndexOf("/") + 1);
        String attachNoPrefix = attachmentText.substring(attachPrefix.length());
        //remove up to 2 heading slashes:
        // *********WARNING! This may cause problems under UNIX!!!
        attachNoPrefix= attachNoPrefix.replaceFirst("^/{1,2}", "");
        // store in ordered list
        attachmentLinkList.add(attachNoPrefix);
                
        // store in map for lookup by attachment name
        if (!nameToLinkMap.containsKey(attachName))
            nameToLinkMap.put(attachName, attachNoPrefix);
        return attachName;
    }

    /*
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        chain.setProperty(ATTACH_LIST_PROPERTY, nameToLinkMap);
        super.startDocument();
    }

    /**
     * Process a start element event from the XML stream and check if the
     * attributes contain attachment links that start with the
     * <code>ATTACH_PREFIX</code> prefix. When an attachment link is found, it
     * is added to the list of attachments and <code>processAttachment</code>
     * is called for each attachment value.
     * 
     * @see org.xml.sax.ContentHandler#startElement(String, String, String,
     * org.xml.sax.Attributes)
     */
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts) throws SAXException {
        // check character buffer to see if there is a prefix match

    	if (charBuffer.length() > 0) {
            String charData = charBuffer.toString().trim();
            if (charData.startsWith(attachPrefix)) {
                // attachment found in character data
                processAttachment(charData);
            }
            // clear the character buffer
            charBuffer.setLength(0);
        }

        // push onto the stack to keep track of current element
        elementStack.push(new ElementWithAtts(namespaceURI, localName, qName,
                atts));

        // look for attachments in the attributes
        /*
         * Attributes may have whitespace-separated lists of attachments so we
         * parse the attribute value and process each one individually.
         */
        for (int i = 0; i < atts.getLength(); i++) {
            // get the attribute value
            String attrValue = atts.getValue(i);

            // check if it starts with the file:// prefix

            if (attrValue.startsWith(attachPrefix)) {
                // attachment(s) found
                // parse list of attachments
                StringTokenizer tokens = new StringTokenizer(attrValue);
                while (tokens.hasMoreTokens()) {
                    // process the attachment value
                    processAttachment(tokens.nextToken());
                }
            }
        }

        // return original event
        super.startElement(namespaceURI, localName, qName, atts);
    }
}