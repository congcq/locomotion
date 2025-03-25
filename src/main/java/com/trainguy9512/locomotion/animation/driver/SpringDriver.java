package com.trainguy9512.locomotion.animation.driver;

import com.trainguy9512.locomotion.LocomotionMain;
import com.trainguy9512.locomotion.util.Interpolator;
import org.joml.Vector3f;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class SpringDriver<D> extends VariableDriver<D> {

    private final float stiffness;
    private final float damping;
    private final float mass;

    private final BiFunction<D, D, D> addition;
    private final BiFunction<D, Float, D> multiplication;
    private final Function<D, D> floorDisplacement;
    private final boolean returnsDelta;

    private D currentTargetValue;
    private D previousTargetValue;
    private D velocity;

    protected SpringDriver(
            float stiffness,
            float damping,
            float mass,
            Supplier<D> initialValue,
            Interpolator<D> interpolator,
            BiFunction<D, D, D> addition,
            BiFunction<D, Float, D> multiplication,
            Function<D, D> floorDisplacement,
            boolean returnsDelta
    ) {
        super(initialValue, interpolator);
        this.stiffness = stiffness;
        this.damping = damping;
        this.mass = Math.max(mass, 0.1f);

        this.addition = addition;
        this.multiplication = multiplication;
        this.floorDisplacement = floorDisplacement;
        this.returnsDelta = returnsDelta;

        this.currentTargetValue = initialValue.get();
        this.previousTargetValue = initialValue.get();
        this.velocity = multiplication.apply(initialValue.get(), 0f);
    }

    public static <D> SpringDriver<D> of(float stiffness, float damping, float mass, Supplier<D> initialValue, Interpolator<D> interpolator, BiFunction<D, D, D> addition, BiFunction<D, Float, D> multiplication, Function<D, D> floorDisplacement, boolean returnsDelta) {
        return new SpringDriver<>(stiffness, damping, mass, initialValue, interpolator, addition, multiplication, floorDisplacement, returnsDelta);
    }

    public static SpringDriver<Float> ofFloat(float stiffness, float damping, float mass, Supplier<Float> initialValue, boolean returnsDelta) {
        return SpringDriver.of(stiffness, damping, mass, initialValue,
                Interpolator.FLOAT,
                Float::sum,
                (a, b) -> a * b,
                (a) -> (Math.round(a * 100f) / 100f),
                returnsDelta
        );
    }

    public static SpringDriver<Vector3f> ofVector(float stiffness, float damping, float mass, Supplier<Vector3f> initialValue, boolean returnsDelta) {
        return SpringDriver.of(stiffness, damping, mass, initialValue,
                Interpolator.VECTOR,
                (a, b) -> a.add(b, new Vector3f()),
                (a, b) -> a.mul(b, new Vector3f()),
                a -> {
                    if (Math.abs(a.x) < 0.01f) {
                        a.x = 0;
                    }
                    if (Math.abs(a.y) < 0.01f) {
                        a.y = 0;
                    }
                    if (Math.abs(a.z) < 0.01f) {
                        a.z = 0;
                    }
                    return a;
                },
                returnsDelta
        );
    }

    @Override
    public void setValue(D value) {
        this.currentTargetValue = value;
    }

    @Override
    public void pushCurrentToPrevious() {
        super.pushCurrentToPrevious();
        this.previousTargetValue = this.currentTargetValue;
    }

    @Override
    public void reset() {
        super.reset();
        this.previousTargetValue = this.initialValue.get();
        this.currentTargetValue = this.initialValue.get();
    }

    @Override
    public D getValueInterpolated(float partialTicks) {
        D interpolatedValue = super.getValueInterpolated(partialTicks);
        if (this.returnsDelta) {
            D targetInterpolatedValue = this.interpolator.interpolate(this.previousTargetValue, this.currentTargetValue, partialTicks);
            return this.addition.apply(targetInterpolatedValue, this.multiplication.apply(interpolatedValue, -1f));
        } else {
            return interpolatedValue;
        }
    }

    @Override
    public void tick() {
        super.tick();

        D displacement = this.addition.apply(this.currentValue, this.multiplication.apply(this.currentTargetValue, -1f));
        //displacement = this.floorDisplacement.apply(displacement);
        D springForce = this.multiplication.apply(displacement, -this.stiffness);
        D dampingForce = this.multiplication.apply(this.velocity, -this.damping);
        D acceleration = this.multiplication.apply(this.addition.apply(springForce, dampingForce), this.mass);

        this.velocity = this.addition.apply(this.velocity, acceleration);

        /*
        if (this.velocity instanceof Vector3f value) {
            if (Math.abs(((Vector3f)this.velocity).z) < 0.001) {
                ((Vector3f)this.velocity).z = 0;
                ((Vector3f) this.currentValue).z = ((Vector3f)this.currentTargetValue).z;
            }
        }
         */

        if (this.stiffness == 0.5f) {
            //LocomotionMain.LOGGER.info(this.velocity + "\t" + this.currentTargetValue);
        }

        this.currentValue = this.addition.apply(this.currentValue, this.velocity);
    }
}
