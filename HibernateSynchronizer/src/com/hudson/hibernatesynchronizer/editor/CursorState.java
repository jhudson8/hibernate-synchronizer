/**
 * This software is licensed under the general public license.  See http://www.gnu.org/copyleft/gpl.html
 * for more information.
 */
package com.hudson.hibernatesynchronizer.editor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.TimeZone;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.contentassist.CompletionProposal;

/**
 * @author <a href="mailto: joe@binamics.com">Joe Hudson </a>
 */
public class CursorState {

    private static final char LT = '<';

    private static final char GT = '>';

    private static final char EXC = '!';

    private static final char QST = '?';

    private static final char EQL = '=';

    private static final char DASH = '-';

    private static final char USC = '_';

    private static final char SPACE = ' ';

    private static final char QUOTE = '"';

    private static final char SLASH = '/';

    private static final char RTN = '\r';

    private static final char NLN = '\n';

    private static final char TAB = '\t';

    private static final char CDTS = '[';

    private static final char CDTE = ']';

    private static final int NODE_DEFAULT = 0;

    private static final int NODE_VERSION = 1;

    private static final int NODE_DIRECTIVE = 2;

    private static final int NODE_HEADER = 3;

    private static final int NODE_FOOTER = 4;

    private static final int NODE_COMMENT = 5;

    private static final int NODE_STANDARD = 6;

    private static final int NODE_TOKEN = 7;

    private static final int NODE_HEADER_SINGLE = 8;

    private static final int NODE_CDATA = 9;

    private static final int STATE_DEFAULT = 0;

    private static final int STATE_NODE_START = 1;

    private static final int STATE_RECEIVED_SLASH = 2;

    private static final int STATE_NODE_NAME = 3;

    private static final int STATE_ATTRIBUTE = 4;

    private static final int STATE_ATTRIBUTE_VALUE = 5;

    private static final int STATE_START_COMMENT1 = 6;

    private static final int STATE_END_COMMENT1 = 7;

    private static final int STATE_END_COMMENT2 = 8;

    private static final int STATE_WAITING_FOR_NODE_NAME = 9;

    private static final int STATE_WAITING = 10;

    private static final int STATE_WAITING_FOR_EQUAL = 11;

    private static final int STATE_WAITING_FOR_ATTRIBUTE_VALUE = 12;

    private static final int STATE_WAITING_FOR_END = 13;

    private static final int STATE_WAITING_INVALID = 14;

    private IProject project;

    private Stack nodeStack = new Stack();

    private String currentValue;

    private String rest;

    private boolean hasQuotes;

    private String currentAttribute;

    private int state;

    private int nodeState;

    static {
        setupSuggestions();
    }

    public static CursorState getInstance(IProject project, InputStream is,
            int offset) throws IOException {
        InputStreamReader reader = new InputStreamReader(is);
        char[] buffer = new char[256];
        StringWriter writer = new StringWriter();
        int totalBytesRead = 0;
        int bytes_read;
        while ((bytes_read = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, bytes_read);
            totalBytesRead += bytes_read;
            if (totalBytesRead > offset) {
                if (bytes_read == 256) {
                    buffer = new char[30];
                    bytes_read = reader.read(buffer);
                    if (bytes_read > 0) {
                        writer.write(buffer, 0, bytes_read);
                        totalBytesRead += bytes_read;
                    }
                }
                break;
            }
        }

        CursorState state = new CursorState();
        state.project = project;
        int nodeState = NODE_DEFAULT;
        int currentState = STATE_DEFAULT;
        String currentNode = null;
        char[] arr = writer.toString().toCharArray();
        char c = Character.MIN_VALUE;
        for (int i = 0; i < offset; i++) {
            c = arr[i];
            if (c == LT) {
                if (nodeState == NODE_DEFAULT) {
                    nodeState = NODE_TOKEN;
                    currentState = STATE_WAITING_FOR_NODE_NAME;
                }
            } else if (c == QST) {
                if (nodeState == NODE_TOKEN) {
                    nodeState = NODE_VERSION;
                }
            } else if (c == EXC) {
                if (nodeState == NODE_TOKEN) {
                    nodeState = NODE_DIRECTIVE;
                }
            } else if (c == CDTS) {
                if (nodeState == NODE_DIRECTIVE) {
                    if (arr.length > i + 6) {
                        if (arr[i + 1] == 'C' && arr[i + 2] == 'D'
                                && arr[i + 3] == 'A' && arr[i + 4] == 'T'
                                && arr[i + 5] == 'A' && arr[i + 6] == CDTS) {
                            nodeState = NODE_CDATA;
                            currentState = STATE_WAITING_INVALID;
                            i = i + 6;
                        }
                    }
                }
            } else if (c == CDTE) {
                if (nodeState == NODE_CDATA) {
                    if (arr.length > i + 2) {
                        if (arr[i + 1] == CDTE && arr[i + 2] == GT) {
                            nodeState = NODE_DEFAULT;
                            i = i + 2;
                        }
                    }
                }
            } else if (c == QUOTE) {
                if (nodeState == NODE_HEADER) {
                    if (currentState == STATE_WAITING_FOR_ATTRIBUTE_VALUE) {
                        currentState = STATE_ATTRIBUTE_VALUE;
                    } else if (currentState == STATE_ATTRIBUTE_VALUE) {
                        currentState = STATE_WAITING_INVALID;
                    }
                }
            } else if (c == EQL) {
                if (nodeState == NODE_HEADER) {
                    if (currentState == STATE_WAITING_FOR_EQUAL
                            || currentState == STATE_ATTRIBUTE) {
                        currentState = STATE_WAITING_FOR_ATTRIBUTE_VALUE;
                    }
                }
            } else if (c == GT) {
                if (currentState == STATE_END_COMMENT2) {
                    if (currentNode.equals("comment")) {
                        state.popNode();
                        currentNode = state.peekNode();
                        nodeState = NODE_DEFAULT;
                        currentState = STATE_DEFAULT;
                    }
                } else if (nodeState == NODE_HEADER) {
                    if (currentState == STATE_NODE_NAME) {
                        currentState = STATE_WAITING;
                        int count = 0;
                        int index = i - 1;
                        while (true) {
                            count++;
                            if (Character.isLetterOrDigit(arr[index])
                                    || arr[index] == DASH || arr[index] == USC)
                                index = index - 1;
                            else
                                break;
                        }
                        char[] nodeName = new char[count - 1];
                        int start = i - count;
                        for (index = 0; index < count - 1; index++) {
                            nodeName[index] = arr[++start];
                        }
                        currentNode = new String(nodeName);
                        state.pushNode(currentNode);

                    } else if (currentState == STATE_WAITING_INVALID) {
                        currentState = STATE_DEFAULT;
                        nodeState = NODE_DEFAULT;
                    }
                    nodeState = NODE_DEFAULT;
                } else if (nodeState == NODE_FOOTER) {
                    if (null != currentNode && !currentNode.equals("comment")) {
                        state.popNode();
                        currentNode = state.peekNode();
                        nodeState = NODE_DEFAULT;
                        currentState = STATE_DEFAULT;
                    }
                } else if (nodeState == NODE_DIRECTIVE
                        || nodeState == NODE_VERSION) {
                    nodeState = NODE_DEFAULT;
                    currentState = STATE_DEFAULT;
                } else if (nodeState == NODE_HEADER_SINGLE) {
                    nodeState = NODE_DEFAULT;
                    state.popNode();
                    currentNode = state.peekNode();
                }
            } else if (c == SPACE || c == RTN || c == NLN || c == TAB) {
                if (nodeState == NODE_TOKEN) {
                    nodeState = NODE_HEADER;
                    currentState = STATE_WAITING_FOR_NODE_NAME;
                } else if (nodeState == NODE_HEADER) {
                    if (currentState == STATE_NODE_NAME) {
                        currentState = STATE_WAITING;
                        int count = 0;
                        int index = i - 1;
                        while (true) {
                            count++;
                            if (Character.isLetterOrDigit(arr[index])
                                    || arr[index] == DASH || arr[index] == USC)
                                index = index - 1;
                            else
                                break;
                        }
                        char[] nodeName = new char[count - 1];
                        int start = i - count;
                        for (index = 0; index < count - 1; index++) {
                            nodeName[index] = arr[++start];
                        }
                        currentNode = new String(nodeName);
                        state.pushNode(currentNode);

                    } else if (currentState == STATE_ATTRIBUTE) {
                        currentState = STATE_WAITING_FOR_EQUAL;
                    } else if (currentState == STATE_WAITING_INVALID) {
                        currentState = STATE_WAITING;
                    }
                } else {
                    if (nodeState != NODE_COMMENT && nodeState != NODE_CDATA) {
                        nodeState = NODE_DEFAULT;
                    }
                }
            } else if (c == SLASH) {
                if (nodeState == NODE_TOKEN) {
                    nodeState = NODE_FOOTER;
                } else if (nodeState == NODE_HEADER) {
                    nodeState = NODE_HEADER_SINGLE;
                }
            } else if (c == DASH) {
                if (nodeState == NODE_DIRECTIVE) {
                    nodeState = NODE_HEADER;
                    currentState = STATE_START_COMMENT1;
                } else if (nodeState == NODE_HEADER) {
                    if (currentState == STATE_START_COMMENT1) {
                        // comment started
                        nodeState = NODE_DEFAULT;
                        state.pushNode("comment");
                        currentNode = "comment";
                    }
                } else if (currentNode == "comment") {
                    if (currentState == STATE_END_COMMENT1) {
                        currentState = STATE_END_COMMENT2;
                    } else if (currentState != STATE_END_COMMENT2) {
                        currentState = STATE_END_COMMENT1;
                    }
                }
            } else {
                if (nodeState == NODE_TOKEN
                        || currentState == STATE_WAITING_FOR_NODE_NAME) {
                    if (nodeState != NODE_FOOTER) {
                        if (null != currentNode
                                && currentNode.equals("comment")) {
                            nodeState = NODE_DEFAULT;
                        } else {
                            nodeState = NODE_HEADER;
                        }
                    }
                    currentState = STATE_NODE_NAME;
                } else if (nodeState == NODE_HEADER) {
                    if (currentState == STATE_WAITING) {
                        currentState = STATE_ATTRIBUTE;
                    } else if (currentState == STATE_WAITING_FOR_ATTRIBUTE_VALUE) {
                        currentState = STATE_ATTRIBUTE_VALUE;
                    }
                }
            }
        }

        if (nodeState == NODE_HEADER || nodeState == NODE_FOOTER
                || nodeState == NODE_TOKEN) {
            //if (c != SPACE) {
            int end = offset;
            int start = offset - 1;
            while (start >= 0) {
                char startChar = arr[start];
                if (startChar == SPACE || startChar == SLASH || startChar == LT
                        || startChar == EXC || startChar == QST
                        || startChar == EQL || startChar == QUOTE
                        || startChar == NLN || startChar == RTN
                        || startChar == TAB) {
                    start = start + 1;
                    break;
                }
                start = start - 1;
            }
            char[] valueArr = new char[end - start];
            for (int i = start; i < end; i++) {
                valueArr[i - start] = arr[i];
            }
            state.setCurrentValue(new String(valueArr));

            // figure out the rest
            int tempStart = start;
            start = offset;
            end = start;
            boolean spacesEncountered = false;
            int count = 0;
            while (end < arr.length) {
                char endChar = arr[end];
                if (endChar == SLASH || endChar == LT || endChar == EXC
                        || endChar == QST) {
                    break;
                } else if (endChar == QUOTE) {
                    end = end + 1;
                    break;
                } else if (endChar == RTN || endChar == NLN) {
                    break;
                } else if (endChar == SPACE) {
                    if (nodeState == NODE_TOKEN
                            || currentState == STATE_NODE_NAME) {
                        spacesEncountered = true;
                    } else if (currentState == STATE_WAITING && count == 0) {
                        break;
                    }
                } else if (nodeState == NODE_TOKEN
                        || currentState == STATE_NODE_NAME) {
                    if (spacesEncountered) {
                        break;
                    }
                }
                end = end + 1;
                count = count + 1;
            }
            char[] restArr = new char[end - start];
            for (int i = start; i < end; i++) {
                restArr[i - start] = arr[i];
            }
            state.setRest(new String(restArr));

            start = tempStart;
            if (STATE_WAITING_FOR_ATTRIBUTE_VALUE == currentState
                    || STATE_ATTRIBUTE_VALUE == currentState) {
                // determine the attribute value
                start = start - 1;
                while (start >= 0) {
                    c = arr[start];
                    if (Character.isLetterOrDigit(c)) {
                        start = start + 1;
                        break;
                    } else {
                        start = start - 1;
                    }
                }
                // we should be at the end of the attribute
                end = start;
                while (start >= 0) {
                    char startChar = arr[start];
                    if (startChar == SPACE || startChar == RTN
                            || startChar == TAB) {
                        start = start + 1;
                        break;
                    }
                    start = start - 1;
                }
                valueArr = new char[end - start];
                for (int i = start; i < end; i++) {
                    valueArr[i - start] = arr[i];
                }
            }
            state.setCurrentAttribute(new String(valueArr));
        }
        //}
        state.state = currentState;
        state.nodeState = nodeState;
        return state;
    }

    /**
     * @return Returns the currentValue.
     */
    public String getCurrentValue() {
        return currentValue;
    }

    /**
     * @param currentValue
     *            The currentValue to set.
     */
    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    /**
     * @return Returns the nodeStack.
     */
    public Stack getNodeStack() {
        return nodeStack;
    }

    /**
     * @param nodeStack
     *            The nodeStack to set.
     */
    public void setNodeStack(Stack nodeStack) {
        this.nodeStack = nodeStack;
    }

    public void pushNode(String node) {
        nodeStack.push(node);
    }

    public String popNode() {
        String s = (String) nodeStack.pop();
        return s;
    }

    public String peekNode() {
        return (String) nodeStack.peek();
    }

    public static Map nodeSuggestions;

    public static Map attributeSuggestions;

    public static Map valueSuggestions;

    public static Map classSuggestions;

    public static void setupSuggestions() {
        String[] TRUE_FALSE = { "true", "false" };
        String[] TRUE_FALSE_AUTO = { "true", "false", "auto" };
        String[] ALL_KNOWN_TYPES = { "binary", "int", "integer", "long",
                "short", "float", "double", "char", "character", "byte",
                "boolean", "yes_no", "true_false", "string", "date", "time",
                "timestamp", "calendar", "calendar_date", "big_decimal",
                "locale", "timezone", "currency", "class", "binary", "text",
                "serializable", "clob", "blob", "object",
                String.class.getName(), Integer.class.getName(),
                "java.util.Currency", BigDecimal.class.getName(),
                Character.class.getName(), Calendar.class.getName(),
                Date.class.getName(), Timestamp.class.getName(),
                Time.class.getName(), Locale.class.getName(),
                TimeZone.class.getName(), Class.class.getName(),
                Object.class.getName(), Blob.class.getName(),
                Clob.class.getName(), Float.class.getName(),
                Long.class.getName(), Double.class.getName(),
                Boolean.class.getName(), Byte.class.getName() };
        String[] PRIMITIVE_TYPES = { "int", "float", "long", "double", "char",
                "boolean", "byte" };
        String[] KEY_TYPES = ALL_KNOWN_TYPES;
        String[] VERSION_TYPES = { "int", "long", "timestamp", "date",
                "calendar" };
        String[] ACCESS_TYPES = { "field", "property", "ClassName" };
        String[] UNSAVED_VALUE_TYPES = { "any", "none" };
        String[] CASCADE_TYPES = { "all", "none", "save-update", "delete" };
        String[] CASCADE_TYPES_ORPHAN = { "all", "none", "save-update",
                "delete", "delete-orphan" };
        String[] SORT_TYPES = { "unsorted", "natural", "comparatorClass" };

        nodeSuggestions = new HashMap();
        nodeSuggestions.put("hibernate-mapping", new String[] { "class",
                "query", "import", "subclass" });
        nodeSuggestions.put("class", new String[] { "meta", "composite-id",
                "id", "version", "timestamp", "discriminator", "property",
                "many-to-one", "one-to-one", "set", "map", "bag", "array",
                "primitive-array", "list", "subclass", "joined-subclass",
                "component", "dynamic-component", "any", "element",
                "many-to-many", "idbag" });
        nodeSuggestions.put("id", new String[] { "meta", "generator" });
        nodeSuggestions.put("generator", new String[] { "param" });
        nodeSuggestions.put("composite-id", new String[] { "meta",
                "key-property", "key-many-to-one" });
        nodeSuggestions.put("component", new String[] { "meta", "property",
                "many-to-one", "parent" });
        nodeSuggestions.put("dynamic-component", new String[] { "meta",
                "property", "many-to-one", "parent" });
        nodeSuggestions.put("subclass", new String[] { "meta", "version",
                "timestamp", "property", "many-to-one", "one-to-one", "set",
                "map", "bag", "array", "list", "component",
                "dynamic-component", "subclass" });
        nodeSuggestions.put("joined-subclass", new String[] { "meta",
                "version", "timestamp", "property", "many-to-one",
                "one-to-one", "set", "map", "bag", "array", "list",
                "component", "dynamic-component", "key" });
        nodeSuggestions.put("any", new String[] { "meta", "meta-value",
                "column" });
        nodeSuggestions.put("property", new String[] { "meta", "column" });
        nodeSuggestions.put("map", new String[] { "meta", "key", "index",
                "element", "composite-element", "index-many-to-many",
                "one-to-many", "many-to-many" });
        nodeSuggestions.put("set", new String[] { "meta", "key", "element",
                "composite-element", "one-to-many", "many-to-many",
                "many-to-any" });
        nodeSuggestions.put("bag", new String[] { "meta", "key", "element",
                "composite-element", "one-to-many", "many-to-many",
                "many-to-any" });
        nodeSuggestions
                .put("array", new String[] { "meta", "key", "index", "element",
                        "composite-element", "many-to-many", "one-to-many" });
        nodeSuggestions.put("primitive-array", new String[] { "meta", "key",
                "index", "element" });
        nodeSuggestions.put("list", new String[] { "meta", "key", "index",
                "element", "composite-element", "index-many-to-many",
                "one-to-many", "many-to-many" });
        nodeSuggestions.put("composite-element", new String[] { "meta",
                "property" });
        nodeSuggestions.put("idbag", new String[] { "meta", "collection-id",
                "key", "many-to-many", "one-to-many" });
        nodeSuggestions.put("collection-id",
                new String[] { "meta", "generator" });

        attributeSuggestions = new HashMap();
        attributeSuggestions.put("import", new String[] { "class", "rename" });
        attributeSuggestions.put("meta", new String[] { "attribute" });
        attributeSuggestions.put("query", new String[] { "name" });
        attributeSuggestions.put("class", new String[] { "name", "table",
                "discriminator-value", "mutable", "schema", "proxy",
                "dynamic-update", "dynamic-insert", "select-before-update",
                "polymorphism", "where", "persister", "batch-size",
                "optimistic-lock", "lazy" });
        attributeSuggestions.put("id", new String[] { "name", "type", "column",
                "unsaved-value", "access" });
        attributeSuggestions.put("key-property", new String[] { "name", "type",
                "column" });
        attributeSuggestions.put("key",
                new String[] { "name", "type", "column" });
        attributeSuggestions.put("composite-id", new String[] { "name",
                "class", "unsaved-value", "access" });
        attributeSuggestions.put("key-many-to-one", new String[] { "name",
                "type", "column", "class" });
        attributeSuggestions.put("discriminator", new String[] { "column",
                "type", "force" });
        attributeSuggestions.put("version", new String[] { "name", "column",
                "type", "access", "unsaved-value" });
        attributeSuggestions.put("timestamp", new String[] { "column", "name",
                "access", "unsaved-value" });
        attributeSuggestions.put("property", new String[] { "column", "name",
                "type", "update", "insert", "formula", "access", "length",
                "not-null", "unique" });
        attributeSuggestions.put("many-to-one", new String[] { "column",
                "name", "class", "cascade", "outer-join", "update", "insert",
                "property-ref", "access", "unique" });
        attributeSuggestions.put("one-to-one", new String[] { "column", "name",
                "class", "cascade", "outer-join", "property-ref", "access" });
        attributeSuggestions.put("component", new String[] { "name", "class",
                "insert", "update", "access" });
        attributeSuggestions.put("dynamic-component", new String[] { "name",
                "class", "insert", "update", "access" });
        attributeSuggestions.put("subclass", new String[] { "name",
                "discriminator-value", "proxy", "lazy", "dynamic-update",
                "dynamic-insert" });
        attributeSuggestions.put("joined-subclass", new String[] { "name",
                "table", "proxy", "lazy", "dynamic-update", "dynamic-insert",
                "extends" });
        attributeSuggestions.put("any", new String[] { "name", "id-type",
                "meta-type", "cascade", "access" });
        attributeSuggestions.put("meta-value",
                new String[] { "value", "class" });
        attributeSuggestions.put("column", new String[] { "name", "sql-type",
                "length", "not-null", "unique" });
        attributeSuggestions.put("map", new String[] { "name", "table",
                "schema", "lazy", "inverse", "cascade", "sort", "order-by",
                "where", "outer-join", "batch-size", "access" });
        attributeSuggestions.put("index", new String[] { "column", "type" });
        attributeSuggestions.put("element", new String[] { "column", "type" });
        attributeSuggestions.put("index-many-to-many", new String[] { "column",
                "class" });
        attributeSuggestions.put("many-to-many", new String[] { "column",
                "class", "outer-join" });
        attributeSuggestions.put("set", new String[] { "name", "table", "sort",
                "order-by", "inverse", "lazy", "cascade" });
        attributeSuggestions.put("bag", new String[] { "name", "table", "sort",
                "order-by", "inverse", "lazy", "cascade" });
        attributeSuggestions.put("array", new String[] { "name", "table",
                "sort", "order-by", "cascade", "inverse" });
        attributeSuggestions.put("primitive-array", new String[] { "name",
                "table", "sort", "order-by", "cascade", "inverse" });
        attributeSuggestions.put("list", new String[] { "name", "table",
                "sort", "order-by", "inverse", "lazy", "cascade" });
        attributeSuggestions.put("composite-element", new String[] { "class" });
        attributeSuggestions.put("idbag", new String[] { "name", "table",
                "lazy" });
        attributeSuggestions.put("collection-id", new String[] { "column",
                "type" });
        attributeSuggestions.put("parent", new String[] { "name" });
        attributeSuggestions.put("generator", new String[] { "class" });

        classSuggestions = new HashMap();
        classSuggestions.put(new NodeAttribute("key-many-to-one", "class"),
                Boolean.TRUE);
        classSuggestions.put(new NodeAttribute("many-to-one", "class"),
                Boolean.TRUE);
        classSuggestions.put(new NodeAttribute("one-to-one", "class"),
                Boolean.TRUE);

        valueSuggestions = new HashMap();
        valueSuggestions.put(new NodeAttribute("class", "dynamic-update"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("class", "dynamic-insert"),
                TRUE_FALSE);
        valueSuggestions.put(
                new NodeAttribute("class", "select-before-update"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("class", "polymorphism"),
                new String[] { "implicit", "explicit" });
        valueSuggestions.put(new NodeAttribute("class", "optimistic-lock"),
                new String[] { "none", "version", "dirty", "all" });
        valueSuggestions.put(new NodeAttribute("class", "lazy"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("id", "unsaved-value"),
                UNSAVED_VALUE_TYPES);
        valueSuggestions.put(new NodeAttribute("id", "access"), ACCESS_TYPES);
        valueSuggestions.put(new NodeAttribute("id", "type"), ALL_KNOWN_TYPES);
        valueSuggestions.put(new NodeAttribute("generator", "class"),
                new String[] { "increment", "identity", "sequence", "hilo",
                        "seqhilo", "uuid.hex", "uuid.string", "native",
                        "assigned", "foreign", "vm" });
        valueSuggestions.put(new NodeAttribute("composite-id", "access"),
                ACCESS_TYPES);
        valueSuggestions.put(
                new NodeAttribute("composite-id", "unsaved-value"),
                UNSAVED_VALUE_TYPES);
        valueSuggestions.put(new NodeAttribute("key-property", "type"),
                KEY_TYPES);
        valueSuggestions.put(new NodeAttribute("discriminator", "type"),
                ALL_KNOWN_TYPES);
        valueSuggestions.put(new NodeAttribute("discriminator", "force"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("version", "type"),
                VERSION_TYPES);
        valueSuggestions.put(new NodeAttribute("version", "access"),
                ACCESS_TYPES);
        valueSuggestions.put(new NodeAttribute("version", "unsaved-value"),
                new String[] { "null", "negative", "undefined" });
        valueSuggestions.put(new NodeAttribute("timestamp", "access"),
                ACCESS_TYPES);
        valueSuggestions.put(new NodeAttribute("version", "unsaved-value"),
                new String[] { "null", "undefined" });
        valueSuggestions.put(new NodeAttribute("property", "type"),
                ALL_KNOWN_TYPES);
        valueSuggestions.put(new NodeAttribute("property", "update"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("property", "insert"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("property", "access"),
                ACCESS_TYPES);
        valueSuggestions.put(new NodeAttribute("property", "not-null"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("property", "unique"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("many-to-one", "outer-join"),
                TRUE_FALSE_AUTO);
        valueSuggestions.put(new NodeAttribute("many-to-one", "update"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("many-to-one", "insert"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("many-to-one", "access"),
                ACCESS_TYPES);
        valueSuggestions.put(new NodeAttribute("many-to-one", "unique"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("many-to-one", "not-null"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("many-to-one", "cascade"),
                CASCADE_TYPES);
        valueSuggestions.put(new NodeAttribute("one-to-one", "cascade"),
                CASCADE_TYPES);
        valueSuggestions.put(new NodeAttribute("one-to-one", "constrained"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("one-to-one", "outer-join"),
                TRUE_FALSE_AUTO);
        valueSuggestions.put(new NodeAttribute("one-to-one", "access"),
                ACCESS_TYPES);
        valueSuggestions.put(new NodeAttribute("one-to-one", "not-null"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("component", "insert"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("component", "update"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("component", "access"),
                ACCESS_TYPES);
        valueSuggestions.put(new NodeAttribute("dynamic-component", "insert"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("dynamic-component", "update"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("dynamic-component", "access"),
                ACCESS_TYPES);
        valueSuggestions.put(new NodeAttribute("subclass", "dynamic-update"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("subclass", "dynamic-insert"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("subclass", "lazy"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("joined-subclass",
                "dynamic-update"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("joined-subclass",
                "dynamic-insert"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("joined-subclass", "lazy"),
                TRUE_FALSE);
        valueSuggestions
                .put(new NodeAttribute("any", "cascade"), CASCADE_TYPES);
        valueSuggestions.put(new NodeAttribute("any", "access"), ACCESS_TYPES);
        valueSuggestions.put(new NodeAttribute("column", "not-null"),
                TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("column", "unique"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("map", "lazy"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("map", "inverse"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("map", "cascade"),
                CASCADE_TYPES_ORPHAN);
        valueSuggestions.put(new NodeAttribute("map", "sort"), SORT_TYPES);
        valueSuggestions.put(new NodeAttribute("map", "outer-join"),
                TRUE_FALSE_AUTO);
        valueSuggestions.put(new NodeAttribute("map", "access"), ACCESS_TYPES);
        valueSuggestions.put(new NodeAttribute("set", "lazy"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("set", "inverse"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("set", "cascade"),
                CASCADE_TYPES_ORPHAN);
        valueSuggestions.put(new NodeAttribute("bag", "lazy"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("bag", "inverse"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("bag", "cascade"),
                CASCADE_TYPES_ORPHAN);
        valueSuggestions.put(new NodeAttribute("list", "lazy"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("list", "inverse"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("list", "cascade"),
                CASCADE_TYPES_ORPHAN);
        valueSuggestions.put(new NodeAttribute("index", "type"), KEY_TYPES);
        valueSuggestions.put(new NodeAttribute("element", "type"),
                ALL_KNOWN_TYPES);
        valueSuggestions.put(new NodeAttribute("many-to-many", "outer-join"),
                TRUE_FALSE_AUTO);
        valueSuggestions.put(new NodeAttribute("array", "cascade"),
                CASCADE_TYPES);
        valueSuggestions.put(new NodeAttribute("primitive-array", "cascade"),
                CASCADE_TYPES);
        valueSuggestions.put(new NodeAttribute("idbag", "lazy"), TRUE_FALSE);
        valueSuggestions.put(new NodeAttribute("collection-id", "type"),
                KEY_TYPES);
    }

    public List getSuggestions(int offset, HibernateEditor editor) {
        String[] arr = null;
        int mode = 0;
        if (NODE_TOKEN == nodeState || NODE_HEADER == nodeState) {
            if (STATE_WAITING_FOR_NODE_NAME == state
                    || STATE_NODE_NAME == state) {
                // node name
                mode = 1;
                String currentNode = (String) nodeStack.peek();
                if (null != currentNode) {
                    arr = (String[]) nodeSuggestions.get(currentNode);
                }
            }
        }
        if (NODE_HEADER == nodeState) {
            if (STATE_WAITING == state || STATE_ATTRIBUTE == state) {
                // attribute
                mode = 2;
                String currentNode = (String) nodeStack.peek();
                if (null != currentNode) {
                    arr = (String[]) attributeSuggestions.get(currentNode);
                }
            } else if (STATE_WAITING_FOR_ATTRIBUTE_VALUE == state
                    || STATE_ATTRIBUTE_VALUE == state) {
                // attribute value
                mode = 3;
                String currentNode = (String) nodeStack.peek();
                String currentAttribute = getCurrentAttribute();
                if (null != currentAttribute) {
                    NodeAttribute na = new NodeAttribute(currentNode,
                            currentAttribute);
                    if (null != classSuggestions.get(na)) {
                        arr = editor.getAllClassChoices(project);
                    } else {
                        arr = (String[]) valueSuggestions.get(na);
                    }
                }
            }
        } else if (NODE_FOOTER == nodeState) {
            // footer
            mode = 4;
            String currentNode = (String) nodeStack.peek();
            if (null != currentNode) {
                arr = new String[1];
                arr[0] = currentNode;
            }
        }

        if (null == arr)
            return null;
        List values = new ArrayList(arr.length);
        for (int i = 0; i < arr.length; i++) {
            String suggestion = arr[i];
            values.add(suggestion);
        }
        Collections.sort(values);

        int i = 0;
        List newValues = new ArrayList(values.size());
        if (null != currentValue)
            currentValue = currentValue.trim();
        if (null != currentAttribute)
            currentAttribute = currentAttribute.trim();
        for (Iterator iterator = values.iterator(); iterator.hasNext(); i++) {
            String suggestion = (String) iterator.next();
            String rest = getRest();
            if (null == getCurrentValue()
                    || (null != getCurrentValue() && suggestion
                            .startsWith(getCurrentValue()))) {
                String actSuggestion = null;
                if (null != getCurrentValue()) {
                    String enteredText = getCurrentValue();
                    actSuggestion = suggestion.substring(enteredText.length(),
                            suggestion.length());
                } else {
                    actSuggestion = suggestion;

                }
                if (mode == 1) {
                    actSuggestion = actSuggestion + " ";
                } else if (mode == 2) {
                    //if (null != rest && rest.endsWith("\"")) {
                    //	rest = rest.substring(0, rest.length() - 1);
                    //}
                    actSuggestion = actSuggestion + "=\"";
                } else if (mode == 3) {
                    actSuggestion = actSuggestion + "\" ";
                } else if (mode == 4) {
                    actSuggestion = actSuggestion + ">";
                }
                int restLength = 0;
                if (null != rest)
                    restLength = rest.length();
                CompletionProposal prop = new CompletionProposal(actSuggestion,
                        offset, restLength, actSuggestion.length(), null,
                        suggestion, null, null);
                newValues.add(prop);
            }
        }
        return newValues;
    }

    /**
     * @return Returns the currentAttribute.
     */
    public String getCurrentAttribute() {
        return currentAttribute;
    }

    /**
     * @param currentAttribute
     *            The currentAttribute to set.
     */
    public void setCurrentAttribute(String currentAttribute) {
        this.currentAttribute = currentAttribute;
    }

    /**
     * @return Returns the rest.
     */
    public String getRest() {
        return rest;
    }

    /**
     * @param rest
     *            The rest to set.
     */
    public void setRest(String rest) {
        this.rest = rest;
    }
}