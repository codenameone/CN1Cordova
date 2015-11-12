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
import com.codename1.io.JSONParser;
import com.codename1.io.Log;
import com.codename1.javascript.JSFunction;
import com.codename1.javascript.JSObject;
import com.codename1.javascript.JavascriptContext;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Command;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import com.codename1.ui.layouts.BorderLayout;
import com.codename1.util.Base64;
import com.codename1.util.Callback;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author shannah
 */
public class CordovaApplication extends Form {
    private final BrowserComponent webview;
    private JavascriptContext context;
    private Map<String,CordovaPlugin> pluginMap = new HashMap<String,CordovaPlugin>();
    private static Map<String,CordovaPlugin> globalPluginMap = new HashMap<String,CordovaPlugin>();
    private JSONParser jsonParser = new JSONParser();
    
    public static enum Result {
        NO_RESULT,
        OK,
        CLASS_NOT_FOUND_EXCEPTION,
        ILLEGAL_ACCESS_EXCEPTION,
        INSTANTIATION_EXCEPTION,
        MALFORMED_URL_EXCEPTION,
        IO_EXCEPTION,
        INVALID_ACTION,
        JSON_EXCEPTION,
        ERROR
    };
    
    
    public CordovaApplication() {
        pluginMap.putAll(globalPluginMap);
        webview = new BrowserComponent();
        webview.addWebEventListener("onError", new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            System.out.println("There was an error: "+evt.getSource());
        
            }
        });
        
        
        webview.addWebEventListener("onLoad", new ActionListener() {
            public void actionPerformed(ActionEvent evt){
                context = new JavascriptContext(webview);
                context.getWindow().set("echo", new JSFunction() {

                    @Override
                    public void apply(JSObject self, Object[] args) {
                        System.out.println(""+args[0]);
                    }

                }, true);
                
                
                context.getWindow().set("CN1Exec", new JSFunction() {

                        @Override
                        public void apply(JSObject self, Object[] arguments) {
                            //System.out.println("Inside CN1Excec");
                            //System.out.println(Arrays.toString(arguments));
                            final String callbackId = (String)arguments[0];
                            String service = (String)arguments[1];
                            
                            CordovaPlugin plugin = pluginMap.get(service);
                            if (plugin == null) {
                                webview.execute("cordova.callbackError("+JSONObject.quote(callbackId)+", {message:'Plugin not found', status:"+Result.INVALID_ACTION.ordinal()+", keepCallback:false})");
                                return;
                            }
                            
                            
                            String action = (String)arguments[2];
                            
                            Callback callback = new Callback() {

                                @Override
                                public void onSucess(Object value) {


                                    String valueJSON = null;
                                    if (value == null) {
                                        valueJSON = "null";
                                    } else if (value instanceof String) {
                                        valueJSON = JSONObject.quote((String)value);
                                    } else if (value instanceof ArrayList) {
                                        JSONArray jsonArr = new JSONArray((ArrayList)value);
                                        valueJSON = jsonArr.toString();
                                    } else if (value instanceof HashMap) {
                                        JSONObject jsonObj = new JSONObject((HashMap)value);
                                        valueJSON = jsonObj.toString();
                                    } else if (value instanceof Double || value instanceof Integer || value instanceof Float || value instanceof Short) {
                                        try {
                                            valueJSON = JSONObject.numberToString(value);
                                        } catch (Exception ex) {
                                            valueJSON = "null";
                                        }
                                    } else if (value instanceof Boolean) {
                                        valueJSON = ((Boolean)value).booleanValue() ? "true" : "false";
                                    } else if (value instanceof Character) {
                                        valueJSON = "\""+value+"\"";
                                    } else {
                                        throw new RuntimeException("Result value "+value+" is not a recognized type");
                                    }
                                    //System.out.println("About call cordova.callbackSuccess "+valueJSON);
                                    webview.execute("cordova.callbackSuccess("+JSONObject.quote(callbackId)+
                                            ", {message:"+valueJSON+", status: "+Result.OK.ordinal()+", keepCallback:false})");

                                }

                                @Override
                                public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                                    webview.execute("cordova.callbackError("+JSONObject.quote(callbackId)+", {message:"+errorMessage+", status: "+Result.ERROR.ordinal()+", keepCallback:false})");
                                }

                            };

                        


                    
                            
                            
                            String actionArgsJSON = (String)arguments[3];
                            
                            // First try the version of the plugin that accepts a JSON string as an argument.
                            if (plugin.execute(action, actionArgsJSON, callback)) {
                                return;
                            }
                            
                            // If that version of execute doesn't find a match, then we will convert the JSON string into
                            // a List and try the other version.
                            List actionArgs = null;
                            if (actionArgsJSON == null) {
                                actionArgs = new ArrayList();
                            } else {
                                InputStreamReader r = null;
                                try {
                                    r = new InputStreamReader(new ByteArrayInputStream(actionArgsJSON.getBytes()));
                                    actionArgs = (List)jsonParser.parseJSON(r).get("root");
                                } catch (Exception ex) {
                                    Log.e(ex);
                                    throw new RuntimeException(ex.getMessage());
                                } finally {
                                    if (r != null) {
                                        try {
                                            r.close();
                                        } catch (Exception ex){}
                                    }
                                }


                            }


                            for (int i=0; i<actionArgs.size(); i++) {
                                Object arg = actionArgs.get(i);
                                if (arg instanceof Map && "ArrayBuffer".equals(((Map)arg).get("CDVType"))) {
                                    byte[] bytes = Base64.decode(((String)((Map)arg).get("data")).getBytes());
                                    actionArgs.set(i, bytes);
                                }
                            }

                            
                            if (!plugin.execute(action, actionArgs, callback)) {
                                webview.execute("cordova.callbackError("+JSONObject.quote(callbackId)+", {message:'Action not found', status:"+Result.ERROR.ordinal()+", keepCallback:false})");
                            }
                        }
                }
                    , true
                );
                
                //webview.execute("console.log('about to call native ready');");
                webview.execute("window._nativeReady = true; cordova.require('cordova/channel').onNativeReady.fire()");
                
            }
        });
        
        setLayout(new BorderLayout());
        getAllStyles().setPadding(0, 0, 0, 0);
        getAllStyles().setMargin(0, 0, 0, 0);
        webview.getAllStyles().setPadding(0, 0, 0, 0);
        webview.getAllStyles().setMargin(0, 0, 0, 0);
        getTitleArea().setPreferredH(0);
        addComponent(BorderLayout.CENTER, webview);
        
        setBackCommand(new Command("") {

            @Override
            public void actionPerformed(ActionEvent evt) {
                webview.execute("cordova.require('cordova/channel').onPause.fire()");
            }
            
        });
    }
    
    
    
    
    
    
    public void load(String url) throws IOException {
        webview.setURLHierarchy(url);
        
    }
    
    
    public void addPlugin(String service, CordovaPlugin plugin) {
        pluginMap.put(service, plugin);
    }
    
    
    public void pause() {
        webview.execute("cordova.require('cordova/channel').onPause.fire()");
    }
    
    public void resume() {
        webview.execute("cordova.require('cordova/channel').onResume.fire()");
    }
    
    public static void addGlobalPlugin(String service, CordovaPlugin plugin) {
        globalPluginMap.put(service, plugin);
    }
    
   
}
