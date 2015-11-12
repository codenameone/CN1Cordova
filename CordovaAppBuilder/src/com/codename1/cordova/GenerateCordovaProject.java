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
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Property;
import org.apache.tools.ant.types.FileSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * ANT task to generate a new Codename One Cordova Project.  This is to be used inside
 * the cn1-cordova-tools build.xml.
 * 
 * <p>There are two variants:</p>
 * <ol>
 * <li>Creating a new project from scratch</li>
 * <li>Importing an existing project</li>
 * </ol>
 * 
 * <p>Usage:</p>
 * <p>To import an existing app.
 * <p><code>ant create -Dsource=/path/to/cordova/app [-Ddest=/path/to/outputdir]</code></p>
 * 
 * <p>To create a new app.</p>
 * <p><code>ant create -Did=com.example.hello -Dname="Hello World" [-Ddest=/path/to/outputdir]</code></p>
 * 
 * @author shannah
 */
public class GenerateCordovaProject extends Task {

    @Override
    public void execute() throws BuildException {
        List<File> filesToDelete = new ArrayList<File>();
        try {
            String source = this.getProject().getProperty("source");
            
            // The packageId to set with the project.   We'll either get this
            // from the 'source' project's config.xml file or the 'id' parameter.
            String packageId = null;
            
            // The name of the app.  We'll either get this from the 'name' parameter
            // or the source project's config.xml file.
            String appName = null;
            
            // For the normalized App name used for the directory name for the project
            // This is the app name with all non-alpha-numeric characters removed.
            String normalizedAppName = null;
            
            // The project template directory should always be present.  We use this as a 
            // base to copy the project.
            File projectTemplate = new File(getProject().getBaseDir(), "CordovaProjectTemplate");
            if (!projectTemplate.exists()) {
                throw new BuildException("Could not find project template at "+projectTemplate);
            }

            if (source != null) {
                // 'source' argument was specified so we use it to try to load 
                // properties from existing cordova project's config.xml file.
                File sourceDir = new File(source);
                if (!sourceDir.exists()) {
                    throw new BuildException(sourceDir + " does not exist.");
                }
                File configXml = new File(sourceDir, "config.xml");
                if (!configXml.exists()) {
                    throw new BuildException(sourceDir + " is not a valid Cordova app.  Failed to find "+configXml);
                }

                Element config = null;
                try {
                    config = loadConfigFile(configXml);
                } catch (Exception ex) {
                    throw new BuildException("Failed to load "+configXml, ex);
                }

                normalizedAppName = getAppName(config);
                if (normalizedAppName == null) {
                    throw new BuildException("Could not find app name in config file "+configXml);
                }

                normalizedAppName = normalizedAppName.replaceAll("[^a-zA-Z0-9]", "");
                if (normalizedAppName.length() == 0) {
                    throw new BuildException("App name must contain at least one letter or digit");
                }
            } else if (getProject().getProperty("name") != null && getProject().getProperty("id") != null) {
                // 'source' was not specified, but name and id were specified
                // We'll copy the project template and update the package ID and name 
                // appropriately.
                packageId = getProject().getProperty("id");
                appName = getProject().getProperty("name");
                normalizedAppName = appName.replaceAll("[^a-zA-Z0-9]", "");
                if (normalizedAppName.length() == 0) {
                    throw new BuildException("App name must contain at least one letter or digit");
                }
                
            } else {
                throw new BuildException("Please specify a source with -Dsource=<...> CLI option to migrate an existing project, or use the -Did and -Dname options to create a new project.");
            }

            File outputDir = getProject().getProperty("dest") == null ? getProject().getBaseDir() : new File(getProject().getProperty("dest"));

            log("Output directory is "+outputDir);
            
            if (!outputDir.exists()) {
                throw new BuildException("Output directory "+outputDir+" does not exist");
            }


            File destDir = new File(outputDir, normalizedAppName);
            if (destDir.exists()) {
                throw new BuildException("The directory "+destDir+" already exists");
            }

            Copy copy = (Copy)getProject().createTask("copy");
            copy.setTodir(destDir);
            FileSet includes = new FileSet();
            includes.setDir(projectTemplate);
            includes.setIncludes("**");
            copy.addFileset(includes);
            copy.execute();


            if (source != null) {
                // If a source project was specified to migrate from, we will
                // just run the "import" target of the project's cordova-tools
                // project.
                Ant ant = (Ant)getProject().createTask("ant");
                ant.setUseNativeBasedir(true);
                File cordovaToolsDir = new File(destDir, "cordova-tools");
                //File cordovaToolsBuildFile = new File(cordovaToolsDir, "build.xml");
                //ant.setAntfile(cordovaToolsBuildFile.getAbsolutePath());
                ant.setDir(cordovaToolsDir);
                ant.setTarget("import");
                ant.setInheritAll(false);
                Property sourceProp = ant.createProperty();
                sourceProp.setName("source");
                sourceProp.setValue(source);

                ant.execute();
            } else {
                // No source project was specified, so we will update the project
                // properties separately.
                
                CodenameOneCordovaProject project = new CodenameOneCordovaProject();
                project.setProjectDir(destDir);
                project.setProjectName(appName);
                project.setPackageId(packageId);
                project.updateProject(this);
            }

            log("Your Codename One Cordova Project has been created at "+destDir+".  \nYou can now open this project in NetBeans");


            //}
        } finally {
            for (File f : filesToDelete) {
                if (f.exists()) {
                    if (!f.isDirectory()) {
                        f.delete();
                    } else {
                        Delete del = (Delete)getProject().createTask("delete");
                        del.setDir(f);
                        del.execute();
                    }
                }
            }
        }
                
    }
    
    private Element loadConfigFile(File configXml) throws SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new BuildException(ex);
        }
        Document config = null;
        config = dBuilder.parse(configXml);
        return config.getDocumentElement();
    }
    
    private String getAppName(Element widgetEl) {
        NodeList children = widgetEl.getChildNodes();
        int len = children.getLength();
        for (int i=0; i<len; i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element el = (Element)node;
                if ("name".equals(el.getTagName())) {
                    return el.getTextContent();
                }
            }
        }
        return null;
    }
    
}
