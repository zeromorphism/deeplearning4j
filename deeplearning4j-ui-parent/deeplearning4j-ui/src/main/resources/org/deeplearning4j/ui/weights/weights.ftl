<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8"/>
    <title>Weights/Gradients/Score</title>
<style>
    .bar rect {
        fill: steelblue;
        shape-rendering: crispEdges;
    }

    .bar text {
        fill: #EFEFEF;
    }

    .area {
        fill: steelblue;
    }

    .axis path, .axis line {
        fill: none;
        stroke: #000;
        shape-rendering: crispEdges;
    }

    .tick line {
        opacity: 0.2;
        shape-rendering: crispEdges;
    }

    path {
        stroke: steelblue;
        stroke-width: 2;
        fill: none;
    }

    .legend {
        font-size: 12px;
        text-anchor: middle;
    }

    .brush .extent {
        stroke: #fff;
        fill-opacity: .125;
        shape-rendering: crispEdges;
    }

</style>

    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-2.2.0.min.js"></script>

    <link href='http://fonts.googleapis.com/css?family=Roboto:400,300' rel='stylesheet' type='text/css'>

    <!-- Latest compiled and minified CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" integrity="sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7" crossorigin="anonymous" />

    <!-- Optional theme -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.min.css" integrity="sha384-fLW2N01lMqjakBkx3l/M9EahuwpSfeNvV63J5ezn3uZzapT0u7EYsXMjQV+0En5r" crossorigin="anonymous" />

    <!-- Latest compiled and minified JavaScript -->
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js" integrity="sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS" crossorigin="anonymous"></script>

    <!-- d3 -->
    <script src="//d3js.org/d3.v3.min.js" charset="utf-8"></script>

    <script src="/assets/jquery-fileupload.js"></script>

    <!-- Booststrap Notify plugin-->
    <script src="/assets/bootstrap-notify.min.js"></script>

    <!-- Latest compiled and minified CSS -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.10.0/css/bootstrap-select.min.css">

    <!-- Latest compiled and minified JavaScript -->
    <script src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.10.0/js/bootstrap-select.min.js"></script>

    <!-- DateTime formatter-->
    <script src="/assets/DateTimeFormat.js"></script>

    <script src="/assets/renderWeightsProper.js"></script>

    <script src="/assets/common.js"></script>

    <style>
        body {
        font-family: 'Roboto', sans-serif;
        color: #333;
        font-weight: 300;
        font-size: 16px;
        }
        .hd {
        background-color: #000000;
        font-size: 18px;
        color: #FFFFFF;
        }
        .block {
        width: 250px;
        height: 350px;
        display: inline-block;
        border: 1px solid #DEDEDE;
        margin-right: 64px;
        }
        .hd-small {
        background-color: #000000;
        font-size: 14px;
        color: #FFFFFF;
        }
        .charts, .chart {
            font-size: 10px;
            font-color: #000000;
            position: relative;
        }
        .scoreboard {
            position: absolute;
            top:  20px;
            right: 10px;
            z-index: 1000;
            font-size: 11px;
        }
        .score {
            font-size: 11px;
        }
    </style>


</head>
<body>
<table style="width: 100%; padding: 5px;" class="hd">
    <tbody>
    <tr>
        <td style="width: 48px;"><a href="/"><img src="/assets/deeplearning4j.img" border="0"/></a></td>
        <td>DeepLearning4j UI</td>
        <td style="width: 512px; text-align: right;" class="hd-small">&nbsp; Available sessions: <select class="selectpicker" id="sessionSelector" onchange="window.location.href = 'weights?sid='+ this.options[this.selectedIndex].value ;" style="color: #000000; display: inline-block; width: 256px;">
            <option value="0" selected="selected">Pick a session to track</option>
        </select>&nbsp;&nbsp;
<script>
    buildSessionSelector("HISTOGRAM");
</script>
        </td>
        <td style="width: 256px;" class="hd-small">&nbsp;Updated at: <b><span id="updatetime">No updates so far</span></b>&nbsp;</td>
    </tr>
    </tbody>
</table>

<div style="width: 100%; text-align: center;">
    <div id="display" style="width: 1540px; height: 900px; text-align: left; background-color: #FFFFFF; display: inline-block; overflow: hidden; ">
        <div id="scores" style="background-color: #EFEFEF; display: block; float: left; width: 750px; height: 400px; border: 1px solid #CECECE; margin: 10px;">
            <h5>&nbsp;&nbsp;Score vs. Iteration #</h5>
            <div class="chart" id="schart">
            </div>
        </div>
        <div id="model" style="position: relative; background-color: #EFEFEF; display: block; float: left; width: 750px; height: 400px; border: 1px solid #CECECE; margin: 10px;">
            <h5>&nbsp;&nbsp;Model</h5>
            <div class="charts"></div>
            <div style="position: absolute; top: 5px; right: 5px;">
                <select id="modelSelector" onchange="selectModel();">
                </select>
            </div>
        </div>
        <div id="gradient" style="position: relative; background-color: #EFEFEF; display: block; float: left; width: 750px; height: 400px; border: 1px solid #CECECE; margin: 10px;">
            <h5>&nbsp;&nbsp;Gradient</h5>
            <div class="charts"></div>
            <div style="position: absolute; top: 5px; right: 5px;">
                <select id="gradientSelector" onchange="selectGradient();">
                </select>
            </div>
        </div>
        <div id="magnitudes" style="position: relative; background-color: #EFEFEF; display: block; float: left; width: 750px; height: 400px; border: 1px solid #CECECE; margin: 10px;">
            <h5>&nbsp;&nbsp;Mean Magnitudes: Parameters and Updates</h5>
            <div class="charts"></div>
            <div style="position: absolute; top: 5px; right: 5px;">
                <select id="magnitudeSelector" onchange="selectMagnitude();">
                </select>
            </div>
        </div>
        <!--<div id="lastupdate">
            <div class="updatetime">-1</div>
        </div>-->
    </div>

    <!--
    <div style="display: block;">
        nav bar
    </div> -->
</div>
<!--
<div id="score" style="display: inline-block; width: 650px; height: 400px; border: 1px solid #CECECE;">
    <h4>Score</h4>
    <div class="score"></div>
</div>-->

</body>
</html>