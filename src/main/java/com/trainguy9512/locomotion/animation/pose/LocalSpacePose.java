package com.trainguy9512.locomotion.animation.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trainguy9512.locomotion.animation.joint.skeleton.BlendMask;
import com.trainguy9512.locomotion.animation.joint.skeleton.BlendProfile;
import com.trainguy9512.locomotion.animation.joint.skeleton.JointSkeleton;
import com.trainguy9512.locomotion.animation.joint.JointChannel;
import com.trainguy9512.locomotion.util.TimeSpan;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class LocalSpacePose extends Pose {

    private LocalSpacePose(JointSkeleton jointSkeleton) {
        super(jointSkeleton);
    }

    private LocalSpacePose(Pose pose) {
        super(pose);
    }


    /**
     * Creates a blank animation pose using a joint skeleton as the template.
     * @param jointSkeleton         Template joint skeleton
     * @return                      New animation pose
     */
    public static LocalSpacePose of(JointSkeleton jointSkeleton) {
        return new LocalSpacePose(jointSkeleton);
    }


    public static LocalSpacePose of(Pose pose) {
        return new LocalSpacePose(pose);
    }

    /**
     * Creates a local space pose from this component space pose.
     */
    public ComponentSpacePose convertedToComponentSpace() {
        ComponentSpacePose pose = ComponentSpacePose.of(this);
        pose.convertChildrenJointsToComponentSpace(this.getJointSkeleton().getRootJoint(), new PoseStack());
        return pose;
    }

    /**
     * Creates an animation pose from a point in time within the provided animation sequence
     * @param jointSkeleton         Template joint skeleton
     * @param sequenceLocation      Animation sequence resource location
     * @param time                  Point of time in the animation to get.
     * @param looping               Whether the animation should be looped or not.
     * @return                      New animation pose
     */
    public static LocalSpacePose fromAnimationSequence(JointSkeleton jointSkeleton, ResourceLocation sequenceLocation, TimeSpan time, boolean looping) {
        LocalSpacePose pose = LocalSpacePose.of(jointSkeleton);
        for(String joint : jointSkeleton.getJoints()){
            pose.setJointChannel(joint, JointChannel.ofJointFromAnimationSequence(sequenceLocation, joint, time, looping));
        }
        return pose;
    }

    public LocalSpacePose mirrored() {
        LocalSpacePose mirroredPose = new LocalSpacePose(this);
        this.jointChannels.keySet().forEach(joint -> {
            JointSkeleton.JointConfiguration configuration = this.getJointSkeleton().getJointConfiguration(joint);
            String mirrorJoint = configuration.mirrorJoint() != null ? configuration.mirrorJoint() : joint;
            JointChannel mirroredTransform = this.getJointChannel(mirrorJoint).mirrored();
            mirroredPose.setJointChannel(joint, mirroredTransform);
        });
        return mirroredPose;
    }

    /**
     * Returns an animation pose interpolated between this pose and the provided pose.
     * @param other     Animation pose to interpolate to.
     * @param weight    Weight value, 0 is the original pose and 1 is the other pose.
     * @param destination       Pose to save interpolated pose onto.
     * @return          New interpolated animation pose.
     */
    public LocalSpacePose interpolated(
            LocalSpacePose other,
            float weight,
            LocalSpacePose destination
    ) {
        return this.interpolated(other, weight, null, destination);
    }

    /**
     * Returns this animation pose interpolated between this pose and the provided pose.
     * @param other     Animation pose to interpolate to.
     * @param weight    Weight value, 0 is the original pose and 1 is the other pose.
     * @return          New interpolated animation pose.
     */
    public LocalSpacePose interpolated(
            LocalSpacePose other,
            float weight
    ) {
        return this.interpolated(other, weight, this);
    }

    /**
     * Returns an animation pose interpolated between this pose and the provided pose.
     * @param other             Animation pose to interpolate to.
     * @param weight            Weight value, 0 is the original pose and 1 is the other pose.
     * @param blendMask         Blend mask
     * @param destination       Pose to save interpolated pose onto.
     * @return                  New interpolated animation pose.
     */
    public LocalSpacePose interpolated(
            LocalSpacePose other,
            float weight,
            @Nullable BlendMask blendMask,
            LocalSpacePose destination
    ) {
        if (weight == 0) {
            return destination;
        }
        for (String joint : this.jointSkeleton.getJoints()) {
            float jointWeight = weight;
            if (blendMask != null) {
                jointWeight *= blendMask.getProperty(joint, this.jointSkeleton);
            }
            if (jointWeight == 1f) {
                destination.setJointChannel(joint, other.getJointChannel(joint));
            } else {
                destination.setJointChannel(joint, destination.getJointChannel(joint).interpolate(other.getJointChannel(joint), jointWeight));
            }
        }
        return destination;
    }

    /**
     * Returns this animation pose interpolated between this pose and the provided pose.
     * @param other             Animation pose to interpolate to.
     * @param weight            Weight value, 0 is the original pose and 1 is the other pose.
     * @param blendMask         Blend mask
     * @return                  New interpolated animation pose.
     */
    public LocalSpacePose interpolated(
            LocalSpacePose other,
            float weight,
            @Nullable BlendMask blendMask
    ) {
        return this.interpolated(other, weight, blendMask, this);
    }

    /**
     * Returns an animation pose interpolated between this pose and the provided pose using data from a transition.
     * @param other             Animation pose to interpolate to.
     * @param time              Time progress between 0 and 1
     * @param destination       Pose to save interpolated pose onto.
     * @return                  New interpolated animation pose.
     */
    public LocalSpacePose interpolatedByTransition(
            LocalSpacePose other,
            float time,
            LocalSpacePose destination
    ) {
        if (time == 0) {
            return destination;
        }
        return destination;
    }

    public void multiply(LocalSpacePose other, JointChannel.TransformSpace transformSpace) {
        this.jointChannels.forEach((joint, channel) -> channel.multiply(other.jointChannels.get(joint), transformSpace));
    }

    public void invert() {
        this.jointChannels.values().forEach(JointChannel::invert);
    }
}
