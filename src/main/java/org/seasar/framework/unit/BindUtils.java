package org.seasar.framework.unit;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 指定されたフィールドに指定した値をバインディングする.
 */
class BindUtils {
    /** ユーティリティクラスであるため、インスタンスを生成しない. */
    protected BindUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * 自動フィールドバインディングが可能な場合<code>true</code>を返します.
     *
     * @param field
     *            フィールド
     * @return 自動フィールドバインディングが可能な場合<code>true</code>、
     *          そうでない場合<code>false</code>
     */
    public static boolean isAutoBindable(final Field field) {
        final int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            return false;
        }
        if (Modifier.isFinal(modifiers)) {
            return false;
        }
        return !field.getType().isPrimitive();
    }

}
