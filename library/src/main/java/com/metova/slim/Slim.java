package com.metova.slim;

import com.metova.slim.annotation.ActivityFragment;
import com.metova.slim.annotation.Callback;
import com.metova.slim.annotation.CallbackClick;
import com.metova.slim.annotation.Extra;
import com.metova.slim.annotation.Layout;
import com.metova.slim.internal.BundleChecker;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Slim {

    /**
     * Shorthand to inject callbacks and extras within a Fragment. Call during or after <code>onCreate</code>.
     *
     * @param fragment the Fragment to inject
     */
    public static void inject(Fragment fragment) {
        injectCallbacks(fragment);
        injectExtras(fragment.getArguments(), fragment);
    }

    private static Field[] getNonAndroidFields(Object obj) {
        Class<?> objClass = obj.getClass();
        Field[] fields = objClass.getDeclaredFields();

        Class<?> superClass = objClass;
        while ((superClass = superClass.getSuperclass()) != null) {
            String packageName = superClass.getPackage().getName();
            if (packageName.startsWith("com.android") || packageName.startsWith("android.app")) {
                break;
            }

            fields = concatenate(fields, superClass.getDeclaredFields());
        }

        return fields;
    }

    /**
     * <p>Assigns class variables found in <code>obj</code> annotated with <code>@Extra</code>
     * with their assigned extras within the <code>extras</code> Bundle.</p>
     * <p/>
     * <p>Recommended to be called during <code>onCreate()</code> in an Activity or Fragment.</p>
     *
     * @param extras the Bundle to retrieve extras from
     * @param obj    the class with the annotated extras
     */
    public static void injectExtras(Bundle extras, Object obj) {
        Field[] fields = getNonAndroidFields(obj);
        for (Field field : fields) {
            if (field.isAnnotationPresent(Extra.class)) {
                Extra annotation = field.getAnnotation(Extra.class);
                String key = annotation.value();
                Object value = BundleChecker.getExtra(key, null, extras);

                if (value == null) {
                    if (annotation.optional()) {
                        continue;
                    } else {
                        throw new SlimException("Extra '" + key + "' was not found and it is not optional.");
                    }
                }

                if (value instanceof Object[] && !(field.getType().equals(Object[].class))) {
                    // Try to cast it to the array content type
                    Object[] valueArray = (Object[]) value;
                    if (valueArray.length > 0) {
                        Object newValue = Array
                                .newInstance(valueArray[0].getClass(), valueArray.length);
                        for (int i = 0; i < valueArray.length; i++) {
                            Array.set(newValue, i, valueArray[i]);
                        }

                        value = newValue;
                    }
                }

                try {
                    field.setAccessible(true);
                    field.set(obj, value);
                } catch (IllegalAccessException e) {
                    throw new SlimException(e);
                } catch (IllegalArgumentException e) {
                    throw new SlimException(
                            "invalid value " + value + " for field " + field.getName(), e);
                }
            }
        }
    }

    /**
     * Inject callback interfaces within a Fragment.
     *
     * @param fragment the fragment to inject
     */
    public static void injectCallbacks(Fragment fragment) {
        injectCallbacks(fragment, fragment.getActivity());
    }

    /**
     * Inject callback interfaces within an Object.
     *
     * @param child  the Object holding the callback interface implementation
     * @param parent the Object implementing the callback interface
     */
    public static void injectCallbacks(Object child, Object parent) {
        Field[] fields = getNonAndroidFields(child);
        for (Field field : fields) {
            if (field.isAnnotationPresent(Callback.class)) {
                try {
                    field.setAccessible(true);
                    field.set(child, parent);
                } catch (IllegalAccessException e) {
                    throw new SlimException(e);
                } catch (IllegalArgumentException e) {
                    throw new SlimException(
                            parent.getClass().getSimpleName() + " must implement " + field
                                    .getType().getSimpleName()
                    );
                }
            }
        }
    }

    /**
     * Inject the no-arg methods that have a <code>@CallbackClick</code> annotation in a <code>@Callback</code>
     * interface with click listeners. This only works for methods that have zero arguments, as annotations do not support
     * Object arguments.
     *
     * @param fragment the Fragment to inject
     */
    public static void injectCallbacksMethods(Fragment fragment) {
        Field[] fields = getNonAndroidFields(fragment);
        for (Field field : fields) {
            if (field.isAnnotationPresent(Callback.class)) {
                try {
                    field.setAccessible(true);
                    injectCallbackMethods(field.getType(), field.get(fragment), fragment);
                } catch (IllegalAccessException e) {
                    throw new SlimException(e);
                }
            }
        }
    }

    private static void injectCallbackMethods(Class<?> callbackClass, Object callbackObject, Fragment fragment) {
        Method[] methods = callbackClass.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(CallbackClick.class)) {
                if (method.getParameterTypes().length > 0) {
                    throw new SlimException("Methods annotated with CallbackClick must have zero parameters.");
                }

                CallbackClick callbackClick = method.getAnnotation(CallbackClick.class);
                int id = callbackClick.value();
                assignClickListener(fragment, id, callbackObject, method);
            }
        }
    }

    private static void assignClickListener(final Fragment fragment, final int id, final Object callbackObject, final Method method) {
        View v = fragment.getView().findViewById(id);
        if (v == null) {
            throw new SlimException("id does not exist within " + fragment.getClass().getName() + ": " + id);
        }

        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    method.invoke(callbackObject);
                } catch (IllegalAccessException e) {
                    throw new SlimException(e);
                } catch (InvocationTargetException e) {
                    throw new SlimException(e);
                }
            }
        });
    }

    private static Field[] concatenate(Field[] a, Field[] b) {
        int aLength = a.length;
        int bLength = b.length;

        Field[] c = new Field[aLength + bLength];
        System.arraycopy(a, 0, c, 0, aLength);
        System.arraycopy(b, 0, c, aLength, bLength);

        return c;
    }

    /**
     * Helper method to create a layout within an Object annotated with the <code>@Layout</code> annotation.
     *
     * @param context an Activity Context
     * @param obj     the Object with the <code>@Layout</code> annotation
     * @return the layout
     */
    public static View createLayout(Context context, Object obj) {
        return createLayout(context, obj, null);
    }

    /**
     * Helper method to create a layout within an Object annotated with the <code>@Layout</code> annotation.
     *
     * @param context an Activity Context
     * @param obj     the Object with the <code>@Layout</code> annotation
     * @param parent  The view's parent, used for inflating layout attributes
     *                (does not automatically attach layout to the parent)
     * @return the layout
     */
    public static View createLayout(Context context, Object obj, ViewGroup parent) {
        Layout layout = obj.getClass().getAnnotation(Layout.class);
        if (layout == null) {
            Class<?>[] superClasses = getNonAndroidSuperClasses(obj);
            for (Class<?> superClass : superClasses) {
                layout = superClass.getAnnotation(Layout.class);
                if (layout != null) {
                    break;
                }
            }

            if (layout == null) {
                return null;
            }
        }

        return LayoutInflater.from(context).inflate(layout.value(), parent, false);
    }

    public static void injectFragment(FragmentActivity activity) {
        ActivityFragment activityFragment = activity.getClass().getAnnotation(ActivityFragment.class);
        if (activityFragment == null) {
            Class<?>[] superClasses = getNonAndroidSuperClasses(activity);
            for (Class<?> superClass : superClasses) {
                activityFragment = superClass.getAnnotation(ActivityFragment.class);
                if (activityFragment != null) {
                    break;
                }
            }

            if (activityFragment == null) {
                return;
            }
        }

        Class<?> fragmentClass = activityFragment.value();
        int id = activityFragment.id();
        String tag = activityFragment.tag();

        if (id == 0) {
            id = android.R.id.content;
        }

        if (TextUtils.isEmpty(tag)) {
            tag = null;
        }

        Fragment fragment = Fragment.instantiate(activity, fragmentClass.getName());
        addFragment(activity, fragment, id, tag);
    }

    public static void addFragment(FragmentActivity activity, Fragment fragment) {
        addFragment(activity, fragment, android.R.id.content);
    }

    public static void addFragment(FragmentActivity activity, Fragment fragment, String tag) {
        addFragment(activity, fragment, android.R.id.content, tag);
    }

    public static void addFragment(FragmentActivity activity, Fragment fragment, int id) {
        addFragment(activity, fragment, id, null);
    }

    public static void addFragment(FragmentActivity activity, Fragment fragment, int id, String tag) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        if (tag == null) {
            ft.add(id, fragment);
        } else {
            ft.add(id, fragment, tag);
        }
        ft.commit();
    }

    private static Class<?>[] getNonAndroidSuperClasses(Object obj) {
        List<Class<?>> superClassList = new ArrayList<>();
        Class<?> superClass = obj.getClass();
        while ((superClass = superClass.getSuperclass()) != null) {
            String packageName = superClass.getPackage().getName();
            if (packageName.startsWith("com.android") || packageName.startsWith("android.app")) {
                break;
            }

            superClassList.add(superClass);
        }

        return superClassList.toArray(new Class<?>[superClassList.size()]);
    }
}
