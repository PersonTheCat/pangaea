package personthecat.pangaea.world.weight;

public interface TwoArgumentWeightFunction extends WeightFunction {
    WeightFunction argument1();
    WeightFunction argument2();
}
