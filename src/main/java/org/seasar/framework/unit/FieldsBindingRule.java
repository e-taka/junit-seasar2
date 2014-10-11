package org.seasar.framework.unit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.seasar.framework.util.StringUtil;
import org.seasar.framework.util.tiger.CollectionsUtil;
import org.seasar.framework.util.tiger.ReflectionUtil;

public class FieldsBindingRule extends Statement {
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
    private List<Field> boundFields = CollectionsUtil.newArrayList();

    public FieldsBindingRule(
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
     * フィールドにコンポーネントをバインディングします。
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
     * 指定されたフィールドにコンポーネントをバインディングします。
     *
     * @param field
     *            フィールド
     */
    private void bindField(final Field field) {
        if (!isAutoBindable(field)) {
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
            bindField(field, component);
        }
    }

    /**
     * 指定されたフィールドに指定された値をバインディングします。
     *
     * @param field
     *            フィールド
     * @param object
     *            値
     */
    private void bindField(final Field field, final Object object) {
        ReflectionUtil.setValue(field, _test, object);
        boundFields.add(field);
    }

    /**
     * 自動フィールドバインディングが可能な場合<code>true</code>を返します。
     *
     * @param field
     *            フィールド
     * @return 自動フィールドバインディングが可能な場合<code>true</code>、そうでない場合<code>false</code>
     */
    protected boolean isAutoBindable(final Field field) {
        final int modifiers = field.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            return false;
        }
        if (Modifier.isFinal(modifiers)) {
            return false;
        }
        return !field.getType().isPrimitive();
    }

    /**
     * フィールドからコンポーネントの名前を解決します。
     *
     * @param filed
     *            フィールド
     * @return コンポーネント名
     */
    private String resolveComponentName(final Field filed) {
//        if (_testContext.isEjb3Enabled()) {
//            final EJB ejb = filed.getAnnotation(EJB.class);
//            if (ejb != null) {
//                if (!StringUtil.isEmpty(ejb.beanName())) {
//                    return ejb.beanName();
//                } else if (!StringUtil.isEmpty(ejb.name())) {
//                    return ejb.name();
//                }
//            }
//        }
        return normalizeName(filed.getName());
    }

    /**
     * コンポーネント名を正規化します。
     *
     * @param name
     *            コンポーネント名
     * @return 正規化されたコンポーネント名
     */
    private String normalizeName(final String name) {
        return StringUtil.replace(name, "_", "");
    }

    /**
     * フィールドとコンポーネントのバインディングを解除します。
     *
     * @throws Throwable
     *
     */
    private void unbindFields() throws Throwable {
        List<Method> preUnbindFieldsMethods =
                _introspector.getPreUnbindFieldsMethods(_testClass);
        for (final Method m : preUnbindFieldsMethods) {
            m.invoke(_test);
        }

        for (final Field field : boundFields) {
            try {
                field.set(_test, null);
            } catch (IllegalArgumentException e) {
                System.err.println(e);
            } catch (IllegalAccessException e) {
                System.err.println(e);
            }
        }

        boundFields.clear();
    }
}
