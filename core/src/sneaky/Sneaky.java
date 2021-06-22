package sneaky;

import sneaky.plugin.SneakyPlugin.*;

import java.lang.invoke.*;
import java.lang.invoke.MethodHandles.*;
import java.lang.reflect.*;

public class Sneaky{
    /**
     * Tries to change the internal field returned by {@link Class#getName()}. If successful, it will log the new
     * name, and throws a {@link RuntimeException} otherwise.
     * @param args Command line arguments, currently no use.
     */
    @Sneak
    public static void main(String[] args){
        try{
            Field name = doTry(
                () -> Class.class.getDeclaredField("classNameString"), doTry(
                () -> Class.class.getDeclaredField("name"),
                null
            ));

            switch(name.getName()){
                // Java 8
                case "name" -> {
                    name.setAccessible(true);
                    name.set(Class.class, "java.lang.NotClass");
                }

                // Java 16
                case "classNameString" -> {
                    Class<?> type = Class.forName("sun.misc.Unsafe");
                    Field theUnsafe = type.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);

                    Lookup lookup = MethodHandles.lookup().in(Sneaky.class);
                    MethodHandle objectFieldOffset = lookup.findVirtual(type, "objectFieldOffset", MethodType.methodType(long.class, Field.class));
                    MethodHandle putObjectVolatile = lookup.findVirtual(type, "putObjectVolatile", MethodType.methodType(void.class, Object.class, long.class, Object.class));

                    Object unsafe = theUnsafe.get(null);
                    long offset = (long)objectFieldOffset.invoke(unsafe, name);
                    putObjectVolatile.invoke(unsafe, Class.class, offset, "java.lang.NotClass");
                }
            }

            System.out.println("Class' name is `" + Class.class.getName() + "`!");
        }catch(Throwable e){
            throw new RuntimeException("An error occurred", e);
        }

        System.out.println("By the way, I think this should print `java.lang.Object@<hashcode>`: '" + new Object() + "'.");
    }

    public static <T> T doTry(UnsafeProvider<T> value, T def){
        try{
            return value.get();
        }catch(Throwable t){
            return def;
        }
    }

    public interface UnsafeProvider<T>{
        T get() throws Throwable;
    }
}
