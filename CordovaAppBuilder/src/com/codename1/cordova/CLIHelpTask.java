/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.cordova;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 *
 * @author shannah
 */
public class CLIHelpTask extends Task {

    @Override
    public void execute() throws BuildException {
        try {
            String type = getProject().getProperty("type");
            if (type == null) {
                type = "*";
            }
            switch (type) {
                case "project" :
                    // Help with the ant task inside a project's cordova-tools directory.
                    Desktop.getDesktop().browse(new URI("https://github.com/codenameone/CN1Cordova/wiki/Project-cordova-tools-CLI-Usage"));
                    break;
                case "library-project" :
                    Desktop.getDesktop().browse(new URI("https://github.com/codenameone/CN1Cordova/wiki/Library-project-cordova-tools-CLI-usage"));
                    break;
                default:
                    Desktop.getDesktop().browse(new URI("https://github.com/codenameone/CN1Cordova/wiki/cn1-cordova-tools-CLI-usage"));
                    break;       
            }
        } catch (IOException ex) {
            Logger.getLogger(CLIHelpTask.class.getName()).log(Level.SEVERE, null, ex);
            throw new BuildException("Failed to open documentation in browser.", ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(CLIHelpTask.class.getName()).log(Level.SEVERE, null, ex);
            throw new BuildException("Failed to open documentation in browser.", ex);
        }
    }
    
}
