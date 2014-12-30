package org.seasar.framework.unit;

import org.junit.runners.model.Statement;
import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.factory.S2ContainerFactory;

/**
 * DIでロードしたクラスを開放できるように、テスト終了後にクラスローダーを破棄する.
 *
 */
class ClassLoaderStatement extends Statement {
    /** 元の statement */
    private final Statement _statement;

    /**
     * クラスローダーを作成、破棄する statement を作成する.
     *
     * @param statement 元のstatement
     */
    public ClassLoaderStatement(final Statement statement) {
        _statement = statement;
    }

    @Override
    public void evaluate() throws Throwable {
        ClassLoader originalClassLoader = getOriginalClassLoader();
        ClassLoader unitClassLoader = new UnitClassLoader(originalClassLoader);
        Thread.currentThread().setContextClassLoader(unitClassLoader);

        try {
            _statement.evaluate();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            unitClassLoader = null;
            originalClassLoader = null;
        }
    }

    /**
     * オリジナルのクラスローダーを返します.
     *
     * @return オリジナルのクラスローダー
     */
    protected ClassLoader getOriginalClassLoader() {
        S2Container container =
                S2ContainerFactory.getConfigurationContainer();
        if (container == null) {
            ;
        } else if (container.hasComponentDef(ClassLoader.class)) {
            return ClassLoader.class.cast(
                    container.getComponent(ClassLoader.class));
        }
        return Thread.currentThread().getContextClassLoader();
    }
}
