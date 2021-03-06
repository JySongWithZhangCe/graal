/*
 * Copyright (c) 2017, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.core.jdk;

import java.awt.Toolkit;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(java.awt.Toolkit.class)
final class Target_java_awt_Toolkit {

    @Substitute
    private static Toolkit getDefaultToolkit() {
        throw new IllegalArgumentException("AWT is currently not supported on Substrate VM");
    }
}

/** Dummy class to have a class with the file's name. */
public final class JavaAWTSubstitutions {
}

/**
 * The purpose of this substitution is to prevent standard color profile loading deferral. The only
 * reason this substitution exists is to ensure that our fix for loading standard color profiles
 * works. It should be removed once standard color profile loading is tested.
 */
@TargetClass(className = "sun.java2d.cmm.ProfileDeferralMgr", onlyWith = JDK11OrLater.class)
final class Target_sun_java2d_cmm_ProfileDeferralMgr {

    @Alias//
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)//
    public static boolean deferring = false;

}
