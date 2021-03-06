package cn.misection.cvac.codegen.bst.btype.basic;

import cn.misection.cvac.codegen.bst.btype.ITargetType;
import cn.misection.cvac.codegen.bst.instructor.Instructable;

/**
 * @author Military Intelligence 6 root
 * @version 1.0.0
 * @ClassName TargetType
 * @Description TODO
 * @CreateTime 2021年02月20日 14:37:00
 */
public enum EnumTargetType
        implements ITargetType, Instructable
{
    /**
     * void;
     */
    TARGET_VOID("@void", "V"),

    TARGET_BYTE("@byte"),

    TARGET_CHAR("@char"),

    TARGET_SHORT("@short"),

    TARGET_INT("@int", "I"),

    TARGET_LONG("@long"),

    TARGET_FLOAT("@float"),

    TARGET_DOUBLE("@double"),

    TARGET_POINTER("@pointer"),

    TARGET_STRING("@string"),

    TARGET_ARRAY("@array"),

    TARGET_STRUCT("@struct"),

    TARGET_CLASS("@class"),

    TARGET_ENUM("@enum"),
    ;

    /**
     * instance domain;
     */
    private final String literal;

    private String instruction;

    EnumTargetType(String literal)
    {
        this.literal = literal;
    }

    EnumTargetType(String literal, String instruction)
    {
        this.literal = literal;
        this.instruction = instruction;
    }

    @Override
    public EnumTargetType toEnum()
    {
        return this;
    }

    @Override
    public String toString()
    {
        return literal;
    }

    @Override
    public String toInst()
    {
        return instruction;
    }

    public String getLiteral()
    {
        return literal;
    }
    /*
     * end instance domain;
     */

    /**
     * class static domain;
     * @param type t;
     * @return boolean;
     */
    public static boolean isBasicType(EnumTargetType type)
    {
        return type.ordinal() >= TARGET_VOID.ordinal()
                && type.ordinal() <= TARGET_DOUBLE.ordinal();
    }

    public static boolean isAdvanceType(EnumTargetType type)
    {
        return type.ordinal() >= TARGET_POINTER.ordinal()
                && type.ordinal() <= TARGET_ARRAY.ordinal();
    }

    public static boolean isReferenceType(EnumTargetType type)
    {
        return type.ordinal() >= TARGET_STRUCT.ordinal();
    }

    /**
     * 是否是整形, byte short等都是;
     * @param type t;
     * @return 包括byte char short long 等;
     */
    public static boolean isInteger(EnumTargetType type)
    {
        return type.ordinal() >= TARGET_BYTE.ordinal()
                && type.ordinal() <= TARGET_LONG.ordinal();
    }

    public static boolean isFloatPoint(EnumTargetType type)
    {
        return type == TARGET_FLOAT || type == TARGET_DOUBLE;
    }

    /**
     * 是否是前端的数字, boolean在后端是, 但在前端不是;
     * @return isint;
     */
    public static boolean isNumber(EnumTargetType type)
    {
        return isInteger(type) || isFloatPoint(type);
    }
}
