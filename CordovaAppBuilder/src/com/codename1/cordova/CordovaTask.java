/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.cordova;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import javax.lang.model.element.Modifier;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Unpack;
import org.apache.tools.ant.types.FileSet;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author shannah
 */
public class CordovaTask extends Task {
    String appId;
    File baseDir,
            cn1ProjectDir,
            cn1ProjectSrcDir,
            cn1ProjectHtmlDir,
            docRootDir
            ;
    

    @Override
    public void execute() throws BuildException {
        
        baseDir = this.getProject().getBaseDir();
        File platformsDir = new File(baseDir, "platforms");
        File cn1PlatformDir = new File(platformsDir, "cn1");
        File wwwPlatformDir = new File(cn1PlatformDir, "www");
        File wwwDir = new File(baseDir, "www");
        File pluginsDir = new File(baseDir, "plugins");
        File configXmlFile = new File(baseDir, "config.xml");
        
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new BuildException(ex);
        }
        Document config = null;
        try {
            config = dBuilder.parse(configXmlFile);
        } catch (SAXException ex) {
            throw new BuildException(ex);
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
       
        
        // We will use the appId as the package name for the codename one app
        appId = config.getDocumentElement().getAttribute("id");
        
        String cn1ProjectPath = this.getProject().getProperty("cn1.project.basedir");
        if (cn1ProjectPath == null) {
            cn1ProjectPath = new File(cn1PlatformDir, "NetbeansProject").getPath();
        }
        cn1ProjectDir = new File(cn1ProjectPath);
        cn1ProjectSrcDir = new File(cn1ProjectDir, "src");
        cn1ProjectHtmlDir = new File(cn1ProjectSrcDir, "html");
        
        String docRootPath = getProject().getProperty("cn1.docroot");
        if (docRootPath == null) {
            docRootPath = cn1ProjectHtmlDir.getPath();
        }
        docRootDir = new File(docRootPath);
        
        if (!cn1ProjectDir.exists()) {
            File templateFile = null;
            try {
                templateFile = Util.copyToTempFile(this.getClass().getResourceAsStream("NetbeansProjectTemplate.zip"));
                Unpack unzip = (Unpack)this.getProject().createTask("unzip");
                unzip.setDest(cn1ProjectDir);
                unzip.setSrc(templateFile);
                unzip.execute();
                unzip.setDescription("Extracting Netbeans project template");
            } catch (IOException ex) {
                throw new BuildException(ex);
            } finally {
                if (templateFile != null) {
                    templateFile.delete();
                }
            }
        }
        
        if (!docRootDir.exists()) {
            docRootDir.mkdirs();
        }
        Copy copy = (Copy)this.getProject().createTask("copy");
        FileSet wwwFiles = new FileSet();
        wwwFiles.setDir(wwwDir);
        wwwFiles.setIncludes("*");
        copy.addFileset(wwwFiles);
        copy.setTodir(docRootDir);
        copy.execute();
        
        File cordovaJSFile = new File(docRootDir, "cordova.js");
        try {
            Util.copyToFile(this.getClass().getResourceAsStream("cordova.js"), cordovaJSFile);
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
        
        writeMainJavaFile();
        
        // Update the properties
        
        Properties cn1SettingsProps = new Properties();
        File cn1SettingsPropsFile = new File(cn1ProjectDir, "codenameone_settings.properties");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(cn1SettingsPropsFile);
            cn1SettingsProps.load(fis);
        } catch (Exception ex) {
            throw new BuildException(ex);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ex){}
            }
        }
        
        cn1SettingsProps.setProperty("codename1.packageName", appId);
        cn1SettingsProps.setProperty("codename1.mainName", "Main");
        String iosAppId = cn1SettingsProps.getProperty("codename1.ios.appid");
        iosAppId = iosAppId.substring(0, iosAppId.indexOf(".")) + "." + appId;
        cn1SettingsProps.setProperty("codename1.ios.appid", iosAppId);
        
        
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(cn1SettingsPropsFile);
            cn1SettingsProps.store(fos, "Updated by CordovaAppBuilder "+new Date());
        } catch (Exception ex) {
            throw new BuildException(ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ex){}
            }
        }
         
        new File(cn1ProjectDir, "override").mkdirs();
        new File(cn1ProjectDir, "native" + File.separator + "internal_tmp").mkdirs();
        
        this.log("A Codename One Netbeans project has been created at "+cn1ProjectDir);
            
    }
    
    void writeMainJavaFile() {
        
        try {
            MethodSpec init = MethodSpec.methodBuilder("init")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addParameter(Object.class, "context")
                    .addStatement("theme = $T.initFirstTheme(\"/theme\");", ClassName.get("com.codename1.ui.plaf", "UIManager"))
                    .build();
            
            
            
            /*public void start() {
            if(current != null){
            current.show();
            return;
            }
            CordovaApplication app = new CordovaApplication();
            try {
            app.load("index.html");
            } catch (Exception ex) {
            Log.e(ex);
            }
            app.show();
            }*/
            MethodSpec start = MethodSpec.methodBuilder("start")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .beginControlFlow("if (current != null)")
                    .addStatement("current.show()")
                    .addStatement("return")
                    .endControlFlow()
                    .addStatement("$T app = new $T();", ClassName.get("com.codename1.cordova", "CordovaApplication"), ClassName.get("com.codename1.cordova", "CordovaApplication"))
                    .beginControlFlow("try")
                    .addStatement("app.load(\"index.html\")")
                    .beginControlFlow("catch ($T ex)", Exception.class)
                    .addStatement("Log.e(ex)")
                    .endControlFlow()
                    .addStatement("app.show();")
                    .build();
            
            
            /*public void stop() {
            current = Display.getInstance().getCurrent();
            }
            
            public void destroy() {
            }
            */
            
            
            
            MethodSpec stop = MethodSpec.methodBuilder("stop")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addStatement("current = $T.getInstance().getCurrent();", ClassName.get("com.codename1.ui", "Display"))
                    .build();
            
            
            MethodSpec destroy = MethodSpec.methodBuilder("destroy")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .build();
            
            TypeSpec mainClass = TypeSpec.classBuilder("Main")
                    
                    .addField(ClassName.get("com.codename1.ui.util", "Resources"), "theme")
                    .addField(ClassName.get("com.codename1.ui", "Form"), "current")
                    .addModifiers(Modifier.PUBLIC)
                    .addMethod(init)
                    .addMethod(start)
                    .addMethod(stop)
                    .addMethod(destroy)
                    .build();
            
            JavaFile classFile = JavaFile.builder(appId, mainClass)
                    .addFileComment("This file was automatically generated.  Changes may be overwritten")
                    .build();
            
            classFile.writeTo(cn1ProjectSrcDir);
        } catch (IOException ex) {
            throw new BuildException(ex);
        }
        
    }
    
    
    
}
