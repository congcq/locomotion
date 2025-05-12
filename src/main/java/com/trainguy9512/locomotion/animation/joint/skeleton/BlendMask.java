package com.trainguy9512.locomotion.animation.joint.skeleton;

import com.google.common.collect.Maps;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.Set;

/**
 * Configuration that defines the weight influences for each joint, used when blending different poses together that shouldn't blend every joint.
 * <p>Default mask value for unassigned joints is 0.</p>
 */
public class BlendMask extends SkeletonPropertyDefinition<Float> {

    private BlendMask(Map<String, Float> jointProperties, boolean mirrored) {
        super(jointProperties, mirrored, 0f);
    }

    @Override
    public SkeletonPropertyDefinition<Float> getMirrored() {
        return new BlendMask(this.jointProperties, !this.isMirrored);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Float> jointWeights;

        public Builder() {
            jointWeights = Maps.newHashMap();
        }

        /**
         * Defines a weight for the provided joint.
         * @param jointName             Name of the joint
         * @param weight                Weight value between 0 and 1.
         */
        public Builder defineForJoint(String jointName, float weight) {
            this.jointWeights.put(jointName, Mth.clamp(weight, 0f, 1f));
            return this;
        }

        /**
         * Defines a duration multiplier for multiple joints.
         * @param jointNames            Set of joint names to assign the provided weight to.
         * @param weight                Weight value between 0 and 1.
         */
        public Builder defineForMultipleJoints(Set<String> jointNames, float weight) {
            jointNames.forEach(jointName -> this.defineForJoint(jointName, weight));
            return this;
        }


        public BlendMask build() {
            return new BlendMask(this.jointWeights, false);
        }
    }
}
