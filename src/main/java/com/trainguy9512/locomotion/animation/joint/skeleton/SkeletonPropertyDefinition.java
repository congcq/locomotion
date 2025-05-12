package com.trainguy9512.locomotion.animation.joint.skeleton;

import java.util.Map;

public abstract class SkeletonPropertyDefinition<D> {

    protected final Map<String, D> jointProperties;
    protected final boolean isMirrored;
    protected final D defaultValue;

    protected SkeletonPropertyDefinition(Map<String, D> jointProperties, boolean mirrored, D defaultValue) {
        this.jointProperties = jointProperties;
        this.isMirrored = mirrored;
        this.defaultValue = defaultValue;
    }

    public D getProperty(String jointName, JointSkeleton skeleton) {
        if (!skeleton.containsJoint(jointName)) {
            return this.defaultValue;
        }
        if (this.isMirrored) {
            jointName = skeleton.getJointConfiguration(jointName).mirrorJoint();
        }
        if (!this.jointProperties.containsKey(jointName)) {
            return this.defaultValue;
        }
        return this.jointProperties.get(jointName);
    }

    public abstract SkeletonPropertyDefinition<D> getMirrored();
}
