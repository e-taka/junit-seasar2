package org.seasar.framework.unit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import javax.ejb.EJB;

import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.seasar.framework.util.StringUtil;
import org.seasar.framework.util.tiger.CollectionsUtil;
import org.seasar.framework.util.tiger.ReflectionUtil;

/**
 * テスト実行前に、テストクラスの各フィールドに対して DI する.
 */
class FieldsBindingStatement extends Statement {
    /** 元の statement */
    private final Statement _statement;

    /** テストオブジェクト */
    private final Object _test;
    /** テストクラス */
    private final Class<?> _testClass;

    /** テストクラスのイントロスペクター */
    private final S2TestIntrospector _introspector;
    /** S2JUnit4の内部的なテストコンテキスト */
    private InternalTestContext _testContext;

    /** バインディングが行われたフィールドのリスト */
    private List<Field> _boundFields = CollectionsUtil.newArrayList();

    /**
     * テストクラスの各フィールドに DI する statement を作成する.
     *
     * @param statement 元の statement
     * @param test      テストクラスのインスタンス
     * @param testClass テストクラス
     */
    public FieldsBindingStatement(
            final Statement statement,
            final Object test, final TestClass testClass) {
        _statement = statement;
        _test = test;
        _testClass = testClass.getJavaClass();

        _introspector = ConventionIntrospectorRepository.get();
    }

    @Override
    public void evaluate() throws Throwable {
        _testContext = TestContextRepository.get();
        bindFields();
        try {
            _statement.evaluate();
        } finally {
            unbindFields();
        }
    }

    /**
     * フィールドにコンポーネントをバインディングします.
     *
     * @throws Throwable
     *             何らかの例外またはエラーが発生した場合
     */
    private void bindFields() throws Throwable {
        for (Class<?> clazz = _testClass;
                clazz != Object.class;
                clazz = clazz.getSuperclass()) {
            final Field[] fields = clazz.getDeclaredFields();
            for (final Field field : fields) {
                bindField(field);
            }
        }

        List<Method> postBindFieldsMethods =
                _introspector.getPostBindFieldsMethods(_testClass);
        for (final Method m : postBindFieldsMethods) {
            m.invoke(_test);
        }
    }

    /**
     * 指定されたフィールドにコンポーネントをバインディングします.
     *
     * @param field
     *            フィールド
     */
    private void bindField(final Field field) {
        if (!BindUtils.isAutoBindable(field)) {
            return;
        }

        field.setAccessible(true);
        if (ReflectionUtil.getValue(field, _test) != null) {
            return;
        }

        final String name = resolveComponentName(field);
        Object component = null;
        if (_testContext.hasComponentDef(name)) {
            component = _testContext.getComponent(name);
            if (component != null) {
                Class<?> componentClass = component.getClass();
                if (!field.getType().isAssignableFrom(componentClass)) {
                    component = null;
                }
            }
        }

        if (component != null) {
            ;
        } else if (_testContext.hasComponentDef(field.getType())) {
            component = _testContext.getComponent(field.getType());
        }

        if (component != null) {
            ReflectionUtil.setValue(field, _test, component);
            _boundFields.add(field);
        }
    }

    /**
     * フィールドからコンポーネントの名前を解決します.
     *
     * @param filed
     *            フィールド
     * @return コンポーネント名
     */
    private String resolveComponentName(final Field filed) {
        if (_testContext.isEjb3Enabled()) {
            final EJB ejb = filed.getAnnotation(EJB.class);
            if (ejb != null) {
                if (!StringUtil.isEmpty(ejb.beanName())) {
                    return ejb.beanName();
                } else if (!StringUtil.isEmpty(ejb.name())) {
                    return ejb.name();
                }
            }
        }
        return normalizeName(filed.getName());
    }

    /**
     * コンポーネント名を正規化します.
     *
     * @param name
     *            コンポーネント名
     * @return 正規化されたコンポーネント名
     */
    private String normalizeName(final String name) {
        return StringUtil.replace(name, "_", "");
    }

    /**
     * フィールドとコンポーネントのバインディングを解除します.
     *
     * @throws Throwable
     *             何らかの例外またはエラーが発生した場合
     */
    private void unbindFields() throws Throwable {
        List<Method> preUnbindFieldsMethods =
                _introspector.getPreUnbindFieldsMethods(_testClass);
        for (final Method m : preUnbindFieldsMethods) {
            m.invoke(_test);
        }

        for (final Field field : _boundFields) {
            try {
                field.set(_test, null);
            } catch (IllegalArgumentException e) {
                System.err.println(e);
            } catch (IllegalAccessException e) {
                System.err.println(e);
            }
        }

        _boundFields.clear();
    }
}
