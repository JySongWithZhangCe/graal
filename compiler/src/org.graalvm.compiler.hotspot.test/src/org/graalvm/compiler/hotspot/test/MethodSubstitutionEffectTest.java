/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.compiler.hotspot.test;

import static org.graalvm.compiler.core.common.CompilationIdentifier.INVALID_COMPILATION_ID;
import static org.graalvm.compiler.debug.DebugOptions.DumpOnError;

import java.util.ArrayList;

import org.graalvm.compiler.api.directives.GraalDirectives;
import org.graalvm.compiler.api.replacements.ClassSubstitution;
import org.graalvm.compiler.api.replacements.MethodSubstitution;
import org.graalvm.compiler.api.test.Graal;
import org.graalvm.compiler.core.test.GraalCompilerTest;
import org.graalvm.compiler.debug.GraalError;
import org.graalvm.compiler.debug.TTY;
import org.graalvm.compiler.java.BytecodeParserOptions;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.StructuredGraph.AllowAssumptions;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins.Registration;
import org.graalvm.compiler.options.OptionValues;
import org.graalvm.compiler.phases.tiers.Suites;
import org.graalvm.compiler.replacements.ReplacementsImpl;
import org.graalvm.compiler.replacements.classfile.ClassfileBytecodeProvider;
import org.graalvm.compiler.runtime.RuntimeProvider;
import org.junit.Assert;
import org.junit.Test;

import jdk.vm.ci.meta.MetaAccessProvider;
import jdk.vm.ci.meta.ResolvedJavaMethod;

public class MethodSubstitutionEffectTest extends GraalCompilerTest {
    public static int ValueFountain;

    static class Substitutee {

        public static int singleEffect(@SuppressWarnings("unused") int a) {
            return 0;
        }

        public static int sequentialEffectInvalid(@SuppressWarnings("unused") int a) {
            return 0;
        }

        public static void sequentialEffectInvalidVoid(@SuppressWarnings("unused") int a) {
        }

        public static int splitEffect(@SuppressWarnings("unused") int a) {
            return 0;
        }

        public static void splitEffectVoid(@SuppressWarnings("unused") int a) {
        }

        public static int splitEffectWrong(@SuppressWarnings("unused") int a) {
            return 0;
        }

        public static int splitParitalIntrinsicExit(@SuppressWarnings("unused") int a) {
            return 0;
        }
    }

    @ClassSubstitution(Substitutee.class)
    public static class Substitutor {

        @MethodSubstitution
        public static int singleEffect(int a) {
            return GraalDirectives.sideEffect(a);
        }

        @MethodSubstitution
        public static int sequentialEffectInvalid(int a) {
            GraalDirectives.sideEffect(a);
            return GraalDirectives.sideEffect(a);
        }

        @MethodSubstitution
        public static void sequentialEffectInvalidVoid(int a) {
            GraalDirectives.sideEffect(a);
            GraalDirectives.sideEffect(a);
        }

        @MethodSubstitution
        public static int splitEffect(int a) {
            int i;
            if (a > 0) {
                GraalDirectives.sideEffect(a);
                i = a;
            } else {
                GraalDirectives.sideEffect(42);
                i = 42;
            }
            return i;
        }

        @MethodSubstitution
        public static void splitEffectVoid(int a) {
            if (a > 0) {
                GraalDirectives.sideEffect(a);
            } else {
                GraalDirectives.sideEffect(42);
            }
        }

        @MethodSubstitution
        public static int splitEffectWrong(int a) {
            int i;
            if (a > 0) {
                GraalDirectives.sideEffect(a);
                GraalDirectives.sideEffect(a);
                i = a;
            } else {
                i = 42;
                GraalDirectives.controlFlowAnchor();
            }
            return i;
        }

        @MethodSubstitution
        public static int splitParitalIntrinsicExit(int a) {
            int i;
            if (a > 0) {
                i = GraalDirectives.sideEffect(a);
            } else {
                i = splitParitalIntrinsicExit(a);
            }
            return i;
        }
    }

    @Override
    protected void registerInvocationPlugins(InvocationPlugins invocationPlugins) {
        ClassfileBytecodeProvider bytecodeProvider = getSystemClassLoaderBytecodeProvider();
        Registration r = new Registration(invocationPlugins, Substitutee.class, getReplacements(), bytecodeProvider);
        r.registerMethodSubstitution(Substitutor.class, "singleEffect", int.class);
        r.registerMethodSubstitution(Substitutor.class, "sequentialEffectInvalid", int.class);
        r.registerMethodSubstitution(Substitutor.class, "sequentialEffectInvalidVoid", int.class);
        r.registerMethodSubstitution(Substitutor.class, "splitEffect", int.class);
        r.registerMethodSubstitution(Substitutor.class, "splitEffectVoid", int.class);
        r.registerMethodSubstitution(Substitutor.class, "splitEffectWrong", int.class);
        r.registerMethodSubstitution(Substitutor.class, "splitParitalIntrinsicExit", int.class);
        super.registerInvocationPlugins(invocationPlugins);
    }

    private ClassfileBytecodeProvider getSystemClassLoaderBytecodeProvider() {
        ReplacementsImpl d = (ReplacementsImpl) getReplacements();
        MetaAccessProvider metaAccess = d.getProviders().getMetaAccess();
        ClassfileBytecodeProvider bytecodeProvider = new ClassfileBytecodeProvider(metaAccess, d.snippetReflection, ClassLoader.getSystemClassLoader());
        return bytecodeProvider;
    }

    static void snippet01() {
        Substitutee.singleEffect(42);
        if (ValueFountain == 42) {
            GraalDirectives.deoptimize();
        }
    }

    static void snippet02() {
        Substitutee.sequentialEffectInvalid(42);
        if (ValueFountain == 42) {
            GraalDirectives.deoptimize();
        }
    }

    static void snippet03() {
        Substitutee.sequentialEffectInvalidVoid(42);
        if (ValueFountain == 42) {
            GraalDirectives.deoptimize();
        }
    }

    static void snippet04() {
        Substitutee.splitEffect(ValueFountain);
        if (ValueFountain == 42) {
            GraalDirectives.deoptimize();
        }
    }

    static void snippet05() {
        Substitutee.splitEffectVoid(ValueFountain);
        if (ValueFountain == 42) {
            GraalDirectives.deoptimize();
        }
    }

    static void snippet06() {
        Substitutee.splitEffectWrong(ValueFountain);
        if (ValueFountain == 42) {
            GraalDirectives.deoptimize();
        }
    }

    static void snippet07() {
        if (Substitutee.splitParitalIntrinsicExit(ValueFountain) == 42) {
            GraalDirectives.deoptimize();
        }
    }

    StructuredGraph getGraph(String snippet) {
        OptionValues options = new OptionValues(getInitialOptions(), DumpOnError, false);
        /*
         * We do not want to inline partial intrinsic exits in this test to test the state of the
         * self recursive call.
         */
        options = new OptionValues(getInitialOptions(), BytecodeParserOptions.InlinePartialIntrinsicExitDuringParsing, false);
        StructuredGraph g = parseEager(getResolvedJavaMethod(snippet), AllowAssumptions.NO, options);
        Suites s = Graal.getRequiredCapability(RuntimeProvider.class).getHostBackend().getSuites().getDefaultSuites(getInitialOptions());
        s.getHighTier().apply(g, getDefaultHighTierContext());
        s.getMidTier().apply(g, getDefaultMidTierContext());
        return g;
    }

    @Test
    public void test1() {
        getGraph("snippet01");
    }

    @Test
    @SuppressWarnings("try")
    public void test2() {
        try (AutoCloseable c = new TTY.Filter()) {
            getGraph("snippet02");
        } catch (Throwable t) {
            if (t.getCause() instanceof GraalError && t.getMessage().contains("unexpected node between return StateSplit and last instruction")) {
                return;
            }
            Assert.fail();
        }
    }

    @Test
    @SuppressWarnings("try")
    public void test3() {
        try (AutoCloseable c = new TTY.Filter()) {
            getGraph("snippet03");
        } catch (Throwable t) {
            if (t.getCause() instanceof GraalError && t.getMessage().contains(" produced invalid framestate")) {
                return;
            }
            Assert.fail();
        }
    }

    @Test
    public void test4() {
        getGraph("snippet04");
    }

    @Test
    public void test5() {
        getGraph("snippet05");
    }

    @Test
    @SuppressWarnings("try")
    public void test6() {
        try (AutoCloseable c = new TTY.Filter()) {
            getGraph("snippet06");
        } catch (Throwable t) {
            if (t.getCause() instanceof GraalError && t.getMessage().contains(" produced invalid framestate")) {
                return;
            }
            Assert.fail();
        }
    }

    @Test
    public void test7() {
        getGraph("snippet07");
    }

    @Test
    @SuppressWarnings("try")
    public void testRootCompiles() {
        ArrayList<ResolvedJavaMethod> intrinisicsWithoutErrors = new ArrayList<>();
        ArrayList<ResolvedJavaMethod> intrinisicsErrors = new ArrayList<>();

        intrinisicsWithoutErrors.add(getResolvedJavaMethod(Substitutee.class, "singleEffect"));
        intrinisicsWithoutErrors.add(getResolvedJavaMethod(Substitutee.class, "singleEffect"));
        intrinisicsWithoutErrors.add(getResolvedJavaMethod(Substitutee.class, "splitEffect"));
        intrinisicsWithoutErrors.add(getResolvedJavaMethod(Substitutee.class, "splitEffectVoid"));
        intrinisicsWithoutErrors.add(getResolvedJavaMethod(Substitutee.class, "splitParitalIntrinsicExit"));

        intrinisicsErrors.add(getResolvedJavaMethod(Substitutee.class, "sequentialEffectInvalid"));
        intrinisicsErrors.add(getResolvedJavaMethod(Substitutee.class, "sequentialEffectInvalidVoid"));
        intrinisicsErrors.add(getResolvedJavaMethod(Substitutee.class, "splitEffectWrong"));

        for (ResolvedJavaMethod method : intrinisicsWithoutErrors) {
            StructuredGraph graph = getProviders().getReplacements().getIntrinsicGraph(method, INVALID_COMPILATION_ID, getDebugContext(), null);
            getCode(method, graph);
        }
        for (ResolvedJavaMethod method : intrinisicsErrors) {
            try (AutoCloseable c = new TTY.Filter()) {
                StructuredGraph graph = getProviders().getReplacements().getIntrinsicGraph(method, INVALID_COMPILATION_ID, getDebugContext(), null);
                getCode(method, graph);
            } catch (Throwable t) {
                if ((t.getCause() instanceof GraalError || t instanceof GraalError) && t.getMessage().contains("invalid state")) {
                    continue;
                }
                Assert.fail(method.toString() + "-->" + t.getMessage());
            }
        }

    }
}
