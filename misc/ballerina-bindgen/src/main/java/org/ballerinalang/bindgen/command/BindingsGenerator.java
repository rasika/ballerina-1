/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.bindgen.command;

import org.ballerinalang.bindgen.components.JClass;
import org.ballerinalang.bindgen.exceptions.BindgenException;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.ballerinalang.bindgen.utils.BindgenConstants.ARRAY_UTILS_FILE_NAME;
import static org.ballerinalang.bindgen.utils.BindgenConstants.ARRAY_UTILS_TEMPLATE_NAME;
import static org.ballerinalang.bindgen.utils.BindgenConstants.BALLERINA_BINDINGS_DIR;
import static org.ballerinalang.bindgen.utils.BindgenConstants.BAL_FILE_EXTENSION;
import static org.ballerinalang.bindgen.utils.BindgenConstants.BBGEN_CLASS_TEMPLATE_NAME;
import static org.ballerinalang.bindgen.utils.BindgenConstants.CONSTANTS_FILE_NAME;
import static org.ballerinalang.bindgen.utils.BindgenConstants.CONSTANTS_TEMPLATE_NAME;
import static org.ballerinalang.bindgen.utils.BindgenConstants.DEFAULT_TEMPLATE_DIR;
import static org.ballerinalang.bindgen.utils.BindgenConstants.DEPENDENCIES_DIR_NAME;
import static org.ballerinalang.bindgen.utils.BindgenConstants.JOBJECT_FILE_NAME;
import static org.ballerinalang.bindgen.utils.BindgenConstants.JOBJECT_TEMPLATE_NAME;
import static org.ballerinalang.bindgen.utils.BindgenConstants.USER_DIR;
import static org.ballerinalang.bindgen.utils.BindgenConstants.UTILS_DIR;
import static org.ballerinalang.bindgen.utils.BindgenUtils.createDirectory;
import static org.ballerinalang.bindgen.utils.BindgenUtils.getClassLoader;
import static org.ballerinalang.bindgen.utils.BindgenUtils.isPublicClass;
import static org.ballerinalang.bindgen.utils.BindgenUtils.writeOutputFile;

/**
 * Class for generating Ballerina bindings for Java APIs.
 */
public class BindingsGenerator {

    private String outputPath;
    private Path modulePath;
    private Path dependenciesPath;
    public static boolean directJavaClass = true;
    private Set<String> classPaths = new HashSet<>();
    private Set<String> classNames = new HashSet<>();
    private static final PrintStream errStream = System.err;
    private static final PrintStream outStream = System.out;
    private static Path userDir = Paths.get(System.getProperty(USER_DIR));

    public static Set<String> allClasses = new HashSet<>();
    public static Set<String> classListForLooping = new HashSet<>();
    public static Set<String> allJavaClasses = new HashSet<>();

    void generateJavaBindings() throws BindgenException {

        ClassLoader classLoader;
        try {
            if (!this.classPaths.isEmpty()) {
                classLoader = getClassLoader(this.classPaths, this.getClass().getClassLoader());
            } else {
                classLoader = this.getClass().getClassLoader();
            }
        } catch (BindgenException e) {
            throw new BindgenException("Error while loading the classpaths.", e);
        }
        if (classLoader != null) {
            if (this.outputPath == null) {
                this.modulePath = Paths.get(userDir.toString(), BALLERINA_BINDINGS_DIR);
                this.dependenciesPath = Paths.get(userDir.toString(), BALLERINA_BINDINGS_DIR, DEPENDENCIES_DIR_NAME);
            } else {
                this.modulePath = Paths.get(outputPath, BALLERINA_BINDINGS_DIR);
                this.dependenciesPath = Paths.get(outputPath, BALLERINA_BINDINGS_DIR, DEPENDENCIES_DIR_NAME);
            }
            outStream.println("Generating bindings for: ");
            generateBindings(classNames, classLoader, modulePath);
            createDirectory(Paths.get(modulePath.toString(), UTILS_DIR).toString());
            writeOutputFile(null, DEFAULT_TEMPLATE_DIR, JOBJECT_TEMPLATE_NAME,
                    Paths.get(modulePath.toString() + UTILS_DIR, JOBJECT_FILE_NAME).toString());
            writeOutputFile(null, DEFAULT_TEMPLATE_DIR, ARRAY_UTILS_TEMPLATE_NAME,
                    Paths.get(modulePath.toString() + UTILS_DIR, ARRAY_UTILS_FILE_NAME).toString());

            outStream.println("\nGenerating dependency bindings for: ");
            while (!classListForLooping.isEmpty()) {
                Set<String> newSet = new HashSet<>(classListForLooping);
                allJavaClasses.addAll(newSet);
                classListForLooping.clear();
                generateBindings(newSet, classLoader, dependenciesPath);
                directJavaClass = false;
            }
            Set<String> names = new HashSet<>(allClasses);
            writeOutputFile(names, DEFAULT_TEMPLATE_DIR, CONSTANTS_TEMPLATE_NAME,
                    Paths.get(modulePath.toString() + UTILS_DIR, CONSTANTS_FILE_NAME).toString());
            if (classLoader instanceof URLClassLoader) {
                try {
                    ((URLClassLoader) classLoader).close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    void setOutputPath(String outputPath) {

        this.outputPath = outputPath;
    }

    void setDependentJars(String[] jarPaths) {

        Collections.addAll(this.classPaths, jarPaths);
    }

    void setClassNames(List<String> classNames) {

        this.classNames = new HashSet<>(classNames);
    }

    private void generateBindings(Set<String> classList, ClassLoader classLoader, Path modulePath)
            throws BindgenException {

        createDirectory(modulePath.toString());
        for (String c : classList) {
            try {
                if (classLoader != null) {
                    Class classInstance = classLoader.loadClass(c);
                    if (classInstance != null && isPublicClass(classInstance) && !classInstance.isEnum()) {
                        JClass jClass = new JClass(classInstance);
                        String outputFile = Paths.get(modulePath.toString(), jClass.packageName).toString();
                        createDirectory(outputFile);
                        String filePath = Paths.get(outputFile, jClass.shortClassName + BAL_FILE_EXTENSION).toString();
                        writeOutputFile(jClass, DEFAULT_TEMPLATE_DIR, BBGEN_CLASS_TEMPLATE_NAME, filePath);
                        outStream.println("\t" + c);
                    }
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                errStream.println("Bindings for class " + c + " could not be created.");
            } catch (BindgenException e) {
                throw new BindgenException("Error while generating Ballerina bridge code: " + e);
            }
        }
    }
}
