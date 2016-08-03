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

package org.deeplearning4j.nn.conf.layers;

import lombok.*;

/**
 *  Autoencoder.
 * Add Gaussian noise to input and learn
 * a reconstruction function.
 *
 */
@Data @NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AutoEncoder extends BasePretrainNetwork {
    protected double corruptionLevel;
    protected double sparsity;

    // Builder
    private AutoEncoder(Builder builder) {
    	super(builder);
        this.corruptionLevel = builder.corruptionLevel;
        this.sparsity = builder.sparsity;
    }

    @AllArgsConstructor
    public static class Builder extends BasePretrainNetwork.Builder<Builder> {
        private double corruptionLevel = 3e-1f;
        private double sparsity = 0f;

        public Builder() {}

        public Builder(double corruptionLevel) {
            this.corruptionLevel = corruptionLevel;
        }
        
        public Builder corruptionLevel(double corruptionLevel){
        	this.corruptionLevel = corruptionLevel;
        	return this;
        }
        
        public Builder sparsity(double sparsity){
        	this.sparsity = sparsity;
        	return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public AutoEncoder build() {
            return new AutoEncoder(this);
        }
    }
}
