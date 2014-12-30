package org.seasar.framework.unit;

import java.lang.reflect.Method;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.seasar.framework.unit.impl.ConventionTestIntrospector;

/**
 * テスト実行前後で、DI コンテナを初期化、廃棄する.
 */
class ContainerStatement extends Statement {
    /** 元の statement */
    private final Statement _statement;
    /** S2JUnit4の内部的なテストコンテキスト */
    private InternalTestContext _testContext = null;

    /** テストオブジェクト */
    private final Object _test;
    /** テストメソッド */
    private final Method _method;

    /**
     * DI コンテナを初期化、廃棄する statement を作成する.
     *
     * @param statement 元の statement
     * @param target テストクラスのインスタンス
     * @param method テストメソッド
     */
    public ContainerStatement(
            final Statement statement,
            final Object target,
            final FrameworkMethod method) {
        _statement = statement;
        _test = target;
        _method = method.getMethod();
    }

    @Override
    public void evaluate() throws Throwable {
        initContainer();
        try {
            _statement.evaluate();
        } finally {
            if (_testContext != null) {
                _testContext.destroyContainer();
            }
        }
    }

    /**
     * コンテナを初期化します.
     */
    protected void initContainer() {
        _testContext = TestContextRepository.get();
        if (_testContext == null) {
            return;
        }

        _testContext.include();
        ConventionTestIntrospector introspector =
                ConventionIntrospectorRepository.get();
        introspector.createMock(_method, _test, _testContext);
        _testContext.initContainer();
    }
}
