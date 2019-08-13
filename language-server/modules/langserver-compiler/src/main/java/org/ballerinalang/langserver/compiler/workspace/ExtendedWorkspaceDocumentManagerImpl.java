/*
 * Copyright (c) 2018, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.langserver.compiler.workspace;

import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.Range;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * This class provides an abstraction layer for a given filePath. All other changes are reflected to the underline
 * WorkspaceDocumentManagerImpl except given filePath.
 *
 * This class is being used by the composer to provide a flexible Workspace DocumentManager. Operations such as
 * openFile() are aware of the browser refreshes that may cause file already opened exceptions.
 */
public class ExtendedWorkspaceDocumentManagerImpl extends WorkspaceDocumentManagerImpl {

    private volatile WorkspaceDocument tempDocument;

    private volatile boolean isExplicitMode;

    private static final ExtendedWorkspaceDocumentManagerImpl INSTANCE = new ExtendedWorkspaceDocumentManagerImpl();

    public static ExtendedWorkspaceDocumentManagerImpl getInstance() {
        return INSTANCE;
    }

    private ExtendedWorkspaceDocumentManagerImpl() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFileOpen(Path filePath) {
        if (isExplicitMode && isTempFile(filePath)) {
            return true;
        }
        return super.isFileOpen(filePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openFile(Path filePath, String content) throws WorkspaceDocumentException {
        // If file is already open; gracefully handle it
        openOrUpdateFile(filePath, null, content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFile(Path filePath, String updatedContent) throws WorkspaceDocumentException {
        // if file is not already open; gracefully handle it
        openOrUpdateFile(filePath, null, updatedContent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateFileRange(Path filePath, Range range, String updatedContent) throws WorkspaceDocumentException {
        // if file is not already open; gracefully handle it
        openOrUpdateFile(filePath, range, updatedContent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCodeLenses(Path filePath, List<CodeLens> codeLens) throws WorkspaceDocumentException {
        if (isExplicitMode && isTempFile(filePath)) {
            // If explicit mode is on and temp file, handle it locally
            tempDocument.setCodeLenses(codeLens);
        } else if (super.isFileOpen(filePath)) {
            // If file open, call parent class
            super.setCodeLenses(filePath, codeLens);
        } else {
            throw new WorkspaceDocumentException("File " + filePath.toString() + " is not opened in document manager.");
        }
    }

    private void openOrUpdateFile(Path filePath, Range range, String content) throws WorkspaceDocumentException {
        if (isExplicitMode && isTempFile(filePath)) {
            // If explicit mode is on and temp file, handle it locally
            tempDocument.setContent(content);
        } else {
            // Or else, call parent class
            if (super.isFileOpen(filePath)) {
                super.updateFileRange(filePath, range, content);
            } else {
                super.openFile(filePath, content);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeFile(Path filePath) throws WorkspaceDocumentException {
        if (!isExplicitMode && !isTempFile(filePath)) {
            super.closeFile(filePath);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CodeLens> getCodeLenses(Path filePath) {
        if (isExplicitMode && isTempFile(filePath)) {
            // If explicit mode is on and temp file, return local code lenses
            return tempDocument.getCodeLenses();
        }
        // Or else, call parent class
        return super.getCodeLenses(filePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFileContent(Path filePath) throws WorkspaceDocumentException {
        if (isExplicitMode && isTempFile(filePath)) {
            // If explicit mode is on and temp file, return local file content
            return tempDocument.getContent();
        }
        // Or else, call parent class
        return super.getFileContent(filePath);
    }

    private boolean isTempFile(Path filePath) {
        try {
            return tempDocument != null && Files.isSameFile(tempDocument.getPath(), filePath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Enables explicit mode. When explicit mode is enabled; changes for the temp file will not be reflected to the
     * WorkspaceDocument Manager and kept in an abstraction layer. Changes for the other files will be served as usual.
     *
     * Usage example:
     * <pre>
     * Optional&lt;Lock&gt; lock = documentManager.enableExplicitMode(tempFile);
     * try {
     *      //your code
     * } finally {
     *      documentManager.disableExplicitMode(lock.orElse(null));
     * }
     * </pre>
     *
     * @param tempFile temp file path
     * @return file lock
     *
     * @see ExtendedWorkspaceDocumentManagerImpl#disableExplicitMode(Lock)
     */
    public Lock enableExplicitMode(Path tempFile) {
        // Acquire a lock for the temp-file
        Lock lock = super.lock();
        if (tempDocument == null) {
            tempDocument = new WorkspaceDocument(tempFile, "", true);
        } else {
            tempDocument.setPath(tempFile);
        }
        this.isExplicitMode = true;
        return lock;
    }

    /**
     * Disables explicit mode. When explicit mode is disabled; All changes for the files will be served as usual.
     *
     * Usage example:
     * <pre>
     * Optional&lt;Lock&gt; lock = documentManager.enableExplicitMode(tempFile);
     * try {
     *      //your code
     * } finally {
     *      documentManager.disableExplicitMode(lock.orElse(null));
     * }
     * </pre>
     *
     * @param lock that returned from enableExplicitMode
     *
     * @see ExtendedWorkspaceDocumentManagerImpl#enableExplicitMode
     */
    public void disableExplicitMode(Lock lock) {
        this.isExplicitMode = false;
        // Release the lock of the temp-file
        lock.unlock();
    }
}
