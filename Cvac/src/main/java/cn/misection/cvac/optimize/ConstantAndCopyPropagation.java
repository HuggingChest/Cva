package cn.misection.cvac.optimize;

import cn.misection.cvac.ast.IVisitor;
import cn.misection.cvac.ast.clas.*;
import cn.misection.cvac.ast.decl.*;
import cn.misection.cvac.ast.entry.*;
import cn.misection.cvac.ast.expr.*;
import cn.misection.cvac.ast.expr.binary.*;
import cn.misection.cvac.ast.expr.unary.*;
import cn.misection.cvac.ast.method.*;
import cn.misection.cvac.ast.program.*;
import cn.misection.cvac.ast.statement.*;
import cn.misection.cvac.ast.type.basic.EnumCvaType;
import cn.misection.cvac.ast.type.reference.CvaClassType;
import cn.misection.cvac.ast.type.advance.CvaStringType;


import java.util.HashMap;
import java.util.Map;

/**
 * Created by MI6 root 1/28.
 */
public final class ConstantAndCopyPropagation
        implements IVisitor, Optimizable
{
    /**
     * // constant or copy in current method;
     */
    private Map<String, AbstractExpression> conorcopy;
    private AbstractExpression curExpr;
    private boolean canChange;

    /**
     * // if in while body, the left of assign should be delete from conorcopy
     */
    private boolean inWhile;
    private boolean isOptimizing;

    private boolean isEqual(AbstractExpression fir, AbstractExpression sec)
    {
        return (fir instanceof CvaConstIntExpr
                && sec instanceof CvaConstIntExpr
                && ((CvaConstIntExpr) fir).getValue() == ((CvaConstIntExpr) sec).getValue())
                || (fir instanceof CvaIdentifierExpr
                && sec instanceof CvaIdentifierExpr
                && ((CvaIdentifierExpr) fir).getLiteral().equals(((CvaIdentifierExpr) sec).getLiteral()));

    }

    private Map<String, AbstractExpression> intersection(
            Map<String, AbstractExpression> first,
            Map<String, AbstractExpression> second)
    {
        Map<String, AbstractExpression> result = new HashMap<>();
        first.forEach((k, v) ->
        {
            if (second.containsKey(k) && isEqual(v, second.get(k)))
            {
                result.put(k, v);
            }
        });
        return result;
    }

    @Override
    public void visit(EnumCvaType basicType) {}

    @Override
    public void visit(CvaStringType type) {}

    @Override
    public void visit(CvaClassType type) {}

    @Override
    public void visit(CvaDeclaration decl) {}

    @Override
    public void visit(CvaAddExpr expr)
    {
        this.visit(expr.getLeft());
        if (this.canChange)
        {
            expr.setLeft(this.curExpr);
        }
        this.visit(expr.getRight());
        if (this.canChange)
        {
            expr.setRight(this.curExpr);
        }
        this.canChange = false;
    }

    @Override
    public void visit(CvaAndAndExpr expr)
    {
        this.visit(expr.getLeft());
        if (this.canChange)
        {
            expr.setLeft(this.curExpr);
        }
        this.visit(expr.getRight());
        if (this.canChange)
        {
            expr.setRight(this.curExpr);
        }
        this.canChange = false;
    }

    @Override
    public void visit(CvaCallExpr expr)
    {
        this.visit(expr.getExpr());
        for (int i = 0; i < expr.getArgs().size(); i++)
        {
            this.visit(expr.getArgs().get(i));
            if (this.canChange)
            {
                expr.getArgs().set(i, this.curExpr);
            }
        }
        this.canChange = false;
    }

    @Override
    public void visit(CvaConstFalseExpr expr)
    {
        this.curExpr = expr;
        this.canChange = true;
    }

    @Override
    public void visit(CvaIdentifierExpr expr)
    {
        if (this.conorcopy.containsKey(expr.getLiteral()))
        {
            this.isOptimizing = true;
            this.canChange = true;
            this.curExpr = this.conorcopy.get(expr.getLiteral());
        }
        else
        {
            this.canChange = false;
        }
    }

    @Override
    public void visit(CvaLessThanExpr expr)
    {
        this.visit(expr.getLeft());
        if (this.canChange)
        {
            expr.setLeft(this.curExpr);
        }
        this.visit(expr.getRight());
        if (this.canChange)
        {
            expr.setRight(this.curExpr);
        }
        this.canChange = false;
    }

    @Override
    public void visit(CvaNewExpr expr)
    {
        this.canChange = false;
    }

    @Override
    public void visit(CvaNegateExpr expr)
    {
        this.visit(expr.getExpr());
        if (this.canChange)
        {
            expr.setExpr(this.curExpr);
        }
        this.canChange = false;
    }

    @Override
    public void visit(CvaConstIntExpr expr)
    {
        this.curExpr = expr;
        this.canChange = true;
    }

    @Override
    public void visit(CvaConstStringExpr expr)
    {
        // FIXME
    }

    @Override
    public void visit(CvaSubExpr expr)
    {
        this.visit(expr.getLeft());
        if (this.canChange)
        {
            expr.setLeft(this.curExpr);
        }
        this.visit(expr.getRight());
        if (this.canChange)
        {
            expr.setRight(this.curExpr);
        }
        this.canChange = false;
    }

    @Override
    public void visit(CvaThisExpr expr)
    {
        this.canChange = false;
    }

    @Override
    public void visit(CvaMulExpr expr)
    {
        this.visit(expr.getLeft());
        if (this.canChange)
        {
            expr.setLeft(this.curExpr);
        }
        this.visit(expr.getRight());
        if (this.canChange)
        {
            expr.setRight(this.curExpr);
        }
        this.canChange = false;
    }

    @Override
    public void visit(CvaConstTrueExpr expr)
    {
        this.curExpr = expr;
        this.canChange = true;
    }

    @Override
    public void visit(CvaAssignStatement stm)
    {
        if (this.inWhile)
        {
            this.conorcopy.remove(stm.getLiteral());
            return;
        }

        if (stm.getExpr() instanceof CvaIdentifierExpr || stm.getExpr() instanceof CvaConstIntExpr)
        {
            this.conorcopy.put(stm.getLiteral(), stm.getExpr());
        }
        else
        {
            this.visit(stm.getExpr());
            if (this.canChange)
            {
                stm.setExpr(this.curExpr);
            }
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
        if (this.inWhile)
        {
            return;
        }

        this.visit(stm.getCondition());
        if (this.canChange)
        {
            stm.setCondition(this.curExpr);
        }

        Map<String, AbstractExpression> originalMap = new HashMap<>();
        this.conorcopy.forEach(originalMap::put);
        this.visit(stm.getThenStatement());

        Map<String, AbstractExpression> leftMap = this.conorcopy;
        this.conorcopy = originalMap;
        if (stm.getElseStatement() != null)
        {
            this.visit(stm.getElseStatement());
        }
        this.conorcopy = intersection(leftMap, this.conorcopy);
    }


    @Override
    public void visit(CvaWriteStatement stm)
    {
        if (this.inWhile)
        {
            return;
        }

        this.visit(stm.getExpr());
        if (this.canChange)
        {
            stm.setExpr(curExpr);
        }
    }

    @Override
    public void visit(CvaWhileStatement stm)
    {
        // TODO: it is wrong when in multi-layer-loop
        // delete the var which be changed
        this.inWhile = true;
        this.visit(stm.getBody());
        this.inWhile = false;

        this.visit(stm.getCondition());
        this.visit(stm.getBody());
    }

    @Override
    public void visit(CvaIncreStatement stm)
    {
        // TODO;
    }

    @Override
    public void visit(CvaMethod cvaMethod)
    {
        this.conorcopy = new HashMap<>();
        cvaMethod.getStatementList().forEach(this::visit);
        this.visit(cvaMethod.getRetExpr());
        if (this.canChange)
        {
            cvaMethod.setRetExpr(this.curExpr);
        }
    }

    @Override
    public void visit(CvaMainMethod entryMethod)
    {
        // FIXME;
    }

    @Override
    public void visit(CvaClass cvaClass)
    {
        cvaClass.getMethodList().forEach(m ->
        {
            this.canChange = false;
            this.visit(m);
        });
    }

    @Override
    public void visit(CvaEntryClass entryClass)
    {
    }

    @Override
    public void visit(CvaProgram program)
    {
        this.isOptimizing = false;
        program.getClassList().forEach(this::visit);
    }

    @Override
    public boolean isOptimizing()
    {
        return isOptimizing;
    }
}
