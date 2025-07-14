package com.toddmo.bydmate.client.helper;

import android.content.Context;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class InstanceHelper {

    /**
     * Creates an instance of a class using its constructor that accepts a Context parameter.
     * The constructor's accessibility will be set to public if it's not already.
     *
     * @param clazz The Class object of the type to be instantiated.
     * @param context The Android Context to be passed to the constructor.
     * @param <T> The type of the object to be instantiated.
     * @return An instance of the specified class, or null if instantiation fails.
     */
    public static <T> T getInstance(Class<T> clazz, Context context) {
        try {
            // Get the constructor that takes a Context parameter
            Constructor<T> constructor = clazz.getConstructor(Context.class);

            // Set the constructor to be accessible if it's not public
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }

            // Create and return a new instance using the constructor
            return constructor.newInstance(context);

        } catch (NoSuchMethodException e) {
            System.err.println("Error: No constructor found with a single Context parameter for class " + clazz.getName());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.err.println("Error: Constructor threw an exception for class " + clazz.getName());
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Error: Class " + clazz.getName() + " cannot be instantiated (e.g., it's an abstract class or an interface).");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            System.err.println("Error: Cannot access the constructor for class " + clazz.getName());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred while creating instance for class " + clazz.getName());
            e.printStackTrace();
        }
        return null;
    }
}
