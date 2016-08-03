/*
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package org.deeplearning4j.clustering.algorithm.condition;

import org.deeplearning4j.clustering.algorithm.iteration.IterationHistory;
import org.nd4j.linalg.indexing.conditions.Condition;
import org.nd4j.linalg.indexing.conditions.LessThan;

public class VarianceVariationCondition implements ClusteringAlgorithmCondition {

	private Condition varianceVariationCondition;
	private int	period;
	
	
	protected VarianceVariationCondition(Condition varianceVariationCondition, int period) {
		super();
		this.varianceVariationCondition = varianceVariationCondition;
		this.period = period;
	}

	public static VarianceVariationCondition varianceVariationLessThan(double varianceVariation, int period) {
		Condition condition = new LessThan(varianceVariation);
		return new VarianceVariationCondition(condition, period);
	}

	
	public boolean isSatisfied(IterationHistory iterationHistory) {
		if( iterationHistory.getIterationCount()<=period )
			return false;
		
		for( int i=0, j=iterationHistory.getIterationCount();i<period;i++) {
			double variation = iterationHistory.getIterationInfo(j-i).getClusterSetInfo().getPointDistanceFromClusterVariance();
			variation -= iterationHistory.getIterationInfo(j-i-1).getClusterSetInfo().getPointDistanceFromClusterVariance();
			variation /= iterationHistory.getIterationInfo(j-i-1).getClusterSetInfo().getPointDistanceFromClusterVariance();
			if( !varianceVariationCondition.apply(variation) )
				return false;
		}
		
		return true;
	}

	

}
