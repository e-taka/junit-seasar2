package org.seasar.framework.unit;

import java.util.List;

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
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

    /**
     * Returns a Statement that, when executed, either returns normally if
     * {@code method} passes, or throws an exception if {@code method} fails.
     *
     * Here is an outline of the default implementation:
     *
     * <ul>
     * <li>Invoke {@code method} on the result of {@code createTest()}, and
     * throw any exceptions thrown by either operation.
     * <li>HOWEVER, if {@code method}'s {@code @Test} annotation has the {@code
     * expecting} attribute, return normally only if the previous step threw an
     * exception of the correct type, and throw an exception otherwise.
     * <li>HOWEVER, if {@code method}'s {@code @Test} annotation has the {@code
     * timeout} attribute, throw an exception if the previous step takes more
     * than the specified number of milliseconds.
     * <li>ALWAYS run all non-overridden {@code @Before} methods on this class
     * and superclasses before any of the previous steps; if any throws an
     * Exception, stop execution and pass the exception on.
     * <li>ALWAYS run all non-overridden {@code @After} methods on this class
     * and superclasses after any of the previous steps; all After methods are
     * always executed: exceptions thrown by previous steps are combined, if
     * necessary, with exceptions from After methods into a
     * {@link MultipleFailureException}.
     * <li>ALWAYS allow {@code @Rule} fields to modify the execution of the
     * above steps. A {@code Rule} may prevent all execution of the above steps,
     * or add additional behavior before and after, or modify thrown exceptions.
     * For more information, see {@link TestRule}
     * </ul>
     *
     * This can be overridden in subclasses, either by overriding this method,
     * or the implementations creating each sub-statement.
     *
     * @param method テストメソッド
     * @return テストメソッドを実行する statement
     */
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
        return new ContainerRule(statement);
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
     * Returns a {@link Statement}: apply all non-static {@link Value} fields
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
