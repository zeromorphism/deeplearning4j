<!DOCTYPE html>
<html>
    <head>
        <title>DeepLearning4j UI</title>
        <meta charset="utf-8" />

        <!-- jQuery -->
        <script src="https://code.jquery.com/jquery-2.2.0.min.js"></script>

        <link href='http://fonts.googleapis.com/css?family=Roboto:400,300' rel='stylesheet' type='text/css'>

        <!-- Latest compiled and minified CSS -->
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" integrity="sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7" crossorigin="anonymous" />

        <!-- Optional theme -->
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap-theme.min.css" integrity="sha384-fLW2N01lMqjakBkx3l/M9EahuwpSfeNvV63J5ezn3uZzapT0u7EYsXMjQV+0En5r" crossorigin="anonymous" />

        <!-- Latest compiled and minified JavaScript -->
        <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js" integrity="sha384-0mSbJDEHialfmuBBQP6A4Qrprq5OVfW37PRR3j5ELqxss1yVqOtnepnHVP9aJ7xS" crossorigin="anonymous"></script>

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

                    margin-right: 64px;
            }
            .hd-small {
                    background-color: #000000;
                    font-size: 14px;
                    color: #FFFFFF;
                }
        </style>

        <!-- Booststrap Notify plugin-->
        <script src="./assets/bootstrap-notify.min.js"></script>

        <script src="./assets/default.js"></script>
    </head>
    <body>
    <table style="width: 100%; padding: 5px;" class="hd">
        <tbody>
            <tr>
                <td style="width: 48px;"><img src="./assets/deeplearning4j.img"  border="0"/></td>
                <td>DeepLearning4j UI</td>
                <td style="width: 128px;">&nbsp; <!-- placeholder for future use --></td>
            </tr>
        </tbody>
    </table>

    <br />
    <br />
    <br />
<!--
    Here we should provide nav to available modules:
    T-SNE visualization
    NN activations
    HistogramListener renderer
 -->
<div style="width: 100%; text-align: center">
    <div class="block">
        <!-- TSNE block. It's session-dependant. -->
        <b>T-SNE</b><br/><br/>
        <a href="#"  onclick="trackSessionHandle('TSNE','tsne'); return false;"><img src="./assets/i_plot.img" border="0" style="opacity: 1.0" id="TSNE"/></a><br/><br/>
        <div style="text-align: left; margin: 5px;">
            &nbsp;Plot T-SNE data uploaded by user or retrieved from DL4j.
        </div>
    </div>

    <div class="block">
        <!-- W2V block -->
        <b>WordVectors</b><br/><br/>
        <a href="./word2vec"><img src="./assets/i_nearest.img" border="0" /></a><br/><br/>
        <div style="text-align: left; margin: 5px;">
            &nbsp;wordsNearest UI for WordVectors (GloVe/Word2Vec compatible)
        </div>
    </div>

    <div class="block">
        <b>Activations</b><br/><br/>
        <a href="#"  onclick="trackSessionHandle('ACTIVATIONS','activations'); return false;"><img src="./assets/i_ladder.img" border="0"  style="opacity: 0.2" id="ACTIVATIONS" /></a><br/><br/>
        <div style="text-align: left; margin: 5px;">
            &nbsp;Activations retrieved from Convolution Neural network.
        </div>
    </div>

    <div class="block">
        <!-- Histogram block. It's session-dependant block -->
        <b>Histograms &amp; Score</b><br/><br/>
        <a href="#" onclick="trackSessionHandle('HISTOGRAM','weights'); return false;"><img id="HISTOGRAM" src="./assets/i_histo.img" border="0" style="opacity: 0.2"/></a><br/><br/>
        <div style="text-align: left; margin: 5px;">
            &nbsp;Neural network scores retrieved from DL4j during training.
        </div>
    </div>

    <div class="block">
        <!-- Flow  block. It's session-dependant block -->
        <b>Model flow</b><br/><br/>
        <a href="#" onclick="trackSessionHandle('FLOW','flow'); return false;"><img id="FLOW" src="./assets/i_flow.img" border="0" style="opacity: 0.2"/></a><br/><br/>
        <div style="text-align: left; margin: 5px;">
            &nbsp;MultiLayerNetwork/ComputationalGraph model state rendered.
        </div>
    </div>

    <div class="block">
        <!-- Arbiter block. It's session-dependant block -->
        <b>Arbiter </b><br/><br/>
        <a href="#" onclick="trackSessionHandle('ARBITER','arbiter'); return false;"><img id="ARBITER" src="./assets/i_arbiter.img" border="0" style="opacity: 0.2"/></a><br/><br/>
        <div style="text-align: left; margin: 5px;">
            &nbsp;State &amp; management for Arbiter optimization processes.
        </div>
    </div>
</div>

    <div  id="sessionSelector" style="position: fixed; top: 0px; bottom: 0px; left: 0px; right: 0px; z-index: 95; display: none;">
        <div style="position: fixed; top: 50%; left: 50%; -webkit-transform: translate(-50%, -50%); transform: translate(-50%, -50%); z-index: 100;   background-color: rgba(255, 255, 255,255); border: 1px solid #CECECE; height: 400px; width: 300px; -moz-box-shadow: 0 0 3px #ccc; -webkit-box-shadow: 0 0 3px #ccc; box-shadow: 0 0 3px #ccc;">

                <table class="table table-hover" style="margin-left: 10px; margin-right: 10px; margin-top: 5px; margin-bottom: 5px;">
                    <thead style="display: block; margin-bottom: 3px; width: 100%;">
                        <tr style="width: 100%">
                            <th style="width: 100%">Available sessions</th>
                        </tr>
                    </thead>
                    <tbody id="sessionList" style="display: block; width: 95%; height: 300px; overflow-y: auto; overflow-x: hidden;">

                    </tbody>
                </table>

            <div style="display: inline-block; position: fixed; bottom: 3px; left: 50%;  -webkit-transform: translate(-50%); transform: translate(-50%); ">
                <input type="button" class="btn btn-default" style="" value=" Cancel " onclick="$('#sessionSelector').css('display','none');"/>
            </div>
        </div>
    </div>
    </body>
</html>