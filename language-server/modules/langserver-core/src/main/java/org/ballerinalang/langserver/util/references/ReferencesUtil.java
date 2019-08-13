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
package org.ballerinalang.langserver.util.references;

import org.ballerinalang.langserver.command.ExecuteCommandKeys;
import org.ballerinalang.langserver.common.CommonKeys;
import org.ballerinalang.langserver.common.constants.NodeContextKeys;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.compiler.LSContext;
import org.ballerinalang.langserver.compiler.LSModuleCompiler;
import org.ballerinalang.langserver.compiler.common.LSCustomErrorStrategy;
import org.ballerinalang.langserver.compiler.common.LSDocument;
import org.ballerinalang.langserver.compiler.exception.CompilationFailedException;
import org.ballerinalang.langserver.compiler.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.compiler.workspace.WorkspaceDocumentManager;
import org.ballerinalang.langserver.exception.UserErrorException;
import org.ballerinalang.langserver.hover.util.HoverUtil;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangCompilationUnit;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static org.ballerinalang.langserver.compiler.LSCompilerUtil.getUntitledFilePath;

/**
 * Utility class for go to definition functionality of language server.
 */
public class ReferencesUtil {
    private ReferencesUtil() {
    }

    /**
     * Compile modules and find references for this position.
     *
     * @param fileUri        file uri
     * @param docManager     {@link WorkspaceDocumentManager}
     * @param position       current cursor {@link Position}
     * @param context        {@link LSContext}
     * @param compileProject if `True`, compiles the all modules
     * @return list of {@link BLangPackage}
     * @throws WorkspaceDocumentException when couldn't find file for uri
     * @throws CompilationFailedException when compilation failed
     */
    public static List<BLangPackage> compileModulesAndFindReferences(String fileUri,
                                                                     WorkspaceDocumentManager docManager,
                                                                     Position position, LSContext context,
                                                                     boolean compileProject)
            throws WorkspaceDocumentException, CompilationFailedException {
        Optional<Path> defFilePath = CommonUtil.getPathFromURI(fileUri);
        if (!defFilePath.isPresent()) {
            return new ArrayList<>();
        }
        Path compilationPath = getUntitledFilePath(defFilePath.toString()).orElse(defFilePath.get());
        Lock lock = docManager.lock();
        Class errStrategy = LSCustomErrorStrategy.class;
        try {
            context.put(DocumentServiceKeys.FILE_URI_KEY, fileUri);
            context.put(NodeContextKeys.REFERENCES_KEY, new SymbolReferencesModel());

            // With the sub-rule parser, find the token
            String documentContent = docManager.getFileContent(compilationPath);
            ReferencesSubRuleParser.parseCompilationUnit(documentContent, context, position);

            if (context.get(NodeContextKeys.NODE_NAME_KEY) == null) {
                throw new IllegalStateException("Couldn't find a valid identifier token at cursor!");
            }

            return LSModuleCompiler.getBLangPackages(context, docManager, true, errStrategy, compileProject, false);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the definition.
     *
     * @param modules  List of project modules
     * @param context  Definition context
     * @param position Cursor Position
     * @return {@link List}     List of definition locations
     */
    public static List<Location> getDefinition(List<BLangPackage> modules, LSContext context, Position position) {
        prepareReferences(modules, context, position);
        SymbolReferencesModel referencesModel = context.get(NodeContextKeys.REFERENCES_KEY);
        // If the definition list contains an item after the prepare reference mode, then return it.
        // In this case, definition is in the current compilation unit it self
        if (!referencesModel.getDefinitions().isEmpty()) {
            return getLocations(Collections.singletonList(referencesModel.getDefinitions().get(0)), context);
        }
        Optional<SymbolReferencesModel.Reference> symbolAtCursor = referencesModel.getReferenceAtCursor();
        // Ignore the optional check since it has been handled during prepareReference and throws exception
        String symbolPkgName = symbolAtCursor.get().getSymbolPkgName();
        Optional<BLangPackage> module = modules.stream()
                .filter(bLangPackage -> bLangPackage.symbol.getName().getValue().equals(symbolPkgName))
                .findAny();
        if (!module.isPresent()) {
            return new ArrayList<>();
        }
        for (BLangCompilationUnit compilationUnit : module.get().getCompilationUnits()) {
            SymbolReferenceFindingVisitor refVisitor = new SymbolReferenceFindingVisitor(context, symbolPkgName,
                    position);
            refVisitor.visit(compilationUnit);
            if (!referencesModel.getDefinitions().isEmpty()) {
                break;
            }
        }

        return getLocations(referencesModel.getDefinitions(), context);
    }

    public static SymbolReferencesModel.Reference getReferenceAtCursor(LSContext context, LSDocument document,
                                                                       Position position)
            throws WorkspaceDocumentException, CompilationFailedException {
        WorkspaceDocumentManager documentManager = context.get(ExecuteCommandKeys.DOCUMENT_MANAGER_KEY);
        List<BLangPackage> modules = ReferencesUtil.compileModulesAndFindReferences(document.getURIString(),
                                                                                    documentManager,
                                                                                    position, context, true);
        prepareReferences(modules, context, position);
        SymbolReferencesModel referencesModel = context.get(NodeContextKeys.REFERENCES_KEY);
        Optional<SymbolReferencesModel.Reference> symbolAtCursor = referencesModel.getReferenceAtCursor();
        return symbolAtCursor.orElse(null);
    }

    /**
     * Get the rename workspace edits.
     *
     * @param modules  Build modules in project
     * @param context  Language server context
     * @param newName  New name to replace
     * @param position Cursor position
     * @return {@link WorkspaceEdit}    Rename workspace edit
     */
    public static WorkspaceEdit getRenameWorkspaceEdits(List<BLangPackage> modules, LSContext context, String newName,
                                                        Position position) {
        SymbolReferencesModel referencesModel = context.get(NodeContextKeys.REFERENCES_KEY);
        String nodeName = context.get(NodeContextKeys.NODE_NAME_KEY);
        if (CommonKeys.NEW_KEYWORD_KEY.equals(nodeName)) {
            throw new IllegalStateException("Symbol at cursor '" + nodeName + "' not supported or could not find!");
        }
        prepareReferences(modules, context, position);
        fillAllReferences(modules, context, position);
        return getWorkspaceEdit(referencesModel, context, newName);
    }

    public static List<Location> getReferences(List<BLangPackage> modules, LSContext context, Position position,
                                               boolean includeDeclaration) {
        SymbolReferencesModel referencesModel = context.get(NodeContextKeys.REFERENCES_KEY);
        prepareReferences(modules, context, position);
        fillAllReferences(modules, context, position);
        List<SymbolReferencesModel.Reference> references = new ArrayList<>();
        if (includeDeclaration) {
            references.addAll(referencesModel.getDefinitions());
        }
        references.addAll(referencesModel.getReferences());
        if (!referencesModel.getDefinitions().contains(referencesModel.getReferenceAtCursor().get())) {
            references.add(referencesModel.getReferenceAtCursor().get());
        }

        return getLocations(references, context);
    }

    /**
     * Get the hover content.
     *
     * @param modules  Only the Current blang module is contained in the list
     * @param context  Hover operation context
     * @param position Cursor position
     * @return {@link Hover} Hover content
     */
    public static Hover getHover(List<BLangPackage> modules, LSContext context, Position position) {
        SymbolReferencesModel referencesModel = context.get(NodeContextKeys.REFERENCES_KEY);
        prepareReferences(modules, context, position);
        Optional<SymbolReferencesModel.Reference> symbolAtCursor = referencesModel.getReferenceAtCursor();

        // Ignore the optional check since it has been handled during prepareReference and throws exception
        BSymbol bSymbol = symbolAtCursor.get().getSymbol();
        return bSymbol != null
                ? HoverUtil.getHoverFromDocAttachment(HoverUtil.getMarkdownDocForSymbol(bSymbol), bSymbol, context)
                : HoverUtil.getDefaultHoverObject();
    }

    private static void fillAllReferences(List<BLangPackage> modules, LSContext context, Position position) {
        SymbolReferencesModel referencesModel = context.get(NodeContextKeys.REFERENCES_KEY);
        Optional<SymbolReferencesModel.Reference> symbolAtCursor = referencesModel.getReferenceAtCursor();
        // Ignore the optional check since it has been handled during prepareReference and throws exception
        String symbolOwnerPkg = symbolAtCursor.get().getSymbol().pkgID.toString();

        modules.forEach(bLangPackage -> {
            List<String> imports = bLangPackage.getImports().stream()
                    .map(bLangImportPackage -> bLangImportPackage.symbol.pkgID.toString())
                    .collect(Collectors.toList());
            if (!symbolOwnerPkg.equals(bLangPackage.packageID.toString()) && !imports.contains(symbolOwnerPkg)) {
                return;
            }

            for (BLangCompilationUnit compilationUnit : bLangPackage.getCompilationUnits()) {
                // Possible Reference tokens found within the cUnit
                String symbolPkgName = bLangPackage.symbol.getName().value;
                SymbolReferenceFindingVisitor refVisitor = new SymbolReferenceFindingVisitor(context, symbolPkgName,
                        position);
                refVisitor.visit(compilationUnit);
            }
        });
    }

    private static void prepareReferences(List<BLangPackage> modules, LSContext context, Position position) {
        String currentPkgName = context.get(DocumentServiceKeys.CURRENT_PKG_NAME_KEY);
        /*
        In windows platform, relative file path key components are separated with "\" while antlr always uses "/"
         */
        String currentCUnitName = context.get(DocumentServiceKeys.RELATIVE_FILE_PATH_KEY).replace("\\", "/");
        Optional<BLangPackage> currentPkg = modules.stream()
                .filter(pkg -> pkg.symbol.getName().getValue().equals(currentPkgName))
                .findAny();

        if (!currentPkg.isPresent()) {
            throw new UserErrorException("Not supported due to compilation failures!");
        }

        Optional<BLangCompilationUnit> currentCUnit = currentPkg.get().getCompilationUnits().stream()
                .filter(cUnit -> cUnit.name.equals(currentCUnitName))
                .findAny();

        SymbolReferenceFindingVisitor refVisitor = new SymbolReferenceFindingVisitor(context, currentPkgName, position,
                true);

        refVisitor.visit(currentCUnit.get());

        // Prune the found symbol references
        SymbolReferencesModel symbolReferencesModel = context.get(NodeContextKeys.REFERENCES_KEY);
        if (!symbolReferencesModel.getReferenceAtCursor().isPresent()) {
            String nodeName = context.get(NodeContextKeys.NODE_NAME_KEY);
            throw new IllegalStateException("Symbol at cursor '" + nodeName + "' not supported or could not find!");
        }

        SymbolReferencesModel.Reference symbolAtCursor = symbolReferencesModel.getReferenceAtCursor().get();
        BSymbol cursorSymbol = symbolAtCursor.getSymbol();
        symbolReferencesModel.getDefinitions()
                .removeIf(reference -> reference.getSymbol() != cursorSymbol
                        && (reference.getSymbol().type.tsymbol != cursorSymbol)
                        && !(cursorSymbol.type.tag == TypeTags.ERROR
                        && reference.getSymbol().type.tsymbol == cursorSymbol.type.tsymbol));
        symbolReferencesModel.getReferences()
                .removeIf(reference -> reference.getSymbol() != cursorSymbol
                        && (reference.getSymbol().type.tsymbol != cursorSymbol
                        && !(cursorSymbol.type.tag == TypeTags.ERROR
                        && reference.getSymbol().type.tsymbol == cursorSymbol.type.tsymbol)));
    }

    private static List<Location> getLocations(List<SymbolReferencesModel.Reference> references, LSContext context) {
        return references.stream().map(reference -> {
            DiagnosticPos position = reference.getPosition();
            String sourceRoot = context.get(DocumentServiceKeys.SOURCE_ROOT_KEY);
            Path baseRoot = reference.getSourcePkgName().equals(".") ? Paths.get(sourceRoot)
                    : Paths.get(sourceRoot).resolve("src").resolve(reference.getSourcePkgName());
            String fileURI = baseRoot.resolve(reference.getCompilationUnit()).toUri().toString();
            return new Location(fileURI, getRange(position));
        }).collect(Collectors.toList());
    }

    private static WorkspaceEdit getWorkspaceEdit(SymbolReferencesModel referencesModel, LSContext context,
                                                  String newName) {
        WorkspaceEdit workspaceEdit = new WorkspaceEdit();
        SymbolReferencesModel.Reference symbolAtCursor = referencesModel.getReferenceAtCursor().get();
        List<SymbolReferencesModel.Reference> references = new ArrayList<>();
        references.add(symbolAtCursor);
        if (!referencesModel.getDefinitions().contains(symbolAtCursor)) {
            references.addAll(referencesModel.getDefinitions());
        }
        references.addAll(referencesModel.getReferences());
        LSDocument sourceDoc = context.get(DocumentServiceKeys.LS_DOCUMENT_KEY);

        references.forEach(reference -> {
            DiagnosticPos referencePos = reference.getPosition();
            String pkgName = reference.getSourcePkgName();
            String cUnitName = reference.getCompilationUnit();
            String uri;
            Path basePath = sourceDoc.getProjectRootPath();
            if (sourceDoc.isWithinProject()) {
                basePath = basePath.resolve("src").resolve(pkgName);
            }
            basePath = basePath.resolve(cUnitName);

            uri = basePath.toUri().toString();
            TextEdit textEdit = new TextEdit(getRange(referencePos), newName);
            if (workspaceEdit.getChanges().containsKey(uri)) {
                workspaceEdit.getChanges().get(uri).add(textEdit);
            } else {
                List<TextEdit> textEdits = new ArrayList<>();
                textEdits.add(textEdit);
                workspaceEdit.getChanges().put(uri, textEdits);
            }
        });

        return workspaceEdit;
    }

    private static Range getRange(DiagnosticPos referencePos) {
        Position start = new Position(referencePos.sLine, referencePos.sCol);
        Position end = new Position(referencePos.eLine, referencePos.eCol);
        return new Range(start, end);
    }
}
