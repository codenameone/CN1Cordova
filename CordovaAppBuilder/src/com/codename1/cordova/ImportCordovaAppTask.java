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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author shannah
 */
public class ImportCordovaAppTask extends Task {

    @Override
    public void execute() throws BuildException {
        
        String source = getProject().getProperty("source");
        if (source == null) {
            throw new BuildException("No source path was provided.  Please set the source path for the application to be imported.  -Dsource=<path>");
        }
        
        File sourceDir = new File(source);
        if (!sourceDir.exists()) {
            throw new BuildException("Source path provided ("+sourceDir+") doesn't exist.");
        }
        
        if (!sourceDir.isDirectory()) {
            throw new BuildException(sourceDir+" is not a directory");
        }
        
        File configXml = new File(sourceDir, "config.xml");
        if (!configXml.exists()) {
            throw new BuildException(sourceDir+" does not appear to be a valid cordova project.  It is missing the config.xml file");
        }
        
        Element config = null;
        try {
            config = loadConfigFile(configXml);
        } catch (Exception ex) {
            throw new BuildException("Failed to parse config.xml", ex);
        }
        
        String packageId = config.getAttribute("id");
        String version = config.getAttribute("version");
        String name = null;
        NodeList children = config.getChildNodes();
        int len = children.getLength();
        for (int i=0; i<len; i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element el = (Element)node;
                if ("name".equals(el.getTagName())) {
                    name = el.getTextContent();
                } else if ("plugin".equals(el.getTagName())) {
                    log("The plugin "+el.getAttribute("name")+" was not automatically imported.  Please install the equivalent plugin as a .cn1lib");
                }
            }
        }
        
        
        File wwwDir = new File(sourceDir, "www");
        if (!wwwDir.exists()) {
            throw new BuildException("Expected to find www directory at "+wwwDir+" but found none.");
        }
        
        File srcDir = new File(getProject().getBaseDir(), "src");
        File htmlDir = new File(srcDir, "html");
        if (htmlDir.exists()) {
            File backupDir = new File(getProject().getBaseDir(), "backups");
            backupDir.mkdir();
            File backupSnapshot = new File(backupDir, "html"+System.currentTimeMillis());
            htmlDir.renameTo(backupSnapshot);
        }
        
        
        Copy copyWWW = (Copy)getProject().createTask("copy");
        FileSet wwwFS = new FileSet();
        wwwFS.setDir(wwwDir);
        wwwFS.setIncludes("**");
        copyWWW.addFileset(wwwFS);
        
        htmlDir.mkdir();
        copyWWW.setTodir(htmlDir);
        copyWWW.execute();
        
        //Properties codenameOneSettings = new Properties();
        CodenameOneCordovaProject project = new CodenameOneCordovaProject();
        project.setProjectDir(getProject().getBaseDir());
        project.setProjectName(name);
        project.setPackageId(packageId);
        project.setVersion(version);
        project.updateProject(this);
        log("Successfully imported cordova project at "+sourceDir);
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
}
