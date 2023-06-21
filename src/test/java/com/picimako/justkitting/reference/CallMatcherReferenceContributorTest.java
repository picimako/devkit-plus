//Copyright 2023 Tamás Balog. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.picimako.justkitting.reference;

import static org.assertj.core.api.Assertions.assertThat;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.ResolveResult;

import com.picimako.justkitting.JustKittingTestBase;
import com.picimako.justkitting.ThirdPartyLibraryLoader;

/**
 * Functional test for {@link CallMatcherReferenceContributor}.
 */
public class CallMatcherReferenceContributorTest extends JustKittingTestBase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ThirdPartyLibraryLoader.loadJavaImpl(myFixture);
    }

    //Class reference

    public void testClassReferenceWithNoMethodNameSpecified() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   CallMatcher callMatcher = CallMatcher.instanceCall("java.uti<caret>l.List");
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false);

        assertThat(resolveResults).hasSize(1);
        assertThat(((PsiClass) resolveResults[0].getElement()).getQualifiedName()).isEqualTo("java.util.List");
    }

    public void testClassReferenceWithMethodNameSpecified() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   CallMatcher callMatcher = CallMatcher.instanceCall("java.uti<caret>l.List", "add");
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false);

        assertThat(resolveResults).hasSize(1);
        assertThat(((PsiClass) resolveResults[0].getElement()).getQualifiedName()).isEqualTo("java.util.List");
    }

    public void testNoClassReference() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   CallMatcher callMatcher = CallMatcher.instanceCall("<caret>List", "add");
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        assertThat(element.getReferences()).isEmpty();
    }

    //Method reference

    public void testSingleMethodReferenceResult() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   CallMatcher callMatcher = CallMatcher.instanceCall("java.util.List", "cl<caret>ear");
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false);

        assertThat(resolveResults).hasSize(2);
        assertThat(((PsiMethod) resolveResults[0].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: clear([])"); //from java.util.List
        assertThat(((PsiMethod) resolveResults[1].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: clear([])"); //from java.util.Collection
    }
    
    public void testSingleMethodReferenceResultEvaluated() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   private static final String CALL_MATCHER = "com.siyeh.ig.callMatcher.CallMatcher"   CallMatcher callMatcher = CallMatcher.instanceCall(CALL_MATCHER, "na<caret>mes");
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false);

        assertThat(resolveResults).hasSize(1);
        assertThat(((PsiMethod) resolveResults[0].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: names([])");
    }

    public void testMultipleMethodReferenceResults() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   CallMatcher callMatcher = CallMatcher.instanceCall("java.util.List", "a<caret>dd");
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false);

        assertThat(resolveResults).hasSize(3);
        assertThat(((PsiMethod) resolveResults[0].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: add([PsiType:E])"); //from java.util.List
        assertThat(((PsiMethod) resolveResults[1].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: add([PsiType:int, PsiType:E])"); //from java.util.List
        assertThat(((PsiMethod) resolveResults[2].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: add([PsiType:E])"); //from java.util.Collection
    }

    public void testNoMethodReferenceResult() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   CallMatcher callMatcher = CallMatcher.instanceCall("java.util.List", "asda<caret>sd");
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false);

        assertThat(resolveResults).isEmpty();
    }

    public void testMethodReferenceInExactClass() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   CallMatcher callMatcher = CallMatcher.exactInstanceCall("CallMatcherChecker.Inner", "some<caret>Method");

                   private static final class Inner extends SuperInner {
                       public void someMethod() {
                       }
                   }

                   private static class SuperInner {
                       public void someMethod(int i) {
                      }
                   }
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false);

        assertThat(resolveResults).hasSize(1);
        assertThat(((PsiMethod) resolveResults[0].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: someMethod([])");
    }

    public void testMethodReferenceInExactAndSuperClass() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   CallMatcher callMatcher = CallMatcher.instanceCall("CallMatcherChecker.Inner", "some<caret>Method");

                   private static final class Inner extends SuperInner {
                       public void someMethod() {
                       }
                   }

                   private static class SuperInner {
                       public void someMethod(int i) {
                      }
                   }
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false);

        assertThat(resolveResults).hasSize(2);
        assertThat(((PsiMethod) resolveResults[0].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: someMethod([])");
        assertThat(((PsiMethod) resolveResults[1].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: someMethod([PsiType:int])");
    }

    public void testMethodReferenceForStaticMethod() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   CallMatcher callMatcher = CallMatcher.staticCall("CallMatcherChecker.Inner", "some<caret>Method");

                   private static final class Inner {
                       public void someMethod() {
                       }
                       public static void someMethod() {
                       }
                   }
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false);

        assertThat(resolveResults).hasSize(1);
        assertThat(((PsiMethod) resolveResults[0].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: someMethod([])");
    }

    public void testMethodReferenceForStaticInterfaceMethod() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   CallMatcher callMatcher = CallMatcher.staticCall("CallMatcherChecker.InnerInt", "some<caret>Method");

                   private static final class Inner {
                       public void someMethod() {
                       }
                       public static void someMethod() {
                       }
                   }
                   private static final interface InnerInt {
                       public static void someMethod();
                   }
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false);

        assertThat(resolveResults).hasSize(1);
        assertThat(((PsiMethod) resolveResults[0].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: someMethod([])");
    }

    public void testMethodReferenceFoExternalStaticMethod() {
        myFixture.configureByText("CallMatcherChecker.java",
            """
                import com.siyeh.ig.callMatcher.CallMatcher;

                public class CallMatcherChecker {
                   CallMatcher callMatcher = CallMatcher.staticCall("java.lang.Integer", "toUnsignedSt<caret>ring");
                }""");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();
        ResolveResult[] resolveResults = ((PsiPolyVariantReference) element.getReferences()[0]).multiResolve(false);

        assertThat(resolveResults).hasSize(2);
        assertThat(((PsiMethod) resolveResults[0].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: toUnsignedString([PsiType:int, PsiType:int])");
        assertThat(((PsiMethod) resolveResults[1].getElement()).getSignature(PsiSubstitutor.EMPTY)).hasToString("MethodSignatureBackedByPsiMethod: toUnsignedString([PsiType:int])");
    }
}
