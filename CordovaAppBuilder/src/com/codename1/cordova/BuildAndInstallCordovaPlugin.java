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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.CallTarget;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Ant;




/**
 * ANT task to be called in the context of a CordovaLibraryProject that will build
 * it as a .cn1lib, and install it in a CordovaProject residing one level up 
 * from the library project.
 * 
 * <p>This ant task should be made available from the library project's build.xml file.</p>
 * @author shannah
 */
public class BuildAndInstallCordovaPlugin extends Task {
    
    

    @Override
    public void init() throws BuildException {
        super.init(); //To change body of generated methods, choose Tools | Templates.
        
    }
    
    

    @Override
    public void execute() throws BuildException {
        
        File appProjectDir = getProject().getBaseDir().getParentFile();
        if ("plugins".equals(appProjectDir.getName())) {
            appProjectDir = appProjectDir.getParentFile();
        }
        if (!new File(appProjectDir, "codenameone_settings.properties").exists()) {
            throw new BuildException("Cannot install plugin because library project directory's parent directory is not a Codename One project");
        }
        
        File distDir = new File(getProject().getBaseDir(), "dist");
        Properties props = null;
        try {
            props = Util.loadProperties(new File(new File(getProject().getBaseDir(), "nbproject"), "project.properties"));
        } catch (IOException ex) {
            throw new BuildException("Failed to load project properties", ex);
        }
        String appTitle = props.getProperty("application.title");
        if (appTitle == null) {
            throw new BuildException("Failed to find application.title in the project properties");
        }
        File cn1libFile = new File(distDir, appTitle+".cn1lib");
        if (!cn1libFile.exists()) {
            if (getProject().getTargets().containsKey("jar")) {
                CallTarget antCall = (CallTarget)getProject().createTask("antcall");
                antCall.setTarget("jar");
                antCall.execute();
            } else {
                Ant ant = (Ant)getProject().createTask("ant");
                ant.setDir(getProject().getBaseDir());
                ant.setUseNativeBasedir(true);
                ant.setTarget("jar");
                ant.setInheritAll(false);
                ant.execute();
            }
            
            if (!cn1libFile.exists()) {
                throw new BuildException("Failed to build cn1lib "+cn1libFile);
            }
        }
        
        Copy copy = (Copy)getProject().createTask("copy");
        copy.setTodir(new File(appProjectDir, "lib"));
        copy.setFile(cn1libFile);
        copy.execute();
        
        Ant ant = (Ant)getProject().createTask("ant");
        ant.setDir(appProjectDir);
        ant.setTarget("refresh-libs");
        ant.setUseNativeBasedir(true);
        ant.setInheritAll(false);
        ant.execute();
        
        File cordovaToolsDir = new File(appProjectDir, "cordova-tools");
        ant = (Ant)getProject().createTask("ant");
        ant.setDir(cordovaToolsDir);
        ant.setTarget("refresh-plugins");
        ant.setUseNativeBasedir(true);
        ant.setInheritAll(false);
        ant.execute();
        
        log("Plugin has been successfully installed in project at "+appProjectDir);
        log("Note:  If the plugin has native components, you'll still need to register them with the CordovaApplication object inside your app's start() method by calling the registerPlugin() method of CordovaApplication.");
        
        
        
    }
    
}
