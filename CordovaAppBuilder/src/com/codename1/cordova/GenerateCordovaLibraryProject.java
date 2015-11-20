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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.types.FileSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * ANT task to generate a cordova library project.
 * <h3>Supported Properties</h3>
 * <ul>
 *  <li><code>id</code> : The ID for the plugin.  E.g. cordova-plugin-camera</li>
 *  <li><code>source</code> : The optional source path of the cordova plugin that is being converted.  This can be 
 *      a relative path, an absolute path, or a github URL to a project that contains the plugin.</li>
 * </ul>
 * @author shannah
 */
public class GenerateCordovaLibraryProject extends Task {

    @Override
    public void execute() throws BuildException {
        List<File> toDelete = new ArrayList<File>();
        try {
            String id = getProject().getProperty("id");
            if (id == null) {
                throw new BuildException("No 'id' parameter specified");
            }

            String sourcePath = getProject().getProperty("source");
            if (sourcePath == null || sourcePath.startsWith("http://") || sourcePath.startsWith("https://")) {
                try {
                    // If we didn't provide a source path, let's create a dummy
                    // cordova project, install the plugin, and try to extract the
                    // plugin from there.

                    ExecTask exec = (ExecTask)getProject().createTask("exec");
                    File tmpDir = File.createTempFile("cordova", "temp");
                    toDelete.add(tmpDir);
                    tmpDir.delete();
                    tmpDir.mkdir();

                    exec.setDir(tmpDir);
                    exec.setExecutable("cordova");
                    exec.createArg().setValue("create");
                    exec.createArg().setValue("hello");
                    exec.createArg().setValue("com.example.hello");
                    exec.createArg().setValue("Hello");
                    exec.execute();


                    // Now add the plugin
                    File appDir = new File(tmpDir, "hello");
                    if (!appDir.exists()) {
                        throw new BuildException("Failed to create temp app directory");
                    }

                    exec = (ExecTask)getProject().createTask("exec");
                    exec.setDir(appDir);
                    exec.setExecutable("cordova");
                    exec.createArg().setValue("plugin");
                    exec.createArg().setValue("add");
                    if (sourcePath != null) {
                        // The source path points to a github URL.
                        // See examples here https://cordova.apache.org/docs/en/latest/guide/cli/index.html
                        exec.createArg().setValue(sourcePath);
                    } else {
                        exec.createArg().setValue(id);
                    }

                    exec.execute();

                    File pluginsDir = new File(appDir, "plugins");
                    File pluginDir = new File(pluginsDir, id);
                    if (!pluginDir.exists()) {
                        throw new BuildException("Failed to create plugin directory in dummy project");
                    }

                    sourcePath = pluginDir.getPath();


                } catch (IOException ex) {
                    throw new BuildException("Attempt to create tmp dir for dummy cordova project failed", ex);
                }
            }

            File sourceDir = new File(sourcePath);

            File libraryProjectTemplateDir = new File(getProject().getBaseDir(), "CordovaLibraryProjectTemplate");
            if (!libraryProjectTemplateDir.exists()) {
                throw new BuildException("CordovaLibraryProjectTemplate is missing");
            }

            File destDir = getProject().getBaseDir();
            if (getProject().getProperty("dest") != null) {
                destDir = new File(getProject().getProperty("dest"));
            }
            if (!destDir.exists()) {
                throw new BuildException("Destination directory "+destDir+" doesn't exist");
            }

            File libProjectDir = new File(destDir, id);
            if (libProjectDir.exists()) {
                throw new BuildException("Target directory "+libProjectDir+" already exists");
            }
            libProjectDir.mkdir();

            Copy copy = (Copy)getProject().createTask("copy");

            copy.setTodir(libProjectDir);
            FileSet toCopy = new FileSet();
            toCopy.setDir(libraryProjectTemplateDir);
            toCopy.setIncludes("**");
            copy.addFileset(toCopy);

            copy.execute();

            File srcDir = new File(libProjectDir, "src");
            if (!srcDir.exists()) {
                throw new BuildException("Failed to create project src directory "+srcDir);
            }

            File htmlDir = new File(srcDir, "html");
            htmlDir.mkdir();

            File htmlPluginsDir = new File(htmlDir, "plugins");
            File htmlPluginDir = new File(htmlPluginsDir, id);
            File htmlPluginWWWDir = new File(htmlPluginDir, "www");
            htmlPluginWWWDir.mkdirs();

            copy = (Copy)getProject().createTask("copy");
            copy.setTodir(htmlPluginWWWDir);
            toCopy = new FileSet();
            toCopy.setDir(new File(sourceDir, "www"));
            toCopy.setIncludes("**");
            copy.addFileset(toCopy);
            copy.execute();

            File pluginXMLDest = new File(srcDir, id + ".xml");
            if (!pluginXMLDest.getName().startsWith("cordova-plugin-")) {
                pluginXMLDest = new File(srcDir, "cordova-plugin-"+id);
            }

            File pluginXML = new File(sourceDir, "plugin.xml");
            if (!pluginXML.exists()) {
                throw new BuildException("No plugin.xml found");
            }

            copy = (Copy)getProject().createTask("copy");
            copy.setTofile(pluginXMLDest);
            copy.setFile(pluginXML);
            copy.execute();

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


            try {
                Document doc = builder.parse(pluginXML);
                Element root = doc.getDocumentElement();
                NodeList children = root.getChildNodes();
                int len = children.getLength();
                for (int i=0; i<len; i++) {
                    Node node = children.item(i);
                    if (node instanceof Element) {
                        Element el = (Element)node;
                        if ("js-module".equals(el.getTagName())) {
                            String modPath = el.getAttribute("src").replace("/", File.separator);
                            File modJSFile = new File(htmlPluginDir, modPath);
                            if (!modJSFile.exists()) {
                                log("plugin.xml file includes js-module for "+modJSFile+" but the file cannot be found.  Skipping...");
                            }

                            String jsContents = Util.readToString(modJSFile);
                            jsContents = "cordova.define(\""+id+"."+el.getAttribute("name")+"\", function(require, exports, module) { \n" 
                                    + jsContents + "\n});";

                            Util.writeStringToFile(jsContents, modJSFile);
                        }
                    }
                }

            } catch (SAXException ex) {
                throw new BuildException("Failed to parse plugin.xml file at "+pluginXML, ex);
            } catch (IOException ex) {
                throw new BuildException("Failed to read plugin.xml file at "+pluginXML, ex);
            }
            
            // Now to update the properties file.
            File projectProperties = new File(new File(libProjectDir, "nbproject"), "project.properties");
            if (!projectProperties.exists()) {
                throw new BuildException("Failed to update project properties because project.properties not found at "+projectProperties);
            }
            
            try {
                Properties props = Util.loadProperties(projectProperties);
                props.setProperty("application.title", id);
                props.setProperty("dist.jar", "${dist.dir}/"+id+".jar");
                Util.saveProperties(props, projectProperties, "Project properties updated by GenerateCordovaLibraryProject "+new Date());
                
            } catch (IOException ex) {
                throw new BuildException("Failed to load project properties", ex);
            }
            
            log("Codename One Cordova library project successfully created at "+libProjectDir);
        } finally {
            // We created some temporary directories in this process.
            // We'll delete them here.
            for (File f : toDelete) {
                Delete delete = (Delete)getProject().createTask("delete");
                delete.setDir(f);
                delete.execute();
            }
        }

        
        
    }
    
}
