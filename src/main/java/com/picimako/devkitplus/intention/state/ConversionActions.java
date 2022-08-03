//Copyright 2021 Tamás Balog. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.picimako.devkitplus.intention.state;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.picimako.devkitplus.resources.DevKitPlusBundle;
import org.jetbrains.annotations.NotNull;

/**
 * Provides intention actions for the {@link MakeClassPersistentStateComponentIntention} what users can choose from.
 * <p>
 * Code generation is based on the <a href="https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html#implementing-the-persistentstatecomponent-interface">
 * Plugin SDK > Persisting State of Components > Implementing the PersistentStateComponent Interface</a> document.
 */
final class ConversionActions {

    private ConversionActions() {
    }

    /**
     * Converts the class using a standalone inner class ({@code State}) as the state object.
     * All existing fields, methods, etc. within the class remain untouched.
     * <p>
     * <h3>From:</h3>
     * <pre>
     * public class SomeComponent {
     * }
     * </pre>
     * <h3>To:</h3>
     * <pre>
     * import com.intellij.openapi.components.State;
     * import com.intellij.openapi.components.Storage;
     * import com.intellij.openapi.components.PersistentStateComponent;
     *
     * &#064;State(name = "SomeComponent", storages = @Storage("TODO: INSERT STORAGE NAME"))
     * public class SomeComponent implements PersistentStateComponent&lt;SomeComponent.State> {
     *
     *   private State myState = new State();
     *
     *   public State getState() {
     *     return myState;
     *   }
     *
     *   public void loadState(State state) {
     *     myState = state;
     *   }
     *
     *   static final class State {
     *   }
     * }
     * </pre>
     *
     * @since 0.1.0
     */
    static final class WithStandaloneStateObject extends BasePersistentStateComponentConversionIntention {
        static final WithStandaloneStateObject INSTANCE = new WithStandaloneStateObject();
        private static final CodeInsightActionHandler HANDLER = (project, editor, file) -> {
            ConversionContext context = createContext(project, editor, file);

            WriteCommandAction.runWriteCommandAction(project, () -> {
                addStateAnnotation(context);
                addPersistentStateComponentToImplementsList(context, context.targetClass.getName() + ".State");
                addStandaloneStateClass(context);
                //Add getState() and loadState() methods with the corresponding state field
                context.targetClass.add(context.factory.createFieldFromText("private State myState = new State();", context.targetClass));
                context.targetClass.add(context.factory.createMethodFromText("public State getState() {return myState;}", context.targetClass));
                context.targetClass.add(context.factory.createMethodFromText("public void loadState(State state) {myState = state;}", context.targetClass));

            });
        };

        @Override
        protected void update(@NotNull Presentation presentation, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
            super.update(presentation, project, editor, file);
            presentation.setText(DevKitPlusBundle.message("devkit.intention.persistent.state.use.standalone.state.object"));
        }

        @Override
        protected @NotNull CodeInsightActionHandler getHandler() {
            return HANDLER;
        }
    }

    /**
     * Converts the class using the class itself as the state object.
     * All existing fields, methods, etc. within the class remain untouched.
     * <p>
     * <h3>From:</h3>
     * <pre>
     * public class SomeComponent {
     * }
     * </pre>
     * <h3>To:</h3>
     * <pre>
     * import com.intellij.openapi.components.State;
     * import com.intellij.openapi.components.Storage;
     * import com.intellij.openapi.components.PersistentStateComponent;
     * import com.intellij.util.xmlb.XmlSerializerUtil;
     *
     * &#064;State(name = "SomeComponent", storages = @Storage("TODO: INSERT STORAGE NAME"))
     * public class SomeComponent implements PersistentStateComponent&lt;SomeComponent> {
     *
     *   public SomeComponent getState() {
     *     return this;
     *   }
     *
     *   public void loadState(SomeComponent state) {
     *     XmlSerializerUtil.copyBean(state, this);
     *   }
     * }
     * </pre>
     *
     * @since 0.1.0
     */
    static final class WithSelfAsState extends BasePersistentStateComponentConversionIntention {
        static final WithSelfAsState INSTANCE = new WithSelfAsState();
        private static final CodeInsightActionHandler HANDLER = (project, editor, file) -> {
            ConversionContext context = createContext(project, editor, file);

            WriteCommandAction.runWriteCommandAction(project, () -> {
                addStateAnnotation(context);
                String className = context.targetClass.getName();
                addPersistentStateComponentToImplementsList(context, className);
                //Add getState() and loadState() methods
                context.targetClass.add(context.factory.createMethodFromText("public " + className + " getState() {return this;}", context.targetClass));
                context.targetClass.add(context.styleManager.shortenClassReferences(context.factory
                    .createMethodFromText("public void loadState(" + className + " state) {com.intellij.util.xmlb.XmlSerializerUtil.copyBean(state, this);}", context.targetClass)));
            });
        };

        @Override
        protected void update(@NotNull Presentation presentation, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
            super.update(presentation, project, editor, file);
            presentation.setText(DevKitPlusBundle.message("devkit.intention.persistent.state.use.self.as.state"));
        }

        @Override
        protected @NotNull CodeInsightActionHandler getHandler() {
            return HANDLER;
        }
    }
}
