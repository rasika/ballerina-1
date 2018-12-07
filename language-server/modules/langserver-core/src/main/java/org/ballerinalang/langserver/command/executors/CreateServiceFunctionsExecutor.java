package org.ballerinalang.langserver.command.executors;

import com.google.gson.internal.LinkedTreeMap;
import org.apache.commons.lang3.tuple.Pair;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.command.ExecuteCommandKeys;
import org.ballerinalang.langserver.command.LSCommandExecutor;
import org.ballerinalang.langserver.command.LSCommandExecutorException;
import org.ballerinalang.langserver.command.testgen.TestGenerator;
import org.ballerinalang.langserver.common.constants.CommandConstants;
import org.ballerinalang.langserver.compiler.DocumentServiceKeys;
import org.ballerinalang.langserver.compiler.LSCompiler;
import org.ballerinalang.langserver.compiler.LSCompilerException;
import org.ballerinalang.langserver.compiler.LSContext;
import org.ballerinalang.langserver.compiler.workspace.WorkspaceDocumentManager;
import org.wso2.ballerinalang.compiler.tree.BLangNode;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;
import org.wso2.ballerinalang.compiler.tree.BLangService;
import org.wso2.ballerinalang.compiler.tree.BLangSimpleVariable;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangServiceConstructorExpr;
import org.wso2.ballerinalang.compiler.tree.expressions.BLangTypeInit;

import java.util.Optional;

import static org.ballerinalang.langserver.command.CommandUtil.getBLangNode;

/**
 * Represents the create service functions command executor.
 *
 * @since 0.985.0
 */
@JavaSPIService("org.ballerinalang.langserver.command.LSCommandExecutor")
public class CreateServiceFunctionsExecutor implements LSCommandExecutor {
    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(LSContext context) throws LSCommandExecutorException {
        String docUri = null;
        int line = -1;
        int column = -1;

        for (Object arg : context.get(ExecuteCommandKeys.COMMAND_ARGUMENTS_KEY)) {
            String argKey = ((LinkedTreeMap) arg).get(ARG_KEY).toString();
            String argVal = ((LinkedTreeMap) arg).get(ARG_VALUE).toString();
            switch (argKey) {
                case CommandConstants.ARG_KEY_DOC_URI:
                    docUri = argVal;
                    context.put(DocumentServiceKeys.FILE_URI_KEY, docUri);
                    break;
                case CommandConstants.ARG_KEY_NODE_LINE:
                    line = Integer.parseInt(argVal);
                    break;
                case CommandConstants.ARG_KEY_NODE_COLUMN:
                    column = Integer.parseInt(argVal);
                    break;
                default:
            }
        }

        if (line == -1 || column == -1 || docUri == null) {
            throw new LSCommandExecutorException("Invalid parameters received for the create test command!");
        }

        WorkspaceDocumentManager docManager = context.get(ExecuteCommandKeys.DOCUMENT_MANAGER_KEY);
        LSCompiler lsCompiler = context.get(ExecuteCommandKeys.LS_COMPILER_KEY);

        // Compile the source file
        BLangPackage builtSourceFile = null;
        try {
            builtSourceFile = lsCompiler.getBLangPackage(context, docManager, false, null, false);
        } catch (LSCompilerException e) {
            throw new LSCommandExecutorException("Couldn't compile the source", e);
        }

        Pair<BLangNode, Object> nodes = getBLangNode(line, column, docUri, docManager, lsCompiler, context);
        BLangNode bLangNode = nodes.getLeft();
        Object otherNode = nodes.getRight();

        BLangService service;
        if (bLangNode instanceof BLangService) {
            // is a service eg. service {};
            service = (BLangService) bLangNode;
        } else {
            // is a service variable eg. service a = service {};
            service = ((BLangServiceConstructorExpr) (((BLangSimpleVariable) bLangNode).expr)).serviceNode;
        }

        String owner = (service.listenerType != null) ? service.listenerType.tsymbol.owner.name.value : null;
        String serviceTypeName = (service.listenerType != null) ? service.listenerType.tsymbol.name.value : null;
        Optional<BLangTypeInit> optionalServiceInit = TestGenerator.getServiceInit(builtSourceFile, service);
        // Has ServiceInit
        optionalServiceInit.ifPresent(init -> {

        });
//        if (hasServiceConstructor(bLangNode)) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCommand() {
        return CommandConstants.CMD_ADD_SERVICE_FUNCTIONS;
    }
}
