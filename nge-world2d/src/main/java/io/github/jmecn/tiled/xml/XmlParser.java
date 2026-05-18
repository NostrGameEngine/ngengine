/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package io.github.jmecn.tiled.xml;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Very small XML to XmlNode tree builder using kXML2.
 */
public final class XmlParser {
    private XmlParser() {}

    public static XmlNode parse(InputStream in) throws IOException {
        try {
            KXmlParser parser = new KXmlParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, "UTF-8");

            Deque<XmlNode> stack = new ArrayDeque<>();
            XmlNode root = null;

            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    XmlNode node = new XmlNode(parser.getName());
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        node.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
                    }
                    if (stack.isEmpty()) {
                        root = node;
                    } else {
                        stack.peek().addChild(node);
                    }
                    stack.push(node);
                } else if (event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT) {
                    XmlNode top = stack.peek();
                    if (top != null) {
                        String txt = parser.getText();
                        if (txt != null && !txt.trim().isEmpty()) {
                            // concatenate text content if multiple TEXT events
                            String old = top.getTextContent();
                            top.setTextContent(old == null ? txt : old + txt);
                        }
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    stack.pop();
                }
                event = parser.nextToken();
            }
            return root;
        } catch (XmlPullParserException e) {
            IOException ioe = new IOException("XML parse error: " + e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }

    public static List<XmlNode> getElementsByTagName(XmlNode root, String tag) {
        List<XmlNode> out = new ArrayList<>();
        dfs(root, tag, out);
        return out;
    }

    private static void dfs(XmlNode node, String tag, List<XmlNode> out) {
        if (node == null) return;
        if (tag.equals(node.getNodeName())) out.add(node);
        for (XmlNode c : node.getChildNodes()) dfs(c, tag, out);
    }
}
