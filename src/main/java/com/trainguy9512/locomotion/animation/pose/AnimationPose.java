package com.trainguy9512.locomotion.animation.pose;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.trainguy9512.locomotion.animation.joint.JointSkeleton;
import com.trainguy9512.locomotion.animation.joint.JointChannel;
import org.joml.*;

import java.util.*;

public abstract class AnimationPose {

    protected final JointSkeleton jointSkeleton;
    protected final Map<String, JointChannel> jointChannels;
    private final Map<String, Matrix4f> jointParentMatrices;

    protected AnimationPose(JointSkeleton jointSkeleton){
        this.jointSkeleton = jointSkeleton;
        this.jointChannels = Maps.newHashMap();
        this.jointParentMatrices = Maps.newHashMap();

        for(String joint : jointSkeleton.getJoints()){
            this.getJointChannel(joint, JointChannel.ZERO);
        }
    }

    protected AnimationPose(AnimationPose animationPose){
        this.jointSkeleton = animationPose.jointSkeleton;
        this.jointChannels = new HashMap<>(animationPose.jointChannels);
        this.jointParentMatrices = new HashMap<>(animationPose.jointParentMatrices);
    }

    /**
     * Retrieves the animation pose's skeleton.
     * @return                      Joint skeleton
     */
    public JointSkeleton getJointSkeleton(){
        return this.jointSkeleton;
    }

    /**
     * Sets the transform for the supplied joint by its string identifier.
     * @param joint                 Joint string identifier
     * @param jointChannel        Joint transform
     */
    public void getJointChannel(String joint, JointChannel jointChannel){
        if(this.jointSkeleton.containsJoint(joint)){
            this.jointChannels.put(joint, jointChannel);
        }
    }

    /**
     * Retrieves a copy of the transform for the supplied joint.
     * @param joint                 Joint string identifier
     * @return                      Joint transform
     */
    public JointChannel getJointChannel(String joint){
        return JointChannel.of(this.jointChannels.getOrDefault(joint, JointChannel.ZERO));
    }

    protected void convertChildrenJointsToComponentSpace(String parent, PoseStack poseStack){
        JointChannel localParentJointChannel = this.getJointChannel(parent);

        poseStack.pushPose();
        poseStack.mulPose(localParentJointChannel.getTransform());

        this.getJointSkeleton().getDirectChildrenOfJoint(parent).ifPresent(children -> children.forEach(child -> this.convertChildrenJointsToComponentSpace(child, poseStack)));

        Matrix4f componentSpaceMatrix = new Matrix4f(poseStack.last().pose());
        this.jointParentMatrices.put(parent, componentSpaceMatrix);
        this.getJointChannel(parent, JointChannel.of(componentSpaceMatrix, localParentJointChannel.getVisibility()));
        poseStack.popPose();
    }

    protected void convertChildrenJointsToLocalSpace(String parent, Matrix4f parentMatrix){

        this.getJointSkeleton().getDirectChildrenOfJoint(parent).ifPresent(children -> children.forEach(child -> this.convertChildrenJointsToLocalSpace(child, this.jointParentMatrices.get(parent))));

        JointChannel parentJointChannel = this.getJointChannel(parent);
        parentJointChannel.multiply(parentMatrix.invert(new Matrix4f()), JointChannel.TransformSpace.LOCAL);
        this.getJointChannel(parent, parentJointChannel);
    }
}
