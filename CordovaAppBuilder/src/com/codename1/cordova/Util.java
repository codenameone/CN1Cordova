/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.cordova;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Various utility methods used for HTTP/IO operations
 *
 * @author Shai Almog
 */
public class Util {
    /**
     * Copy the input stream into the output stream, closes both streams when finishing or in
     * a case of an exception
     * 
     * @param i source
     * @param o destination
     */
    public static void copy(InputStream i, OutputStream o) throws IOException {
        copy(i, o, 8192);
    }

    /**
     * Copy the input stream into the output stream, closes both streams when finishing or in
     * a case of an exception
     *
     * @param i source
     * @param o destination
     * @param bufferSize the size of the buffer, which should be a power of 2 large enoguh
     */
    public static void copy(InputStream i, OutputStream o, int bufferSize) throws IOException {
        try {
            byte[] buffer = new byte[bufferSize];
            int size = i.read(buffer);
            while(size > -1) {
                o.write(buffer, 0, size);
                size = i.read(buffer);
            }
        } finally {
            try {
                i.close();
            } catch (Exception ex){}
            try {
                o.close();
            } catch (Exception ex){}
        }
    }
    
    public static void copyToFile(InputStream i, File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        copy(i, fos);
    }
    
    public static File copyToTempFile(InputStream i) throws IOException {
        File f = File.createTempFile("temp", "temp");
        f.deleteOnExit();
        copyToFile(i, f);
        return f;
    }
    
    
    
    
    public static Properties loadProperties(File propsFile) throws IOException {
        Properties props = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propsFile);
            props.load(fis);
            return props;
            
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ex) {
                    
                }
            }
            
        }
    }
    
    public static void saveProperties(Properties props, File propsFile, String comment) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(propsFile);
            props.store(fos, comment);
            fos.close();
            fos = null;
            
        } finally {
           
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ex){}
            }
        }
    }
    
    public static void updatePropertiesFile(File propsFile, Map<String,String> values, String comment) throws IOException {
        Properties props = new Properties();
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(propsFile);
            props.load(fis);
            fis.close();
            fis = null;
            props.putAll(values);
            
            fos = new FileOutputStream(propsFile);
            props.store(fos, comment);
            fos.close();
            fos = null;
            
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ex) {
                    
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ex){}
            }
        }
    }
    
    
    /**
     * Reads an input stream to a string
     * 
     * @param i the input stream
     * @param encoding the encoding of the stream
     * @return a string
     * @throws IOException thrown by the stream
     */
    public static String readToString(InputStream i, String encoding) throws IOException {
        byte[] b = readInputStream(i);
        return new String(b, 0, b.length, encoding);
    }
    
    /**
     * Reads an input stream to a string
     * 
     * @param i the input stream
     * @return a UTF-8 string
     * @throws IOException thrown by the stream
     */
    public static String readToString(InputStream i) throws IOException {
        return readToString(i, "UTF-8");
    }
    
    public static String readToString(File file) throws IOException {
        FileInputStream fis=null;
        try {
            fis = new FileInputStream(file);
            return readToString(fis);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception ex){}
            }
        }
    }
    
    public static void writeStringToFile(String str, File f) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            ByteArrayInputStream baos = new ByteArrayInputStream(str.getBytes("UTF-8"));
            Util.copy(baos, fos);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ex){}
            }
        }
    }
    
    /**
     * Converts a small input stream to a byte array
     *
     * @param i the stream to convert
     * @return byte array of the content of the stream
     */
    public static byte[] readInputStream(InputStream i) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        copy(i, b);
        return b.toByteArray();
    }
    
}