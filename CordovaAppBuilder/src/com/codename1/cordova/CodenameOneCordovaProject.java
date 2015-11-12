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
import java.util.Date;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Replace;

/**
 * Encapsulates a Codename One project and allows you to update certain settings
 * of the project like packageId, version, projectName, etc..
 * @author shannah
 */
public class CodenameOneCordovaProject {
    
    /**
     * The project directory that is being modified.
     */
    private File projectDir;
    
    /**
     * The packageName for the project.
     */
    private String packageId;
    
    /**
     * The projectName  (i.e. app name)
     * 
     */
    private String projectName;
    
    /**
     * The project version.
     */
    private String version;

    /**
     * @return the projectDir
     */
    public File getProjectDir() {
        return projectDir;
    }

    /**
     * @param projectDir the projectDir to set
     */
    public void setProjectDir(File projectDir) {
        this.projectDir = projectDir;
    }

    /**
     * @return the packageId
     */
    public String getPackageId() {
        return packageId;
    }

    /**
     * @param packageId the packageId to set
     */
    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    /**
     * @return the projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @param projectName the projectName to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    
    /**
     * Updates the project in the file system.  This includes changing the package name,
     * app name, version, etc... in the various properties and xml files.  If the 
     * package name is changed, it will actually move the old package to the new package
     * and update references to the old package to point to the new package.
     * 
     * @param context The ANT task that is calling this updateProject.  Will be used to gain access to the ANT runtime environment.
     */
    public void updateProject(Task context) throws BuildException {
        //Properties codenameOneSettings = new Properties();
        if (projectDir == null) {
            throw new BuildException("Cannot update project with null projectDir");
        }
        File codenameOneSettingsFile = new File(projectDir, "codenameone_settings.properties");
        if (!codenameOneSettingsFile.exists()) {
            throw new BuildException("No codenameone_settings.properties file found at "+codenameOneSettingsFile);
        }
        
        File srcDir = new File(projectDir, "src");
        
        Properties codenameOneSettings = null;
        try {
            codenameOneSettings = Util.loadProperties(codenameOneSettingsFile);
        } catch (Exception ex) {
            throw new BuildException("Failed to load project settings "+codenameOneSettingsFile, ex);
        }
        
        if (version != null) {
            try {
                double ver = Double.parseDouble(version);
                codenameOneSettings.setProperty("codename1.version", "" + ver);
            } catch(NumberFormatException err) {
                System.out.println("Couldn't convert version, Codename One versions must be decimal numbers for increased portability");
            } 
            
        }
        
        if (packageId != null) {
            String oldPackage = codenameOneSettings.getProperty("codename1.packageName");
            codenameOneSettings.setProperty("codename1.packageName", packageId);
            String iosAppId = codenameOneSettings.getProperty("codename1.ios.appid");
            if (iosAppId == null) {
                iosAppId = "BQ5FVWYLLB.com.codename1.demos.cordova";
            }
            iosAppId = iosAppId.substring(0, iosAppId.indexOf("."))+"."+packageId;
            codenameOneSettings.setProperty("codename1.ios.appid", iosAppId);
            
            if (!packageId.equals(oldPackage)) {
                String oldMainPath = oldPackage.replace(".", File.separator);
                String newMainPath = packageId.replace(".", File.separator);
                File oldMainFile = new File(srcDir, oldMainPath);
                File newMainFile = new File(srcDir, newMainPath);

                if (!oldMainFile.exists()) {
                    throw new BuildException("Could not find old main file: "+oldMainFile);
                }

                File oldMainDir = oldMainFile.getParentFile();
                File newMainDir = newMainFile.getParentFile();

                newMainDir.getParentFile().mkdirs();
                if (newMainDir.exists()) {
                    for (File f : oldMainDir.listFiles()) {
                        if (".".equals(f.getName()) || "..".equals(f.getName())) {
                            continue;
                        }
                        f.renameTo(new File(newMainDir, f.getName()));
                    }
                    oldMainDir.delete();
                } else {

                    oldMainDir.renameTo(newMainDir);
                }

                Replace replaceTask = (Replace)context.getProject().createTask("replace");
                replaceTask.setDir(srcDir);
                replaceTask.setIncludes("**/*.java");
                Replace.NestedString token = replaceTask.createReplaceToken();
                token.addText(oldPackage);

                replaceTask.createReplaceValue().addText(packageId);
                replaceTask.execute();
            }
            
        }
        
        if (projectName != null) {
            codenameOneSettings.setProperty("codename1.displayName", projectName);
        }
        
        
        
        
        try {
            Util.saveProperties(codenameOneSettings, codenameOneSettingsFile, "Updated by ImportCordovaAppTask on "+new Date());
        } catch (IOException ex) {
            throw new BuildException("Failed to save codename one settings file "+codenameOneSettingsFile, ex);
        }
        
        
        File nbprojectDir = new File(projectDir, "nbproject");
        
        new File(projectDir, "override").mkdirs();
        new File(projectDir, "native" + File.separator + "internal_tmp").mkdirs();
        File nbprojectPropertiesFile = new File(nbprojectDir, "project.properties");
        if (nbprojectPropertiesFile.exists()) {
            Properties nbprojectProperties = null;
            context.log("Updating Netbeans properties file "+nbprojectPropertiesFile);
             try {
                nbprojectProperties = Util.loadProperties(nbprojectPropertiesFile);
                
            } catch (Exception ex) {
                throw new BuildException("Failed to load netbeans project properties at "+nbprojectPropertiesFile);
            }
            if (projectName != null) {
                String normalizedName = projectName.replaceAll("[^a-zA-Z0-9]", "");
            
                nbprojectProperties.setProperty("application.title", projectName);
                nbprojectProperties.setProperty("dist.jar", "${dist.dir}/"+normalizedName+".jar");
            }
            
            try {
                Util.saveProperties(nbprojectProperties, nbprojectPropertiesFile, "updated by "+context.getTaskName()+ new Date());
                
            } catch (Exception ex) {
                throw new BuildException("Failed to save changes to "+nbprojectPropertiesFile, ex);
            } 
            
            if (projectName != null) {
            
                Replace replaceProjectName = (Replace)context.getProject().createTask("replace");
                replaceProjectName.setFile(new File(nbprojectDir, "project.xml"));
                replaceProjectName.createReplaceToken().addText("<name>CordovaProjectTemplate</name>");
                replaceProjectName.createReplaceValue().addText("<name>"+projectName.replace("<", "&lt;").replace(">", "&gt;")+"</name>");
                replaceProjectName.execute();
            }
        }
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }
    
    
}
