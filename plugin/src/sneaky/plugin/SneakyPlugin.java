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

import java.lang.annotation.*;

public class SneakyPlugin implements Plugin{
    @Override
    public void init(JavacTask task, String... args){
        Context context = ((BasicJavacTask)task).getContext();
        TreeMaker fac = TreeMaker.instance(context);
        Names names = Names.instance(context);

        //
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
