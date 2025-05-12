package com.trainguy9512.locomotion.animation.joint.skeleton;

import com.google.common.collect.Maps;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.Set;

/**
 * Configuration that allows individual joints to blend faster than others through duration multipliers.
 * @param jointDurationMultipliers      Map of joints to duration multipliers. Duration multipliers can be anywhere between 0 and 1.
 * @param isMirrored                    Whether the blend profile will be mirrored or not, used by {@link BlendProfile#ofMirrored}
 */
public class BlendProfile extends SkeletonPropertyDefinition<Float> {

    private BlendProfile(Map<String, Float> jointProperties, boolean mirrored) {
        super(jointProperties, mirrored, 1f);
    }

    @Override
    public SkeletonPropertyDefinition<Float> getMirrored() {
        return new BlendProfile(this.jointProperties, !this.isMirrored);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Float> jointDurationMultipliers;

        public Builder() {
            jointDurationMultipliers = Maps.newHashMap();
        }

        /**
         * Defines a duration multiplier for the provided joint.
         * @param jointName             Name of the joint
         * @param durationMultiplier    Float value between 0 and 1.
         */
        public Builder defineForJoint(String jointName, float durationMultiplier) {
            this.jointDurationMultipliers.put(jointName, Mth.clamp(durationMultiplier, 0.001f, 1f));
            return this;
        }

        /**
         * Defines a duration multiplier for multiple joints.
         * @param jointNames            Set of joint names to assign the duration multiplier.
         * @param durationMultiplier    Float value between 0 and 1.
         */
        public Builder defineForMultipleJoints(Set<String> jointNames, float durationMultiplier) {
            jointNames.forEach(jointName -> this.defineForJoint(jointName, durationMultiplier));
            return this;
        }


        public BlendProfile build() {
            return new BlendProfile(this.jointDurationMultipliers, false);
        }
    }
}
