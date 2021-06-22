package sneaky.plugin;

import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.source.util.TaskEvent.*;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;

import javax.lang.model.element.*;
import java.lang.annotation.*;
import java.util.*;
import java.util.function.*;

public class SneakyPlugin implements Plugin{
    @Override
    public void init(JavacTask task, String... args){
        Context context = ((BasicJavacTask)task).getContext();
        TreeMaker fac = TreeMaker.instance(context);
        Names names = Names.instance(context);

        // Appends `System.out.println(...)` to methods annotated with @Sneak
        task.addTaskListener(new TaskListener(){
            @Override
            public void finished(TaskEvent e){
                if(e.getKind() != Kind.PARSE) return;

                CompilationUnitTree unit = e.getCompilationUnit();
                unit.accept(new TreeScanner<Void, Void>(){
                    @Override
                    public Void visitMethod(MethodTree node, Void unused){
                        if(node.getModifiers().getAnnotations()
                            .stream()
                            .anyMatch(e -> Sneak.class.getSimpleName().equals(e.getAnnotationType().toString()))
                        ){
                            JCBlock block = (JCBlock)node.getBody();
                            block.stats = block.stats.prepend(
                                fac.at(((JCTree)node).pos).Exec(fac.Apply(
                                    List.nil(),

                                    fac.Select(
                                        fac.Select(
                                            fac.Ident(names.fromString("System")), //System
                                            names.fromString("out") //.out
                                        ),

                                        names.fromString("println") //.println
                                    ),

                                    List.of(fac.Literal(TypeTag.CLASS, "I'm sneaking on you...")) //"I'm sneaking on you..."
                                ))
                            );
                        }

                        return super.visitMethod(node, unused);
                    }
                }, null);
            }
        });

        // Makes all instantiation anonymous and override the `toString()` method
        task.addTaskListener(new TaskListener(){
            @Override
            public void finished(TaskEvent e){
                if(e.getKind() != Kind.PARSE) return;

                CompilationUnitTree unit = e.getCompilationUnit();
                unit.accept(new TreeScanner<Void, Void>(){
                    @Override
                    public Void visitNewClass(NewClassTree node, Void unused){
                        JCNewClass inst = (JCNewClass)node;
                        JCClassDecl decl = inst.def;

                        if(decl == null){
                            decl = inst.def = fac.ClassDef(
                                fac.Modifiers(0, List.nil()),
                                names.fromString(""),
                                List.nil(),
                                null,
                                List.nil(),
                                List.nil(),
                                List.nil()
                            );
                        }

                        Predicate<JCMethodDecl> isToString = meth ->
                            meth.name.toString().equals("toString") &&
                            meth.mods.getFlags().contains(Modifier.PUBLIC) &&
                            meth.restype.toString().equals("String") &&
                            meth.getParameters().isEmpty();

                        Optional<JCMethodDecl> opt = decl.defs.stream()
                            .filter(tree -> {
                                if(tree instanceof JCMethodDecl){
                                    return isToString.test((JCMethodDecl)tree);
                                }else{
                                    return false;
                                }
                            })
                            .map(tree -> (JCMethodDecl)tree)
                            .findFirst();

                        JCMethodDecl tostr;
                        if(opt.isPresent()){
                            tostr = opt.get();
                        }else{
                            tostr = fac.at(decl.pos).MethodDef(
                                fac.Modifiers(Flags.PUBLIC, List.of(fac.Annotation(
                                    fac.Ident(names.fromString("Override")),
                                    List.nil()
                                ))),
                                names.fromString("toString"),
                                fac.Ident(names.fromString("String")),
                                List.nil(),
                                List.nil(),
                                List.nil(),
                                fac.Block(0, List.nil()),
                                null
                            );
                        }

                        tostr.body.stats = List.of(fac.Return(fac.Literal(TypeTag.CLASS, "I sneaked your `toString()`...")));

                        decl.defs.removeIf(tree -> tree instanceof JCMethodDecl && isToString.test((JCMethodDecl)tree));
                        decl.defs = decl.defs.prepend(tostr);

                        return super.visitNewClass(node, unused);
                    }
                }, null);
            }
        });

        System.out.println("Sneaky...");
    }

    @Override
    public String getName(){
        return "sneaky";
    }

    @Override
    public boolean autoStart(){
        return true;
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    public @interface Sneak{}
}
