package org.seasar.framework.unit;

import java.lang.reflect.Method;

import javax.transaction.TransactionManager;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

/**
 * {@link TransactionManager} でトランザクションの開始、ロールバックを実行する.
 */
class TransactionManagerRule extends Statement {
    /** 元の statement */
    private final Statement _statement;

    /** テストクラス */
    private final Class<?> _testClass;
    /** テストメソッド */
    private final Method _method;

    /** テストクラスのイントロスペクター */
    private S2TestIntrospector _introspector;
    /** S2JUnit4の内部的なテストコンテキスト */
    private InternalTestContext _testContext;

    /**
     * トランザクションを開始し、コミットまたはロールバックする
     * {@code Statement} を作成する.
     *
     * @param statement 元の statement
     * @param clazz テストクラス
     * @param method テストメソッド
     */
    public TransactionManagerRule(
            final Statement statement,
            final TestClass clazz,
            final FrameworkMethod method) {
        _statement = statement;
        _testClass = clazz.getJavaClass();
        _method = method.getMethod();
    }

    @Override
    public void evaluate() throws Throwable {
        _testContext = TestContextRepository.get();
        _introspector = ConventionIntrospectorRepository.get();

        if (!_testContext.isJtaEnabled()) {
            _statement.evaluate();
            return;
        }

        TransactionManager tm = null;
        if (_introspector.needsTransaction(_testClass, _method)) {
            try {
                tm = _testContext.getComponent(TransactionManager.class);
                tm.begin();
            } catch (Throwable t) {
                System.err.println(t);
            }
        }

        try {
            _testContext.prepareTestData();
            _statement.evaluate();

            if (tm != null) {
                if (requiresTransactionCommitment()) {
                    tm.commit();
                } else {
                    tm.rollback();
                }
            }
        } catch (Throwable t) {
            if (tm != null) {
                tm.rollback();
            }
            throw t;
        }
    }

    /**
     * テストが失敗していない場合かつトランザクションをコミットするように設定されてい
     * る場合に<code>true</code>を返します.
     *
     * @return テストが失敗していない場合かつトランザクションをコミットするように設
     *          定されている場合に<code>true</code>
     *          、そうでない場合<code>false</code>
     */
    protected boolean requiresTransactionCommitment() {
        return _introspector.requiresTransactionCommitment(_testClass, _method);
    }

}
