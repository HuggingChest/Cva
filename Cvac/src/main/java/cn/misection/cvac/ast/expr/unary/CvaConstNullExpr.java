package cn.misection.cvac.ast.expr.unary;

import cn.misection.cvac.ast.expr.EnumCvaExpr;
import cn.misection.cvac.ast.type.basic.EnumCvaType;

/**
 * @author Military Intelligence 6 root
 * @version 1.0.0
 * @ClassName CvaConstNullExpr
 * @Description TODO
 * @CreateTime 2021年02月21日 09:19:00
 */
public final class CvaConstNullExpr extends AbstractUnaryExpr
{
    public CvaConstNullExpr(int lineNum)
    {
        super(lineNum);
    }

    @Override
    public EnumCvaType resType()
    {
        return EnumCvaType.CVA_VOID;
    }

    @Override
    public EnumCvaExpr toEnum()
    {
        return EnumCvaExpr.CONST_NULL;
    }
}
