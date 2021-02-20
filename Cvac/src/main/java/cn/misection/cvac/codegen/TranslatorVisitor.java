package cn.misection.cvac.codegen;

import cn.misection.cvac.ast.IVisitor;
import cn.misection.cvac.ast.clas.CvaClass;
import cn.misection.cvac.ast.decl.CvaDeclaration;
import cn.misection.cvac.ast.entry.CvaEntryClass;
import cn.misection.cvac.ast.expr.*;
import cn.misection.cvac.ast.expr.binary.*;
import cn.misection.cvac.ast.expr.unary.*;
import cn.misection.cvac.ast.method.CvaMainMethod;
import cn.misection.cvac.ast.method.CvaMethod;
import cn.misection.cvac.ast.program.CvaProgram;
import cn.misection.cvac.ast.statement.*;
import cn.misection.cvac.ast.type.ICvaType;
import cn.misection.cvac.ast.type.basic.EnumCvaType;
import cn.misection.cvac.ast.type.reference.AbstractReferenceType;
import cn.misection.cvac.ast.type.reference.CvaClassType;
import cn.misection.cvac.ast.type.advance.CvaStringType;
import cn.misection.cvac.codegen.bst.Label;
import cn.misection.cvac.codegen.bst.bclas.TargetClass;
import cn.misection.cvac.codegen.bst.bdecl.TargetDeclaration;
import cn.misection.cvac.codegen.bst.bentry.TargetEntryClass;
import cn.misection.cvac.codegen.bst.bmethod.TargetMethod;
import cn.misection.cvac.codegen.bst.bprogram.TargetProgram;
import cn.misection.cvac.codegen.bst.btype.BaseType;
import cn.misection.cvac.codegen.bst.btype.basic.TargetIntType;
import cn.misection.cvac.codegen.bst.btype.reference.TargetClassType;
import cn.misection.cvac.codegen.bst.btype.reference.TargetStringType;
import cn.misection.cvac.codegen.bst.instruction.*;
import cn.misection.cvac.constant.CvaExprClassName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author MI6 root;
 * @Description 将编译器前端翻译给后端;
 */
public final class TranslatorVisitor implements IVisitor
{
    private String className;
    private int index;

    private Map<String, Integer> indexTable;

    private BaseType genType;
    private TargetDeclaration genDecl;
    private List<BaseInstruction> linearInstrList;
    private TargetMethod targetMethod;
    private TargetClass targetClass;
    private TargetEntryClass targetEntryClass;
    private TargetProgram targetProgram;

    public TranslatorVisitor()
    {
        this.className = null;
        this.indexTable = null;
        this.genType = null;
        this.genDecl = null;
        this.linearInstrList = new ArrayList<>();
        this.targetMethod = null;
        this.targetEntryClass = null;
        this.targetClass = null;
        this.targetProgram = null;
    }

    private void emit(BaseInstruction instruction)
    {
        linearInstrList.add(instruction);
    }


    @Override
    public void visit(CvaClassType type)
    {
        genType = new TargetClassType(type.getName());
    }

    @Override
    public void visit(EnumCvaType type)
    {
        switch (type)
        {
            case CVA_INT:
            case CVA_BOOLEAN:
            {
                // FIXME 后端取消;
                genType = new TargetIntType();
                break;
            }
            default:
            {
                // FIXME 处理错误;
                break;
            }
        }
    }

    @Override
    public void visit(CvaStringType type)
    {
        genType = new TargetStringType();
    }

    @Override
    public void visit(CvaDeclaration decl)
    {
        visit(decl.type());
        genDecl = new TargetDeclaration(
                decl.literal(),
                this.genType);
        if (indexTable != null) // if it is field
        {
            indexTable.put(decl.literal(), index++);
        }
    }

    @Override
    public void visit(CvaAddExpr expr)
    {
        visit(expr.getLeft());
        visit(expr.getRight());
        emit(new IAdd());
    }

    @Override
    public void visit(CvaAndAndExpr expr)
    {
        Label f = new Label();
        Label r = new Label();
        visit(expr.getLeft());
        emit(new Ldc<Integer>(1));
        emit(new Ificmplt(f));
        visit(expr.getRight());
        emit(new Ldc<Integer>(1));
        emit(new Ificmplt(f));
        emit(new Ldc<Integer>(1));
        emit(new Goto(r));
        emit(new LabelJ(f));
        emit(new Ldc<Integer>(0));
        emit(new LabelJ(r));
    }

    @Override
    public void visit(CvaCallExpr expr)
    {
        visit(expr.getExpr());
        expr.getArgs().forEach(this::visit);
        visit(expr.getRetType());
        BaseType rt = this.genType;
        List<BaseType> at = new ArrayList<>();
        expr.getArgTypeList().forEach(a ->
        {
            visit(a);
            at.add(this.genType);
        });
        emit(new InvokeVirtual(expr.getLiteral(), expr.getType(), at, rt));
    }

    @Override
    public void visit(CvaConstFalseExpr expr)
    {
        emit(new Ldc<Integer>(0));
    }

    @Override
    public void visit(CvaIdentifierExpr expr)
    {
        if (expr.isField())
        {
            emit(new ALoad(0));
            ICvaType type = expr.getType();
            if (type instanceof CvaClassType)
            {
                emit(new GetField(String.format("%s/%s", this.className, expr.getLiteral()),
                        String.format("L%s;", ((CvaClassType) type).getName())));
            }
            else
            {
                emit(new GetField(String.format("%s/%s", this.className, expr.getLiteral()),
                        "I"));
            }
        }
        else
        {
            int index = this.indexTable.get(expr.getLiteral());
            switch (expr.getType().toEnum())
            {
                // 后面其他类型也一样;
                case CVA_INT:
                {
                    emit(new ILoad(index));
                    break;
                }
                default:
                {
                    emit(new ALoad(index));
                    break;
                }
            }
        }
    }

    @Override
    public void visit(CvaLessThanExpr expr)
    {
        Label t = new Label();
        Label r = new Label();
        visit(expr.getLeft());
        visit(expr.getRight());
        emit(new Ificmplt(t));
        emit(new Ldc<Integer>(0));
        emit(new Goto(r));
        emit(new LabelJ(t));
        emit(new Ldc<Integer>(1));
        emit(new LabelJ(r));
    }

    @Override
    public void visit(CvaNewExpr expr)
    {
        emit(new New(expr.getNewClassName()));
    }

    @Override
    public void visit(CvaNegateExpr expr)
    {
        Label f = new Label();
        Label r = new Label();
        visit(expr.getExpr());
        emit(new Ldc<Integer>(1));
        emit(new Ificmplt(f));
        emit(new Ldc<Integer>(1));
        emit(new Goto(r));
        emit(new LabelJ(f));
        emit(new Ldc<Integer>(0));
        emit(new LabelJ(r));
    }

    @Override
    public void visit(CvaConstIntExpr expr)
    {
        emit(new Ldc<Integer>(expr.getValue()));
    }

    @Override
    public void visit(CvaConstStringExpr expr)
    {
        // FIXME;
        emit(new Ldc<String>(String.format("\"%s\"", expr.getLiteral())));
    }

    @Override
    public void visit(CvaSubExpr expr)
    {
        visit(expr.getLeft());
        visit(expr.getRight());
        emit(new ISub());
    }

    @Override
    public void visit(CvaThisExpr expr)
    {
        emit(new ALoad(0));
    }

    @Override
    public void visit(CvaMulExpr expr)
    {
        visit(expr.getLeft());
        visit(expr.getRight());
        emit(new IMul());
    }

    @Override
    public void visit(CvaConstTrueExpr expr)
    {
        emit(new Ldc<Integer>(1));
    }

    /**
     * @param stm
     * @FIXME 类型添加String是1, 二是要用前面写的switch方法替换;
     * 添加string应该只需要ref type就行;
     */
    @Override
    public void visit(CvaAssignStatement stm)
    {
        try
        {
            int index = this.indexTable.get(stm.getLiteral());
            visit(stm.getExpr());
//            if (assignSta.getType() instanceof CvaClassType)
            if (stm.getType() instanceof AbstractReferenceType)
            {
                emit(new AStore(index));
            }
            else
            {
                emit(new IStore(index));
            }
        }
        catch (NullPointerException e)
        {
            emit(new ALoad(0));
            visit(stm.getExpr());
            emit(new PutField(String.format("%s/%s", this.className, stm.getLiteral()),
                    stm.getType() instanceof CvaClassType ?
                            (String.format("L%s;", ((CvaClassType) stm.getType()).getName()))
                            : "I"));
        }
    }

    @Override
    public void visit(CvaBlockStatement stm)
    {
        stm.getStatementList().forEach(this::visit);
    }

    @Override
    public void visit(CvaIfStatement stm)
    {
        Label l = new Label();
        Label r = new Label();
        visit(stm.getCondition());
        emit(new Ldc<Integer>(1));
        emit(new Ificmplt(l));
        visit(stm.getThenStatement());
        emit(new Goto(r));
        emit(new LabelJ(l));
        if (stm.getElseStatement() != null)
        {
            visit(stm.getElseStatement());
        }
        emit(new LabelJ(r));
    }

    /**
     * @TODO 要针对所有的expr操作判断写类型, 还是麻烦, 想个办法, 最好让抽象expr能返回类型;
     * @deprecated 目前大而化之只是权宜之计;
     * @param stm
     */
    @Override
    public void visit(CvaWriteStatement stm)
    {
        byte mode = stm.getWriteMode();
        AbstractExpression expr = stm.getExpr();
        visit(expr);
        switch (expr.getClass().getSimpleName())
        {
            case CvaExprClassName.CVA_CONST_INT_EXPR:
            {
                emit(new WriteInstruction(mode, EnumCvaType.CVA_INT));
                break;
            }
            case CvaExprClassName.CVA_CONST_STRING_EXPR:
            {
                emit(new WriteInstruction(mode, EnumCvaType.CVA_STRING));
                break;
            }
            case CvaExprClassName.CVA_IDENTIFIER_EXPR:
            {
                EnumCvaType type = ((CvaIdentifierExpr) expr).getType().toEnum();
                emit(new WriteInstruction(mode, type));
                break;
            }
            case CvaExprClassName.CVA_CALL_EXPR:
            {
                EnumCvaType type = ((CvaCallExpr) expr).getRetType().toEnum();
                emit(new WriteInstruction(mode, type));
                break;
            }
            default:
            {
                // FIXME, 断点打在这里debuge可以保证就是遗漏的情况;
                // 因为检查过, 所以其实可以放心弄, 但是这里有一个情况没打印;
                // 目前可能遇到的情况有 idexpr, callfuncexpr, numberintexpr;
                // 注释掉这个可以应对可能的异常;
//                emit(new WriteInt());
                emit(new WriteInstruction(mode, EnumCvaType.CVA_INT));
                break;
            }
        }
    }

//    private byte parseEmitTypeCode(String typeString)
//    {
//        switch (typeString)
//        {
//            case CvaIntType.TYPE_LITERAL:
//            {
//                return CvaTypeCode.CVA_INT_TYPE;
//            }
//            case CvaStringType.TYPE_LITERAL:
//            {
//                return CvaTypeCode.CVA_STRING_TYPE;
//            }
//            default:
//            {
//                // todo;
//                break;
//            }
//        }
//        return -1;
//    }

    @Override
    public void visit(CvaWhileStatement stm)
    {
        Label con = new Label();
        Label end = new Label();
        emit(new LabelJ(con));
        visit(stm.getCondition());
        emit(new Ldc<Integer>(1));
        emit(new Ificmplt(end));
        visit(stm.getBody());
        emit(new Goto(con));
        emit(new LabelJ(end));
    }

    @Override
    public void visit(CvaMethod cvaMethod)
    {
        this.index = 1;
        this.indexTable = new HashMap<>();
        visit(cvaMethod.getRetType());
        BaseType theRetType = this.genType;

        List<TargetDeclaration> formalList = new ArrayList<>();
        cvaMethod.getArgumentList().forEach(f ->
        {
            visit(f);
            formalList.add(this.genDecl);
        });
        List<TargetDeclaration> localList = new ArrayList<>();
        cvaMethod.getLocalVarList().forEach(l ->
        {
            visit(l);
            localList.add(this.genDecl);
        });
        setLinearInstrList(new ArrayList<>());
        // 方法内的;
        cvaMethod.getStatementList().forEach(this::visit);

        visit(cvaMethod.getRetExpr());
        if (cvaMethod.getRetType() instanceof AbstractReferenceType)
        {
            emit(new AReturn());
        }
        else
        {
            emit(new IReturn());
        }
        targetMethod = new TargetMethod(
                cvaMethod.name(),
                theRetType,
                this.className,
                formalList,
                localList,
                this.linearInstrList,
                0,
                this.index);
    }

    @Override
    public void visit(CvaMainMethod mainMethod)
    {
        this.index = 1;
        this.indexTable = new HashMap<>();
        visit(mainMethod.getRetType());
        BaseType theRetType = this.genType;

        List<TargetDeclaration> formalList = new ArrayList<>();
        mainMethod.getArgumentList().forEach(f ->
        {
            visit(f);
            formalList.add(this.genDecl);
        });
        List<TargetDeclaration> localList = new ArrayList<>();
        mainMethod.getLocalVarList().forEach(l ->
        {
            visit(l);
            localList.add(this.genDecl);
        });
        this.linearInstrList = (new ArrayList<>());
        // 方法内的;
        mainMethod.getStatementList().forEach(this::visit);

        targetMethod = new TargetMethod(
                mainMethod.name(),
                theRetType,
                this.className,
                formalList,
                localList,
                this.linearInstrList,
                0,
                this.index);
    }

    @Override
    public void visit(CvaClass cvaClass)
    {
        setClassName(cvaClass.name());
        List<TargetDeclaration> fieldList = new ArrayList<>();
        cvaClass.getFieldList().forEach(f ->
        {
            visit(f);
            fieldList.add(this.genDecl);
        });
        List<TargetMethod> methodList = new ArrayList<>();
        cvaClass.getMethodList().forEach(m ->
        {
            visit(m);
            methodList.add(this.targetMethod);
        });
        targetClass = new TargetClass(
                cvaClass.name(),
                cvaClass.parent(),
                fieldList,
                methodList
        );
    }

    @Override
    public void visit(CvaEntryClass entryClass)
    {
        visit((CvaMainMethod) entryClass.getMainMethod());
//        genEntry = new GenEntry(entryClass.name(),
//                this.linearInstrList);
//        setLinearInstrList(new ArrayList<>());
//        visit(entryClass.statement());

//        entryClass..forEach(this::visit);
        targetEntryClass = new TargetEntryClass(entryClass.name(),
                this.linearInstrList);
        // 会重复使用, 赋给每个域;
        this.linearInstrList = new ArrayList<>();
    }

    @Override
    public void visit(CvaProgram program)
    {
        visit(program.getEntryClass());
        List<TargetClass> classList = new ArrayList<>();
        program.getClassList().forEach(c ->
        {
            visit(c);
            classList.add(this.targetClass);
        });
        targetProgram = new TargetProgram(
                this.targetEntryClass,
                classList);
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public BaseType getGenType()
    {
        return genType;
    }

    public void setGenType(BaseType genType)
    {
        this.genType = genType;
    }

    public TargetDeclaration getGenDecl()
    {
        return genDecl;
    }

    public void setGenDecl(TargetDeclaration genDecl)
    {
        this.genDecl = genDecl;
    }

    public List<BaseInstruction> getLinearInstrList()
    {
        return linearInstrList;
    }

    public void setLinearInstrList(List<BaseInstruction> linearInstrList)
    {
        this.linearInstrList = linearInstrList;
    }

    public TargetMethod getGenMethod()
    {
        return targetMethod;
    }

    public void setGenMethod(TargetMethod targetMethod)
    {
        this.targetMethod = targetMethod;
    }

    public TargetClass getGenClass()
    {
        return targetClass;
    }

    public void setGenClass(TargetClass targetClass)
    {
        this.targetClass = targetClass;
    }

    public TargetEntryClass getGenEntry()
    {
        return targetEntryClass;
    }

    public void setGenEntry(TargetEntryClass targetEntryClass)
    {
        this.targetEntryClass = targetEntryClass;
    }

    public TargetProgram getGenProgram()
    {
        return targetProgram;
    }

    public void setGenProgram(TargetProgram targetProgram)
    {
        this.targetProgram = targetProgram;
    }
}
