package sneaky;

import sneaky.plugin.SneakyPlugin.*;

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
            Field name = Class.class.getDeclaredField("classNameString");
            name.setAccessible(true);

            name.set(Class.class, "NotClass");

            System.out.println("Sneak successful; Class' name is now `" + Class.class.getName() + "`!");
        }catch(Exception e){
            throw new RuntimeException("An error occurred", e);
        }
    }
}
