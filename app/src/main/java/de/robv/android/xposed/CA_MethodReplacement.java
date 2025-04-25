/*
 * This file is part of AliuHook, a library providing XposedAPI bindings to LSPlant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 *
 * Originally written by rovo89 as part of the original Xposed
 * Copyright 2013 rovo89, Tungstwenty
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0
 */

package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XCallback;

/**
 * A special case of {@link CA_MethodHook} which completely replaces the original method.
 */
@SuppressWarnings({"unused", "JavaDoc", "RedundantThrows"})
public abstract class CA_MethodReplacement extends CA_MethodHook {
    /**
     * Creates a new callback with default priority.
     */
    public CA_MethodReplacement() {
        super();
    }

    /**
     * Creates a new callback with a specific priority.
     *
     * @param priority See {@link XCallback#priority}.
     */
    public CA_MethodReplacement(int priority) {
        super(priority);
    }

    /**
     * @hide
     */
    @Override
    protected final void beforeHookedMethod(MethodHookParam param) throws Throwable {
        try {
            Object result = replaceHookedMethod(param);
            param.setResult(result);
        } catch (Throwable t) {
            param.setThrowable(t);
        }
    }

    /**
     * @hide
     */
    @Override
    @SuppressWarnings("EmptyMethod")
    protected final void afterHookedMethod(MethodHookParam param) throws Throwable {
    }

    /**
     * Shortcut for replacing a method completely. Whatever is returned/thrown here is taken
     * instead of the result of the original method (which will not be called).
     *
     * <p>Note that implementations shouldn't call {@code super(param)}, it's not necessary.
     *
     * @param param Information about the method call.
     * @throws Throwable Anything that is thrown by the callback will be passed on to the original caller.
     */
    @SuppressWarnings("UnusedParameters")
    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;

    /**
     * Predefined callback that skips the method without replacements.
     */
    public static final CA_MethodReplacement DO_NOTHING = new CA_MethodReplacement(PRIORITY_HIGHEST * 2) {
        @Override
        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
            return null;
        }
    };

    /**
     * Creates a callback which always returns a specific value.
     *
     * @param result The value that should be returned to callers of the hooked method.
     */
    public static CA_MethodReplacement returnConstant(final Object result) {
        return returnConstant(PRIORITY_DEFAULT, result);
    }

    /**
     * Like {@link #returnConstant(Object)}, but allows to specify a priority for the callback.
     *
     * @param priority See {@link XCallback#priority}.
     * @param result   The value that should be returned to callers of the hooked method.
     */
    public static CA_MethodReplacement returnConstant(int priority, final Object result) {
        return new CA_MethodReplacement(priority) {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                return result;
            }
        };
    }

}