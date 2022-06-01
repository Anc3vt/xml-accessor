/**
 * Copyright (C) 2022 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancevt.xml;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlAccessor {

    public static void main(String[] args) throws FileNotFoundException, IOException {

        String sourceXml;

        final BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(XmlAccessor.class.getClassLoader().getResourceAsStream("simple.xml"))
        );

        final StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }
        bufferedReader.close();

        sourceXml = stringBuilder.toString();

        System.out.println("Source: " + sourceXml);
        System.out.println("--------------------------------------------------");

        final XmlAccessor xmlNav = new XmlAccessor(sourceXml);

        System.out.println(xmlNav.toString(true));


        System.out.println(xmlNav.getElement("recipe.instructions.step").getAttribute("test"));
        System.out.println(xmlNav.getElement("recipe.composition.ingredient[0]").getAttribute("amount"));

        System.out.println(xmlNav.getValue("recipe.composition.ingredient[1].@amount"));
        System.out.println(xmlNav.getValue("recipe.instructions.step[0]").trim());

        System.out.println(xmlNav.sizeOf("recipe.instructions.step"));

        final Element composition = xmlNav.getElement("recipe.composition");
        composition.setAttribute("new-attr", "new attribute value");

        //System.out.println("\n\n" + xmlNav);
    }

    private static final String DOT = ".", REGEX_DOT = "\\.";
    private static final String BRACKET = "[", REGEX_BRACKET = "\\[";
    private static final String ATTRIBUTE = "@";

    private static DocumentBuilderFactory documentBuilderFactory;
    private static DocumentBuilder documentBuilder;

    private static final void initializeBuilders() {
        if (documentBuilderFactory == null && documentBuilder == null) {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            try {
                documentBuilder = documentBuilderFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    private Document document;

    public XmlAccessor(InputStream inputStream) {
        parse(inputStream);
    }

    public XmlAccessor(final String sourceXml) {
        parse(sourceXml);
    }

    public final void parse(final InputStream stream) {
        try {
            document = documentBuilder.parse(stream);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final void parse(final String sourceXml) {
        initializeBuilders();

        final InputStream stream = new ByteArrayInputStream(sourceXml.getBytes(StandardCharsets.UTF_8));
        parse(stream);
    }

    public final Document getDocument() {
        return document;
    }

    public final void clear() {
        document = null;
    }

    public final boolean isEmpty() {
        return document == null;
    }

    public final String getValue(final String path) {

        if (path.contains(ATTRIBUTE)) {
            final String[] splitted = path.split(ATTRIBUTE);
            final String clearPath = splitted[0];
            final String targetAttribute = splitted[1];
            final Element elem = getElement(clearPath);
            return elem.getAttribute(targetAttribute);
        }

        final Element elem = getElement(path);
        return elem.getTextContent();
    }

    public final Element getElement(final String path) {
        if (path == null || path.isEmpty()) return null;

        final String[] splitted = path.split(REGEX_DOT);

        final NodeList nl = document.getElementsByTagName(splitted[0]);
        Element currentElem = (Element) nl.item(0);

        for (int i = 1; i < splitted.length; i++) {
            final String currentName = splitted[i];

            if (currentName.contains(BRACKET)) {
                final int bracketStart = currentName.indexOf(BRACKET);

                final int index = Integer.parseInt(
                        currentName.substring(bracketStart + 1, currentName.length() - 1)
                );

                currentElem = getElementFromElement(
                        currentName.split(REGEX_BRACKET)[0],
                        currentElem,
                        index
                );

            } else {
                currentElem = getElementFromElement(currentName, currentElem);
            }
        }

        return currentElem;
    }

    public final int sizeOf(final String path) {
        final String left = path.substring(0, path.lastIndexOf(DOT));
        final String right = path.substring(path.lastIndexOf(DOT) + 1);

        int size = 0;

        final Element elem = getElement(left);
        final NodeList nl = elem.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            final Node node = nl.item(i);
            if (!(node instanceof Element)) continue;

            final Element currentElem = (Element) node;

            if (right.equals(currentElem.getTagName())) size++;
        }

        return size;
    }

    private static final Element getElementFromElement(final String target, final Element from) {
        return getElementFromElement(target, from, 0);
    }

    private static final Element getElementFromElement(
            final String target, final Element from, final int index) {

        final NodeList nodeList = from.getElementsByTagName(target);
        final Node node = nodeList.item(index);
        return (Element) node;
    }


    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(final boolean indentation) {

        try {
            final Document document = getDocument();
            //System.out.println(getStringFromDocument(document));

            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, indentation ? "yes" : "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(document), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "XmlNavigator[empty]";
    }


    public static String getStringFromDocument(Document doc) throws TransformerException {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);
        return writer.toString();
    }


}















































