/*
 *
 *  * Copyright 2016 Skymind,Inc.
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
package org.deeplearning4j.ui.components.chart;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.deeplearning4j.ui.components.chart.style.StyleChart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Line chart with multiple independent series
 *
 * @author Alex Black
 */
@Data @EqualsAndHashCode(callSuper=true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChartLine extends Chart {
    public static final String COMPONENT_TYPE = "ChartLine";

    private List<double[]> x;
    private List<double[]> y;
    private List<String> seriesNames;

    private ChartLine(Builder builder){
        super(COMPONENT_TYPE, builder);
        x = builder.x;
        y = builder.y;
        seriesNames = builder.seriesNames;
    }

    public ChartLine(){
        super(COMPONENT_TYPE);
        //no-arg constructor for Jackson
    }


    public static class Builder extends Chart.Builder<Builder> {
        private List<double[]> x = new ArrayList<>();
        private List<double[]> y = new ArrayList<>();
        private List<String> seriesNames = new ArrayList<>();
        private boolean showLegend = true;


        public Builder(String title, StyleChart style){
            super(title,style);
        }

        public Builder addSeries(String seriesName, double[] xValues, double[] yValues){
            x.add(xValues);
            y.add(yValues);
            seriesNames.add(seriesName);
            return this;
        }

        public ChartLine build(){
            return new ChartLine(this);
        }

    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("ChartLine(x=[");
        boolean first = true;
        if(x != null) {
            for (double[] d : x) {
                if (!first) sb.append(",");
                sb.append(Arrays.toString(d));
                first = false;
            }
        }
        sb.append("],y=[");
        first = true;
        if(y != null) {
            for (double[] d : y) {
                if (!first) sb.append(",");
                sb.append(Arrays.toString(d));
                first = false;
            }
        }
        sb.append("],seriesNames=");
        if(seriesNames != null) sb.append(seriesNames);
        sb.append(")");
        return sb.toString();
    }

}
