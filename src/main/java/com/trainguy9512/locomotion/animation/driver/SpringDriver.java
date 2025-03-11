package com.trainguy9512.locomotion.animation.driver;

import com.trainguy9512.locomotion.util.Interpolator;
import net.minecraft.util.Mth;

import java.util.function.Function;
import java.util.function.Supplier;

public class SpringDriver<D> extends VariableDriver<D> {

    private final float stiffness;
    private final float damping;
    private final float mass;

    private float targetValue;
    private float velocity;

    protected SpringDriver(float stiffness, float damping, float mass, Supplier<Float> initialValue, ) {
        super(initialValue, Interpolator.FLOAT);
        this.stiffness = stiffness;
        this.damping = damping;
        this.mass = Math.max(mass, 0.1f);
        this.targetValue = initialValue.get();
        this.velocity = 0;

    }

    public static SpringDriver of(float stiffness, float damping, float mass, Supplier<Float> initialValue){
        return new SpringDriver(stiffness, damping, mass, initialValue);

        Function<Float, Float> applyVelocity = velocity -> {
            return currentValue + velocity * 0.5f;
        };
    }

    @Override
    public void setValue(Float value){
        this.targetValue = value;
    }

    @Override
    public void reset(){
        super.reset();
        this.targetValue = this.initialValue.get();
    }

    @Override
    public void tick() {
        super.tick();

        float springForce = -this.stiffness * (this.currentValue - this.targetValue);
        float dampingForce = -this.damping * this.velocity;
        float acceleration = (springForce + dampingForce) / this.mass;

        this.velocity += acceleration;
        this.currentValue += this.velocity;
    }
}
