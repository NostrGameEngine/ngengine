/**
 * Copyright (c) 2025, Nostr Game Engine
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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEUtils;
import org.ngengine.platform.transport.NGEHttpResponseStream;
import org.ngengine.web.WebBindsAsync;

import com.jme3.util.res.ResourceLoader;


public class WebResourceLoader implements ResourceLoader {
 
    public WebResourceLoader() {
      
          
    }

 
    private String getFullPath(Class<?> clazz, String path) throws MalformedURLException {
        String resourcePath = path;
        if (clazz != null) {
            String className = clazz.getName();
            String classPath = className.replace('.', '/') + ".class";
            classPath = classPath.substring(0, classPath.lastIndexOf('/'));
            resourcePath = classPath + "/" + path;
        }
        String url = WebBindsAsync.getBaseURL();
         if(resourcePath.startsWith("/")){
            resourcePath=resourcePath.substring(1);
        }
        url += resourcePath;
        url = NGEUtils.safeURI(url).toString();
        return url;
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
                        return req.body;
                    } catch(Exception ex){
                        throw new IOException("Failed to get resource: "+url.toString()+" - "+ex.toString());
                    }
                }
            };
        }
    }


    @Override
    public URL getResource(String path, Class<?> clazz) {
        try{
            path = getFullPath(clazz, path);            
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

    protected ArrayList<URL> index;

    @Override
    public Enumeration<URL> getResources(String path) throws IOException {
        if (index == null) {
            String indexPath=this.getFullPath(null, "resources.index.txt");
            URL url=new URL(indexPath);
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                InputStream is = url.openStream();
                while ((len = is.read(buffer)) > -1 ) {
                    baos.write(buffer, 0, len);
                }
                baos.flush();
                String content = new String(baos.toByteArray(), Charset.forName("UTF-8"));
                String[] lines = content.split("\n");
                index = new ArrayList<URL>();
                for (String line : lines) {
                    line = line.trim();
                    String[] parts=line.split(" ",2);
                    if(parts.length==2){
                        String hash=parts[0].trim(); // useless
                        String size=parts[1].trim(); // useless
                        String resource=parts[2].trim();
                        if (resource.length() > 0 ) {
                            URL url2 = getResource(line, null);
                            index.add(url2);
                        }
                    }
                } 
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Collections.enumeration(index.stream().filter(f -> f.toString().endsWith(path)).collect(java.util.stream.Collectors.toList()));
    }

}
