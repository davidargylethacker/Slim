package com.metova.slim;

import com.metova.slim.internal.ProcessorValues;
import com.metova.slim.internal.SlimBinder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Slim {

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

    // TODO: This will eventually all be deleted/rewritten. Keeping for reference for now.

//    /**
//     * <p>Assigns class variables found in <code>obj</code> annotated with <code>@Extra</code>
//     * with their assigned extras within the <code>extras</code> Bundle.</p>
//     * <p/>
//     * <p>Recommended to be called during <code>onCreate()</code> in an Activity or Fragment.</p>
//     *
//     * @param extras the Bundle to retrieve extras from
//     * @param obj    the class with the annotated extras
//     */
//    public static void injectExtras(Bundle extras, Object obj) {
//        Field[] fields = getNonAndroidFields(obj);
//        for (Field field : fields) {
//            if (field.isAnnotationPresent(Extra.class)) {
//                Extra annotation = field.getAnnotation(Extra.class);
//                String key = annotation.value();
//                Object value = BundleChecker.getExtra(key, null, extras);
//
//                if (value == null) {
//                    if (annotation.optional()) {
//                        continue;
//                    } else {
//                        throw new SlimException("Extra '" + key + "' was not found and it is not optional.");
//                    }
//                }
//
//                if (value instanceof Object[] && !(field.getType().equals(Object[].class))) {
//                    // Try to cast it to the array content type
//                    Object[] valueArray = (Object[]) value;
//                    if (valueArray.length > 0) {
//                        Object newValue = Array
//                                .newInstance(valueArray[0].getClass(), valueArray.length);
//                        for (int i = 0; i < valueArray.length; i++) {
//                            Array.set(newValue, i, valueArray[i]);
//                        }
//
//                        value = newValue;
//                    }
//                }
//
//                try {
//                    field.setAccessible(true);
//                    field.set(obj, value);
//                } catch (IllegalAccessException e) {
//                    throw new SlimException(e);
//                } catch (IllegalArgumentException e) {
//                    throw new SlimException(
//                            "invalid value " + value + " for field " + field.getName(), e);
//                }
//            }
//        }
//    }
//
//    /**
//     * Inject callback interfaces within an Object.
//     *
//     * @param child  the Object holding the callback interface implementation
//     * @param parent the Object implementing the callback interface
//     */
//    public static void injectCallbacks(Object child, Object parent) {
//        Field[] fields = getNonAndroidFields(child);
//        for (Field field : fields) {
//            if (field.isAnnotationPresent(Callback.class)) {
//                try {
//                    field.setAccessible(true);
//                    field.set(child, parent);
//                } catch (IllegalAccessException e) {
//                    throw new SlimException(e);
//                } catch (IllegalArgumentException e) {
//                    throw new SlimException(
//                            parent.getClass().getSimpleName() + " must implement " + field
//                                    .getType().getSimpleName()
//                    );
//                }
//            }
//        }
//    }

    public static void bind(Object source) {
        Class<?> generatedClass = getGeneratedClass(source);
        if (generatedClass == null) {
            throw new SlimException("Unable to find generated binder for " + source.getClass().getName());
        }

        try {
            SlimBinder binder = (SlimBinder) generatedClass.newInstance();
            binder.bind(source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> getGeneratedClass(Object source) {
        Class<?> bindingClass = null;
        Class<?> holderCls = source.getClass();
        while (isValidClass(holderCls)) {
            try {
                bindingClass = Class.forName(holderCls.getName() + ProcessorValues.SUFFIX);
                break;
            } catch (ClassNotFoundException e) {
                holderCls = holderCls.getSuperclass();
            }
        }

        return bindingClass;
    }

    private static boolean isValidClass(Class<?> cls) {
        String packageName = cls.getPackage().getName();
        return !packageName.startsWith("com.android.") && !packageName.startsWith("android.app.") && !packageName.startsWith("java.");
    }

    private static Field[] concatenate(Field[] a, Field[] b) {
        int aLength = a.length;
        int bLength = b.length;

        Field[] c = new Field[aLength + bLength];
        System.arraycopy(a, 0, c, 0, aLength);
        System.arraycopy(b, 0, c, aLength, bLength);

        return c;
    }

//    /**
//     * Helper method to create a layout within an Object annotated with the <code>@Layout</code> annotation.
//     *
//     * @param context an Activity Context
//     * @param obj     the Object with the <code>@Layout</code> annotation
//     * @return the layout
//     */
//    public static View createLayout(Context context, Object obj) {
//        return createLayout(context, obj, null);
//    }
//
//    /**
//     * Helper method to create a layout within an Object annotated with the <code>@Layout</code> annotation.
//     *
//     * @param context an Activity Context
//     * @param obj     the Object with the <code>@Layout</code> annotation
//     * @param parent  Optional parent used for inflating layout attributes
//     *                (does not automatically attach layout to the parent)
//     * @return the layout
//     */
//    public static View createLayout(Context context, Object obj, ViewGroup parent) {
//        Layout layout = obj.getClass().getAnnotation(Layout.class);
//        if (layout == null) {
//            Class<?>[] superClasses = getNonAndroidSuperClasses(obj);
//            for (Class<?> superClass : superClasses) {
//                layout = superClass.getAnnotation(Layout.class);
//                if (layout != null) {
//                    break;
//                }
//            }
//
//            if (layout == null) {
//                return null;
//            }
//        }
//
//        return LayoutInflater.from(context).inflate(layout.value(), parent, false);
//    }

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
