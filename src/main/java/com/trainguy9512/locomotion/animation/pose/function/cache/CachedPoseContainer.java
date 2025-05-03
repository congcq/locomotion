package com.trainguy9512.locomotion.animation.pose.function.cache;

import com.google.common.collect.Maps;
import com.trainguy9512.locomotion.animation.pose.LocalSpacePose;
import com.trainguy9512.locomotion.animation.pose.function.PoseFunction;

import java.util.Map;
import java.util.Optional;

public class CachedPoseContainer {

    private final Map<String, CachedPoseFunction> cachedPoseFunctions;

    private CachedPoseContainer() {
        this.cachedPoseFunctions = Maps.newHashMap();
    }

    public static CachedPoseContainer of() {
        return new CachedPoseContainer();
    }

    public void register(String identifier, PoseFunction<LocalSpacePose> poseFunction, boolean resetsUponRelevant) {
        if(this.cachedPoseFunctions.containsKey(identifier)){
            throw new IllegalArgumentException("Failed to register saved cached pose for identifier " + identifier + " due to it being already registered.");
        } else {
            this.cachedPoseFunctions.put(identifier, CachedPoseFunction.of(poseFunction, resetsUponRelevant));
        }
    }

    public PoseFunction<LocalSpacePose> getOrThrow(String identifier) {
        return Optional.ofNullable(this.cachedPoseFunctions.get(identifier)).orElseThrow(() -> new IllegalStateException("Missing saved cached pose for identifier " + identifier + ". Maybe it's being accessed before it has been defined?"));
    }

    public void clearCaches() {
        this.cachedPoseFunctions.values().forEach(CachedPoseFunction::clearCache);
    }
}
