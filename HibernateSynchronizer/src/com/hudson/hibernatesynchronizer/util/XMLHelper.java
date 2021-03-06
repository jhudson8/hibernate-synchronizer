/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hudson.hibernatesynchronizer.parser.HibernateDOMParser;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class XMLHelper {

    private PrintWriter out;

    private static final char NL = '\n';

    private static final String NO_NL = "NoNL";

    public XMLHelper(PrintWriter out) {
        this.out = out;
    }

    public void printNode(Node node, String indent) {
        // determine the type of node
        // print the node
        // recurse on children
        printTree(node, indent);
    }

    public void printNodes(NodeList nl, String indent) {
        for (int i = 0; i < nl.getLength(); i++) {
            printTree(nl.item(i), indent);
        }
    }

    public void printNodes(List nodes, String indent) {
        for (Iterator i = nodes.iterator(); i.hasNext();) {
            printTree((Node) i.next(), indent);
        }
    }

    public void printTree(Node node, HibernateDOMParser parser,
            InputStream source) {
        int lineNumber = parser.getFirstNodeLineNumber().intValue() - 2;
        if (lineNumber > 0) {
            int linesRead = 0;
            StringBuffer sb = new StringBuffer();
            int charInt;
            InputStreamReader reader = new InputStreamReader(source);
            try {
                while ((charInt = reader.read()) != -1) {
                    char c = (char) charInt;
                    if (c == NL) {
                        linesRead++;
                        if (linesRead >= lineNumber) {
                            sb.append(c);
                            break;
                        }
                    }
                    sb.append(c);
                }
            } catch (IOException e) {
            } finally {
                try {
                    source.close();
                } catch (IOException ioe) {
                }
            }
            out.write(sb.toString().toCharArray());
        }
        printTree(node, "", null, false);
    }

    public void printTree(Node node, String indent) {
        printTree(node, indent, null, true);
    }

    private String printTree(Node node, String indent, String lastNode,
            boolean showHeader) {
        if (null != node) {
            switch (node.getNodeType()) {
            // print document node
            case Node.DOCUMENT_NODE:
                // print the contents of the Document node
                if (showHeader) {
                    out.println("<?xml version=\"1.0\"?>\n");
                }
                NodeList nodes = node.getChildNodes();
                if (nodes != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        printNode(nodes.item(i), "");
                    }
                }
                break;

            case Node.ELEMENT_NODE:
                // print element and atributes
                String name = node.getNodeName();
                if (!name.equals(lastNode) && !NO_NL.equals(lastNode)) {
                    out.print("\n");
                    lastNode = name;
                } else if (NO_NL.equals(lastNode)) {
                    lastNode = name;
                }
                out.print(indent + "<" + name);

                // print attributes
                NamedNodeMap attributes = node.getAttributes();
                if (attributes.getLength() <= 2) {
                    for (int i = 0; i < attributes.getLength(); i++) {
                        Node current = attributes.item(i);
                        out.print(" " + current.getNodeName() + "=\""
                                + current.getNodeValue() + "\"");
                    }
                } else {
                    for (int i = 0; i < attributes.getLength(); i++) {
                        Node current = attributes.item(i);
                        out.print("\n\t" + indent + current.getNodeName()
                                + "=\"" + current.getNodeValue() + "\"");
                    }
                    out.print("\n" + indent);
                }

                // recurse on each child
                NodeList children = node.getChildNodes();
                if (children != null) {
                    boolean sameLine = false;
                    if (children.getLength() == 0) {
                        sameLine = true;
                        out.println(" />");
                    } else if (children.getLength() == 1
                            && (children.item(0).getNodeType() == Node.CDATA_SECTION_NODE || children
                                    .item(0).getNodeType() == Node.TEXT_NODE)) {
                        String contents = children.item(0).getNodeValue()
                                .trim();
                        if (contents.length() == 0) {
                            sameLine = true;
                            out.println(" />");
                        } else if (contents.length() <= 20
                                && contents.indexOf("\n") == -1) {
                            sameLine = true;
                            out.print(">");
                            out.print(contents);
                            out.println("</" + name + ">");
                        }
                    }
                    if (!sameLine) {
                        out.println(">");
                        int elementNodes = 0;
                        for (int i = 0; i < children.getLength(); i++) {
                            if (children.item(i).getNodeType() == Node.ELEMENT_NODE)
                                elementNodes++;
                        }
                        for (int i = 0; i < children.getLength(); i++) {
                            if (i == 0 || elementNodes <= 4) {
                                lastNode = printTree(children.item(i), indent
                                        + "\t", NO_NL, showHeader);
                            } else {
                                lastNode = printTree(children.item(i), indent
                                        + "\t", lastNode, showHeader);
                            }
                        }
                        out.println(indent + "</" + name + ">");
                    }
                }
                break;

            case Node.TEXT_NODE:
                printTextNode(node, indent);
                break;
            case Node.CDATA_SECTION_NODE:
                out.println(indent + "<![CDATA[");
                printTextNode(node, indent + "\t");
                out.println(indent + "]]>");
                break;

            case Node.PROCESSING_INSTRUCTION_NODE:
                break;

            case Node.ENTITY_REFERENCE_NODE:
                break;

            case Node.DOCUMENT_TYPE_NODE:
                break;

            case Node.COMMENT_NODE:
                String text = node.getNodeValue().trim();
                if (text.indexOf('\n') < 0) {
                    out.println(indent + "<!-- " + text + " -->");
                } else {
                    StringTokenizer st = new StringTokenizer(text, "\n");
                    String[] arr = new String[st.countTokens()];
                    for (int i = 0; st.hasMoreTokens(); i++) {
                        arr[i] = st.nextToken().trim();
                    }
                    if (arr.length == 2 && arr[1].equals("//")) {
                        out.println(indent + "<!-- " + arr[0] + " -->");
                    } else {
                        out.println(indent + "<!--");
                        for (int i = 0; i < arr.length; i++) {
                            if (i != arr.length - 1 || !arr[i].equals("//"))
                                out.println(indent + "\t" + arr[i]);
                        }
                        out.println(indent + "//-->");
                    }
                }
                return NO_NL;

            default:
                break;
            }
        }
        return lastNode;
    }

    private String printTextNode(Node node, String indent) {
        //print text
        String content = node.getNodeValue();
        StringBuffer output = new StringBuffer();
        StringBuffer whitespace = new StringBuffer();
        for (int i = 0; i < content.length(); i++) {
            if (Character.isWhitespace(content.toCharArray()[i])) {
                if (content.toCharArray()[i] == NL)
                    whitespace = new StringBuffer();
                else
                    whitespace.append(content.toCharArray()[i]);
            } else {
                break;
            }
        }
        String whitespaceStr = whitespace.toString();
        content = content.trim();
        if (content.length() > 0) {
            StringTokenizer st = new StringTokenizer(content, "\n");
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                if (s.startsWith(whitespaceStr)) {
                    s = s.substring(whitespace.length(), s.length());
                } else {
                    s = s.trim();
                }
                out.println(indent + s);
            }
        }
        return output.toString();
    }
}

