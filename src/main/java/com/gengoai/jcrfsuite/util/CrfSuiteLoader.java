/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.gengoai.jcrfsuite.util;

import java.io.*;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * <b>Internal only - Do not use this class.</b> This class loads a native
 * library of crfsuite (crfsuite.dll, libcrfsuite.so, etc.) according to the
 * user platform (<i>os.name</i> and <i>os.arch</i>). The natively compiled
 * libraries bundled to crfsuite contain the codes of the original crfsuite and
 * JNI programs to access crfsuite.
 * <p>
 * In default, no configuration is required to use crfsuite, but you can load
 * your own native library created by 'make native' command.
 * <p>
 * This CrfSuiteLoader searches for native libraries (crfsuite.dll,
 * libcrfsuite.so, etc.) in the following order:
 * <ol>
 * <li>If system property <i>org.chokkan.crfsuite.use.systemlib</i> is set to true,
 * lookup folders specified by <i>java.lib.path</i> system property (This is the
 * default path that JVM searches for native libraries)
 * <li>(System property: <i>org.chokkan.crfsuite.lib.path</i>)/(System property:
 * <i>org.chokkan.crfsuite.lib.name</i>)
 * <li>One of the libraries embedded in crfsuite-(version).jar extracted into
 * (System property: <i>java.io.tempdir</i>). If
 * <i>org.chokkan.crfsuite.tempdir</i> is set, use this folder instead of
 * <i>java.io.tempdir</i>.
 * </ol>
 *
 * <p>
 * If you do not want to use folder <i>java.io.tempdir</i>, set the System
 * property <i>org.chokkan.crfsuite.tempdir</i>. For example, to use
 * <i>/tmp/vinhkhuc</i> as a temporary folder to copy native libraries, use -D option
 * of JVM:
 *
 * <pre>
 * <code>
 * java -Dorg.chokkan.crfsuite.tempdir="/tmp/vinhkhuc" ...
 * </code>
 * </pre>
 *
 * </p>
 * <p>
 * NOTE: Adapted from SnappyLoader.java (Snappy Java 1.1.0-SNAPSHOT)
 *
 * @author leo
 * @author Vinh Khuc
 */
public class CrfSuiteLoader {

   public static final String CRFSUITE_SYSTEM_PROPERTIES_FILE = "org-chokkan-crfsuite.properties";
   public static final String KEY_CRFSUITE_LIB_PATH = "org.chokkan.crfsuite.lib.path";
   public static final String KEY_CRFSUITE_LIB_NAME = "org.chokkan.crfsuite.lib.name";
   public static final String KEY_CRFSUITE_TEMPDIR = "org.chokkan.crfsuite.tempdir";
   public static final String KEY_CRFSUITE_USE_SYSTEMLIB = "org.chokkan.crfsuite.use.systemlib";
   // Depreciated, but preserved for backward compatibility
   public static final String KEY_CRFSUITE_DISABLE_BUNDLED_LIBS = "org.chokkan.crfsuite.disable.bundled.libs";

   private static volatile boolean isLoaded = false;

   static {
      loadCrfSuiteSystemProperties();
   }

   /**
    * load system properties when configuration file of the name
    * {@link #CRFSUITE_SYSTEM_PROPERTIES_FILE} is found
    */
   private static void loadCrfSuiteSystemProperties() {
      try {
         InputStream is = Thread.currentThread().getContextClassLoader()
               .getResourceAsStream(CRFSUITE_SYSTEM_PROPERTIES_FILE);

         if(is == null)
            return; // no configuration file is found

         // Load property file
         Properties props = new Properties();
         props.load(is);
         is.close();
         Enumeration<?> names = props.propertyNames();
         while(names.hasMoreElements()) {
            String name = (String) names.nextElement();
            if(name.startsWith("org.chokkan.crfsuite.")) {
               if(System.getProperty(name) == null) {
                  System.setProperty(name, props.getProperty(name));
               }
            }
         }
      } catch(Throwable ex) {
         System.err.println("Could not load '" + CRFSUITE_SYSTEM_PROPERTIES_FILE +
                                  "' from classpath: " + ex.toString());
      }
   }


   /**
    * Load CrfSuiteNative and its JNI native implementation using the root class
    * loader. This hack is for avoiding the JNI multi-loading issue when the
    * same JNI library is loaded by different class loaders.
    * <p>
    * In order to load native code in the root class loader, this method first
    * inject CrfsuiteNativeLoader class into the root class loader, because
    * {@link System#load(String)} method uses the class loader of the caller
    * class when loading native libraries.
    *
    * <pre>
    * (root class loader) -&gt; [CrfSuiteNativeLoader (load JNI code), CrfSuiteNative (has native methods), CrfSuiteNativeAPI, CrfSuiteErrorCode]  (injected by this method)
    *    |
    *    |
    * (child class loader) -&gt; Sees the above classes loaded by the root class loader.
    *   Then creates CrfSuiteNativeAPI implementation by instantiating CrfSuiteNative class.
    * </pre>
    *
    *
    * <pre>
    * (root class loader) -&gt; [CrfSuiteNativeLoader, CrfSuiteNative ...]  -&gt; native code is loaded by once in this class loader
    *   |   \
    *   |    (child2 class loader)
    * (child1 class loader)
    *
    * child1 and child2 share the same CrfSuiteNative code loaded by the root class loader.
    * </pre>
    * <p>
    * Note that Java's class loader first delegates the class lookup to its
    * parent class loader. So once CrfSuiteNativeLoader is loaded by the root
    * class loader, no child class loader initialize CrfSuiteNativeLoader again.
    *
    * @throws Exception
    */
   public static synchronized void load() throws Exception {

      if(!isLoaded) {
         try {
            synchronized(CrfSuiteLoader.class) {
               if(!isLoaded) {
                  File lib = findNativeLibrary();
                  if(lib == null) {
                     System.loadLibrary("crfsuite");
                  }
                  else {
                     System.load(lib.getAbsolutePath());
                  }
               }
               isLoaded = true;
            }
         } catch(Exception e) {
            e.printStackTrace();
            throw e;
         }
      }
   }


   /**
    * Computes the MD5 value of the input stream
    *
    * @param input
    * @return
    * @throws IOException
    * @throws NoSuchAlgorithmException
    */
   static String md5sum(InputStream input) throws IOException {
      BufferedInputStream in = new BufferedInputStream(input);
      try {
         MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
         DigestInputStream digestInputStream = new DigestInputStream(in, digest);
         for(; digestInputStream.read() >= 0; ) {

         }
         ByteArrayOutputStream md5out = new ByteArrayOutputStream();
         md5out.write(digest.digest());
         return md5out.toString();

      } catch(NoSuchAlgorithmException e) {
         throw new IllegalStateException("MD5 algorithm is not available: " + e);

      } finally {
         in.close();
      }
   }

   /**
    * Extract the specified library file to the target folder
    *
    * @param libFolderForCurrentOS
    * @param libraryFileName
    * @param targetFolder
    * @return library file object
    * @throws Exception
    */
   private static File extractLibraryFile(String libFolderForCurrentOS,
                                          String libraryFileName,
                                          String targetFolder) throws Exception {
      String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;
      final String prefix = "crfsuite-" + getVersion() + "-";
      String extractedLibFileName = prefix + libraryFileName;
      File extractedLibFile = new File(targetFolder, extractedLibFileName);

      try {
         if(extractedLibFile.exists()) {
            // test md5sum value
            String md5sum1 = md5sum(CrfSuiteLoader.class.getResourceAsStream(nativeLibraryFilePath));
            String md5sum2 = md5sum(new FileInputStream(extractedLibFile));

            if(md5sum1.equals(md5sum2)) {
               return new File(targetFolder, extractedLibFileName);

            }
            else {
               // remove old native library file
               boolean deletionSucceeded = extractedLibFile.delete();
               if(!deletionSucceeded) {
                  throw new IOException("failed to remove existing native library file: "
                                              + extractedLibFile.getAbsolutePath());
               }
            }
         }

         // Extract a native library file into the target directory
         InputStream reader = CrfSuiteLoader.class.getResourceAsStream(nativeLibraryFilePath);
         FileOutputStream writer = new FileOutputStream(extractedLibFile);
         byte[] buffer = new byte[8192];
         int bytesRead = 0;
         while((bytesRead = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, bytesRead);
         }

         writer.close();
         reader.close();

         // Set executable (x) flag to enable Java to load the native library
         if(!System.getProperty("os.name").contains("Windows")) {
            try {
               Runtime.getRuntime().exec(new String[]{"chmod", "755", extractedLibFile.getAbsolutePath()})
                     .waitFor();
            } catch(Throwable e) {
            }
         }

         return new File(targetFolder, extractedLibFileName);
      } catch(IOException e) {
         e.printStackTrace();
         return null;
      }

   }

   static File findNativeLibrary() throws Exception {

      boolean useSystemLib = Boolean.parseBoolean(System.getProperty(KEY_CRFSUITE_USE_SYSTEMLIB, "false"));
      if(useSystemLib)
         return null;

      boolean disabledBundledLibs = Boolean
            .parseBoolean(System.getProperty(KEY_CRFSUITE_DISABLE_BUNDLED_LIBS, "false"));
      if(disabledBundledLibs)
         return null;

      // Try to load the library in org.chokkan.crfsuite.lib.path  */
      String crfsuiteNativeLibraryPath = System.getProperty(KEY_CRFSUITE_LIB_PATH);
      String crfsuiteNativeLibraryName = System.getProperty(KEY_CRFSUITE_LIB_NAME);

      // Resolve the library file name with a suffix (e.g., dll, .so, etc.)
      if(crfsuiteNativeLibraryName == null)
         crfsuiteNativeLibraryName = System.mapLibraryName("crfsuite");

      if(crfsuiteNativeLibraryPath != null) {
         File nativeLib = new File(crfsuiteNativeLibraryPath, crfsuiteNativeLibraryName);
         if(nativeLib.exists())
            return nativeLib;
      }

      // Load an OS-dependent native library inside a jar file
      crfsuiteNativeLibraryPath = "/crfsuite-0.12/" + OSInfo.getNativeLibFolderPathForCurrentOS();

      if(CrfSuiteLoader.class.getResource(crfsuiteNativeLibraryPath + "/" + crfsuiteNativeLibraryName) != null) {
         // Temporary library folder. Use the value of org.chokkan.crfsuite.tempdir or java.io.tmpdir
         String tempFolder = new File(System.getProperty(KEY_CRFSUITE_TEMPDIR,
                                                         System.getProperty("java.io.tmpdir"))).getAbsolutePath();

         // Extract and load a native library inside the jar file
         return extractLibraryFile(crfsuiteNativeLibraryPath, crfsuiteNativeLibraryName, tempFolder);
      }

      return null; // Use a pre-installed libcrfsuite
   }

   /**
    * Get the crfsuite version by reading pom.properties embedded in jar.
    * This version data is used as a suffix of a dll file extracted from the
    * jar.
    *
    * @return the version string
    */
   public static String getVersion() {

      URL versionFile = CrfSuiteLoader.class
            .getResource("/META-INF/maven/org.chokkan.crfsuite/pom.properties");
      if(versionFile == null)
         versionFile = CrfSuiteLoader.class.getResource("/third_party/org/chokkan/crfsuite/VERSION");

      String version = "unknown";
      try {
         if(versionFile != null) {
            Properties versionData = new Properties();
            versionData.load(versionFile.openStream());
            version = versionData.getProperty("version", version);
            if(version.equals("unknown"))
               version = versionData.getProperty("VERSION", version);
            version = version.trim().replaceAll("[^0-9M\\.]", "");
         }
      } catch(IOException e) {
         e.printStackTrace();
      }
      return version;
   }
}
