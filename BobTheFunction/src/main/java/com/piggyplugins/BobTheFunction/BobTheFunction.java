package com.piggyplugins.BobTheFunction;

import java.util.*;



public class BobTheFunction {

    static Random rand = new Random();

    public static double RandStdDiv(double min, double max)
    {
        double u1 = 1.0 - rand.nextDouble();
        double u2 = 1.0 - rand.nextDouble();
        double randStdNormal = Math.sqrt(-2.0 * Math.log(u1)) * Math.sin(2.0 * Math.PI * u2);
        double randNormal = ((min + max) / 2.0) + ((max - min) / 5.0) * randStdNormal;

        return Math.max(Math.min(randNormal, max), min);
    }

    public static int GetRandTick(int min, int max)
    {
        return (int)(RandStdDiv(min, max) + 0.5);
    }


    //RandTickDescending returns a random number with the probability getting lower the closes to the max value it gets
    public static int GetRandTickDescending(int min, int max)
    {
        double newMin = (-(max - min) / 2.0) * 2.0;
        double newMax = ((max - min) / 2.0) * 2.0;

        return Math.abs((int)RandStdDiv(newMin, newMax)) + min;
    }

    //RandTickAscending returns a random number with the probability getting higher the closer to the max value it gets
    public static int GetRandTickAscending(int min, int max)
    {
        double newMin = (-(max - min) / 2.0) * 2.0;
        double newMax = ((max - min) / 2.0) * 2.0;

        return (max - Math.abs((int)RandStdDiv(newMin, newMax))) + min;
    }
}
