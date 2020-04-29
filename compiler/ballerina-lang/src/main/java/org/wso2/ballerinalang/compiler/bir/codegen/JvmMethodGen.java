/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.ballerinalang.compiler.bir.codegen;

import org.ballerinalang.compiler.BLangCompilerException;
import org.ballerinalang.model.elements.PackageID;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.wso2.ballerinalang.compiler.bir.codegen.JvmErrorGen.ErrorHandlerGenerator;
import org.wso2.ballerinalang.compiler.bir.codegen.JvmInstructionGen.InstructionGenerator;
import org.wso2.ballerinalang.compiler.bir.codegen.JvmLabelGen.LabelGenerator;
import org.wso2.ballerinalang.compiler.bir.codegen.JvmTerminatorGen.TerminatorGenerator;
import org.wso2.ballerinalang.compiler.bir.codegen.interop.JType;
import org.wso2.ballerinalang.compiler.bir.codegen.interop.JTypeTags;
import org.wso2.ballerinalang.compiler.bir.model.BIRInstruction;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRAnnotationArrayValue;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRAnnotationAttachment;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRAnnotationLiteralValue;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRAnnotationRecordValue;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRAnnotationValue;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRBasicBlock;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRFunction;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRFunctionParameter;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRPackage;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRTypeDefinition;
import org.wso2.ballerinalang.compiler.bir.model.BIRNode.BIRVariableDcl;
import org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.BinaryOp;
import org.wso2.ballerinalang.compiler.bir.model.BIROperand;
import org.wso2.ballerinalang.compiler.bir.model.BIRTerminator;
import org.wso2.ballerinalang.compiler.bir.model.BIRTerminator.AsyncCall;
import org.wso2.ballerinalang.compiler.bir.model.BIRTerminator.GOTO;
import org.wso2.ballerinalang.compiler.bir.model.InstructionKind;
import org.wso2.ballerinalang.compiler.bir.model.VarKind;
import org.wso2.ballerinalang.compiler.bir.model.VarScope;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BInvokableSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.symbols.BPackageSymbol;
import org.wso2.ballerinalang.compiler.semantics.model.types.BField;
import org.wso2.ballerinalang.compiler.semantics.model.types.BFutureType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BInvokableType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BNilType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BObjectType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BServiceType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BType;
import org.wso2.ballerinalang.compiler.semantics.model.types.BUnionType;
import org.wso2.ballerinalang.compiler.util.Name;
import org.wso2.ballerinalang.compiler.util.TypeTags;
import org.wso2.ballerinalang.compiler.util.diagnotic.DiagnosticPos;
import org.wso2.ballerinalang.util.Flags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.ClassWriter.COMPUTE_FRAMES;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP_X1;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.V1_8;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ANNOTATION_MAP_NAME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ANNOTATION_UTILS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ARGUMENT_PARSER;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ARRAY_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.BALLERINA;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.BAL_ERRORS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.BAL_EXTENSION;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.BTYPE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.BUILT_IN_PACKAGE_NAME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.CHANNEL_DETAILS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.COMPATIBILITY_CHECKER;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.CURRENT_MODULE_INIT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.DECIMAL_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.DEFAULTABLE_ARGS_ANOT_FIELD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.DEFAULTABLE_ARGS_ANOT_NAME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.ERROR_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.FUNCTION;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.FUNCTION_POINTER;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.FUTURE_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.HANDLE_RETURNED_ERROR_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.HANDLE_STOP_PANIC_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.HANDLE_THROWABLE_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.HANDLE_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.JAVA_PACKAGE_SEPERATOR;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.JAVA_RUNTIME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.JAVA_THREAD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.LAUNCH_UTILS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MAP_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MODULE_INIT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MODULE_INIT_CLASS_NAME;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MODULE_START;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.MODULE_STOP;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.OBJECT;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.OBJECT_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.PANIC_FIELD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.RUNTIME_UTILS;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.SCHEDULER;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.SCHEDULER_START_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.SCHEDULE_FUNCTION_METHOD;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.STRAND;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.STREAM_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.STRING_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.THROWABLE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.TYPEDESC_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.VALUE_CREATOR;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.WINDOWS_PATH_SEPERATOR;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmConstants.XML_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmInstructionGen.B_STRING_VALUE;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmInstructionGen.InstructionGenerator.visitInvokeDyn;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmInstructionGen.addBoxInsn;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmInstructionGen.addUnboxInsn;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmInstructionGen.isBString;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmObservabilityGen.emitReportErrorInvocation;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmObservabilityGen.emitStartObservationInvocation;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmObservabilityGen.emitStopObservationInvocation;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmObservabilityGen.getFullQualifiedRemoteFunctionName;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmPackageGen.JavaClass;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmPackageGen.birFunctionMap;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmPackageGen.currentClass;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmPackageGen.getFunctionWrapper;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmPackageGen.getModuleLevelClassName;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmPackageGen.getPackageName;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmPackageGen.lookupGlobalVarClassName;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmPackageGen.packageToModuleId;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmPackageGen.symbolTable;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmTerminatorGen.TerminatorGenerator.toNameString;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmTerminatorGen.cleanupObjectTypeName;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmTerminatorGen.isExternStaticFunctionCall;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmTerminatorGen.loadChannelDetails;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmTypeGen.loadLocalType;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmTypeGen.loadType;
import static org.wso2.ballerinalang.compiler.bir.codegen.JvmTypeGen.typeOwnerClass;
import static org.wso2.ballerinalang.compiler.bir.codegen.interop.ExternalMethodGen.genJMethodForBExternalFunc;
import static org.wso2.ballerinalang.compiler.bir.codegen.interop.ExternalMethodGen.isBallerinaBuiltinModule;
import static org.wso2.ballerinalang.compiler.bir.codegen.interop.InteropMethodGen.JInstruction;
import static org.wso2.ballerinalang.compiler.bir.codegen.interop.InteropMethodGen.getJTypeSignature;
import static org.wso2.ballerinalang.compiler.bir.codegen.interop.InteropMethodGen.getSignatureForJType;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.ConstantLoad;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.FPLoad;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.FieldAccess;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.IsLike;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.Move;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewArray;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewError;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewInstance;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewStringXMLQName;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewStructure;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewTypeDesc;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewXMLComment;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewXMLElement;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewXMLProcIns;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewXMLQName;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.NewXMLText;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.TypeCast;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.TypeTest;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.UnaryOP;
import static org.wso2.ballerinalang.compiler.bir.model.BIRNonTerminator.XMLAccess;
import static org.wso2.ballerinalang.compiler.bir.model.BIRTerminator.Branch;
import static org.wso2.ballerinalang.compiler.bir.model.BIRTerminator.Call;
import static org.wso2.ballerinalang.compiler.bir.model.BIRTerminator.Return;

/**
 * BIR function to JVM byte code generation class.
 *
 * @since 1.2.0
 */
public class JvmMethodGen {

    public static int nextId = -1;
    public static int nextVarId = -1;

    private static final FunctionParamComparator FUNCTION_PARAM_COMPARATOR = new FunctionParamComparator();
    static BUnionType errorOrNilType;

    static void generateMethod(BIRFunction birFunc,
                               ClassWriter cw,
                               BIRPackage birModule,
                               @Nilable BType attachedType /* = () */,
                               boolean isService /* = false */,
                               String serviceName /* = "" */) {

        if (isExternFunc(birFunc)) {
            genJMethodForBExternalFunc(birFunc, cw, birModule, attachedType);
        } else {
            genJMethodForBFunc(birFunc, cw, birModule, isService, serviceName, attachedType);
        }
    }

    public static void genJMethodForBFunc(BIRFunction func,
                                          ClassWriter cw,
                                          BIRPackage module,
                                          boolean isService,
                                          String serviceName,
                                          @Nilable BType attachedType /* = () */) {

        String currentPackageName = getPackageName(module.org.value, module.name.value);
        BalToJVMIndexMap indexMap = new BalToJVMIndexMap();
        String funcName = cleanupFunctionName(func.name.value);
        int returnVarRefIndex = -1;

        BIRVariableDcl strandVar = new BIRVariableDcl(symbolTable.stringType, new Name("strand"),
                VarScope.FUNCTION, VarKind.ARG);
        int ignoreStrandVarIndex = indexMap.getIndex(strandVar);

        // generate method desc
        String desc = getMethodDesc(func.type.paramTypes, func.type.retType, null, false);
        int access = ACC_PUBLIC;
        int localVarOffset;
        if (attachedType != null) {
            localVarOffset = 1;

            // add the self as the first local var
            // TODO: find a better way
            BIRVariableDcl selfVar = new BIRVariableDcl(symbolTable.anyType, new Name("self"),
                    VarScope.FUNCTION, VarKind.ARG);
            int ignoreSelfVarIndex = indexMap.getIndex(selfVar);
        } else {
            localVarOffset = 0;
            access += ACC_STATIC;
        }

        MethodVisitor mv = cw.visitMethod(access, funcName, desc, null, null);
        InstructionGenerator instGen = new InstructionGenerator(mv, indexMap, module);
        ErrorHandlerGenerator errorGen = new ErrorHandlerGenerator(mv, indexMap, currentPackageName);
        LabelGenerator labelGen = new LabelGenerator();

        mv.visitCode();

        @Nilable Label tryStart = null;
        boolean isObserved = false;
        boolean isWorker = (func.flags & Flags.WORKER) == Flags.WORKER;
        boolean isRemote = (func.flags & Flags.REMOTE) == Flags.REMOTE;
        if ((isService || isRemote || isWorker) && !"__init".equals(funcName) && !"$__init$".equals(funcName)) {
            // create try catch block to start and stop observability.
            isObserved = true;
            tryStart = labelGen.getLabel("try-start");
            mv.visitLabel(tryStart);
        }

        Label methodStartLabel = new Label();
        mv.visitLabel(methodStartLabel);

        // generate method body
        int k = 1;

        // set channel details to strand.
        // these channel info is required to notify datachannels, when there is a panic
        // we cannot set this during strand creation, because function call do not have this info.
        if (func.workerChannels.length > 0) {
            mv.visitVarInsn(ALOAD, localVarOffset);
            loadChannelDetails(mv, Arrays.asList(func.workerChannels));
            mv.visitMethodInsn(INVOKEVIRTUAL, STRAND, "updateChannelDetails",
                    String.format("([L%s;)V", CHANNEL_DETAILS), false);
        }

        // panic if this strand is cancelled
        checkStrandCancelled(mv, localVarOffset);

        func.localVars.sort(FUNCTION_PARAM_COMPARATOR);

        @Nilable List<BIRVariableDcl> localVars = func.localVars;
        while (k < localVars.size()) {
            BIRVariableDcl localVar = getVariableDcl(localVars.get(k));
            int index = indexMap.getIndex(localVar);
            if (localVar.kind != VarKind.ARG) {
                BType bType = localVar.type;
                genDefaultValue(mv, bType, index);
            }
            k += 1;
        }

        BIRVariableDcl varDcl = getVariableDcl(localVars.get(0));
        returnVarRefIndex = indexMap.getIndex(varDcl);
        BType returnType = func.type.retType;
        genDefaultValue(mv, returnType, returnVarRefIndex);

        BIRVariableDcl stateVar = new BIRVariableDcl(symbolTable.stringType, //should  be javaInt
                new Name("state"), null, VarKind.TEMP);
        int stateVarIndex = indexMap.getIndex(stateVar);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, stateVarIndex);

        mv.visitVarInsn(ALOAD, localVarOffset);
        mv.visitFieldInsn(GETFIELD, "org/ballerinalang/jvm/scheduling/Strand", "resumeIndex", "I");
        Label resumeLable = labelGen.getLabel(funcName + "resume");
        mv.visitJumpInsn(IFGT, resumeLable);

        Label varinitLable = labelGen.getLabel(funcName + "varinit");
        mv.visitLabel(varinitLable);

        // uncomment to test yield
        // mv.visitFieldInsn(GETSTATIC, className, "i", "I");
        // mv.visitInsn(ICONST_1);
        // mv.visitInsn(IADD);
        // mv.visitFieldInsn(PUTSTATIC, className, "i", "I");

        // process basic blocks
        @Nilable List<BIRBasicBlock> basicBlocks = func.basicBlocks;

        List<Label> lables = new ArrayList<>();
        List<Integer> states = new ArrayList<>();

        int i = 0;
        int caseIndex = 0;
        while (i < basicBlocks.size()) {
            BIRBasicBlock bb = getBasicBlock(basicBlocks.get(i));
            if (i == 0) {
                lables.add(caseIndex, labelGen.getLabel(funcName + bb.id.value));
                states.add(caseIndex, caseIndex);
                caseIndex += 1;
            }
            lables.add(caseIndex, labelGen.getLabel(funcName + bb.id.value + "beforeTerm"));
            states.add(caseIndex, caseIndex);
            caseIndex += 1;
            i = i + 1;
        }

        TerminatorGenerator termGen = new TerminatorGenerator(mv, indexMap, labelGen, errorGen, module);

        // uncomment to test yield
        // mv.visitFieldInsn(GETSTATIC, className, "i", "I");
        // mv.visitIntInsn(BIPUSH, 100);
        // jvm:Label l0 = labelGen.getLabel(funcName + "l0");
        // mv.visitJumpInsn(IF_ICMPNE, l0);
        // mv.visitVarInsn(ALOAD, 0);
        // mv.visitInsn(ICONST_1);
        // mv.visitFieldInsn(PUTFIELD, "org/ballerinalang/jvm/scheduling/Strand", "yield", "Z");
        // termGen.genReturnTerm({kind:"RETURN"}, returnVarRefIndex, func);
        // mv.visitLabel(l0);

        mv.visitVarInsn(ILOAD, stateVarIndex);
        Label yieldLable = labelGen.getLabel(funcName + "yield");
        mv.visitLookupSwitchInsn(yieldLable, toIntArray(states), lables.toArray(new Label[0]));

        generateBasicBlocks(mv, basicBlocks, labelGen, errorGen, instGen, termGen, func, returnVarRefIndex,
                stateVarIndex, localVarOffset, false, module, currentPackageName, attachedType, isObserved, isService,
                serviceName);

        String frameName = getFrameClassName(currentPackageName, funcName, attachedType);
        mv.visitLabel(resumeLable);
        mv.visitVarInsn(ALOAD, localVarOffset);
        mv.visitFieldInsn(GETFIELD, "org/ballerinalang/jvm/scheduling/Strand", "frames", "[Ljava/lang/Object;");
        mv.visitVarInsn(ALOAD, localVarOffset);
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETFIELD, "org/ballerinalang/jvm/scheduling/Strand", "resumeIndex", "I");
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ISUB);
        mv.visitInsn(DUP_X1);
        mv.visitFieldInsn(PUTFIELD, "org/ballerinalang/jvm/scheduling/Strand", "resumeIndex", "I");
        mv.visitInsn(AALOAD);
        mv.visitTypeInsn(CHECKCAST, frameName);

        generateFrameClassFieldLoad(localVars, mv, indexMap, frameName);
        mv.visitFieldInsn(GETFIELD, frameName, "state", "I");
        mv.visitVarInsn(ISTORE, stateVarIndex);
        mv.visitJumpInsn(GOTO, varinitLable);

        mv.visitLabel(yieldLable);
        mv.visitTypeInsn(NEW, frameName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, frameName, "<init>", "()V", false);

        generateFrameClassFieldUpdate(localVars, mv, indexMap, frameName);

        mv.visitInsn(DUP);
        mv.visitVarInsn(ILOAD, stateVarIndex);
        mv.visitFieldInsn(PUTFIELD, frameName, "state", "I");

        BIRVariableDcl frameVar = new BIRVariableDcl(symbolTable.stringType, new Name("frame"), null, VarKind.TEMP);
        int frameVarIndex = indexMap.getIndex(frameVar);
        mv.visitVarInsn(ASTORE, frameVarIndex);

        mv.visitVarInsn(ALOAD, localVarOffset);
        mv.visitFieldInsn(GETFIELD, "org/ballerinalang/jvm/scheduling/Strand", "frames", "[Ljava/lang/Object;");
        mv.visitVarInsn(ALOAD, localVarOffset);
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETFIELD, "org/ballerinalang/jvm/scheduling/Strand", "resumeIndex", "I");
        mv.visitInsn(DUP_X1);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitFieldInsn(PUTFIELD, "org/ballerinalang/jvm/scheduling/Strand", "resumeIndex", "I");
        mv.visitVarInsn(ALOAD, frameVarIndex);
        mv.visitInsn(AASTORE);

        Label methodEndLabel = new Label();
        // generate the try catch finally to stop observing if an error occurs.
        if (isObserved) {
            Label tryEnd = labelGen.getLabel("try-end");
            Label tryCatch = labelGen.getLabel("try-handler");
            // visitTryCatchBlock visited at the end since order of the error table matters.
            mv.visitTryCatchBlock((Label) tryStart, tryEnd, tryCatch, ERROR_VALUE);
            Label tryFinally = labelGen.getLabel("try-finally");
            mv.visitTryCatchBlock((Label) tryStart, tryEnd, tryFinally, null);
            Label tryCatchFinally = labelGen.getLabel("try-catch-finally");
            mv.visitTryCatchBlock(tryCatch, tryCatchFinally, tryFinally, null);

            BIRVariableDcl catchVarDcl = new BIRVariableDcl(symbolTable.anyType, new Name("$_catch_$"),
                    VarScope.FUNCTION, VarKind.ARG);
            int catchVarIndex = indexMap.getIndex(catchVarDcl);
            BIRVariableDcl throwableVarDcl = new BIRVariableDcl(symbolTable.anyType, new Name("$_throwable_$"),
                    VarScope.FUNCTION, VarKind.ARG);
            int throwableVarIndex = indexMap.getIndex(throwableVarDcl);

            // Try-To-Finally
            mv.visitLabel(tryEnd);
            // emitStopObservationInvocation(mv, localVarOffset);
            Label tryBlock1 = labelGen.getLabel("try-block-1");
            mv.visitLabel(tryBlock1);
            mv.visitJumpInsn(GOTO, methodEndLabel);

            // Catch Block
            mv.visitLabel(tryCatch);
            mv.visitVarInsn(ASTORE, catchVarIndex);
            Label tryBlock2 = labelGen.getLabel("try-block-2");
            mv.visitLabel(tryBlock2);
            emitReportErrorInvocation(mv, localVarOffset, catchVarIndex);
            mv.visitLabel(tryCatchFinally);
            emitStopObservationInvocation(mv, localVarOffset);
            Label tryBlock3 = labelGen.getLabel("try-block-3");
            mv.visitLabel(tryBlock3);
            // re-throw caught error value
            mv.visitVarInsn(ALOAD, catchVarIndex);
            mv.visitInsn(ATHROW);

            // Finally Block
            mv.visitLabel(tryFinally);
            mv.visitVarInsn(ASTORE, throwableVarIndex);
            emitStopObservationInvocation(mv, localVarOffset);
            Label tryBlock4 = labelGen.getLabel("try-block-4");
            mv.visitLabel(tryBlock4);
            mv.visitVarInsn(ALOAD, throwableVarIndex);
            mv.visitInsn(ATHROW);
        }
        mv.visitLabel(methodEndLabel);
        termGen.genReturnTerm(new Return(null), returnVarRefIndex, func, false, -1);

        // Create Local Variable Table
        k = localVarOffset;
        // Add strand variable to LVT
        mv.visitLocalVariable("__strand", String.format("L%s;", STRAND), null, methodStartLabel, methodEndLabel,
                localVarOffset);
        while (k < localVars.size()) {
            BIRVariableDcl localVar = getVariableDcl(localVars.get(k));
            Label startLabel = methodStartLabel;
            Label endLabel = methodEndLabel;
            boolean tmpBoolParam = localVar.type.tag == TypeTags.BOOLEAN && localVar.name.value.startsWith("%syn");
            if (!tmpBoolParam && (localVar.kind == VarKind.LOCAL || localVar.kind == VarKind.ARG)) {
                // local vars have visible range information
                if (localVar.kind == VarKind.LOCAL) {
                    int insOffset = localVar.insOffset;
                    if (localVar.startBB != null) {
                        startLabel = labelGen.getLabel(funcName + localVar.startBB.id.value + "ins" + insOffset);
                    }
                    if (localVar.endBB != null) {
                        endLabel = labelGen.getLabel(funcName + localVar.endBB.id.value + "beforeTerm");
                    }
                }
                String metaVarName = localVar.metaVarName;
                if (metaVarName != null && !"".equals(metaVarName) &&
                        // filter out compiler added vars
                        !((metaVarName.startsWith("$") && metaVarName.endsWith("$"))
                                || (metaVarName.startsWith("$$") && metaVarName.endsWith("$$"))
                                || metaVarName.startsWith("_$$_"))) {
                    mv.visitLocalVariable(metaVarName, getJVMTypeSign(localVar.type), null,
                                          startLabel, endLabel, indexMap.getIndex(localVar));
                }
            }
            k = k + 1;
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static int[] toIntArray(List<Integer> states) {

        int[] ints = new int[states.size()];
        for (int i = 0; i < states.size(); i++) {
            ints[i] = states.get(i);
        }
        return ints;
    }

    private static void generateFrameClassFieldLoad(List<BIRVariableDcl> localVars, MethodVisitor mv,
                                                    BalToJVMIndexMap indexMap, String frameName) {

        int k = 0;
        while (k < localVars.size()) {
            BIRVariableDcl localVar = getVariableDcl(localVars.get(k));
            int index = indexMap.getIndex(localVar);
            BType bType = localVar.type;
            mv.visitInsn(DUP);

            if (TypeTags.isIntegerTypeTag(bType.tag)) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "J");
                mv.visitVarInsn(LSTORE, index);
            } else if (bType.tag == TypeTags.BYTE) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "I");
                mv.visitVarInsn(ISTORE, index);
            } else if (bType.tag == TypeTags.FLOAT) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "D");
                mv.visitVarInsn(DSTORE, index);
            } else if (TypeTags.isStringTypeTag(bType.tag)) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                                  String.format("L%s;", isBString ? B_STRING_VALUE : STRING_VALUE));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == TypeTags.DECIMAL) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", DECIMAL_VALUE));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == TypeTags.BOOLEAN) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "Z");
                mv.visitVarInsn(ISTORE, index);
            } else if (bType.tag == TypeTags.MAP || bType.tag == TypeTags.RECORD) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", MAP_VALUE));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == TypeTags.STREAM) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", STREAM_VALUE));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == TypeTags.ARRAY ||
                    bType.tag == TypeTags.TUPLE) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", ARRAY_VALUE));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == TypeTags.OBJECT) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", OBJECT_VALUE));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == TypeTags.ERROR) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", ERROR_VALUE));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == TypeTags.FUTURE) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", FUTURE_VALUE));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == TypeTags.INVOKABLE) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", FUNCTION_POINTER));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == TypeTags.TYPEDESC) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", TYPEDESC_VALUE));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == TypeTags.NIL ||
                    bType.tag == TypeTags.ANY ||
                    bType.tag == TypeTags.ANYDATA ||
                    bType.tag == TypeTags.UNION ||
                    bType.tag == TypeTags.JSON ||
                    bType.tag == TypeTags.FINITE ||
                    bType.tag == TypeTags.READONLY) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", OBJECT));
                mv.visitVarInsn(ASTORE, index);
            } else if (TypeTags.isXMLTypeTag(bType.tag)) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", XML_VALUE));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == TypeTags.HANDLE) {
                mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", HANDLE_VALUE));
                mv.visitVarInsn(ASTORE, index);
            } else if (bType.tag == JTypeTags.JTYPE) {
                generateFrameClassJFieldLoad(localVar, mv, index, frameName);
            } else {
                throw new BLangCompilerException("JVM generation is not supported for type " +
                        String.format("%s", bType));
            }
            k = k + 1;
        }

    }

    private static void generateFrameClassJFieldLoad(BIRVariableDcl localVar, MethodVisitor mv,
                                                     int index, String frameName) {

        JType jType = (JType) localVar.type;

        if (jType.jTag == JTypeTags.JBYTE) {
            mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "I");
            mv.visitVarInsn(ISTORE, index);
        } else if (jType.jTag == JTypeTags.JCHAR) {
            mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "I");
            mv.visitVarInsn(ISTORE, index);
        } else if (jType.jTag == JTypeTags.JSHORT) {
            mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "I");
            mv.visitVarInsn(ISTORE, index);
        } else if (jType.jTag == JTypeTags.JINT) {
            mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "I");
            mv.visitVarInsn(ISTORE, index);
        } else if (jType.jTag == JTypeTags.JLONG) {
            mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "J");
            mv.visitVarInsn(LSTORE, index);
        } else if (jType.jTag == JTypeTags.JFLOAT) {
            mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "F");
            mv.visitVarInsn(FSTORE, index);
        } else if (jType.jTag == JTypeTags.JDOUBLE) {
            mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "D");
            mv.visitVarInsn(DSTORE, index);
        } else if (jType.jTag == JTypeTags.JBOOLEAN) {
            mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), "Z");
            mv.visitVarInsn(ISTORE, index);
        } else if (jType.jTag == JTypeTags.JARRAY ||
                jType.jTag == JTypeTags.JREF) {
            mv.visitFieldInsn(GETFIELD, frameName, localVar.name.value.replace("%", "_"), getJTypeSignature(jType));
            mv.visitVarInsn(ASTORE, index);
        } else {
            throw new BLangCompilerException("JVM generation is not supported for type " + String.format("%s", jType));
        }
    }

    private static void generateFrameClassFieldUpdate(@Nilable List<BIRVariableDcl> localVars, MethodVisitor mv,
                                                      BalToJVMIndexMap indexMap, String frameName) {

        int k = 0;
        while (k < localVars.size()) {
            BIRVariableDcl localVar = getVariableDcl(localVars.get(k));
            int index = indexMap.getIndex(localVar);
            mv.visitInsn(DUP);

            BType bType = localVar.type;
            if (TypeTags.isIntegerTypeTag(bType.tag)) {
                mv.visitVarInsn(LLOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "J");
            } else if (bType.tag == TypeTags.BYTE) {
                mv.visitVarInsn(ILOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "I");
            } else if (bType.tag == TypeTags.FLOAT) {
                mv.visitVarInsn(DLOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "D");
            } else if (TypeTags.isStringTypeTag(bType.tag)) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                                  String.format("L%s;", isBString ? B_STRING_VALUE : STRING_VALUE));
            } else if (bType.tag == TypeTags.DECIMAL) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", DECIMAL_VALUE));
            } else if (bType.tag == TypeTags.BOOLEAN) {
                mv.visitVarInsn(ILOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "Z");
            } else if (bType.tag == TypeTags.MAP ||
                    bType.tag == TypeTags.RECORD) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", MAP_VALUE));
            } else if (bType.tag == TypeTags.STREAM) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", STREAM_VALUE));
            } else if (bType.tag == TypeTags.ARRAY ||
                    bType.tag == TypeTags.TUPLE) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", ARRAY_VALUE));
            } else if (bType.tag == TypeTags.ERROR) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", ERROR_VALUE));
            } else if (bType.tag == TypeTags.FUTURE) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", FUTURE_VALUE));
            } else if (bType.tag == TypeTags.TYPEDESC) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitTypeInsn(CHECKCAST, TYPEDESC_VALUE);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", TYPEDESC_VALUE));
            } else if (bType.tag == TypeTags.OBJECT) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", OBJECT_VALUE));
            } else if (bType.tag == TypeTags.INVOKABLE) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", FUNCTION_POINTER));
            } else if (bType.tag == TypeTags.NIL ||
                    bType.tag == TypeTags.ANY ||
                    bType.tag == TypeTags.ANYDATA ||
                    bType.tag == TypeTags.UNION ||
                    bType.tag == TypeTags.JSON ||
                    bType.tag == TypeTags.FINITE ||
                    bType.tag == TypeTags.READONLY) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", OBJECT));
            } else if (TypeTags.isXMLTypeTag(bType.tag)) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", XML_VALUE));
            } else if (bType.tag == TypeTags.HANDLE) {
                mv.visitVarInsn(ALOAD, index);
                mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"),
                        String.format("L%s;", HANDLE_VALUE));
            } else if (bType.tag == JTypeTags.JTYPE) {
                generateFrameClassJFieldUpdate(localVar, mv, index, frameName);
            } else {
                throw new BLangCompilerException("JVM generation is not supported for type " +
                        String.format("%s", bType));
            }
            k = k + 1;
        }
    }

    private static void generateFrameClassJFieldUpdate(BIRVariableDcl localVar, MethodVisitor mv,
                                                       int index, String frameName) {

        JType jType = (JType) localVar.type;
        if (jType.jTag == JTypeTags.JBYTE) {
            mv.visitVarInsn(ILOAD, index);
            mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "B");
        } else if (jType.jTag == JTypeTags.JCHAR) {
            mv.visitVarInsn(ILOAD, index);
            mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "C");
        } else if (jType.jTag == JTypeTags.JSHORT) {
            mv.visitVarInsn(ILOAD, index);
            mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "S");
        } else if (jType.jTag == JTypeTags.JINT) {
            mv.visitVarInsn(ILOAD, index);
            mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "I");
        } else if (jType.jTag == JTypeTags.JLONG) {
            mv.visitVarInsn(LLOAD, index);
            mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "J");
        } else if (jType.jTag == JTypeTags.JFLOAT) {
            mv.visitVarInsn(FLOAD, index);
            mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "F");
        } else if (jType.jTag == JTypeTags.JDOUBLE) {
            mv.visitVarInsn(DLOAD, index);
            mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "D");
        } else if (jType.jTag == JTypeTags.JBOOLEAN) {
            mv.visitVarInsn(ILOAD, index);
            mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), "Z");
        } else if (jType.jTag == JTypeTags.JARRAY || jType.jTag == JTypeTags.JREF) {
            String classSig = getJTypeSignature(jType);
            String className = getSignatureForJType(jType);
            mv.visitVarInsn(ALOAD, index);
            mv.visitTypeInsn(CHECKCAST, className);
            mv.visitFieldInsn(PUTFIELD, frameName, localVar.name.value.replace("%", "_"), classSig);
        } else {
            throw new BLangCompilerException("JVM generation is not supported for type " + String.format("%s", jType));
        }
    }

    private static String getJVMTypeSign(BType bType) {

        String jvmType = "";
        if (TypeTags.isIntegerTypeTag(bType.tag)) {
            jvmType = "J";
        } else if (bType.tag == TypeTags.BYTE) {
            jvmType = "I";
        } else if (bType.tag == TypeTags.FLOAT) {
            jvmType = "D";
        } else if (bType.tag == TypeTags.BOOLEAN) {
            jvmType = "Z";
        } else if (TypeTags.isStringTypeTag(bType.tag)) {
            jvmType = String.format("L%s;", STRING_VALUE);
        } else if (bType.tag == TypeTags.DECIMAL) {
            jvmType = String.format("L%s;", DECIMAL_VALUE);
        } else if (bType.tag == TypeTags.MAP || bType.tag == TypeTags.RECORD) {
            jvmType = String.format("L%s;", MAP_VALUE);
        } else if (bType.tag == TypeTags.STREAM) {
            jvmType = String.format("L%s;", STREAM_VALUE);
        } else if (bType.tag == TypeTags.ARRAY ||
                bType.tag == TypeTags.TUPLE) {
            jvmType = String.format("L%s;", ARRAY_VALUE);
        } else if (bType.tag == TypeTags.OBJECT) {
            jvmType = String.format("L%s;", OBJECT_VALUE);
        } else if (bType.tag == TypeTags.ERROR) {
            jvmType = String.format("L%s;", ERROR_VALUE);
        } else if (bType.tag == TypeTags.FUTURE) {
            jvmType = String.format("L%s;", FUTURE_VALUE);
        } else if (bType.tag == TypeTags.INVOKABLE) {
            jvmType = String.format("L%s;", FUNCTION_POINTER);
        } else if (bType.tag == TypeTags.HANDLE) {
            jvmType = String.format("L%s;", HANDLE_VALUE);
        } else if (bType.tag == TypeTags.TYPEDESC) {
            jvmType = String.format("L%s;", TYPEDESC_VALUE);
        } else if (bType.tag == TypeTags.NIL
                || bType.tag == TypeTags.ANY
                || bType.tag == TypeTags.ANYDATA
                || bType.tag == TypeTags.UNION
                || bType.tag == TypeTags.JSON
                || bType.tag == TypeTags.FINITE
                || bType.tag == TypeTags.READONLY) {
            jvmType = String.format("L%s;", OBJECT);
        } else if (bType.tag == JTypeTags.JTYPE) {
            jvmType = getJTypeSignature((JType) bType);
        } else if (TypeTags.isXMLTypeTag(bType.tag)) {
            jvmType = String.format("L%s;", XML_VALUE);
        } else {
            throw new BLangCompilerException("JVM code generation is not supported for type " +
                    String.format("%s", bType));
        }
        return jvmType;
    }

    public static void generateBasicBlocks(MethodVisitor mv, @Nilable List<BIRBasicBlock> basicBlocks,
                                           LabelGenerator labelGen, ErrorHandlerGenerator errorGen,
                                           InstructionGenerator instGen, TerminatorGenerator termGen,
                                           BIRFunction func, int returnVarRefIndex, int stateVarIndex,
                                           int localVarOffset, boolean isArg, BIRPackage module,
                                           String currentPackageName, @Nilable BType attachedType,
                                           boolean isObserved /* = false */,
                                           boolean isService /* = false */, String serviceName /* = "" */) {

        int j = 0;
        String funcName = cleanupFunctionName(func.name.value);

        int caseIndex = 0;

        while (j < basicBlocks.size()) {
            BIRBasicBlock bb = getBasicBlock(basicBlocks.get(j));
            String currentBBName = String.format("%s", bb.id.value);

            // create jvm label
            Label bbLabel = labelGen.getLabel(funcName + bb.id.value);
            mv.visitLabel(bbLabel);
            if (j == 0 && !isArg) {
                // SIPUSH range is (-32768 to 32767) so if the state index goes beyond that, need to use visitLdcInsn
                mv.visitIntInsn(SIPUSH, caseIndex);
                mv.visitVarInsn(ISTORE, stateVarIndex);
                caseIndex += 1;
            }

            String serviceOrConnectorName = serviceName;
            if (isObserved && j == 0) {
                String observationStartMethod = isService ? "startResourceObservation" : "startCallableObservation";
                if (!isService && attachedType != null && attachedType.tag == TypeTags.OBJECT) {
                    // add module org and module name to remote spans.
                    BObjectType attachedTypeObj = (BObjectType) attachedType;
                    serviceOrConnectorName = getFullQualifiedRemoteFunctionName(
                            attachedTypeObj.tsymbol.pkgID.orgName.value,
                            attachedTypeObj.tsymbol.pkgID.name.value, serviceName);
                }
                emitStartObservationInvocation(mv, localVarOffset, serviceOrConnectorName, funcName,
                        observationStartMethod);
            }

            // generate instructions
            int m = 0;
            int insCount = bb.instructions.size();

            InstructionKind insKind;
            while (m < insCount) {
                Label insLabel = labelGen.getLabel(funcName + bb.id.value + "ins" + m);
                mv.visitLabel(insLabel);
                @Nilable BIRInstruction inst = bb.instructions.get(m);
                if (inst == null) {
                    continue;
                } else {
                    insKind = inst.getKind();
                    generateDiagnosticPos(((BIRNode) inst).pos, mv);
                }

                if (inst instanceof BinaryOp) {
                    instGen.generateBinaryOpIns((BinaryOp) inst);
                } else {
                    switch (insKind) {
                        case MOVE:
                            instGen.generateMoveIns((Move) inst);
                            break;
                        case CONST_LOAD:
                            instGen.generateConstantLoadIns((ConstantLoad) inst);
                            break;
                        case NEW_STRUCTURE:
                            instGen.generateMapNewIns((NewStructure) inst, localVarOffset);
                            break;
                        case NEW_INSTANCE:
                            instGen.generateObjectNewIns((NewInstance) inst, localVarOffset);
                            break;
                        case MAP_STORE:
                            instGen.generateMapStoreIns((FieldAccess) inst);
                            break;
                        case NEW_ARRAY:
                            instGen.generateArrayNewIns((NewArray) inst);
                            break;
                        case ARRAY_STORE:
                            instGen.generateArrayStoreIns((FieldAccess) inst);
                            break;
                        case MAP_LOAD:
                            instGen.generateMapLoadIns((FieldAccess) inst);
                            break;
                        case ARRAY_LOAD:
                            instGen.generateArrayValueLoad((FieldAccess) inst);
                            break;
                        case NEW_ERROR:
                            instGen.generateNewErrorIns((NewError) inst);
                            break;
                        case TYPE_CAST:
                            instGen.generateCastIns((TypeCast) inst);
                            break;
                        case IS_LIKE:
                            instGen.generateIsLikeIns((IsLike) inst);
                            break;
                        case TYPE_TEST:
                            instGen.generateTypeTestIns((TypeTest) inst);
                            break;
                        case OBJECT_STORE:
                            instGen.generateObjectStoreIns((FieldAccess) inst);
                            break;
                        case OBJECT_LOAD:
                            instGen.generateObjectLoadIns((FieldAccess) inst);
                            break;
                        case NEW_XML_ELEMENT:
                            instGen.generateNewXMLElementIns((NewXMLElement) inst);
                            break;
                        case NEW_XML_TEXT:
                            instGen.generateNewXMLTextIns((NewXMLText) inst);
                            break;
                        case NEW_XML_COMMENT:
                            instGen.generateNewXMLCommentIns((NewXMLComment) inst);
                            break;
                        case NEW_XML_PI:
                            instGen.generateNewXMLProcIns((NewXMLProcIns) inst);
                            break;
                        case NEW_XML_QNAME:
                            instGen.generateNewXMLQNameIns((NewXMLQName) inst);
                            break;
                        case NEW_STRING_XML_QNAME:
                            instGen.generateNewStringXMLQNameIns((NewStringXMLQName) inst);
                            break;
                        case XML_SEQ_STORE:
                            instGen.generateXMLStoreIns((XMLAccess) inst);
                            break;
                        case XML_SEQ_LOAD:
                            instGen.generateXMLLoadIns((FieldAccess) inst);
                            break;
                        case XML_LOAD:
                            instGen.generateXMLLoadIns((FieldAccess) inst);
                            break;
                        case XML_LOAD_ALL:
                            instGen.generateXMLLoadAllIns((XMLAccess) inst);
                            break;
                        case XML_ATTRIBUTE_STORE:
                            instGen.generateXMLAttrStoreIns((FieldAccess) inst);
                            break;
                        case XML_ATTRIBUTE_LOAD:
                            instGen.generateXMLAttrLoadIns((FieldAccess) inst);
                            break;
                        case FP_LOAD:
                            instGen.generateFPLoadIns((FPLoad) inst);
                            break;
                        case STRING_LOAD:
                            instGen.generateStringLoadIns((FieldAccess) inst);
                            break;
                        case TYPEOF:
                            instGen.generateTypeofIns((UnaryOP) inst);
                            break;
                        case NOT:
                            instGen.generateNotIns((UnaryOP) inst);
                            break;
                        case NEW_TYPEDESC:
                            instGen.generateNewTypedescIns((NewTypeDesc) inst);
                            break;
                        case NEGATE:
                            instGen.generateNegateIns((UnaryOP) inst);
                            break;
                        case PLATFORM:
                            instGen.generatePlatformIns((JInstruction) inst);
                            break;
                        default:
                            throw new BLangCompilerException("JVM generation is not supported for operation " +
                                    String.format("%s", inst));
                    }
                }
                m += 1;
            }

            Label bbEndLable = labelGen.getLabel(funcName + bb.id.value + "beforeTerm");
            mv.visitLabel(bbEndLable);

            BIRTerminator terminator = bb.terminator;
            if (!isArg) {
                // SIPUSH range is (-32768 to 32767) so if the state index goes beyond that, need to use visitLdcInsn
                mv.visitIntInsn(SIPUSH, caseIndex);
                mv.visitVarInsn(ISTORE, stateVarIndex);
                caseIndex += 1;
            }

            // process terminator
            if (!isArg || (!(terminator instanceof Return))) {
                generateDiagnosticPos(terminator.pos, mv);
                if (isModuleInitFunction(module, func) && terminator instanceof Return) {
                    generateAnnotLoad(mv, module.typeDefs, getPackageName(module.org.value, module.name.value));
                }
                termGen.genTerminator(terminator, func, funcName, localVarOffset, returnVarRefIndex, attachedType,
                        isObserved);
            }

            errorGen.generateTryCatch(func, funcName, bb, instGen, termGen, labelGen);

            BIRBasicBlock thenBB = terminator.thenBB;
            if (thenBB != null) {
                genYieldCheck(mv, termGen.labelGen, thenBB, funcName, localVarOffset);
            }
            j += 1;
        }
    }

    private static void genYieldCheck(MethodVisitor mv, LabelGenerator labelGen, BIRBasicBlock thenBB, String funcName,
                                      int localVarOffset) {

        mv.visitVarInsn(ALOAD, localVarOffset);
        mv.visitMethodInsn(INVOKEVIRTUAL, STRAND, "isYielded", "()Z", false);
        Label yieldLabel = labelGen.getLabel(funcName + "yield");
        mv.visitJumpInsn(IFNE, yieldLabel);

        // goto thenBB
        Label gotoLabel = labelGen.getLabel(funcName + thenBB.id.value);
        mv.visitJumpInsn(GOTO, gotoLabel);
    }

    static void generateLambdaMethod(BIRInstruction ins, ClassWriter cw, String lambdaName) {

        @Nilable BType lhsType;
        String orgName;
        String moduleName;
        String funcName;
        int paramIndex = 1;
        boolean isVirtual = false;
        InstructionKind kind = ins.getKind();
        if (kind == InstructionKind.ASYNC_CALL) {
            AsyncCall asyncIns = (AsyncCall) ins;
            isVirtual = asyncIns.isVirtual;
            lhsType = asyncIns.lhsOp != null ? asyncIns.lhsOp.variableDcl.type : null;
            orgName = asyncIns.calleePkg.orgName.value;
            moduleName = asyncIns.calleePkg.name.value;
            funcName = asyncIns.name.getValue();
        } else if (kind == InstructionKind.FP_LOAD) {
            FPLoad fpIns = (FPLoad) ins;
            lhsType = fpIns.lhsOp.variableDcl.type;
            orgName = fpIns.pkgId.orgName.value;
            moduleName = fpIns.pkgId.name.value;
            funcName = fpIns.funcName.getValue();
        } else {
            throw new BLangCompilerException("JVM lambda method generation is not supported for instruction " +
                    String.format("%s", ins));
        }

        boolean isExternFunction = isExternStaticFunctionCall(ins);
        boolean isBuiltinModule = isBallerinaBuiltinModule(orgName, moduleName);

        BType returnType;
        if (lhsType.tag == TypeTags.FUTURE) {
            returnType = ((BFutureType) lhsType).constraint;
        } else if (ins instanceof FPLoad) {
            returnType = ((FPLoad) ins).retType;
            if (returnType.tag == TypeTags.INVOKABLE) {
                returnType = ((BInvokableType) returnType).retType;
            }
        } else {
            throw new BLangCompilerException("JVM generation is not supported for async return type " +
                    String.format("%s", lhsType));
        }

        int closureMapsCount = 0;
        if (kind == InstructionKind.FP_LOAD) {
            closureMapsCount = ((FPLoad) ins).closureMaps.size();
        }
        String closureMapsDesc = getMapValueDesc(closureMapsCount);

        MethodVisitor mv;
        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, cleanupFunctionName(lambdaName),
                String.format("(%s[L%s;)L%s;", closureMapsDesc, OBJECT, OBJECT), null, null);

        mv.visitCode();
        // load strand as first arg
        // strand and other args are in a object[] param. This param comes after closure maps.
        // hence the closureMapsCount is equal to the array's param index.
        mv.visitVarInsn(ALOAD, closureMapsCount);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(AALOAD);
        mv.visitTypeInsn(CHECKCAST, STRAND);

        if (isExternFunction) {
            Label blockedOnExternLabel = new Label();

            mv.visitInsn(DUP);

            mv.visitMethodInsn(INVOKEVIRTUAL, STRAND, "isBlockedOnExtern", "()Z", false);
            mv.visitJumpInsn(IFEQ, blockedOnExternLabel);

            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_0);
            mv.visitFieldInsn(PUTFIELD, STRAND, "blockedOnExtern", "Z");

            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, STRAND, "returnValue", "Ljava/lang/Object;");
            mv.visitInsn(ARETURN);

            mv.visitLabel(blockedOnExternLabel);
        }
        @Nilable List<BType> paramBTypes = new ArrayList<>();

        if (kind == InstructionKind.ASYNC_CALL) {
            AsyncCall asyncIns = (AsyncCall) ins;
            @Nilable List<BIROperand> paramTypes = asyncIns.args;
            if (isVirtual) {
                genLoadDataForObjectAttachedLambdas(asyncIns, mv, closureMapsCount, paramTypes, isBuiltinModule);
                int paramTypeIndex = 1;
                paramIndex = 2;
                while (paramTypeIndex < paramTypes.size()) {
                    generateObjectArgs(mv, paramIndex);
                    paramTypeIndex += 1;
                    paramIndex += 1;
                    if (!isBuiltinModule) {
                        generateObjectArgs(mv, paramIndex);
                        paramIndex += 1;
                    }
                }
            } else {
                // load and cast param values
                int argIndex = 1;
                for (BIROperand paramType : paramTypes) {
                    BIROperand ref = getVarRef(paramType);
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitIntInsn(BIPUSH, argIndex);
                    mv.visitInsn(AALOAD);
                    addUnboxInsn(mv, ref.variableDcl.type);
                    paramBTypes.add(paramIndex - 1, paramType.variableDcl.type);
                    paramIndex += 1;

                    argIndex += 1;
                    if (!isBuiltinModule) {
                        addBooleanTypeToLambdaParamTypes(mv, 0, argIndex);
                        paramBTypes.add(paramIndex - 1, symbolTable.booleanType);
                        paramIndex += 1;
                    }
                    argIndex += 1;
                }
            }
        } else {
            //load closureMaps
            int i = 0;
            while (i < closureMapsCount) {
                mv.visitVarInsn(ALOAD, i);
                mv.visitInsn(ICONST_1);
                i += 1;
            }

            @Nilable List<BIRVariableDcl> paramTypes = ((FPLoad) ins).params;
            // load and cast param values

            int argIndex = 1;
            for (BIRVariableDcl paramType : paramTypes) {
                BIRVariableDcl dcl = getVariableDcl(paramType);
                mv.visitVarInsn(ALOAD, closureMapsCount);
                mv.visitIntInsn(BIPUSH, argIndex);
                mv.visitInsn(AALOAD);
                addUnboxInsn(mv, dcl.type);
                paramBTypes.add(paramIndex - 1, dcl.type);
                paramIndex += 1;
                i += 1;
                argIndex += 1;

                if (!isBuiltinModule) {
                    addBooleanTypeToLambdaParamTypes(mv, closureMapsCount, argIndex);
                    paramBTypes.add(paramIndex - 1, symbolTable.booleanType);
                    paramIndex += 1;
                }
                argIndex += 1;
            }
        }

        if (isVirtual) {
            String methodDesc = String.format("(L%s;L%s;[L%s;)L%s;", STRAND, STRING_VALUE, OBJECT, OBJECT);
            mv.visitMethodInsn(INVOKEINTERFACE, OBJECT_VALUE, "call", methodDesc, true);
        } else {
            String jvmClass;
            String lookupKey = getPackageName(orgName, moduleName) + funcName;
            JvmPackageGen.BIRFunctionWrapper functionWrapper = birFunctionMap.get(lookupKey);
            String methodDesc = getLambdaMethodDesc(paramBTypes, returnType, closureMapsCount);
            if (functionWrapper != null) {
                jvmClass = functionWrapper.fullQualifiedClassName;
            } else {
                BPackageSymbol symbol = CodeGenerator.packageCache.getSymbol(orgName + "/" + moduleName);
                BInvokableSymbol funcSymbol = (BInvokableSymbol) symbol.scope.lookup(new Name(funcName)).symbol;
                BInvokableType type = (BInvokableType) funcSymbol.type;
                ArrayList<BType> params = new ArrayList<>(type.paramTypes);
                if (type.restType != null) {
                    params.add(type.restType);
                }
                for (int j = params.size() - 1; j >= 0; j--) {
                    params.add(j + 1, symbolTable.booleanType);
                }
                String balFileName = funcSymbol.source;

                if (balFileName == null || !balFileName.endsWith(BAL_EXTENSION)) {
                    balFileName = MODULE_INIT_CLASS_NAME;
                }

                jvmClass = getModuleLevelClassName(orgName, moduleName,
                        cleanupPathSeperators(cleanupBalExt(balFileName)));
            }

            mv.visitMethodInsn(INVOKESTATIC, jvmClass, funcName, methodDesc, false);
        }

        if (!isVirtual) {
            addBoxInsn(mv, returnType);
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void genLoadDataForObjectAttachedLambdas(AsyncCall ins, MethodVisitor mv, int closureMapsCount,
                                                            @Nilable List<BIROperand> paramTypes,
                                                            boolean isBuiltinModule) {

        mv.visitInsn(POP);
        mv.visitVarInsn(ALOAD, closureMapsCount);
        mv.visitInsn(ICONST_1);
        BIROperand ref = getVarRef(ins.args.get(0));
        mv.visitInsn(AALOAD);
        addUnboxInsn(mv, ref.variableDcl.type);
        mv.visitVarInsn(ALOAD, closureMapsCount);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(AALOAD);
        mv.visitTypeInsn(CHECKCAST, STRAND);

        mv.visitLdcInsn(cleanupObjectTypeName(ins.name.value));
        int objectArrayLength = paramTypes.size() - 1;
        if (!isBuiltinModule) {
            mv.visitIntInsn(BIPUSH, objectArrayLength * 2);
        } else {
            mv.visitIntInsn(BIPUSH, objectArrayLength);
        }
        mv.visitTypeInsn(ANEWARRAY, OBJECT);
    }

    private static void generateObjectArgs(MethodVisitor mv, int paramIndex) {

        mv.visitInsn(DUP);
        mv.visitIntInsn(BIPUSH, paramIndex - 2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitIntInsn(BIPUSH, paramIndex + 1);
        mv.visitInsn(AALOAD);
        mv.visitInsn(AASTORE);
    }

    private static void addBooleanTypeToLambdaParamTypes(MethodVisitor mv, int arrayIndex, int paramIndex) {

        mv.visitVarInsn(ALOAD, arrayIndex);
        mv.visitIntInsn(BIPUSH, paramIndex);
        mv.visitInsn(AALOAD);
        addUnboxInsn(mv, symbolTable.booleanType);
    }

    private static void genDefaultValue(MethodVisitor mv, BType bType, int index) {

        if (TypeTags.isIntegerTypeTag(bType.tag)) {
            mv.visitInsn(LCONST_0);
            mv.visitVarInsn(LSTORE, index);
        } else if (bType.tag == TypeTags.BYTE) {
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, index);
        } else if (bType.tag == TypeTags.FLOAT) {
            mv.visitInsn(DCONST_0);
            mv.visitVarInsn(DSTORE, index);
        } else if (TypeTags.isStringTypeTag(bType.tag)) {
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, index);
        } else if (bType.tag == TypeTags.BOOLEAN) {
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, index);
        } else if (bType.tag == TypeTags.MAP ||
                bType.tag == TypeTags.ARRAY ||
                bType.tag == TypeTags.STREAM ||
                bType.tag == TypeTags.ERROR ||
                bType.tag == TypeTags.NIL ||
                bType.tag == TypeTags.ANY ||
                bType.tag == TypeTags.ANYDATA ||
                bType.tag == TypeTags.OBJECT ||
                bType.tag == TypeTags.CHAR_STRING ||
                bType.tag == TypeTags.DECIMAL ||
                bType.tag == TypeTags.UNION ||
                bType.tag == TypeTags.RECORD ||
                bType.tag == TypeTags.TUPLE ||
                bType.tag == TypeTags.FUTURE ||
                bType.tag == TypeTags.JSON ||
                TypeTags.isXMLTypeTag(bType.tag) ||
                bType.tag == TypeTags.INVOKABLE ||
                bType.tag == TypeTags.FINITE ||
                bType.tag == TypeTags.HANDLE ||
                bType.tag == TypeTags.TYPEDESC ||
                bType.tag == TypeTags.READONLY) {
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, index);
        } else if (bType.tag == JTypeTags.JTYPE) {
            genJDefaultValue(mv, (JType) bType, index);
        } else {
            throw new BLangCompilerException("JVM generation is not supported for type " + String.format("%s", bType));
        }
    }

    private static void genJDefaultValue(MethodVisitor mv, JType jType, int index) {

        if (jType.jTag == JTypeTags.JBYTE) {
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, index);
        } else if (jType.jTag == JTypeTags.JCHAR) {
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, index);
        } else if (jType.jTag == JTypeTags.JSHORT) {
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, index);
        } else if (jType.jTag == JTypeTags.JINT) {
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, index);
        } else if (jType.jTag == JTypeTags.JLONG) {
            mv.visitInsn(LCONST_0);
            mv.visitVarInsn(LSTORE, index);
        } else if (jType.jTag == JTypeTags.JFLOAT) {
            mv.visitInsn(FCONST_0);
            mv.visitVarInsn(FSTORE, index);
        } else if (jType.jTag == JTypeTags.JDOUBLE) {
            mv.visitInsn(DCONST_0);
            mv.visitVarInsn(DSTORE, index);
        } else if (jType.jTag == JTypeTags.JBOOLEAN) {
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, index);
        } else if (jType.jTag == JTypeTags.JARRAY ||
                jType.jTag == JTypeTags.JREF) {
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, index);
        } else {
            throw new BLangCompilerException("JVM generation is not supported for type " + String.format("%s", jType));
        }
    }

    static void loadDefaultValue(MethodVisitor mv, BType bType) {

        if (TypeTags.isIntegerTypeTag(bType.tag) || bType.tag == TypeTags.BYTE) {
            mv.visitInsn(LCONST_0);
        } else if (bType.tag == TypeTags.FLOAT) {
            mv.visitInsn(DCONST_0);
        } else if (bType.tag == TypeTags.BOOLEAN) {
            mv.visitInsn(ICONST_0);
        } else if (TypeTags.isStringTypeTag(bType.tag) ||
                bType.tag == TypeTags.MAP ||
                bType.tag == TypeTags.ARRAY ||
                bType.tag == TypeTags.ERROR ||
                bType.tag == TypeTags.NIL ||
                bType.tag == TypeTags.ANY ||
                bType.tag == TypeTags.ANYDATA ||
                bType.tag == TypeTags.OBJECT ||
                bType.tag == TypeTags.UNION ||
                bType.tag == TypeTags.RECORD ||
                bType.tag == TypeTags.TUPLE ||
                bType.tag == TypeTags.FUTURE ||
                bType.tag == TypeTags.JSON ||
                TypeTags.isXMLTypeTag(bType.tag) ||
                bType.tag == TypeTags.INVOKABLE ||
                bType.tag == TypeTags.FINITE ||
                bType.tag == TypeTags.HANDLE ||
                bType.tag == TypeTags.TYPEDESC ||
                bType.tag == TypeTags.READONLY) {
            mv.visitInsn(ACONST_NULL);
        } else if (bType.tag == JTypeTags.JTYPE) {
            loadDefaultJValue(mv, (JType) bType);
        } else {
            throw new BLangCompilerException("JVM generation is not supported for type " + String.format("%s", bType));
        }
    }

    private static void loadDefaultJValue(MethodVisitor mv, JType jType) {

        if (jType.jTag == JTypeTags.JBYTE) {
            mv.visitInsn(ICONST_0);
        } else if (jType.jTag == JTypeTags.JCHAR) {
            mv.visitInsn(ICONST_0);
        } else if (jType.jTag == JTypeTags.JSHORT) {
            mv.visitInsn(ICONST_0);
        } else if (jType.jTag == JTypeTags.JINT) {
            mv.visitInsn(ICONST_0);
        } else if (jType.jTag == JTypeTags.JLONG) {
            mv.visitInsn(LCONST_0);
        } else if (jType.jTag == JTypeTags.JFLOAT) {
            mv.visitInsn(FCONST_0);
        } else if (jType.jTag == JTypeTags.JDOUBLE) {
            mv.visitInsn(DCONST_0);
        } else if (jType.jTag == JTypeTags.JBOOLEAN) {
            mv.visitInsn(ICONST_0);
        } else if (jType.jTag == JTypeTags.JARRAY ||
                jType.jTag == JTypeTags.JREF) {
            mv.visitInsn(ACONST_NULL);
        } else {
            throw new BLangCompilerException("JVM generation is not supported for type " + String.format("%s", jType));
        }
    }

    public static String getMethodDesc(@Nilable List<BType> paramTypes, @Nilable BType retType,
                                       @Nilable BType attachedType /* = () */, boolean isExtern /* = false */) {

        StringBuilder desc = new StringBuilder("(Lorg/ballerinalang/jvm/scheduling/Strand;");

        if (attachedType != null) {
            desc.append(getArgTypeSignature(attachedType));
        }

        int i = 0;
        while (i < paramTypes.size()) {
            BType paramType = getType(paramTypes.get(i));
            desc.append(getArgTypeSignature(paramType));
            i += 1;
        }
        String returnType = generateReturnType(retType, isExtern);
        desc.append(returnType);

        return desc.toString();
    }

    private static String getLambdaMethodDesc(@Nilable List<BType> paramTypes, @Nilable BType retType,
                                              int closureMapsCount) {

        StringBuilder desc = new StringBuilder("(Lorg/ballerinalang/jvm/scheduling/Strand;");
        int j = 0;
        while (j < closureMapsCount) {
            j += 1;
            desc.append("L").append(MAP_VALUE).append(";").append("Z");
        }

        int i = 0;
        while (i < paramTypes.size()) {
            BType paramType = getType(paramTypes.get(i));
            desc.append(getArgTypeSignature(paramType));
            i += 1;
        }
        String returnType = generateReturnType(retType, false);
        desc.append(returnType);

        return desc.toString();
    }

    private static String getArgTypeSignature(BType bType) {

        if (TypeTags.isIntegerTypeTag(bType.tag)) {
            return "J";
        } else if (bType.tag == TypeTags.BYTE) {
            return "I";
        } else if (bType.tag == TypeTags.FLOAT) {
            return "D";
        } else if (TypeTags.isStringTypeTag(bType.tag)) {
            return String.format("L%s;", isBString ? B_STRING_VALUE : STRING_VALUE);
        } else if (bType.tag == TypeTags.DECIMAL) {
            return String.format("L%s;", DECIMAL_VALUE);
        } else if (bType.tag == TypeTags.BOOLEAN) {
            return "Z";
        } else if (bType.tag == TypeTags.NIL) {
            return String.format("L%s;", OBJECT);
        } else if (bType.tag == TypeTags.ARRAY || bType.tag == TypeTags.TUPLE) {
            return String.format("L%s;", ARRAY_VALUE);
        } else if (bType.tag == TypeTags.ERROR) {
            return String.format("L%s;", ERROR_VALUE);
        } else if (bType.tag == TypeTags.ANYDATA ||
                bType.tag == TypeTags.UNION ||
                bType.tag == TypeTags.JSON ||
                bType.tag == TypeTags.FINITE ||
                bType.tag == TypeTags.ANY ||
                bType.tag == TypeTags.READONLY) {
            return String.format("L%s;", OBJECT);
        } else if (bType.tag == TypeTags.MAP || bType.tag == TypeTags.RECORD) {
            return String.format("L%s;", MAP_VALUE);
        } else if (bType.tag == TypeTags.FUTURE) {
            return String.format("L%s;", FUTURE_VALUE);
        } else if (bType.tag == TypeTags.STREAM) {
            return String.format("L%s;", STREAM_VALUE);
        } else if (bType.tag == TypeTags.INVOKABLE) {
            return String.format("L%s;", FUNCTION_POINTER);
        } else if (bType.tag == TypeTags.TYPEDESC) {
            return String.format("L%s;", TYPEDESC_VALUE);
        } else if (bType.tag == TypeTags.OBJECT) {
            return String.format("L%s;", OBJECT_VALUE);
        } else if (TypeTags.isXMLTypeTag(bType.tag)) {
            return String.format("L%s;", XML_VALUE);
        } else if (bType.tag == TypeTags.HANDLE) {
            return String.format("L%s;", HANDLE_VALUE);
        } else {
            throw new BLangCompilerException("JVM generation is not supported for type " + String.format("%s", bType));
        }
    }

    private static String generateReturnType(@Nilable BType bType, boolean isExtern /* = false */) {

        if (bType == null || bType.tag == TypeTags.NIL) {
            if (isExtern) {
                return ")V";
            }
            return String.format(")L%s;", OBJECT);
        } else if (TypeTags.isIntegerTypeTag(bType.tag)) {
            return ")J";
        } else if (bType.tag == TypeTags.BYTE) {
            return ")I";
        } else if (bType.tag == TypeTags.FLOAT) {
            return ")D";
        } else if (TypeTags.isStringTypeTag(bType.tag)) {
            return String.format(")L%s;", isBString ? B_STRING_VALUE : STRING_VALUE);
        } else if (bType.tag == TypeTags.DECIMAL) {
            return String.format(")L%s;", DECIMAL_VALUE);
        } else if (bType.tag == TypeTags.BOOLEAN) {
            return ")Z";
        } else if (bType.tag == TypeTags.ARRAY ||
                bType.tag == TypeTags.TUPLE) {
            return String.format(")L%s;", ARRAY_VALUE);
        } else if (bType.tag == TypeTags.MAP ||
                bType.tag == TypeTags.RECORD) {
            return String.format(")L%s;", MAP_VALUE);
        } else if (bType.tag == TypeTags.ERROR) {
            return String.format(")L%s;", ERROR_VALUE);
        } else if (bType.tag == TypeTags.STREAM) {
            return String.format(")L%s;", STREAM_VALUE);
        } else if (bType.tag == TypeTags.FUTURE) {
            return String.format(")L%s;", FUTURE_VALUE);
        } else if (bType.tag == TypeTags.TYPEDESC) {
            return String.format(")L%s;", TYPEDESC_VALUE);
        } else if (bType.tag == TypeTags.ANY ||
                bType.tag == TypeTags.ANYDATA ||
                bType.tag == TypeTags.UNION ||
                bType.tag == TypeTags.JSON ||
                bType.tag == TypeTags.FINITE ||
                bType.tag == TypeTags.READONLY) {
            return String.format(")L%s;", OBJECT);
        } else if (bType.tag == TypeTags.OBJECT) {
            return String.format(")L%s;", OBJECT_VALUE);
        } else if (bType.tag == TypeTags.INVOKABLE) {
            return String.format(")L%s;", FUNCTION_POINTER);
        } else if (TypeTags.isXMLTypeTag(bType.tag)) {
            return String.format(")L%s;", XML_VALUE);
        } else if (bType.tag == TypeTags.HANDLE) {
            return String.format(")L%s;", HANDLE_VALUE);
        } else {
            throw new BLangCompilerException("JVM generation is not supported for type " +
                    String.format("%s", bType));
        }
    }

    static @Nilable
    BIRFunction getMainFunc(@Nilable List<BIRFunction> funcs) {

        @Nilable BIRFunction userMainFunc = null;
        for (BIRFunction func : funcs) {
            if (func != null && func.name.value.equals("main")) {
                userMainFunc = func;
                break;
            }
        }

        return userMainFunc;
    }

    static void createFunctionPointer(MethodVisitor mv, String klass, String lambdaName, int closureMapCount) {

        mv.visitTypeInsn(NEW, FUNCTION_POINTER);
        mv.visitInsn(DUP);
        visitInvokeDyn(mv, klass, cleanupFunctionName(lambdaName), closureMapCount);

        // load null here for type, since these are fp's created for internal usages.
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ICONST_0); // mark as not-concurrent ie: 'parent'
        mv.visitMethodInsn(INVOKESPECIAL, FUNCTION_POINTER, "<init>",
                String.format("(L%s;L%s;Z)V", FUNCTION, BTYPE), false);
    }

    static void generateMainMethod(@Nilable BIRFunction userMainFunc, ClassWriter cw, BIRPackage pkg,
                                   String initClass, boolean serviceEPAvailable) {

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        Label tryCatchStart = new Label();
        Label tryCatchEnd = new Label();
        Label tryCatchHandle = new Label();
        mv.visitTryCatchBlock(tryCatchStart, tryCatchEnd, tryCatchHandle, THROWABLE);
        mv.visitLabel(tryCatchStart);

        // check for java compatibility
        generateJavaCompatibilityCheck(mv);

        // set system properties
        initConfigurations(mv);
        // start all listeners
        startListeners(mv, serviceEPAvailable);

        // register a shutdown hook to call package stop() method.
        registerShutdownListener(mv, initClass);

        BalToJVMIndexMap indexMap = new BalToJVMIndexMap();

        // add main string[] args param first
        BIRVariableDcl argsVar = new BIRVariableDcl(symbolTable.anyType, new Name("argsdummy"), VarScope.FUNCTION,
                VarKind.ARG);
        int ignoreArgsVarIndex = indexMap.getIndex(argsVar);

        boolean isVoidFunction = userMainFunc != null && userMainFunc.type.retType.tag == TypeTags.NIL;

        mv.visitTypeInsn(NEW, SCHEDULER);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitMethodInsn(INVOKESPECIAL, SCHEDULER, "<init>", "(Z)V", false);
        BIRVariableDcl schedulerVar = new BIRVariableDcl(symbolTable.anyType, new Name("schedulerdummy"),
                VarScope.FUNCTION, VarKind.ARG);
        int schedulerVarIndex = indexMap.getIndex(schedulerVar);
        mv.visitVarInsn(ASTORE, schedulerVarIndex);

        if (hasInitFunction(pkg)) {
            mv.visitVarInsn(ALOAD, schedulerVarIndex);
            mv.visitIntInsn(BIPUSH, 1);
            mv.visitTypeInsn(ANEWARRAY, OBJECT);

            // schedule the init method
            String lambdaName = String.format("$lambda$%s$", MODULE_INIT);

            // create FP value
            createFunctionPointer(mv, initClass, lambdaName, 0);

            // no parent strand
            mv.visitInsn(ACONST_NULL);
            BType anyType = symbolTable.anyType;
            loadType(mv, anyType);
            mv.visitMethodInsn(INVOKEVIRTUAL, SCHEDULER, SCHEDULE_FUNCTION_METHOD,
                    String.format("([L%s;L%s;L%s;L%s;)L%s;", OBJECT, FUNCTION_POINTER, STRAND, BTYPE, FUTURE_VALUE),
                    false);
            mv.visitInsn(DUP);
            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, "strand", String.format("L%s;", STRAND));
            mv.visitIntInsn(BIPUSH, 100);
            mv.visitTypeInsn(ANEWARRAY, OBJECT);
            mv.visitFieldInsn(PUTFIELD, STRAND, "frames", String.format("[L%s;", OBJECT));
            handleErrorFromFutureValue(mv);

            BIRVariableDcl futureVar = new BIRVariableDcl(symbolTable.anyType, new Name("initdummy"),
                    VarScope.FUNCTION, VarKind.ARG);
            int futureVarIndex = indexMap.getIndex(futureVar);
            mv.visitVarInsn(ASTORE, futureVarIndex);
            mv.visitVarInsn(ALOAD, futureVarIndex);
            mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, "result", String.format("L%s;", OBJECT));

            mv.visitMethodInsn(INVOKESTATIC, RUNTIME_UTILS, HANDLE_RETURNED_ERROR_METHOD,
                    String.format("(L%s;)V", OBJECT), false);
        }

        if (userMainFunc != null) {
            mv.visitVarInsn(ALOAD, schedulerVarIndex);
            loadCLIArgsForMain(mv, new ArrayList<>(userMainFunc.parameters.keySet()), userMainFunc.restParam != null,
                    userMainFunc.annotAttachments);

            // invoke the user's main method
            String lambdaName = "$lambda$main$";
            createFunctionPointer(mv, initClass, lambdaName, 0);

            // no parent strand
            mv.visitInsn(ACONST_NULL);

            //submit to the scheduler
            loadType(mv, userMainFunc.type.retType);
            mv.visitMethodInsn(INVOKEVIRTUAL, SCHEDULER, SCHEDULE_FUNCTION_METHOD,
                    String.format("([L%s;L%s;L%s;L%s;)L%s;", OBJECT, FUNCTION_POINTER, STRAND, BTYPE, FUTURE_VALUE),
                    false);
            mv.visitInsn(DUP);

            mv.visitInsn(DUP);
            mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, "strand", String.format("L%s;", STRAND));
            mv.visitIntInsn(BIPUSH, 100);
            mv.visitTypeInsn(ANEWARRAY, OBJECT);
            mv.visitFieldInsn(PUTFIELD, STRAND, "frames", String.format("[L%s;", OBJECT));
            handleErrorFromFutureValue(mv);

            // At this point we are done executing all the functions including asyncs
            if (!isVoidFunction) {
                // store future value
                BIRVariableDcl futureVar = new BIRVariableDcl(symbolTable.anyType, new Name("dummy"),
                        VarScope.FUNCTION, VarKind.ARG);
                int futureVarIndex = indexMap.getIndex(futureVar);
                mv.visitVarInsn(ASTORE, futureVarIndex);
                mv.visitVarInsn(ALOAD, futureVarIndex);
                mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, "result", String.format("L%s;", OBJECT));

                mv.visitMethodInsn(INVOKESTATIC, RUNTIME_UTILS, HANDLE_RETURNED_ERROR_METHOD,
                        String.format("(L%s;)V", OBJECT), false);
            }
        }

        if (hasInitFunction(pkg)) {
            scheduleStartMethod(mv, initClass, serviceEPAvailable, indexMap, schedulerVarIndex);
        }

        // stop all listeners
        stopListeners(mv, serviceEPAvailable);
        if (!serviceEPAvailable) {
            mv.visitMethodInsn(INVOKESTATIC, JAVA_RUNTIME, "getRuntime", String.format("()L%s;", JAVA_RUNTIME), false);
            mv.visitInsn(ICONST_0);
            mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_RUNTIME, "exit", "(I)V", false);
        }

        mv.visitLabel(tryCatchEnd);
        mv.visitInsn(RETURN);
        mv.visitLabel(tryCatchHandle);
        mv.visitMethodInsn(INVOKESTATIC, RUNTIME_UTILS, HANDLE_THROWABLE_METHOD,
                String.format("(L%s;)V", THROWABLE), false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void handleErrorFromFutureValue(MethodVisitor mv) {
        mv.visitInsn(DUP);
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, "strand", String.format("L%s;", STRAND));
        mv.visitFieldInsn(GETFIELD, STRAND, "scheduler", String.format("L%s;", SCHEDULER));
        mv.visitMethodInsn(INVOKEVIRTUAL, SCHEDULER, SCHEDULER_START_METHOD, "()V", false);
        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, PANIC_FIELD, String.format("L%s;", THROWABLE));

        // handle any runtime errors
        Label labelIf = new Label();
        mv.visitJumpInsn(IFNULL, labelIf);
        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, PANIC_FIELD, String.format("L%s;", THROWABLE));
        mv.visitMethodInsn(INVOKESTATIC, RUNTIME_UTILS, HANDLE_THROWABLE_METHOD,
                String.format("(L%s;)V", THROWABLE), false);
        mv.visitInsn(RETURN);
        mv.visitLabel(labelIf);
    }

    private static void initConfigurations(MethodVisitor mv) {

        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, LAUNCH_UTILS,
                "initConfigurations", String.format("([L%s;)[L%s;", STRING_VALUE, STRING_VALUE), false);
        mv.visitVarInsn(ASTORE, 0);
    }

    private static void startListeners(MethodVisitor mv, boolean isServiceEPAvailable) {

        mv.visitLdcInsn(isServiceEPAvailable);
        mv.visitMethodInsn(INVOKESTATIC, LAUNCH_UTILS, "startListeners", "(Z)V", false);
    }

    private static void stopListeners(MethodVisitor mv, boolean isServiceEPAvailable) {

        mv.visitLdcInsn(isServiceEPAvailable);
        mv.visitMethodInsn(INVOKESTATIC, LAUNCH_UTILS, "stopListeners", "(Z)V", false);
    }

    private static void registerShutdownListener(MethodVisitor mv, String initClass) {

        String shutdownClassName = initClass + "$SignalListener";
        mv.visitMethodInsn(INVOKESTATIC, JAVA_RUNTIME, "getRuntime", String.format("()L%s;", JAVA_RUNTIME), false);
        mv.visitTypeInsn(NEW, shutdownClassName);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, shutdownClassName, "<init>", "()V", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_RUNTIME, "addShutdownHook", String.format("(L%s;)V", JAVA_THREAD),
                false);
    }

    private static void scheduleStartMethod(MethodVisitor mv, String initClass, boolean serviceEPAvailable,
                                            BalToJVMIndexMap indexMap, int schedulerVarIndex) {

        mv.visitVarInsn(ALOAD, schedulerVarIndex);
        // schedule the start method
        String startLambdaName = String.format("$lambda$%s$", MODULE_START);

        mv.visitIntInsn(BIPUSH, 1);
        mv.visitTypeInsn(ANEWARRAY, OBJECT);

        // create FP value
        createFunctionPointer(mv, initClass, startLambdaName, 0);

        // no parent strand
        mv.visitInsn(ACONST_NULL);
        BType anyType = symbolTable.anyType;
        loadType(mv, anyType);
        mv.visitMethodInsn(INVOKEVIRTUAL, SCHEDULER, SCHEDULE_FUNCTION_METHOD,
                String.format("([L%s;L%s;L%s;L%s;)L%s;", OBJECT, FUNCTION_POINTER, STRAND, BTYPE, FUTURE_VALUE), false);

        mv.visitInsn(DUP);
        mv.visitInsn(DUP);
        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, "strand", String.format("L%s;", STRAND));
        mv.visitIntInsn(BIPUSH, 100);
        mv.visitTypeInsn(ANEWARRAY, OBJECT);
        mv.visitFieldInsn(PUTFIELD, STRAND, "frames", String.format("[L%s;", OBJECT));
        handleErrorFromFutureValue(mv);

        BIRVariableDcl futureVar = new BIRVariableDcl(symbolTable.anyType, new Name("startdummy"), VarScope.FUNCTION,
                VarKind.ARG);
        int futureVarIndex = indexMap.getIndex(futureVar);
        mv.visitVarInsn(ASTORE, futureVarIndex);
        mv.visitVarInsn(ALOAD, futureVarIndex);
        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, "result", String.format("L%s;", OBJECT));

        mv.visitMethodInsn(INVOKESTATIC, RUNTIME_UTILS, HANDLE_RETURNED_ERROR_METHOD, String.format("(L%s;)V", OBJECT),
                false);
        // need to set immortal=true and start the scheduler again
        if (serviceEPAvailable) {
            mv.visitVarInsn(ALOAD, schedulerVarIndex);
            mv.visitInsn(DUP);
            mv.visitInsn(ICONST_1);
            mv.visitFieldInsn(PUTFIELD, SCHEDULER, "immortal", "Z");

            mv.visitMethodInsn(INVOKEVIRTUAL, SCHEDULER, SCHEDULER_START_METHOD, "()V", false);
        }
    }

    /**
     * Generate a lambda function to invoke ballerina main.
     *
     * @param userMainFunc ballerina main function
     * @param cw class visitor
     * @param pkg bir package instance
     * @param mainClass main class that contains the user main
     * @param initClass module init class
     */
    static void generateLambdaForMain(BIRFunction userMainFunc, ClassWriter cw, BIRPackage pkg,
                                      String mainClass, String initClass) {

        BType returnType = userMainFunc.type.retType;

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "$lambda$main$",
                String.format("([L%s;)L%s;", OBJECT, OBJECT), null, null);
        mv.visitCode();

        //load strand as first arg
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(AALOAD);
        mv.visitTypeInsn(CHECKCAST, STRAND);

        // load and cast param values
        @Nilable List<BType> paramTypes = userMainFunc.type.paramTypes;

        int paramIndex = 1;
        for (BType paramType : paramTypes) {
            BType pType = getType(paramType);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitIntInsn(BIPUSH, paramIndex);
            mv.visitInsn(AALOAD);
            addUnboxInsn(mv, pType);
            paramIndex += 1;
        }

        mv.visitMethodInsn(INVOKESTATIC, mainClass, userMainFunc.name.value,
                           getMethodDesc(paramTypes, returnType, null, false), false);
        addBoxInsn(mv, returnType);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void loadCLIArgsForMain(MethodVisitor mv, @Nilable List<BIRFunctionParameter> params,
                                           boolean hasRestParam,
                                           @Nilable List<BIRAnnotationAttachment> annotAttachments) {

        // get defaultable arg names from function annotation
        List<String> defaultableNames = new ArrayList<>();
        int defaultableIndex = 0;
        for (BIRAnnotationAttachment attachment : annotAttachments) {
            if (attachment == null || !attachment.annotTagRef.value.equals(DEFAULTABLE_ARGS_ANOT_NAME)) {
                continue;
            }
            BIRAnnotationRecordValue annotRecValue = (BIRAnnotationRecordValue) attachment.annotValues.get(0);
            Map<String, BIRAnnotationValue> annotFieldMap = annotRecValue.annotValueEntryMap;
            BIRAnnotationArrayValue annotArrayValue =
                    (BIRAnnotationArrayValue) annotFieldMap.get(DEFAULTABLE_ARGS_ANOT_FIELD);
            for (BIRAnnotationValue entryOptional : annotArrayValue.annotArrayValue) {
                BIRAnnotationLiteralValue argValue = (BIRAnnotationLiteralValue) entryOptional;
                defaultableNames.add(defaultableIndex, (String) argValue.value);
                defaultableIndex += 1;
            }
            break;
        }
        // create function info array
        mv.visitIntInsn(BIPUSH, params.size());
        mv.visitTypeInsn(ANEWARRAY, String.format("%s$ParamInfo", RUNTIME_UTILS));
        int index = 0;
        defaultableIndex = 0;
        for (BIRFunctionParameter param : params) {
            mv.visitInsn(DUP);
            mv.visitIntInsn(BIPUSH, index);
            index += 1;
            mv.visitTypeInsn(NEW, String.format("%s$ParamInfo", RUNTIME_UTILS));
            mv.visitInsn(DUP);
            if (param != null) {
                if (param.hasDefaultExpr) {
                    mv.visitInsn(ICONST_1);
                } else {
                    mv.visitInsn(ICONST_0);
                }
                mv.visitLdcInsn(defaultableNames.get(defaultableIndex));
                defaultableIndex += 1;
                // var varIndex = indexMap.getIndex(param);
                loadType(mv, param.type);
            }
            mv.visitMethodInsn(INVOKESPECIAL, String.format("%s$ParamInfo", RUNTIME_UTILS), "<init>",
                    String.format("(ZL%s;L%s;)V", STRING_VALUE, BTYPE), false);
            mv.visitInsn(AASTORE);
        }

        // load string[] that got parsed into to java main
        mv.visitVarInsn(ALOAD, 0);
        if (hasRestParam) {
            mv.visitInsn(ICONST_1);
        } else {
            mv.visitInsn(ICONST_0);
        }

        // invoke ArgumentParser.extractEntryFuncArgs()
        mv.visitMethodInsn(INVOKESTATIC, ARGUMENT_PARSER, "extractEntryFuncArgs",
                String.format("([L%s$ParamInfo;[L%s;Z)[L%s;", RUNTIME_UTILS, STRING_VALUE, OBJECT), false);
    }

    /**
     * Generate a lambda function to invoke ballerina main.
     *
     * @param cw class visitor
     * @param pkg bir package
     * @param mainClass mains class that conatins the user main
     * @param initClass module init class
     * @param depMods dependent module list
     */
    static void generateLambdaForPackageInits(ClassWriter cw, BIRPackage pkg, String mainClass, String initClass,
                                              List<PackageID> depMods) {
        //need to generate lambda for package Init as well, if exist
        if (!hasInitFunction(pkg)) {
            return;
        }
        generateLambdaForModuleFunction(cw, MODULE_INIT, initClass, false);

        // generate another lambda for start function as well
        generateLambdaForModuleFunction(cw, MODULE_START, initClass, false);

        String stopFuncName = "<stop>";
        PackageID currentModId = packageToModuleId(pkg);
        String fullFuncName = calculateModuleSpecialFuncName(currentModId, stopFuncName);

        generateLambdaForDepModStopFunc(cw, cleanupFunctionName(fullFuncName), initClass);

        for (PackageID id : depMods) {
            fullFuncName = calculateModuleSpecialFuncName(id, stopFuncName);
            // String lookupKey = getPackageName(id.orgName, id.name) + fullFuncName;

            // String jvmClass = lookupFullQualifiedClassName(lookupKey);
            String jvmClass = getPackageName(id.orgName, id.name) + MODULE_INIT_CLASS_NAME;
            generateLambdaForDepModStopFunc(cw, cleanupFunctionName(fullFuncName), jvmClass);
        }
    }

    private static void generateLambdaForModuleFunction(ClassWriter cw, String funcName, String initClass,
                                                        boolean voidReturn /* = true */) {

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC,
                String.format("$lambda$%s$", funcName),
                String.format("([L%s;)L%s;", OBJECT, OBJECT), null, null);
        mv.visitCode();

        //load strand as first arg
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(AALOAD);
        mv.visitTypeInsn(CHECKCAST, STRAND);

        mv.visitMethodInsn(INVOKESTATIC, initClass, funcName, String.format("(L%s;)L%s;", STRAND, OBJECT), false);
        addBoxInsn(mv, errorOrNilType);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static void generateLambdaForDepModStopFunc(ClassWriter cw, String funcName, String initClass) {

        MethodVisitor mv;
        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC,
                String.format("$lambda$%s", funcName),
                String.format("([L%s;)L%s;", OBJECT, OBJECT), null, null);
        mv.visitCode();

        //load strand as first arg
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(AALOAD);
        mv.visitTypeInsn(CHECKCAST, STRAND);

        mv.visitMethodInsn(INVOKESTATIC, initClass, funcName, String.format("(L%s;)L%s;", STRAND, OBJECT), false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private static boolean hasInitFunction(BIRPackage pkg) {

        for (BIRFunction func : pkg.functions) {
            if (func != null && isModuleInitFunction(pkg, func)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isModuleInitFunction(BIRPackage module, BIRFunction func) {

        String moduleInit = getModuleInitFuncName(module);
        return func.name.value.equals(moduleInit);
    }

    // TODO: remove and use calculateModuleInitFuncName
    private static String getModuleInitFuncName(BIRPackage module) {

        return calculateModuleInitFuncName(packageToModuleId(module));
    }

    private static String calculateModuleInitFuncName(PackageID id) {

        return calculateModuleSpecialFuncName(id, "<init>");
    }

    private static String calculateModuleSpecialFuncName(PackageID id, String funcSuffix) {

        String orgName = id.orgName.value;
        String moduleName = id.name.value;
        String version = id.version.value;

        String funcName;
        if (moduleName.equals(".")) {
            funcName = ".." + funcSuffix;
        } else if (version.equals("")) {
            funcName = moduleName + "." + funcSuffix;
        } else {
            funcName = moduleName + ":" + version + "." + funcSuffix;
        }

        if (!orgName.equalsIgnoreCase("$anon")) {
            funcName = orgName + "/" + funcName;
        }

        return funcName;
    }

    static void addInitAndTypeInitInstructions(BIRPackage pkg, BIRFunction func) {

        @Nilable List<BIRBasicBlock> basicBlocks = new ArrayList<>();
        nextId = -1;
        BIRBasicBlock nextBB = new BIRBasicBlock(getNextBBId());
        basicBlocks.add(nextBB);

        PackageID modID = packageToModuleId(pkg);

        BIRBasicBlock typeOwnerCreateBB = new BIRBasicBlock(getNextBBId());
        basicBlocks.add(typeOwnerCreateBB);

        nextBB.terminator = new Call(null, InstructionKind.CALL, false, modID, new Name(CURRENT_MODULE_INIT),
                new ArrayList<>(), null, typeOwnerCreateBB);

        if (func.basicBlocks.size() == 0) {
            typeOwnerCreateBB.terminator = new Return(func.pos);
            func.basicBlocks = basicBlocks;
            return;
        }

        typeOwnerCreateBB.terminator = new GOTO(null, func.basicBlocks.get(0));

        basicBlocks.addAll(func.basicBlocks);
        func.basicBlocks = basicBlocks;
    }

    static void enrichPkgWithInitializers(Map<String, JavaClass> jvmClassMap, String typeOwnerClass,
                                          BIRPackage pkg, List<PackageID> depModArray) {

        JavaClass javaClass = jvmClassMap.get(typeOwnerClass);
        BIRFunction initFunc = generateDepModInit(depModArray, pkg, MODULE_INIT, "<init>");
        javaClass.functions.add(initFunc);
        pkg.functions.add(initFunc);

        BIRFunction startFunc = generateDepModInit(depModArray, pkg, MODULE_START, "<start>");
        javaClass.functions.add(startFunc);
        pkg.functions.add(startFunc);

    }

    private static BIRFunction generateDepModInit(List<PackageID> imprtMods, BIRPackage pkg, String funcName,
                                                  String initName) {

        nextId = -1;
        nextVarId = -1;

        BIRVariableDcl retVar = new BIRVariableDcl(null, errorOrNilType, new Name("%ret"),
                VarScope.FUNCTION, VarKind.RETURN, "");
        BIROperand retVarRef = new BIROperand(retVar);

        BIRFunction modInitFunc = new BIRFunction(null, new Name(funcName), 0,
                new BInvokableType(Collections.emptyList(), null, errorOrNilType, null), null, 0, null);
        modInitFunc.localVars.add(retVar);
        BIRBasicBlock ignoreNextBB = addAndGetNextBasicBlock(modInitFunc);

        BIRVariableDcl boolVal = addAndGetNextVar(modInitFunc, symbolTable.booleanType);
        BIROperand boolRef = new BIROperand(boolVal);

        for (PackageID id : imprtMods) {
            String initFuncName = calculateModuleSpecialFuncName(id, initName);
            BIRBasicBlock ignoreBB = addCheckedInvocation(modInitFunc, id, initFuncName, retVarRef, boolRef);
        }

        PackageID currentModId = packageToModuleId(pkg);
        String currentInitFuncName = calculateModuleSpecialFuncName(currentModId, initName);
        BIRBasicBlock lastBB = addCheckedInvocation(modInitFunc, currentModId, currentInitFuncName, retVarRef, boolRef);

        lastBB.terminator = new Return(null);

        return modInitFunc;
    }

    private static Name getNextBBId() {

        String bbIdPrefix = "genBB";
        nextId += 1;
        return new Name(bbIdPrefix + nextId);
    }

    static Name getNextVarId() {

        String varIdPrefix = "%";
        nextVarId += 1;
        return new Name(varIdPrefix + nextVarId);
    }

    private static BIRBasicBlock addCheckedInvocation(BIRFunction func, PackageID modId, String initFuncName,
                                                      BIROperand retVar, BIROperand boolRef) {

        BIRBasicBlock lastBB = func.basicBlocks.get(func.basicBlocks.size() - 1);
        BIRBasicBlock nextBB = addAndGetNextBasicBlock(func);
        // TODO remove once lang.annotation is fixed
        if (modId.orgName.value.equals(BALLERINA) && modId.name.value.equals(BUILT_IN_PACKAGE_NAME)) {
            lastBB.terminator = new Call(null, InstructionKind.CALL, false, modId,
                    new Name(initFuncName), Collections.emptyList(), null, nextBB);
            return nextBB;
        }
        lastBB.terminator = new Call(null, InstructionKind.CALL, false, modId, new Name(initFuncName),
                Collections.emptyList(), retVar, nextBB);

        TypeTest typeTest = new TypeTest(null, symbolTable.errorType, boolRef, retVar);
        nextBB.instructions.add(typeTest);

        BIRBasicBlock trueBB = addAndGetNextBasicBlock(func);
        BIRBasicBlock retBB = addAndGetNextBasicBlock(func);
        retBB.terminator = new Return(null);
        trueBB.terminator = new GOTO(null, retBB);

        BIRBasicBlock falseBB = addAndGetNextBasicBlock(func);
        nextBB.terminator = new Branch(null, boolRef, trueBB, falseBB);
        return falseBB;
    }

    private static BIRBasicBlock addAndGetNextBasicBlock(BIRFunction func) {

        BIRBasicBlock nextbb = new BIRBasicBlock(getNextBBId());
        func.basicBlocks.add(nextbb);
        return nextbb;
    }

    private static BIRVariableDcl addAndGetNextVar(BIRFunction func, BType typeVal) {

        BIRVariableDcl nextLocalVar = new BIRVariableDcl(typeVal, getNextVarId(), VarScope.FUNCTION, VarKind.LOCAL);
        func.localVars.add(nextLocalVar);
        return nextLocalVar;
    }

    private static void generateAnnotLoad(MethodVisitor mv, @Nilable List<BIRTypeDefinition> typeDefs, String pkgName) {

        String typePkgName = ".";
        if (!"".equals(pkgName)) {
            typePkgName = pkgName;
        }

        for (BIRTypeDefinition optionalTypeDef : typeDefs) {
            if (optionalTypeDef.isBuiltin) {
                continue;
            }
            BIRTypeDefinition typeDef = getTypeDef(optionalTypeDef);
            BType bType = typeDef.type;

            if (bType.tag == TypeTags.FINITE || bType instanceof BServiceType) {
                continue;
            }

            loadAnnots(mv, typePkgName, typeDef);
        }
    }

    private static void loadAnnots(MethodVisitor mv, String pkgName, BIRTypeDefinition typeDef) {

        String pkgClassName = pkgName.equals(".") || pkgName.equals("") ? MODULE_INIT_CLASS_NAME :
                lookupGlobalVarClassName(pkgName, ANNOTATION_MAP_NAME);
        mv.visitFieldInsn(GETSTATIC, pkgClassName, ANNOTATION_MAP_NAME, String.format("L%s;", MAP_VALUE));
        loadLocalType(mv, typeDef);
        String funcName = isBString ? "processAnnotations_bstring" : "processAnnotations";
        mv.visitMethodInsn(INVOKESTATIC, String.format("%s", ANNOTATION_UTILS), funcName,
                String.format("(L%s;L%s;)V", MAP_VALUE, BTYPE), false);
    }

    static void generateFrameClasses(BIRPackage pkg, Map<String, byte[]> pkgEntries) {

        for (BIRFunction func : pkg.functions) {
            generateFrameClassForFunction(pkg, func, pkgEntries, null);
        }

        for (BIRTypeDefinition typeDef : pkg.typeDefs) {
            @Nilable List<BIRFunction> attachedFuncs = typeDef.attachedFuncs;
            if (attachedFuncs != null) {
                @Nilable BType attachedType;
                if (typeDef.type.tag == TypeTags.RECORD) {
                    // Only attach function of records is the record init. That should be
                    // generated as a static function.
                    attachedType = null;
                } else {
                    attachedType = typeDef.type;
                }
                for (BIRFunction func : attachedFuncs) {
                    generateFrameClassForFunction(pkg, func, pkgEntries, attachedType);
                }
            }
        }
    }

    private static void generateFrameClassForFunction(BIRPackage pkg, @Nilable BIRFunction func,
                                                      Map<String, byte[]> pkgEntries,
                                                      @Nilable BType attachedType /* = () */) {

        String pkgName = getPackageName(pkg.org.value, pkg.name.value);
        BIRFunction currentFunc = getFunction(func);
        String frameClassName = getFrameClassName(pkgName, currentFunc.name.value, attachedType);
        ClassWriter cw = new BallerinaClassWriter(COMPUTE_FRAMES);
        if (currentFunc.pos != null && currentFunc.pos.src != null) {
            cw.visitSource(currentFunc.pos.src.cUnitName, null);
        }
        currentClass = frameClassName;
        cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, frameClassName, null, OBJECT, null);
        generateDefaultConstructor(cw, OBJECT);

        int k = 0;
        @Nilable List<BIRVariableDcl> localVars = currentFunc.localVars;
        while (k < localVars.size()) {
            BIRVariableDcl localVar = getVariableDcl(localVars.get(k));
            BType bType = localVar.type;
            String fieldName = localVar.name.value.replace("%", "_");
            generateField(cw, bType, fieldName, false);
            k = k + 1;
        }

        FieldVisitor fv = cw.visitField(ACC_PUBLIC, "state", "I", null, null);
        fv.visitEnd();

        cw.visitEnd();

        // panic if there are errors in the frame class. These cannot be logged, since
        // frame classes are internal implementation details.
        pkgEntries.put(frameClassName + ".class", cw.toByteArray());
    }

    private static String getFrameClassName(String pkgName, String funcName, @Nilable BType attachedType) {

        String frameClassName = pkgName;
        if (attachedType != null) {
            if (attachedType.tag == TypeTags.OBJECT) {
                frameClassName += cleanupTypeName(toNameString(attachedType)) + "_";
            } else if (attachedType instanceof BServiceType) {
                frameClassName += cleanupTypeName(toNameString(attachedType)) + "_";
            } else if (attachedType.tag == TypeTags.RECORD) {
                frameClassName += cleanupTypeName(toNameString(attachedType)) + "_";
            }
        }

        return frameClassName + cleanupFunctionName(funcName) + "Frame";
    }

    /**
     * Cleanup type name by replacing '$' with '_'.
     *
     * @param name name to be replaced and cleaned
     * @return cleaned name
     */
    static String cleanupTypeName(String name) {

        return name.replace("$", "_");
    }

    static String cleanupBalExt(String name) {

        return name.replace(BAL_EXTENSION, "");
    }

    static String cleanupPathSeperators(String name) {
        //TODO: should use file_path:getPathSeparator();
        return name.replace(WINDOWS_PATH_SEPERATOR, JAVA_PACKAGE_SEPERATOR);
    }

    static void generateField(ClassWriter cw, BType bType, String fieldName, boolean isPackage) {

        String typeSig;
        if (TypeTags.isIntegerTypeTag(bType.tag)) {
            typeSig = "J";
        } else if (bType.tag == TypeTags.BYTE) {
            typeSig = "I";
        } else if (bType.tag == TypeTags.FLOAT) {
            typeSig = "D";
        } else if (TypeTags.isStringTypeTag(bType.tag)) {
            typeSig = String.format("L%s;", isBString ? B_STRING_VALUE : STRING_VALUE);
        } else if (bType.tag == TypeTags.DECIMAL) {
            typeSig = String.format("L%s;", DECIMAL_VALUE);
        } else if (bType.tag == TypeTags.BOOLEAN) {
            typeSig = "Z";
        } else if (bType.tag == TypeTags.NIL) {
            typeSig = String.format("L%s;", OBJECT);
        } else if (bType.tag == TypeTags.MAP) {
            typeSig = String.format("L%s;", MAP_VALUE);
        } else if (bType.tag == TypeTags.STREAM) {
            typeSig = String.format("L%s;", STREAM_VALUE);
        } else if (bType.tag == TypeTags.RECORD) {
            typeSig = String.format("L%s;", MAP_VALUE);
        } else if (bType.tag == TypeTags.ARRAY ||
                bType.tag == TypeTags.TUPLE) {
            typeSig = String.format("L%s;", ARRAY_VALUE);
        } else if (bType.tag == TypeTags.ERROR) {
            typeSig = String.format("L%s;", ERROR_VALUE);
        } else if (bType.tag == TypeTags.FUTURE) {
            typeSig = String.format("L%s;", FUTURE_VALUE);
        } else if (bType.tag == TypeTags.OBJECT) {
            typeSig = String.format("L%s;", OBJECT_VALUE);
        } else if (TypeTags.isXMLTypeTag(bType.tag)) {
            typeSig = String.format("L%s;", XML_VALUE);
        } else if (bType.tag == TypeTags.TYPEDESC) {
            typeSig = String.format("L%s;", TYPEDESC_VALUE);
        } else if (bType.tag == TypeTags.ANY ||
                bType.tag == TypeTags.ANYDATA ||
                bType.tag == TypeTags.UNION ||
                bType.tag == TypeTags.JSON ||
                bType.tag == TypeTags.FINITE ||
                bType.tag == TypeTags.READONLY) {
            typeSig = String.format("L%s;", OBJECT);
        } else if (bType.tag == TypeTags.INVOKABLE) {
            typeSig = String.format("L%s;", FUNCTION_POINTER);
        } else if (bType.tag == TypeTags.HANDLE) {
            typeSig = String.format("L%s;", HANDLE_VALUE);
        } else if (bType.tag == JTypeTags.JTYPE) {
            typeSig = getJTypeSignature((JType) bType);
        } else {
            throw new BLangCompilerException("JVM generation is not supported for type " + String.format("%s", bType));
        }

        FieldVisitor fv;
        if (isPackage) {
            fv = cw.visitField(ACC_PUBLIC + ACC_STATIC, fieldName, typeSig, null, null);
        } else {
            fv = cw.visitField(ACC_PUBLIC, fieldName, typeSig, null, null);
        }
        fv.visitEnd();
    }

    static void generateDefaultConstructor(ClassWriter cw, String ownerClass) {

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, ownerClass, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private static void generateDiagnosticPos(DiagnosticPos pos, MethodVisitor mv) {

        if (pos != null && pos.sLine != 0x80000000) {
            Label label = new Label();
            mv.visitLabel(label);
            mv.visitLineNumber(pos.sLine, label);
        }
    }

    static String cleanupFunctionName(String functionName) {

        return functionName.replaceAll("[\\.:/<>]", "_");
    }

    public static BIRVariableDcl getVariableDcl(@Nilable BIRVariableDcl localVar) {

        if (localVar == null) {
            throw new BLangCompilerException("Invalid variable declarion");
        }

        return localVar;
    }

    static BIRFunctionParameter getFunctionParam(@Nilable BIRFunctionParameter localVar) {

        if (localVar == null) {
            throw new BLangCompilerException("Invalid function parameter");
        }

        return localVar;
    }

    static BIRBasicBlock getBasicBlock(@Nilable BIRBasicBlock bb) {

        if (bb == null) {
            throw new BLangCompilerException("Invalid basic block");
        }

        return bb;
    }

    static BIRFunction getFunction(@Nilable BIRFunction bfunction) {

        if (bfunction == null) {
            throw new BLangCompilerException("Invalid function");
        }

        return bfunction;
    }

    static BIRTypeDefinition getTypeDef(@Nilable BIRTypeDefinition typeDef) {

        if (typeDef == null) {
            throw new BLangCompilerException("Invalid type definition");
        }

        return typeDef;
    }

    static BField getObjectField(@Nilable BField objectField) {

        if (objectField == null) {
            throw new BLangCompilerException("Invalid object field");
        }

        return objectField;
    }

    static BField getRecordField(@Nilable BField recordField) {

        if (recordField != null) {
            return recordField;
        } else {
            throw new BLangCompilerException("Invalid record field");
        }
    }

    static boolean isExternFunc(BIRFunction func) {

        return (func.flags & Flags.NATIVE) == Flags.NATIVE;
    }

    private static BIROperand getVarRef(@Nilable BIROperand varRef) {

        if (varRef == null) {
            throw new BLangCompilerException("Invalid variable reference");
        } else {
            return varRef;
        }
    }

    static BType getType(@Nilable BType bType) {

        if (bType == null) {
            throw new BLangCompilerException("Invalid type");
        } else {
            return bType;
        }
    }

    private static String getMapValueDesc(int count) {

        int i = count;
        StringBuilder desc = new StringBuilder();
        while (i > 0) {
            desc.append("L").append(MAP_VALUE).append(";");
            i -= 1;
        }

        return desc.toString();
    }

    static List<BIRFunction> getFunctions(List<BIRFunction> functions) {

        if (functions == null) {
            throw new BLangCompilerException(String.format("Invalid functions: %s", functions));
        } else {
            return functions;
        }
    }

    private static void checkStrandCancelled(MethodVisitor mv, int localVarOffset) {

        mv.visitVarInsn(ALOAD, localVarOffset);
        mv.visitFieldInsn(GETFIELD, STRAND, "cancel", "Z");
        Label notCancelledLabel = new Label();
        mv.visitJumpInsn(IFEQ, notCancelledLabel);
        mv.visitMethodInsn(INVOKESTATIC, BAL_ERRORS, "createCancelledFutureError",
                String.format("()L%s;", ERROR_VALUE), false);
        mv.visitInsn(ATHROW);

        mv.visitLabel(notCancelledLabel);
    }

    static void generateModuleInitializer(ClassWriter cw, BIRPackage module) {

        String orgName = module.org.value;
        String moduleName = module.name.value;
        String version = module.version.value;
        String pkgName = getPackageName(orgName, moduleName);

        // Using object return type since this is similar to a ballerina function without a return.
        // A ballerina function with no returns is equivalent to a function with nil-return.
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, CURRENT_MODULE_INIT,
                String.format("(L%s;)L%s;", STRAND, OBJECT), null, null);
        mv.visitCode();

        mv.visitMethodInsn(INVOKESTATIC, typeOwnerClass, "$createTypes", "()V", false);
        mv.visitTypeInsn(NEW, typeOwnerClass);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, typeOwnerClass, "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitLdcInsn(orgName);
        mv.visitLdcInsn(moduleName);
        mv.visitLdcInsn(version);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKESTATIC, String.format("%s", VALUE_CREATOR), "addValueCreator",
                String.format("(L%s;L%s;L%s;L%s;)V", STRING_VALUE, STRING_VALUE, STRING_VALUE, VALUE_CREATOR),
                false);

        // Add a nil-return
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        //Adding this java method to the function map because this is getting called from a bir instruction.
        BIRFunction func = new BIRFunction(null, new Name(CURRENT_MODULE_INIT),
                0, new BInvokableType(Collections.emptyList(), null, new BNilType(), null)
                , new Name(""), 0, null);
        birFunctionMap.put(pkgName + CURRENT_MODULE_INIT, getFunctionWrapper(func, orgName, moduleName,
                version, typeOwnerClass));
    }

    static void generateExecutionStopMethod(ClassWriter cw, String initClass, BIRPackage
            module, List<PackageID> imprtMods) {

        String orgName = module.org.value;
        String moduleName = module.name.value;
        String version = module.version.value;
        String pkgName = getPackageName(orgName, moduleName);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, MODULE_STOP, "()V", null, null);
        mv.visitCode();

        BalToJVMIndexMap indexMap = new BalToJVMIndexMap();
        ErrorHandlerGenerator errorGen = new ErrorHandlerGenerator(mv, indexMap, pkgName);

        BIRVariableDcl argsVar = new BIRVariableDcl(symbolTable.anyType, new Name("schedulerVar"),
                VarScope.FUNCTION, VarKind.ARG);
        int schedulerIndex = indexMap.getIndex(argsVar);
        BIRVariableDcl futureVar = new BIRVariableDcl(symbolTable.anyType, new Name("futureVar"),
                VarScope.FUNCTION, VarKind.ARG);
        int futureIndex = indexMap.getIndex(futureVar);

        mv.visitTypeInsn(NEW, SCHEDULER);
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_1);
        mv.visitInsn(ICONST_0);
        mv.visitMethodInsn(INVOKESPECIAL, SCHEDULER, "<init>", "(IZ)V", false);

        mv.visitVarInsn(ASTORE, schedulerIndex);

        String stopFuncName = "<stop>";

        PackageID currentModId = packageToModuleId(module);
        String fullFuncName = calculateModuleSpecialFuncName(currentModId, stopFuncName);

        scheduleStopMethod(mv, initClass, cleanupFunctionName(fullFuncName), errorGen, indexMap, schedulerIndex,
                futureIndex);

        int i = imprtMods.size() - 1;
        while (i >= 0) {
            PackageID id = imprtMods.get(i);
            i -= 1;
            fullFuncName = calculateModuleSpecialFuncName(id, stopFuncName);

            scheduleStopMethod(mv, initClass, cleanupFunctionName(fullFuncName), errorGen, indexMap, schedulerIndex,
                    futureIndex);
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        //Adding this java method to the function map because this is getting called from a bir instruction.
        BIRFunction func = new BIRFunction(null, new Name(MODULE_STOP), 0, new BInvokableType(Collections.emptyList(),
                null, new BNilType(), null), new Name(""), 0, null);
        birFunctionMap.put(pkgName + MODULE_STOP, getFunctionWrapper(func, orgName, moduleName,
                version, typeOwnerClass));
    }

    private static void scheduleStopMethod(MethodVisitor mv, String initClass, String stopFuncName,
                                           ErrorHandlerGenerator errorGen, BalToJVMIndexMap indexMap,
                                           int schedulerIndex, int futureIndex) {

        String lambdaFuncName = "$lambda$" + stopFuncName;
        // Create a schedular. A new schedular is used here, to make the stop function to not to
        // depend/wait on whatever is being running on the background. eg: a busy loop in the main.

        mv.visitVarInsn(ALOAD, schedulerIndex);

        mv.visitIntInsn(BIPUSH, 1);
        mv.visitTypeInsn(ANEWARRAY, OBJECT);

        // create FP value
        createFunctionPointer(mv, initClass, lambdaFuncName, 0);

        // no parent strand
        mv.visitInsn(ACONST_NULL);

        loadType(mv, new BNilType());
        mv.visitMethodInsn(INVOKEVIRTUAL, SCHEDULER, SCHEDULE_FUNCTION_METHOD,
                String.format("([L%s;L%s;L%s;L%s;)L%s;", OBJECT, FUNCTION_POINTER, STRAND, BTYPE, FUTURE_VALUE), false);

        mv.visitVarInsn(ASTORE, futureIndex);

        mv.visitVarInsn(ALOAD, futureIndex);

        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, "strand", String.format("L%s;", STRAND));
        mv.visitIntInsn(BIPUSH, 100);
        mv.visitTypeInsn(ANEWARRAY, OBJECT);
        mv.visitFieldInsn(PUTFIELD, STRAND, "frames", String.format("[L%s;", OBJECT));

        mv.visitVarInsn(ALOAD, futureIndex);
        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, "strand", String.format("L%s;", STRAND));
        mv.visitFieldInsn(GETFIELD, STRAND, "scheduler", String.format("L%s;", SCHEDULER));
        mv.visitMethodInsn(INVOKEVIRTUAL, SCHEDULER, SCHEDULER_START_METHOD, "()V", false);

        mv.visitVarInsn(ALOAD, futureIndex);
        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, PANIC_FIELD, String.format("L%s;", THROWABLE));

        // handle any runtime errors
        Label labelIf = new Label();
        mv.visitJumpInsn(IFNULL, labelIf);

        mv.visitVarInsn(ALOAD, futureIndex);
        mv.visitFieldInsn(GETFIELD, FUTURE_VALUE, PANIC_FIELD, String.format("L%s;", THROWABLE));
        mv.visitMethodInsn(INVOKESTATIC, RUNTIME_UTILS, HANDLE_STOP_PANIC_METHOD, String.format("(L%s;)V", THROWABLE),
                false);
        mv.visitLabel(labelIf);
    }

    private static void generateJavaCompatibilityCheck(MethodVisitor mv) {

        mv.visitLdcInsn(getJavaVersion());
        mv.visitMethodInsn(INVOKESTATIC, COMPATIBILITY_CHECKER, "verifyJavaCompatibility",
                String.format("(L%s;)V", STRING_VALUE), false);
    }

    private static String getJavaVersion() {

        String versionProperty = "java.version";
        @Nilable String javaVersion = System.getProperty(versionProperty);
        if (javaVersion != null) {
            return javaVersion;
        } else {
            return "";
        }
    }

    /**
     * A map that keeps JVM stack the index values against the variables.
     *
     * @since 1.2.0
     */
    public static class BalToJVMIndexMap {

        private int localVarIndex = 0;
        private Map<String, Integer> jvmLocalVarIndexMap = new HashMap<>();

        void add(BIRVariableDcl varDcl) {

            String varRefName = this.getVarRefName(varDcl);
            this.jvmLocalVarIndexMap.put(varRefName, this.localVarIndex);

            BType bType = varDcl.type;

            if (TypeTags.isIntegerTypeTag(bType.tag) || bType.tag == TypeTags.FLOAT) {
                this.localVarIndex = this.localVarIndex + 2;
            } else if (bType.tag == JTypeTags.JTYPE) {
                JType jType = (JType) bType;
                if (jType.jTag == JTypeTags.JLONG || jType.jTag == JTypeTags.JDOUBLE) {
                    this.localVarIndex = this.localVarIndex + 2;
                } else {
                    this.localVarIndex = this.localVarIndex + 1;
                }
            } else {
                this.localVarIndex = this.localVarIndex + 1;
            }
        }

        private String getVarRefName(BIRVariableDcl varDcl) {

            return varDcl.name.value;
        }

        public int getIndex(BIRVariableDcl varDcl) {

            String varRefName = this.getVarRefName(varDcl);
            if (!(this.jvmLocalVarIndexMap.containsKey(varRefName))) {
                this.add(varDcl);
            }

            Integer index = this.jvmLocalVarIndexMap.get(varRefName);
            return index != null ? index : -1;
        }
    }
}
