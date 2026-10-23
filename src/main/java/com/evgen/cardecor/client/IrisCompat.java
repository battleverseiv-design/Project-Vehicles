package com.evgen.cardecor.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

public final class IrisCompat {
    private static final Logger LOGGER = LogManager.getLogger("CarDecor/IrisCompat");

    private static volatile boolean apiProbed = false;
    private static Object apiInstance = null;
    private static Method isInUseMethod = null;

    private static volatile boolean pipelineReflectionSetup = false;
    private static Method getPipelineManager = null;
    private static Method getPipelineNullable = null;
    private static Method getRenderTargetStateListener = null;
    private static Method setIsMainBound = null;

    private IrisCompat() {}

    public static boolean isOculusOrIrisInstalled() {
        if (!apiProbed) {
            probeApi();
        }
        return apiInstance != null;
    }

    public static boolean isShaderPackInUse() {
        if (!apiProbed) {
            probeApi();
        }
        if (apiInstance == null || isInUseMethod == null) {
            return false;
        }
        try {
            Object result = isInUseMethod.invoke(apiInstance);
            return Boolean.TRUE.equals(result);
        } catch (Throwable t) {
            return false;
        }
    }

    private static volatile boolean shadowActiveFieldSetup = false;
    private static java.lang.reflect.Field shadowRendererActiveField = null;

    public static boolean isRenderingShadowPass() {
        if (!shadowActiveFieldSetup) {
            setupShadowActiveField();
        }
        if (shadowRendererActiveField == null) {
            return false;
        }
        try {
            return shadowRendererActiveField.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    private static synchronized void setupShadowActiveField() {
        if (shadowActiveFieldSetup) return;
        shadowActiveFieldSetup = true;

        try {
            Class<?> clazz = Class.forName("net.irisshaders.iris.shadows.ShadowRenderer");
            shadowRendererActiveField = clazz.getField("ACTIVE");
        } catch (Throwable ignored) {
            shadowRendererActiveField = null;
        }
    }

    private static synchronized void probeApi() {
        if (apiProbed) return;
        apiProbed = true;

        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstanceMethod = apiClass.getMethod("getInstance");
            apiInstance = getInstanceMethod.invoke(null);
            isInUseMethod = apiClass.getMethod("isShaderPackInUse");
        } catch (Throwable ignored) {
            apiInstance = null;
            isInUseMethod = null;
        }
    }

    public static void suspendIrisShaderOverrides() {
        if (!pipelineReflectionSetup) {
            setupPipelineReflection();
        }
        if (getPipelineManager == null) return;
        setIsMainBoundReflectively(false);
    }

    public static void resumeIrisShaderOverrides() {
        if (!pipelineReflectionSetup) {
            setupPipelineReflection();
        }
        if (getPipelineManager == null) return;
        setIsMainBoundReflectively(true);
    }

    private static java.lang.reflect.Field renderTargetsField = null;
    private static Method getDepthTextureMethod = null;
    private static volatile boolean depthTextureReflectionSetup = false;

    public static int getIrisDepthTexture() {
        if (!depthTextureReflectionSetup) {
            setupDepthTextureReflection();
        }
        try {
            if (getPipelineManager == null) return -1;
            Object pipelineManager = getPipelineManager.invoke(null);
            if (pipelineManager == null) return -1;

            Object pipeline = getPipelineNullable.invoke(pipelineManager);
            if (pipeline == null) return -1;

            if (renderTargetsField == null) {
                Class<?> clazz = pipeline.getClass();
                while (clazz != null && clazz != Object.class) {
                    try {
                        renderTargetsField = clazz.getDeclaredField("renderTargets");
                        renderTargetsField.setAccessible(true);
                        break;
                    } catch (NoSuchFieldException e) {
                        clazz = clazz.getSuperclass();
                    }
                }
            }
            if (renderTargetsField == null) return -1;

            Object renderTargets = renderTargetsField.get(pipeline);
            if (renderTargets == null) return -1;

            if (getDepthTextureMethod == null) {
                getDepthTextureMethod = renderTargets.getClass().getMethod("getDepthTexture");
            }
            Object result = getDepthTextureMethod.invoke(renderTargets);
            if (result instanceof Number) {
                return ((Number) result).intValue();
            }
        } catch (Throwable t) {
            LOGGER.warn("[IrisCompat] Failed to get Iris depth texture: {}", t.getMessage());
        }
        return -1;
    }

    private static synchronized void setupDepthTextureReflection() {
        if (depthTextureReflectionSetup) return;
        depthTextureReflectionSetup = true;
        if (!pipelineReflectionSetup) {
            setupPipelineReflection();
        }
    }

    private static void setIsMainBoundReflectively(boolean bound) {
        try {
            if (getPipelineManager == null) return;
            Object pipelineManager = getPipelineManager.invoke(null);
            if (pipelineManager == null) return;

            Object pipeline = getPipelineNullable.invoke(pipelineManager);
            if (pipeline == null) return;

            Object listener = getRenderTargetStateListener.invoke(pipeline);
            if (listener == null) return;

            setIsMainBound.invoke(listener, bound);
        } catch (Throwable ignored) {}
    }

    private static synchronized void setupPipelineReflection() {
        if (pipelineReflectionSetup) return;
        pipelineReflectionSetup = true;

        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            getPipelineManager = irisClass.getMethod("getPipelineManager");

            Class<?> pipelineManagerClass = Class.forName("net.irisshaders.iris.pipeline.PipelineManager");
            getPipelineNullable = pipelineManagerClass.getMethod("getPipelineNullable");

            Class<?> pipelineClass = Class.forName("net.irisshaders.iris.pipeline.WorldRenderingPipeline");
            getRenderTargetStateListener = pipelineClass.getMethod("getRenderTargetStateListener");

            Class<?> listenerClass = Class.forName("net.irisshaders.iris.targets.RenderTargetStateListener");
            setIsMainBound = listenerClass.getMethod("setIsMainBound", boolean.class);
        } catch (Throwable t) {
            getPipelineManager = null;
            getPipelineNullable = null;
            getRenderTargetStateListener = null;
            setIsMainBound = null;
        }
    }

    public static boolean isRenderingWorld() {
        if (!pipelineReflectionSetup) {
            setupPipelineReflection();
        }
        try {
            if (getPipelineManager == null) return false;
            Object pipelineManager = getPipelineManager.invoke(null);
            if (pipelineManager == null) return false;

            Object pipeline = getPipelineNullable.invoke(pipelineManager);
            if (pipeline == null) return false;

            Class<?> clazz = pipeline.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    java.lang.reflect.Field field = clazz.getDeclaredField("isRenderingWorld");
                    field.setAccessible(true);
                    return field.getBoolean(pipeline);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
