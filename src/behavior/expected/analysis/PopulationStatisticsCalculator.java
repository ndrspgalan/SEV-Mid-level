package behavior.expected.analysis;

import behavior.expected.profile.PopulationStatistics;
import java.util.*;

/** Calculates descriptive statistics over the full bounded population (variance divided by N). */
public final class PopulationStatisticsCalculator {
    public PopulationStatistics calculate(Collection<Double> source){
        Objects.requireNonNull(source);
        if(source.isEmpty())throw new IllegalArgumentException("population must not be empty");
        double[] values=source.stream().mapToDouble(v->{if(v==null||!Double.isFinite(v)||v<0)throw new IllegalArgumentException("invalid metric value");return v;}).sorted().toArray();
        int active=(int)Arrays.stream(values).filter(v->v>0).count();
        double mean=Arrays.stream(values).average().orElseThrow();
        double variance=Arrays.stream(values).map(v->(v-mean)*(v-mean)).sum()/values.length;
        return new PopulationStatistics(values.length,active,values.length-active,values[0],values[values.length-1],mean,
                percentile(values,50),mode(values),Math.sqrt(variance),percentile(values,25),percentile(values,50),percentile(values,75),percentile(values,75)-percentile(values,25));
    }
    private static double percentile(double[] values,double percentile){
        if(values.length==1)return values[0];
        double rank=(percentile/100d)*(values.length-1); int lower=(int)Math.floor(rank), upper=(int)Math.ceil(rank);
        if(lower==upper)return values[lower]; double fraction=rank-lower; return values[lower]+fraction*(values[upper]-values[lower]);
    }
    private static OptionalDouble mode(double[] values){
        double best=0; int bestCount=0; boolean tie=false;
        for(int i=0;i<values.length;){int j=i+1;while(j<values.length&&Double.compare(values[j],values[i])==0)j++;int count=j-i;
            if(count>bestCount){best=values[i];bestCount=count;tie=false;}else if(count==bestCount)tie=true;i=j;}
        return tie?OptionalDouble.empty():OptionalDouble.of(best);
    }
}
