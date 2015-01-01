package org.seasar.framework.unit;

import java.lang.reflect.Method;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.seasar.framework.container.S2Container;
import org.seasar.framework.container.factory.S2ContainerFactory;
import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.unit.impl.ConventionTestIntrospector;
import org.seasar.framework.util.StringUtil;

/**
 * ルートコンテナを作成する.
 */
class RootContainerStatement extends Statement {
    /** S2JUnit4のデフォルトの設定ファイルのパス */
    protected static final String DEFAULT_S2JUNIT4_PATH = "s2junit4.dicon";
    /** S2JUnit4の設定ファイルのパス */
    protected static String _s2junit4Path = DEFAULT_S2JUNIT4_PATH;

    /** 元の statement */
    private final Statement _statement;

    /** テストクラス */
    private final Class<?> _testClass;
    /** テストメソッド */
    private final Method _method;

    /**
     * ルートコンテナを作成する statement を作成する.
     *
     * @param statement 元のstatement
     * @param clazz  テストクラス
     * @param method テストメソッド
     */
    public RootContainerStatement(
            final Statement statement,
            final TestClass clazz,
            final FrameworkMethod method) {
        _statement = statement;
        _testClass = clazz.getJavaClass();
        _method = method.getMethod();
    }

    @Override
    public void evaluate() throws Throwable {
        createRootContainer();
        _statement.evaluate();
    }

    /**
     * ルートのコンテナを返します.
     *
     * @return ルートのコンテナ
     */
    protected S2Container createRootContainer() {
        S2Container container = null;
        ConventionTestIntrospector introspector =
                ConventionIntrospectorRepository.get();
        String rootDicon = introspector.getRootDicon(_testClass, _method);
        if (StringUtil.isEmpty(rootDicon)) {
            container = S2ContainerFactory.create(_s2junit4Path);
        } else {
            container = S2ContainerFactory.create(rootDicon);
            S2ContainerFactory.include(container, _s2junit4Path);
        }
        SingletonS2ContainerFactory.setContainer(container);
        return container;
    }

}
