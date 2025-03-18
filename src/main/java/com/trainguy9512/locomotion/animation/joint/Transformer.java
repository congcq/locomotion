package com.trainguy9512.locomotion.animation.joint;

@FunctionalInterface
public interface Transformer<X> {
    void transform(JointChannel jointChannel, X value, JointChannel.TransformSpace transformSpace, JointChannel.TransformType transformType);
}
