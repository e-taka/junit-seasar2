package org.seasar.framework.unit;

import java.util.List;

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * S2JUnit4を実行するための{@link org.junit.runner.Runner}です。
 * <p>
 * {@link org.junit.runner.RunWith}に指定して次のように利用します。
 *
 * <pre>
 * &#064;RunWith(Seasar24.class)
 * public class HogeTest {
 *   ...
 * }
 * </pre>
 *
 * </p>
 *
 */
public class Seasar24 extends BlockJUnit4ClassRunner {
    /**
     * テストクラス {@code klass} を実行するためのテストランナーを作成する.
     *
     * @param klass テストクラス
     * @throws InitializationError テストクラスが不正だった場合
     */
    public Seasar24(final Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected Statement classBlock(final RunNotifier notifier) {
        Statement statement = super.classBlock(notifier);
        statement = withEnvironment(statement);
        return statement;
    }

    /**
     * S2JUnit4 の振る舞いを設定する.
     *
     * @param statement 元の statement
     * @return S2JUnit4 の振る舞いを設定する statement
     */
    protected Statement withEnvironment(final Statement statement) {
        return new EnvironmentRule(statement);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Statement methodBlock(final FrameworkMethod method) {
        Object test;
        try {
            test = new ReflectiveCallable() {
                @Override
                protected Object runReflectiveCall() throws Throwable {
                    return createTest();
                }
            }.run();
        } catch (Throwable e) {
            return new Fail(e);
        }

        Statement statement = methodInvoker(method, test);
        statement = withTransaction(method, test, statement);
        statement = possiblyExpectingExceptions(method, test, statement);
        statement = withPotentialTimeout(method, test, statement);
        statement = withFieldsBinding(method, test, statement);
        statement = withContainer(method, test, statement);
        statement = withBefores(method, test, statement);
        statement = withAfters(method, test, statement);
        statement = withContext(method, test, statement);
        statement = withRules(method, test, statement);
        return statement;
    }

    /**
     * {@link org.seasar.framework.unit.TestContext} を作成し、
     * テストクラスのフィールドにバインディングする.
     *
     * @param method テストメソッド
     * @param target テストクラスのインスタンス
     * @param statement 元の statement
     * @return {@link org.seasar.framework.unit.TestContext} を作成する statement
     */
    protected Statement withContext(
            final FrameworkMethod method,
            final Object target,
            final Statement statement) {
        return new TestContextRule(statement, target, getTestClass(), method);
    }

    /**
     * DI コンテナを作成する.
     *
     * @param method テストメソッド
     * @param target テストクラスのインスタンス
     * @param statement 元の statement
     * @return DI コンテナを作成する statement
     */
    protected Statement withContainer(
            final FrameworkMethod method,
            final Object target,
            final Statement statement) {
        return new ContainerRule(statement, target, method);
    }

    /**
     * テストクラスの各フィールドに、
     * DI コンテナで管理されているオブジェクトをバインディングする.
     *
     * @param method テストメソッド
     * @param target テストクラスのインスタンス
     * @param statement 元の statement
     * @return オブジェクトをバインディングする statement
     */
    protected Statement withFieldsBinding(
            final FrameworkMethod method,
            final Object target,
            final Statement statement) {
        return new FieldsBindingRule(statement, target, getTestClass());
    }

    /**
     * トランザクションを開始し、コミットまたはロールバックする.
     *
     * @param method テストメソッド
     * @param target テストクラスのインスタンス
     * @param statement 元の statement
     * @return オブジェクトをバインディングする statement
     */
    protected Statement withTransaction(
            final FrameworkMethod method,
            final Object target,
            final Statement statement) {
        return new TransactionManagerRule(statement, getTestClass(), method);
    }

    /**
     * フィールド変数で定義したルール、メソッドで定義したルールを実行する.
     *
     * @param method テストメソッド
     * @param target テストクラスのインスタンス
     * @param statement 元の statement
     * @return ルールを実行する statement
     */
    protected Statement withRules(
            final FrameworkMethod method,
            final Object target,
            final Statement statement) {
        List<TestRule> testRules = getTestRules(target);
        Statement result = statement;
        result = withMethodRules(method, testRules, target, result);
        result = withTestRules(method, testRules, result);

        return result;
    }

    /**
     * フィールド変数で定義したルールを実行する.
     *
     * @param method テストメソッド
     * @param testRules ルール
     * @param target テストクラスのインスタンス
     * @param statement 元の statement
     * @return メソッドレベルのルールを実行する statement
     */
    private Statement withMethodRules(
            final FrameworkMethod method,
            final List<TestRule> testRules,
            final Object target,
            final Statement statement) {
        Statement result = statement;
        for (org.junit.rules.MethodRule each : getMethodRules(target)) {
            if (!testRules.contains(each)) {
                result = each.apply(result, method, target);
            }
        }
        return result;
    }

    /**
     * フィールド変数のルールを取得する.
     *
     * @param target テストクラスのインスタンス
     * @return ルール
     */
    private List<org.junit.rules.MethodRule>
    getMethodRules(final Object target) {
        return rules(target);
    }

    /**
     * Returns a {@link Statement}: apply all non-static {@code Value} fields
     * annotated with {@link org.junit.Rule}.
     *
     * @param method テストメソッド
     * @param testRules ルール
     * @param statement The base statement
     * @return a RunRules statement if any class-level {@link org.junit.Rule}s
     *          are found, or the base statement
     */
    private Statement withTestRules(
            final FrameworkMethod method, final List<TestRule> testRules,
            final Statement statement) {
        if (testRules.isEmpty()) {
            return statement;
        } else {
            return new RunRules(statement, testRules, describeChild(method));
        }
    }
}
