/**
 * Copyright 2015 Codename One
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.codename1.cordova;

import ca.weblite.codename1.json.JSONArray;
import ca.weblite.codename1.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An ANT task that is designed to be run in the "precompile" step target of a 
 * Codename One cordova project.  This will generate the necessary cordova_plugins.js
 * file to allow the installed plugins to work.
 * 
 * <p>This is meant to be run from the build
 * @author shannah
 */
public class SetupCordovaPluginsTask extends Task {

    @Override
    public void execute() throws BuildException {
        File srcDir = new File(getProject().getBaseDir(), "src");
        File htmlDir = new File(srcDir, "html");
        if (!htmlDir.exists()) {
            log("No html directory found.  Skipping cordova plugins.js generation");
            return;
        }
        if (!isCordovaApp()) {
            log("This project is not a cordova project.  No cordova.js file was found.  Skipping cordova plugins.js generation");
            return;
        }
        FileWriter fos = null;
        try {
            File cordovaPluginsJs = new File(htmlDir, "cordova_plugins.js");
            
            fos = new FileWriter(cordovaPluginsJs);
            fos.write(generateCordovaPluginsJavascript());
            log(cordovaPluginsJs + " was generated successfully.");
        } catch (Exception ex) {
            throw new BuildException(ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ex){}
            }
        }
        
    }
    
    private boolean isCordovaApp() {
        File srcDir = new File(getProject().getBaseDir(), "src" + File.separator + "html");
        File libImplClsDir = new File(getProject().getBaseDir(), "lib" + File.separator + "impl" + File.separator + "cls" + File.separator + "html");
        List<File> out = new ArrayList<File>();
        for (File dir : new File[]{srcDir, libImplClsDir}) {
            for (File xmlFile : dir.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return "cordova.js".equals(name);
                }
            })) {
                out.add(xmlFile);
            };
        }
        return !out.isEmpty();
    }
    
    private List<File> findPluginDescriptors() {
        File srcDir = new File(getProject().getBaseDir(), "src");
        File libImplClsDir = new File(getProject().getBaseDir(), "lib" + File.separator + "impl" + File.separator + "cls");
        List<File> out = new ArrayList<File>();
        for (File dir : new File[]{srcDir, libImplClsDir}) {
            for (File xmlFile : dir.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("cordova-plugin-") && name.endsWith(".xml");
                }
            })) {
                out.add(xmlFile);
            };
        }
        return out;
    }
    
    /**
     * Generates the contents for the cordova_plugins.js file based on the registered
     * plugins.
     * @return 
     */
    private String generateCordovaPluginsJavascript() throws SAXException, IOException {
        ArrayList exports = new ArrayList();
        HashMap metadata = new HashMap();
        //Get the DOM Builder Factory
        DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();

        //Get the DOM Builder
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new BuildException(ex);
        }

    //Load and Parse the XML document
        //document contains the complete XML as a Tree.
        for (File configFile : findPluginDescriptors()) {
            Document document
                    = builder.parse(
                            new FileInputStream(configFile));
            Element root = document.getDocumentElement();
            String pluginId = root.getAttribute("id");
            NodeList jsModules = root.getElementsByTagName("js-module");
            
            for (int i=0; i<jsModules.getLength(); i++) {
                Element module = (Element)jsModules.item(i);
                if (module.getParentNode() != root) {
                    continue;
                }
                String id = pluginId + "." + module.getAttribute("name");
                String file = "plugins/" + pluginId+"/" + module.getAttribute("src");
                ArrayList<String> clobbers = new ArrayList<String>();
                NodeList clobbersList = module.getElementsByTagName("clobbers");
                for (int j=0; j < clobbersList.getLength(); j++) {
                    Element clobbersEl = (Element)clobbersList.item(j);
                    clobbers.add(clobbersEl.getAttribute("target"));
                }
                Map map = new HashMap();
                map.put("id", id);
                map.put("file", file);
                map.put("clobbers", clobbers);
                exports.add(map);
                        
            }
            
            metadata.put(pluginId, root.getAttribute("version"));
            
        }
        
        JSONArray outJSON = new JSONArray(exports);
        
        StringBuilder sb = new StringBuilder();
        sb.append("cordova.define('cordova/plugin_list', function(require, exports, module) {\n")
                .append("module.exports =")
                .append(outJSON.toString())
                .append(";\n");
        
        JSONObject metadataJSON = new JSONObject(metadata);
        sb.append("module.exports.metadata = ").append(metadataJSON.toString()).append(";\n").append("});");
        
        return sb.toString();
    }
    
}
