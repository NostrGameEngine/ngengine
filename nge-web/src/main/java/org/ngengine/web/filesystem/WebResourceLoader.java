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

package org.ngengine.web.filesystem;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEUtils;
import org.ngengine.platform.transport.NGEHttpResponseStream;
import org.ngengine.web.WebBindsAsync;

import com.jme3.util.res.ResourceLoader;


public class WebResourceLoader implements ResourceLoader {
    private static final Logger logger = Logger.getLogger(WebResourceLoader.class.getName());
    private static final String RESOURCE_INDEX = "resources.index.txt";

    private Set<String> resourceIndex;
    private boolean resourceIndexLoadAttempted;
 
    public WebResourceLoader() {
      
          
    }

 
    private String getResourcePath(Class<?> clazz, String path) {
        String resourcePath = path;
        if (clazz != null) {
            String className = clazz.getName();
            String classPath = className.replace('.', '/') + ".class";
            classPath = classPath.substring(0, classPath.lastIndexOf('/'));
            resourcePath = classPath + "/" + path;
        }
        if(resourcePath.startsWith("/")){
            resourcePath=resourcePath.substring(1);
        }
        return resourcePath;
    }

    private String getFullPath(Class<?> clazz, String path) throws MalformedURLException {
        String resourcePath = getResourcePath(clazz, path);
        String url = WebBindsAsync.getBaseURL();
        url += resourcePath;
        url = NGEUtils.safeURI(url).toString();
        return url;
    }

    private synchronized void loadResourceIndex() throws IOException {
        if (resourceIndexLoadAttempted) return;
        resourceIndexLoadAttempted = true;

        Set<String> loadedIndex = new HashSet<>();
        String indexUrl = getFullPath(null, RESOURCE_INDEX);
        URL url = new URL(null, indexUrl, new WebUrlStreamHandler());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                url.openConnection().getInputStream(), Charset.forName("UTF-8")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int firstSeparator = line.indexOf(' ');
                int secondSeparator = firstSeparator < 0
                        ? -1
                        : line.indexOf(' ', firstSeparator + 1);
                if (secondSeparator < 0) continue;
                String resource = line.substring(secondSeparator + 1).trim();
                if (!resource.isEmpty()) loadedIndex.add(resource);
            }
        }
        resourceIndex = loadedIndex;
    }

    private boolean isIndexedResource(String resourcePath) {
        if (RESOURCE_INDEX.equals(resourcePath)) return true;
        try {
            loadResourceIndex();
        } catch (IOException exception) {
            logger.log(Level.WARNING,
                    "Unable to load the web resource index; falling back to optimistic lookup",
                    exception);
        }
        return resourceIndex == null || resourceIndex.contains(resourcePath);
    }

    private static class WebUrlStreamHandler extends URLStreamHandler {
        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            return new URLConnection(u) {
                @Override
                public void connect() {}

                @Override
                public InputStream getInputStream() throws IOException {
                    try{
                        NGEHttpResponseStream req = NGEPlatform.get().httpRequestStream("GET", url.toString(), null, null, null).await();
                        if (!req.status()) {
                            try {
                                req.body().close();
                            } catch (Exception ignored) {
                                // Preserve the HTTP failure as the useful resource error.
                            }
                            throw new IOException("HTTP " + req.statusCode() + " while reading " + url);
                        }
                        return req.body;
                    } catch(Exception ex){
                        if (ex instanceof IOException) throw (IOException) ex;
                        throw new IOException("Failed to get resource: "+url.toString()+" - "+ex.toString());
                    }
                }
            };
        }
    }


    @Override
    public URL getResource(String path, Class<?> clazz) {
        try{
            String resourcePath = getResourcePath(clazz, path);
            if (!isIndexedResource(resourcePath)) return null;
            path = getFullPath(null, resourcePath);
            return new URL(null,path, new WebUrlStreamHandler());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path, Class<?> clazz) {
        URL url = getResource(path, clazz);
        if(url==null) return null;
        try {
            return url.openConnection().getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String path) throws IOException {
        loadResourceIndex();
        if (resourceIndex == null) return Collections.emptyEnumeration();

        String normalizedPath = getResourcePath(null, path);
        ArrayList<URL> matches = new ArrayList<>();
        for (String resource : resourceIndex) {
            if (resource.equals(normalizedPath) || resource.endsWith("/" + normalizedPath)) {
                matches.add(new URL(null, getFullPath(null, resource), new WebUrlStreamHandler()));
            }
        }
        return Collections.enumeration(matches);
    }

}
