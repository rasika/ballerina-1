/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerinalang.langserver.compiler.workspace;

import org.ballerinalang.langserver.compiler.LSCompilerUtil;
import org.ballerinalang.langserver.compiler.common.LSDocument;
import org.ballerinalang.langserver.compiler.workspace.repository.LangServerFSProjectDirectory;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Range;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * An in-memory document manager that keeps dirty files in-memory and will match the collection of files currently open
 * in tool's workspace.
 */
public class WorkspaceDocumentManagerImpl implements WorkspaceDocumentManager {

    private Lock lock = new ReentrantLock(true);

    private volatile Map<Path, WorkspaceDocument> documentList = new ConcurrentHashMap<>();

    private static final WorkspaceDocumentManagerImpl INSTANCE = new WorkspaceDocumentManagerImpl();

    protected WorkspaceDocumentManagerImpl() {
    }
    
    public static WorkspaceDocumentManagerImpl getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFileOpen(Path filePath) {
        return filePath != null
                && documentList.containsKey(filePath)
                && documentList.get(filePath) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openFile(Path filePath, String content) throws WorkspaceDocumentException {
        if (isFileOpen(filePath)) {
            throw new WorkspaceDocumentException(
                    "File " + filePath.toString() + " is already opened in document manager."
            );
        }
        documentList.put(filePath, new WorkspaceDocument(filePath, content));
        LSDocument document = new LSDocument(filePath.toUri().toString());
        if (document.isWithinProject()) {
            rescanProjectRoot(filePath);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFile(Path filePath, String updatedContent) throws WorkspaceDocumentException {
        if (isFileOpen(filePath)) {
            documentList.get(filePath).setContent(updatedContent);
        } else {
            throw new WorkspaceDocumentException("File " + filePath.toString() + " is not opened in document manager.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFileRange(Path filePath, Range range, String updatedContent) throws WorkspaceDocumentException {
        if (isFileOpen(filePath)) {
            documentList.get(filePath).setContent(range, updatedContent);
        } else {
            throw new WorkspaceDocumentException("File " + filePath.toString() + " is not opened in document manager.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCodeLenses(Path filePath, List<CodeLens> codeLens) throws WorkspaceDocumentException {
        if (isFileOpen(filePath)) {
            documentList.get(filePath).setCodeLenses(codeLens);
        } else {
            throw new WorkspaceDocumentException("File " + filePath.toString() + " is not opened in document manager.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrunedContent(Path filePath, String prunedSource) throws WorkspaceDocumentException {
        if (isFileOpen(filePath)) {
            documentList.get(filePath).setPrunedContent(prunedSource);
        } else {
            throw new WorkspaceDocumentException("File " + filePath.toString() + " is not opened in document manager.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeFile(Path filePath) throws WorkspaceDocumentException {
        if (isFileOpen(filePath)) {
            try {
                lock.lock();
                documentList.remove(filePath);
            } finally {
                lock.unlock();
            }
            // TODO: within the workspace document we need to keep the LSDocument
            LSDocument document = new LSDocument(filePath.toUri().toString());
            if (document.isWithinProject()) {
                rescanProjectRoot(filePath);
            }
        } else {
            throw new WorkspaceDocumentException("File " + filePath.toString() + " is not opened in document manager.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CodeLens> getCodeLenses(Path filePath) {
        if (isFileOpen(filePath) && documentList.get(filePath) != null) {
            return documentList.get(filePath).getCodeLenses();
        }
        return new ArrayList<>();
    }

    @Override
    public LSDocument getLSDocument(Path filePath) throws WorkspaceDocumentException {
        WorkspaceDocument document = documentList.get(filePath);
        if (isFileOpen(filePath) && document != null) {
            return document.getLSDocument();
        }
        throw new WorkspaceDocumentException("Cannot find LSDocument for the give file path: ["
                + filePath.toString() + "]");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileContent(Path filePath) throws WorkspaceDocumentException {
        if (isFileOpen(filePath) && documentList.get(filePath) != null) {
            return documentList.get(filePath).getContent();
        }
        return readFromFileSystem(filePath);
    }

    /**
     * {@inheritDoc}
     */
    public Lock lock() {
        lock.lock();
        return lock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Path> getAllFilePaths() {
        return documentList.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)).keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAllFilePaths() {
        documentList.clear();
    }

    private String readFromFileSystem(Path filePath) throws WorkspaceDocumentException {
        try {
            if (Files.exists(filePath)) {
                byte[] encoded = Files.readAllBytes(filePath);
                return new String(encoded, Charset.defaultCharset());
            }
            throw new WorkspaceDocumentException("Error in reading non-existent file '" + filePath);
        } catch (IOException e) {
            throw new WorkspaceDocumentException("Error in reading file '" + filePath + "': " + e.getMessage(), e);
        }
    }

    private void rescanProjectRoot(Path filePath) {
        Path projectRoot = Paths.get(LSCompilerUtil.getProjectRoot(filePath));
        LangServerFSProjectDirectory projectDirectory = LangServerFSProjectDirectory.getInstance(projectRoot, this);
        projectDirectory.rescanProjectRoot();
    }
}
