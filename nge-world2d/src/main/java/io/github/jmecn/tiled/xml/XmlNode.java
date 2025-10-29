package io.github.jmecn.tiled.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal DOM-like node used to avoid javax.xml/org.w3c.dom.
 */
public class XmlNode {
    private final String name;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final List<XmlNode> children = new ArrayList<>();
    private XmlNode parent;
    private String textContent;

    public XmlNode(String name) {
        this.name = name;
    }

    void setParent(XmlNode parent) {
        this.parent = parent;
    }

    public XmlNode getParent() {
        return parent;
    }

    public String getNodeName() {
        return name;
    }

    public void addChild(XmlNode child) {
        children.add(child);
        child.setParent(this);
    }

    public List<XmlNode> getChildNodes() {
        return Collections.unmodifiableList(children);
    }

    public boolean hasChildNodes() {
        return !children.isEmpty();
    }

    public XmlNode getFirstChild() {
        return children.isEmpty() ? null : children.get(0);
    }

    public XmlNode getNextSibling() {
        if (parent == null) return null;
        List<XmlNode> siblings = parent.children;
        int idx = siblings.indexOf(this);
        if (idx >= 0 && idx + 1 < siblings.size()) {
            return siblings.get(idx + 1);
        }
        return null;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }
}
