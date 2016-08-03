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

package org.deeplearning4j.clustering.algorithm.strategy;

import org.deeplearning4j.clustering.algorithm.condition.ClusteringAlgorithmCondition;
import org.deeplearning4j.clustering.algorithm.condition.ConvergenceCondition;
import org.deeplearning4j.clustering.algorithm.condition.FixedIterationCountCondition;
import org.deeplearning4j.clustering.algorithm.iteration.IterationHistory;
import org.deeplearning4j.clustering.algorithm.optimisation.ClusteringOptimization;
import org.deeplearning4j.clustering.algorithm.optimisation.ClusteringOptimizationType;

public class OptimisationStrategy extends BaseClusteringStrategy {
	public static int									defaultIterationCount	= 100;

	private ClusteringOptimization	clusteringOptimisation;
	private ClusteringAlgorithmCondition clusteringOptimisationApplicationCondition;

	protected OptimisationStrategy(int initialClusterCount, String distanceFunction) {
		super(ClusteringStrategyType.OPTIMIZATION, initialClusterCount, distanceFunction, false);
	}

	public static OptimisationStrategy setup(int initialClusterCount,String distanceFunction) {
		return new OptimisationStrategy(initialClusterCount, distanceFunction);
	}
	
	public OptimisationStrategy optimize(ClusteringOptimizationType type, double value) {
		clusteringOptimisation = new ClusteringOptimization(type, value);
		return this;
	}
	
	public OptimisationStrategy optimizeWhenIterationCountMultipleOf(int value) {
		clusteringOptimisationApplicationCondition = FixedIterationCountCondition.iterationCountGreaterThan(value);
		return this;
	}
	
	public OptimisationStrategy optimizeWhenPointDistributionVariationRateLessThan(double rate) {
		clusteringOptimisationApplicationCondition = ConvergenceCondition.distributionVariationRateLessThan(rate);
		return this;
	}
	

	public double getClusteringOptimizationValue() {
		return clusteringOptimisation.getValue();
	}
	
	public boolean isClusteringOptimizationType(ClusteringOptimizationType type) {
		return clusteringOptimisation!=null && clusteringOptimisation.getType().equals(type);
	}

	public boolean isOptimizationDefined() {
		return clusteringOptimisation != null;
	}

	public boolean isOptimizationApplicableNow(IterationHistory iterationHistory) {
		return clusteringOptimisationApplicationCondition!=null && clusteringOptimisationApplicationCondition.isSatisfied(iterationHistory);
	}

}
