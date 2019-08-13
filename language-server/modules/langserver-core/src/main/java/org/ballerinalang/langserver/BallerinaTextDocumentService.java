/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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
package org.ballerinalang.langserver;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.ballerinalang.langserver.codelenses.CodeLensUtil;
import org.ballerinalang.langserver.codelenses.LSCodeLensesProviderFactory;
import org.ballerinalang.langserver.command.CommandUtil;
import org.ballerinalang.langserver.command.ExecuteCommandKeys;
import org.ballerinalang.langserver.common.CommonKeys;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.compiler.LSContext;
import org.ballerinalang.langserver.compiler.LSModuleCompiler;
import org.ballerinalang.langserver.compiler.LSServiceOperationContext;
import org.ballerinalang.langserver.compiler.common.LSCustomErrorStrategy;
import org.ballerinalang.langserver.compiler.common.LSDocument;
import org.ballerinalang.langserver.compiler.exception.CompilationFailedException;
import org.ballerinalang.langserver.compiler.format.FormattingVisitorEntry;
import org.ballerinalang.langserver.compiler.format.TextDocumentFormatUtil;
import org.ballerinalang.langserver.compiler.sourcegen.FormattingSourceGen;
import org.ballerinalang.langserver.compiler.workspace.WorkspaceDocumentManager;
import org.ballerinalang.langserver.completions.CompletionKeys;
import org.ballerinalang.langserver.completions.SymbolInfo;
import org.ballerinalang.langserver.completions.util.CompletionUtil;
import org.ballerinalang.langserver.diagnostic.DiagnosticsHelper;
import org.ballerinalang.langserver.exception.UserErrorException;
import org.ballerinalang.langserver.implementation.GotoImplementationCustomErrorStrategy;
import org.ballerinalang.langserver.implementation.GotoImplementationUtil;
import org.ballerinalang.langserver.index.LSIndexImpl;
import org.ballerinalang.langserver.signature.SignatureHelpUtil;
import org.ballerinalang.langserver.signature.SignatureKeys;
import org.ballerinalang.langserver.signature.SignatureTreeVisitor;
import org.ballerinalang.langserver.sourceprune.SourcePruner;
import org.ballerinalang.langserver.symbols.SymbolFindingVisitor;
import org.ballerinalang.langserver.util.Debouncer;
import org.ballerinalang.langserver.util.references.ReferencesUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureInformation;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.tree.BLangCompilationUnit;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.util.ProjectDirConstants;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.ballerinalang.langserver.command.CommandUtil.getCommandForNodeType;
import static org.ballerinalang.langserver.common.utils.CommonUtil.logError;
import static org.ballerinalang.langserver.common.utils.CommonUtil.notifyUser;
import static org.ballerinalang.langserver.compiler.LSCompilerUtil.getUntitledFilePath;
import static org.ballerinalang.langserver.signature.SignatureHelpUtil.getFuncSymbolInfo;
import static org.ballerinalang.langserver.signature.SignatureHelpUtil.getFunctionInvocationDetails;
import static org.wso2.ballerinalang.compiler.util.ProjectDirConstants.TEST_DIR_NAME;

/**
 * Text document service implementation for ballerina.
 */
class BallerinaTextDocumentService implements TextDocumentService {
    // indicates the frequency to send diagnostics to server upon document did change
    private static final int DIAG_PUSH_DEBOUNCE_DELAY = 750;
    private final BallerinaLanguageServer languageServer;
    private final WorkspaceDocumentManager documentManager;
    private final DiagnosticsHelper diagnosticsHelper;
    private final LSIndexImpl lsIndex;
    private TextDocumentClientCapabilities clientCapabilities;

    private final Debouncer diagPushDebouncer;

    BallerinaTextDocumentService(LSGlobalContext globalContext) {
        this.languageServer = globalContext.get(LSGlobalContextKeys.LANGUAGE_SERVER_KEY);
        this.documentManager = globalContext.get(LSGlobalContextKeys.DOCUMENT_MANAGER_KEY);
        this.diagnosticsHelper = globalContext.get(LSGlobalContextKeys.DIAGNOSTIC_HELPER_KEY);
        this.lsIndex = globalContext.get(LSGlobalContextKeys.LS_INDEX_KEY);
        this.diagPushDebouncer = new Debouncer(DIAG_PUSH_DEBOUNCE_DELAY);
    }

    /**
     * Set the Text Document Capabilities.
     *
     * @param clientCapabilities Client's Text Document Capabilities
     */
    void setClientCapabilities(TextDocumentClientCapabilities clientCapabilities) {
        this.clientCapabilities = clientCapabilities;
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
        final List<CompletionItem> completions = new ArrayList<>();
        return CompletableFuture.supplyAsync(() -> {
            LSServiceOperationContext context = new LSServiceOperationContext();
            String fileUri = position.getTextDocument().getUri();
            Optional<Path> completionPath = CommonUtil.getPathFromURI(fileUri);
            if (!completionPath.isPresent()) {
                return Either.forLeft(completions);
            }
            Lock lock = documentManager.lock();
            context.put(DocumentServiceKeys.POSITION_KEY, position);
            context.put(DocumentServiceKeys.FILE_URI_KEY, fileUri);
            context.put(CompletionKeys.DOC_MANAGER_KEY, documentManager);
            context.put(CompletionKeys.CLIENT_CAPABILITIES_KEY, this.clientCapabilities.getCompletion());
            context.put(LSGlobalContextKeys.LS_INDEX_KEY, this.lsIndex);

            try {
                SourcePruner.pruneSource(context);
                /*
                If the token at cursor is within the hidden channel we stop calculating the completions. This will
                avoid completions within the line comments
                 */
                if (context.get(DocumentServiceKeys.TERMINATE_OPERATION_KEY) != null
                        && context.get(DocumentServiceKeys.TERMINATE_OPERATION_KEY)) {
                    return Either.forLeft(completions);
                }
                LSModuleCompiler.getBLangPackage(context, documentManager, false, null, false);
                // Fill the current file imports
                context.put(DocumentServiceKeys.CURRENT_DOC_IMPORTS_KEY, CommonUtil.getCurrentFileImports(context));
                CompletionUtil.resolveSymbols(context);
                if (context.get(DocumentServiceKeys.TERMINATE_OPERATION_KEY) != null
                        && context.get(DocumentServiceKeys.TERMINATE_OPERATION_KEY)) {
                    return Either.forLeft(completions);
                }
                completions.addAll(CompletionUtil.getCompletionItems(context));
            } catch (Throwable e) {
                // Note: Not catching UserErrorException separately to avoid flooding error msgs popups
                String msg = "Operation 'text/completion' failed!";
                logError(msg, e, languageServer.getClient(), position.getTextDocument(), position.getPosition());
            } finally {
                lock.unlock();
            }
            return Either.forLeft(completions);
        });
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
        return null;
    }

    @Override
    public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
        return CompletableFuture.supplyAsync(() -> {
            String fileUri = position.getTextDocument().getUri();
            LSServiceOperationContext context = new LSServiceOperationContext();
            Hover hover;
            try {
                List<BLangPackage> modules = ReferencesUtil.compileModulesAndFindReferences(fileUri, documentManager,
                                                                                            position.getPosition(),
                                                                                            context, false);
                hover = ReferencesUtil.getHover(modules, context, position.getPosition());
            } catch (Throwable e) {
                // Note: Not catching UserErrorException separately to avoid flooding error msgs popups
                String msg = "Operation 'text/hover' failed!";
                logError(msg, e, languageServer.getClient(), position.getTextDocument(), position.getPosition());
                hover = new Hover();
                List<Either<String, MarkedString>> contents = new ArrayList<>();
                contents.add(Either.forLeft(""));
                hover.setContents(contents);
            }
            return hover;
        });
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
        return CompletableFuture.supplyAsync(() -> {
            String uri = position.getTextDocument().getUri();
            Optional<Path> sigFilePath = CommonUtil.getPathFromURI(uri);
            if (!sigFilePath.isPresent()) {
                return new SignatureHelp();
            }
            Lock lock = documentManager.lock();
            LSServiceOperationContext context = new LSServiceOperationContext();
            context.put(DocumentServiceKeys.POSITION_KEY, position);
            context.put(DocumentServiceKeys.FILE_URI_KEY, uri);
            context.put(SignatureKeys.SIGNATURE_HELP_CAPABILITIES_KEY, clientCapabilities.getSignatureHelp());
            context.put(CompletionKeys.DOC_MANAGER_KEY, documentManager);
            try {
                // Prepare content for source-prune
                // This fix added to handle cases such as `foo(` that causes pruner to remove all RHS tokens.
                SignatureHelpUtil.preprocessSourcePrune(context);

                // Prune the source and compile
                SourcePruner.pruneSource(context);
                BLangPackage bLangPackage = LSModuleCompiler.getBLangPackage(context, documentManager, false,
                        LSCustomErrorStrategy.class, false);

                // Capture visible symbols of the cursor position
                SignatureTreeVisitor signatureTreeVisitor = new SignatureTreeVisitor(context);
                bLangPackage.accept(signatureTreeVisitor);
                int activeParamIndex = 0;
                List<SymbolInfo> visibleSymbols = context.get(CommonKeys.VISIBLE_SYMBOLS_KEY);
                if (visibleSymbols == null) {
                    throw new Exception("Couldn't find the symbol, visible symbols are NULL!");
                }
                // Search function invocation symbol
                List<SignatureInformation> signatures = new ArrayList<>();
                List<SymbolInfo> symbols = new ArrayList<>(visibleSymbols);
                Pair<Optional<String>, Integer> funcPathAndParamIndexPair = getFunctionInvocationDetails(context);
                Optional<String> funcPath = funcPathAndParamIndexPair.getLeft();
                activeParamIndex = funcPathAndParamIndexPair.getRight();
                funcPath.ifPresent(pathStr -> {
                    Optional<SymbolInfo> searchSymbol = getFuncSymbolInfo(context, pathStr,
                                                                          symbols);
                    searchSymbol.ifPresent(s -> {
                        if (s.getScopeEntry().symbol instanceof BInvokableSymbol) {
                            BInvokableSymbol symbol = (BInvokableSymbol) s.getScopeEntry().symbol;
                            signatures.add(SignatureHelpUtil.getSignatureInformation(symbol, context));
                        }
                    });
                });
                SignatureHelp signatureHelp = new SignatureHelp();
                signatureHelp.setActiveParameter(activeParamIndex);
                signatureHelp.setActiveSignature(0);
                signatureHelp.setSignatures(signatures);
                return signatureHelp;
            } catch (UserErrorException e) {
                notifyUser(e, languageServer.getClient());
                return new SignatureHelp();
            } catch (Throwable e) {
                String msg = "Operation 'text/signature' failed!";
                logError(msg, e, languageServer.getClient(), position.getTextDocument(), position.getPosition());
                return new SignatureHelp();
            } finally {
                lock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams position) {
        return CompletableFuture.supplyAsync(() -> {
            String fileUri = position.getTextDocument().getUri();
            LSServiceOperationContext context = new LSServiceOperationContext();
            try {
                List<BLangPackage> modules = ReferencesUtil.compileModulesAndFindReferences(fileUri, documentManager,
                                                                                            position.getPosition(),
                                                                                            context, true);
                return ReferencesUtil.getDefinition(modules, context, position.getPosition());
            } catch (UserErrorException e) {
                notifyUser(e, languageServer.getClient());
                return new ArrayList<>();
            } catch (Throwable e) {
                String msg = "Operation 'text/definition' failed!";
                logError(msg, e, languageServer.getClient(), position.getTextDocument(), position.getPosition());
                return new ArrayList<>();
            }
        });
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
        return CompletableFuture.supplyAsync(() -> {
            String fileUri = params.getTextDocument().getUri();
            LSServiceOperationContext context = new LSServiceOperationContext();
            try {
                List<BLangPackage> modules = ReferencesUtil.compileModulesAndFindReferences(fileUri, documentManager,
                                                                                            params.getPosition(),
                                                                                            context, true);
                boolean includeDeclaration = params.getContext().isIncludeDeclaration();
                return ReferencesUtil.getReferences(modules, context, params.getPosition(), includeDeclaration);
            } catch (UserErrorException e) {
                notifyUser(e, languageServer.getClient());
                return new ArrayList<>();
            } catch (Throwable e) {
                String msg = "Operation 'text/references' failed!";
                logError(msg, e, languageServer.getClient(), params.getTextDocument(), params.getPosition());
                return new ArrayList<>();
            }
        });
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(
            TextDocumentPositionParams position) {
        return null;
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>>
    documentSymbol(DocumentSymbolParams params) {
        return CompletableFuture.supplyAsync(() -> {
            List<Either<SymbolInformation, DocumentSymbol>> symbols = new ArrayList<>();
            String fileUri = params.getTextDocument().getUri();
            Optional<Path> docSymbolFilePath = CommonUtil.getPathFromURI(fileUri);
            if (!docSymbolFilePath.isPresent()) {
                return symbols;
            }
            Lock lock = documentManager.lock();
            try {
                LSServiceOperationContext symbolsContext = new LSServiceOperationContext();
                symbolsContext.put(DocumentServiceKeys.FILE_URI_KEY, fileUri);
                symbolsContext.put(DocumentServiceKeys.SYMBOL_LIST_KEY, symbols);
                BLangPackage bLangPackage = LSModuleCompiler.getBLangPackage(symbolsContext, documentManager, false,
                        LSCustomErrorStrategy.class, false);
                Optional<BLangCompilationUnit> documentCUnit = bLangPackage.getCompilationUnits().stream()
                        .filter(cUnit -> (fileUri.endsWith(cUnit.getName())))
                        .findFirst();

                documentCUnit.ifPresent(cUnit -> {
                    SymbolFindingVisitor visitor = new SymbolFindingVisitor(symbolsContext);
                    cUnit.accept(visitor);
                });

                return symbols;
            } catch (UserErrorException e) {
                notifyUser(e, languageServer.getClient());
                return symbols;
            } catch (Throwable e) {
                String msg = "Operation 'text/documentSymbol' failed!";
                logError(msg, e, languageServer.getClient(), params.getTextDocument(), (Position) null);
                return symbols;
            } finally {
                lock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        return CompletableFuture.supplyAsync(() -> {
            List<CodeAction> actions = new ArrayList<>();
            TextDocumentIdentifier identifier = params.getTextDocument();
            String fileUri = identifier.getUri();
            Optional<Path> filePath = CommonUtil.getPathFromURI(fileUri);
            if (!filePath.isPresent()) {
                return new ArrayList<>();
            }
            Lock lock = documentManager.lock();
            try {
                LSDocument document = documentManager.getLSDocument(filePath.get());
                int line = params.getRange().getStart().getLine();
                int col = params.getRange().getStart().getCharacter();
                List<Diagnostic> diagnostics = params.getContext().getDiagnostics();

                String topLevelNodeType = CommonUtil.topLevelNodeInLine(identifier, line, documentManager);

                // Add create test commands
                Path modulePath = document.getOwnerModulePath() == null ? document.getProjectRootPath()
                        : document.getOwnerModulePath();
                String innerDirName = modulePath.relativize(document.getPath()).toString()
                        .split(Pattern.quote(File.separator))[0];
                String moduleName = document.getOwnerModule();
                if (topLevelNodeType != null && diagnostics.isEmpty() && document.isWithinProject() &&
                        !TEST_DIR_NAME.equals(innerDirName) && !moduleName.isEmpty() &&
                        !moduleName.endsWith(ProjectDirConstants.BLANG_SOURCE_EXT)) {
                    /*
                    Test generation suggested only when no code diagnosis exists, inside a bal project,
                    inside a module, not inside /tests folder
                     */
                    actions.addAll(CommandUtil.getTestGenerationCommand(topLevelNodeType, fileUri, params,
                            documentManager));
                }

                // Add commands base on node diagnostics
                if (!diagnostics.isEmpty()) {
                    LSContext context = new LSServiceOperationContext();
                    context.put(ExecuteCommandKeys.DOCUMENT_MANAGER_KEY, documentManager);
                    diagnostics.forEach(diagnostic -> {
                        int sLine = diagnostic.getRange().getStart().getLine();
                        int sCol = diagnostic.getRange().getStart().getCharacter();
                        int eLine = diagnostic.getRange().getEnd().getLine();
                        int eCol = diagnostic.getRange().getEnd().getCharacter();
                        if ((line == sLine && col >= sCol) || (line == eLine && col <= eCol) ||
                                (line > sLine && eLine < line)) {
                            actions.addAll(CommandUtil.getCommandsByDiagnostic(document, diagnostic, params, context));
                        }
                    });
                }

                // Add commands base on node type
                if (topLevelNodeType != null) {
                    actions.addAll(getCommandForNodeType(topLevelNodeType, fileUri, line));
                }
            } catch (UserErrorException e) {
                notifyUser(e, languageServer.getClient());
            } catch (Throwable e) {
                String msg = "Operation 'text/codeAction' failed!";
                Range range = params.getRange();
                logError(msg, e, languageServer.getClient(), params.getTextDocument(), range.getStart(),
                         range.getEnd());
            } finally {
                lock.unlock();
            }
            return actions.stream().map(
                    (Function<CodeAction, Either<Command, CodeAction>>) Either::forRight).collect(Collectors.toList());
        });
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        return CompletableFuture.supplyAsync(() -> {
            List<CodeLens> lenses;
            if (!LSCodeLensesProviderFactory.getInstance().isEnabled()) {
                // Disabled ballerina codeLens feature
                clientCapabilities.setCodeLens(null);
                // Skip code lenses if codeLens disabled
                return new ArrayList<>();
            }

            String fileUri = params.getTextDocument().getUri();
            Optional<Path> docSymbolFilePath = CommonUtil.getPathFromURI(fileUri);
            if (!docSymbolFilePath.isPresent()) {
                return new ArrayList<>();
            }
            Path compilationPath = getUntitledFilePath(docSymbolFilePath.toString()).orElse(docSymbolFilePath.get());
            Lock lock = documentManager.lock();
            try {
                // Compile source document
                lenses = CodeLensUtil.compileAndGetCodeLenses(fileUri, documentManager);
                documentManager.setCodeLenses(compilationPath, lenses);
                return lenses;
            } catch (UserErrorException e) {
                notifyUser(e, languageServer.getClient());
                // Source compilation failed, serve from cache
                return documentManager.getCodeLenses(compilationPath);
            } catch (Throwable e) {
                String msg = "Operation 'text/codeLens' failed!";
                logError(msg, e, languageServer.getClient(), params.getTextDocument(), (Position) null);
                // Source compilation failed, serve from cache
                return documentManager.getCodeLenses(compilationPath);
            } finally {
                lock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
        return null;
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        return CompletableFuture.supplyAsync(() -> {
            String textEditContent;
            TextEdit textEdit = new TextEdit();

            String fileUri = params.getTextDocument().getUri();
            Optional<Path> formattingFilePath = CommonUtil.getPathFromURI(fileUri);
            if (!formattingFilePath.isPresent()) {
                return Collections.singletonList(textEdit);
            }
            Lock lock = documentManager.lock();
            try {
                LSServiceOperationContext formatCtx = new LSServiceOperationContext();
                formatCtx.put(DocumentServiceKeys.FILE_URI_KEY, fileUri);

                // Build the given ast.
                JsonObject ast = TextDocumentFormatUtil.getAST(formattingFilePath.get(), documentManager, formatCtx);
                JsonObject model = ast.getAsJsonObject("model");
                FormattingSourceGen.build(model, "CompilationUnit");

                // Format the given ast.
                FormattingVisitorEntry formattingUtil = new FormattingVisitorEntry();
                formattingUtil.accept(model);

                //Generate source for the ast.
                textEditContent = FormattingSourceGen.getSourceOf(model);
                Matcher matcher = Pattern.compile("\r\n|\r|\n").matcher(textEditContent);
                int totalLines = 0;
                while (matcher.find()) {
                    totalLines++;
                }

                int lastNewLineCharIndex = Math.max(textEditContent.lastIndexOf('\n'),
                        textEditContent.lastIndexOf('\r'));
                int lastCharCol = textEditContent.substring(lastNewLineCharIndex + 1).length();

                Range range = new Range(new Position(0, 0), new Position(totalLines, lastCharCol));
                textEdit = new TextEdit(range, textEditContent);
                return Collections.singletonList(textEdit);
            } catch (UserErrorException e) {
                notifyUser(e, languageServer.getClient());
                return Collections.singletonList(textEdit);
            } catch (Throwable e) {
                String msg = "Operation 'text/formatting' failed!";
                logError(msg, e, languageServer.getClient(), params.getTextDocument(), (Position) null);
                return Collections.singletonList(textEdit);
            } finally {
                lock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
        return CompletableFuture.supplyAsync(() -> {
            String fileUri = params.getTextDocument().getUri();
            LSServiceOperationContext context = new LSServiceOperationContext();
            Position position = params.getPosition();
            try {
                List<BLangPackage> modules = ReferencesUtil.compileModulesAndFindReferences(fileUri, documentManager,
                                                                                            position, context, true);
                return ReferencesUtil.getRenameWorkspaceEdits(modules, context, params.getNewName(), position);
            } catch (UserErrorException e) {
                notifyUser(e, languageServer.getClient());
                return null;
            } catch (Throwable e) {
                String msg = "Operation 'text/rename' failed!";
                logError(msg, e, languageServer.getClient(), params.getTextDocument(), params.getPosition());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<List<? extends Location>> implementation(TextDocumentPositionParams position) {
        return CompletableFuture.supplyAsync(() -> {
            String fileUri = position.getTextDocument().getUri();
            List<Location> implementationLocations = new ArrayList<>();
            LSServiceOperationContext context = new LSServiceOperationContext();
            LSDocument lsDocument = new LSDocument(fileUri);
            Lock lock = documentManager.lock();

            context.put(DocumentServiceKeys.POSITION_KEY, position);
            context.put(DocumentServiceKeys.FILE_URI_KEY, fileUri);

            try {
                BLangPackage bLangPackage = LSModuleCompiler.getBLangPackage(context, documentManager, false,
                        GotoImplementationCustomErrorStrategy.class, false);
                implementationLocations.addAll(GotoImplementationUtil.getImplementationLocation(bLangPackage, context,
                        position.getPosition(), lsDocument.getProjectRoot()));
            } catch (UserErrorException e) {
                notifyUser(e, languageServer.getClient());
                return null;
            } catch (Throwable e) {
                String msg = "Operation 'text/implementation' failed!";
                logError(msg, e, languageServer.getClient(), position.getTextDocument(), position.getPosition());
            } finally {
                lock.unlock();
            }

            return implementationLocations;
        });
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        String docUri = params.getTextDocument().getUri();
        Path compilationPath;
        try {
            compilationPath = Paths.get(new URL(docUri).toURI());
        } catch (URISyntaxException | MalformedURLException e) {
            compilationPath = null;
        }
        if (compilationPath != null) {
            String content = params.getTextDocument().getText();
            // TODO: check the untitled file path issue
            Lock lock = documentManager.lock();
            try {
                documentManager.openFile(Paths.get(new URL(docUri).toURI()), content);
                LanguageClient client = this.languageServer.getClient();
                LSServiceOperationContext context = new LSServiceOperationContext();
                context.put(DocumentServiceKeys.FILE_URI_KEY, docUri);
                diagnosticsHelper.compileAndSendDiagnostics(client, context, documentManager);
            } catch (CompilationFailedException e) {
                String msg = "Computing 'diagnostics' failed!";
                TextDocumentIdentifier identifier = new TextDocumentIdentifier(params.getTextDocument().getUri());
                logError(msg, e, languageServer.getClient(), identifier, (Position) null);
            } catch (UserErrorException e) {
                notifyUser(e, languageServer.getClient());
            } catch (Throwable e) {
                String msg = "Operation 'text/didOpen' failed!";
                TextDocumentIdentifier identifier = new TextDocumentIdentifier(params.getTextDocument().getUri());
                logError(msg, e, languageServer.getClient(), identifier, (Position) null);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        String fileUri = params.getTextDocument().getUri();
        Optional<Path> changedPath = CommonUtil.getPathFromURI(fileUri);
        if (!changedPath.isPresent()) {
            return;
        }
        Path compilationPath = getUntitledFilePath(changedPath.toString()).orElse(changedPath.get());
        Lock lock = documentManager.lock();
        try {
            // Update content
            List<TextDocumentContentChangeEvent> changes = params.getContentChanges();
            for (TextDocumentContentChangeEvent changeEvent : changes) {
                Range changesRange = changeEvent.getRange();
                documentManager.updateFileRange(compilationPath, changesRange, changeEvent.getText());
            }
            // Update code lenses only if in incremental synchronization mode (if the language client is using
            // incremental synchronization, range of content changes should not be null).
            // Todo - Revisit after adding codelens support for full sync mode.
            if (changes.get(changes.size() - 1).getRange() != null) {
                List<CodeLens> lenses = documentManager.getCodeLenses(compilationPath);
                CodeLensUtil.updateCachedCodeLenses(lenses, changes);
            }
            // Schedule diagnostics
            LanguageClient client = this.languageServer.getClient();
            this.diagPushDebouncer.call(compilationPath, () -> {
                // Need to lock since debouncer triggers later
                Lock nLock = documentManager.lock();
                try {
                    LSServiceOperationContext context = new LSServiceOperationContext();
                    String fileURI = params.getTextDocument().getUri();
                    context.put(DocumentServiceKeys.FILE_URI_KEY, fileURI);
                    diagnosticsHelper.compileAndSendDiagnostics(client, context, documentManager);
                } catch (CompilationFailedException e) {
                    String msg = "Computing 'diagnostics' failed!";
                    logError(msg, e, client, params.getTextDocument(), (Position) null);
                } finally {
                    nLock.unlock();
                }
            });
        } catch (UserErrorException e) {
            notifyUser(e, languageServer.getClient());
        } catch (Throwable e) {
            String msg = "Operation 'text/didChange' failed!";
            logError(msg, e, languageServer.getClient(), params.getTextDocument(), (Position) null);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        Optional<Path> closedPath = CommonUtil.getPathFromURI(params.getTextDocument().getUri());

        if (!closedPath.isPresent()) {
            return;
        }

        try {
            Path compilationPath = getUntitledFilePath(closedPath.toString()).orElse(closedPath.get());
            this.documentManager.closeFile(compilationPath);
        } catch (UserErrorException e) {
            notifyUser(e, languageServer.getClient());
        } catch (Throwable e) {
            String msg = "Operation 'text/didClose' failed!";
            logError(msg, e, languageServer.getClient(), params.getTextDocument(), (Position) null);
        }
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
    }
}
