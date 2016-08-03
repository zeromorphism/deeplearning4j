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

package org.deeplearning4j.models.paragraphvectors;


import lombok.NonNull;
import org.datavec.api.util.ClassPathResource;
import org.deeplearning4j.models.embeddings.learning.impl.sequence.DM;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.AbstractCache;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.InMemoryLookupCache;
import org.deeplearning4j.text.documentiterator.FileLabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelledDocument;
import org.deeplearning4j.text.documentiterator.LabelsSource;
import org.deeplearning4j.text.sentenceiterator.AggregatingSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.FileSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.deeplearning4j.util.SerializationUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by agibsonccc on 12/3/14.
 */
public class ParagraphVectorsTest {
    private static final Logger log = LoggerFactory.getLogger(ParagraphVectorsTest.class);

    @Before
    public void before() {
        new File("word2vec-index").delete();
    }


/*
    @Test
    public void testWord2VecRunThroughVectors() throws Exception {
        ClassPathResource resource = new ClassPathResource("/big/raw_sentences.txt");
        File file = resource.getFile().getParentFile();
        LabelAwareSentenceIterator iter = LabelAwareUimaSentenceIterator.createWithPath(file.getAbsolutePath());


        TokenizerFactory t = new UimaTokenizerFactory();


        ParagraphVectors vec = new ParagraphVectors.Builder()
                .minWordFrequency(1).iterations(5).labels(Arrays.asList("label1", "deeple"))
                .layerSize(100)
                .stopWords(new ArrayList<String>())
                .windowSize(5).iterate(iter).tokenizerFactory(t).build();

        assertEquals(new ArrayList<String>(), vec.getStopWords());


        vec.fit();
        double sim = vec.similarity("day","night");
        log.info("day/night similarity: " + sim);
        new File("cache.ser").delete();

    }
*/

    /**
     * This test checks, how vocab is built using SentenceIterator provided, without labels.
     *
     * @throws Exception
     */
    @Test
    public void testParagraphVectorsVocabBuilding1() throws Exception {
        ClassPathResource resource = new ClassPathResource("/big/raw_sentences.txt");
        File file = resource.getFile();//.getParentFile();
        SentenceIterator iter = new BasicLineIterator(file); //UimaSentenceIterator.createWithPath(file.getAbsolutePath());

        int numberOfLines = 0;
        while (iter.hasNext()) {
            iter.nextSentence();
            numberOfLines++;
        }

        iter.reset();

        InMemoryLookupCache cache = new InMemoryLookupCache(false);

        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());

       // LabelsSource source = new LabelsSource("DOC_");

        ParagraphVectors vec = new ParagraphVectors.Builder()
                .minWordFrequency(1).iterations(5)
                .layerSize(100)
          //      .labelsGenerator(source)
                .windowSize(5)
                .iterate(iter)
                .vocabCache(cache)
                .tokenizerFactory(t)
                .build();

        vec.buildVocab();

        LabelsSource source = vec.getLabelsSource();


        //VocabCache cache = vec.getVocab();
        log.info("Number of lines in corpus: " + numberOfLines);
        assertEquals(numberOfLines, source.getLabels().size());
        assertEquals(97162, source.getLabels().size());

        assertNotEquals(null, cache);
        assertEquals(97406, cache.numWords());

        // proper number of words for minWordsFrequency = 1 is 244
        assertEquals(244, cache.numWords() - source.getLabels().size());
    }

    @Test
    public void testParagraphVectorsModelling1() throws Exception {
        ClassPathResource resource = new ClassPathResource("/big/raw_sentences.txt");
        File file = resource.getFile();
        SentenceIterator iter = new BasicLineIterator(file);

        InMemoryLookupCache cache = new InMemoryLookupCache(false);

        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());

        LabelsSource source = new LabelsSource("DOC_");

        ParagraphVectors vec = new ParagraphVectors.Builder()
                .minWordFrequency(1)
                .iterations(3)
                .epochs(1)
                .layerSize(100)
                .learningRate(0.025)
                .labelsSource(source)
                .windowSize(5)
                .iterate(iter)
                .trainWordVectors(true)
                .vocabCache(cache)
                .tokenizerFactory(t)
                .sampling(0)
                .build();

        vec.fit();


        int cnt1 = cache.wordFrequency("day");
        int cnt2 = cache.wordFrequency("me");

        assertNotEquals(1, cnt1);
        assertNotEquals(1, cnt2);
        assertNotEquals(cnt1, cnt2);

        assertEquals(97406, cache.numWords());

        assertTrue(vec.hasWord("DOC_16392"));
        assertTrue(vec.hasWord("DOC_3720"));

        /*
            We have few lines that contain pretty close words invloved.
            These sentences should be pretty close to each other in vector space
         */
        // line 3721: This is my way .
        // line 6348: This is my case .
        // line 9836: This is my house .
        // line 12493: This is my world .
        // line 16393: This is my work .

        // this is special sentence, that has nothing common with previous sentences
        // line 9853: We now have one .

        double similarityD = vec.similarity("day", "night");
        log.info("day/night similarity: " + similarityD);

        if (similarityD < 0.0) {
            log.info("Day: " + Arrays.toString(vec.getWordVectorMatrix("day").dup().data().asDouble()));
            log.info("Night: " + Arrays.toString(vec.getWordVectorMatrix("night").dup().data().asDouble()));
        }


        List<String> labelsOriginal = vec.labelsSource.getLabels();

        double similarityW = vec.similarity("way", "work");
        log.info("way/work similarity: " + similarityW);

        double similarityH = vec.similarity("house", "world");
        log.info("house/world similarity: " + similarityH);

        double similarityC = vec.similarity("case", "way");
        log.info("case/way similarity: " + similarityC);

        double similarity1 = vec.similarity("DOC_9835", "DOC_12492");
        log.info("9835/12492 similarity: " + similarity1);
//        assertTrue(similarity1 > 0.7d);

        double similarity2 = vec.similarity("DOC_3720", "DOC_16392");
        log.info("3720/16392 similarity: " + similarity2);
//        assertTrue(similarity2 > 0.7d);

        double similarity3 = vec.similarity("DOC_6347", "DOC_3720");
        log.info("6347/3720 similarity: " + similarity3);
//        assertTrue(similarity2 > 0.7d);

        // likelihood in this case should be significantly lower
        double similarityX = vec.similarity("DOC_3720", "DOC_9852");
        log.info("3720/9852 similarity: " + similarityX);
        assertTrue(similarityX < 0.5d);

        File tempFile = File.createTempFile("paravec", "ser");
        tempFile.deleteOnExit();

        INDArray day = vec.getWordVectorMatrix("day").dup();

        /*
            Testing txt serialization
         */
        File tempFile2 = File.createTempFile("paravec", "ser");
        tempFile2.deleteOnExit();

        WordVectorSerializer.writeWordVectors(vec, tempFile2);

        ParagraphVectors vec3 = WordVectorSerializer.readParagraphVectorsFromText(tempFile2);

        INDArray day3 = vec3.getWordVectorMatrix("day").dup();

        List<String> labelsRestored = vec3.labelsSource.getLabels();

        assertEquals(day, day3);

        assertEquals(labelsOriginal.size(), labelsRestored.size());

           /*
            Testing binary serialization
         */
        SerializationUtils.saveObject(vec, tempFile);


        ParagraphVectors vec2 = (ParagraphVectors) SerializationUtils.readObject(tempFile);
        INDArray day2 = vec2.getWordVectorMatrix("day").dup();

        List<String> labelsBinary = vec2.labelsSource.getLabels();

        assertEquals(day, day2);

        tempFile.delete();


        assertEquals(labelsOriginal.size(), labelsBinary.size());
    }


    @Test
    public void testParagraphVectorsDM() throws Exception {
        ClassPathResource resource = new ClassPathResource("/big/raw_sentences.txt");
        File file = resource.getFile();
        SentenceIterator iter = new BasicLineIterator(file);

//        InMemoryLookupCache cache = new InMemoryLookupCache(false);
        AbstractCache<VocabWord> cache = new AbstractCache.Builder<VocabWord>().build();

        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());

        LabelsSource source = new LabelsSource("DOC_");

        ParagraphVectors vec = new ParagraphVectors.Builder()
                .minWordFrequency(1)
                .iterations(3)
                .epochs(1)
                .layerSize(100)
                .learningRate(0.025)
                .labelsSource(source)
                .windowSize(5)
                .iterate(iter)
                .trainWordVectors(true)
                .vocabCache(cache)
                .tokenizerFactory(t)
                .sampling(0)
                .sequenceLearningAlgorithm(new DM<VocabWord>())
                .build();

        vec.fit();


        int cnt1 = cache.wordFrequency("day");
        int cnt2 = cache.wordFrequency("me");

        assertNotEquals(1, cnt1);
        assertNotEquals(1, cnt2);
        assertNotEquals(cnt1, cnt2);

        double similarity1 = vec.similarity("DOC_9835", "DOC_12492");
        log.info("9835/12492 similarity: " + similarity1);
//        assertTrue(similarity1 > 0.2d);

        double similarity2 = vec.similarity("DOC_3720", "DOC_16392");
        log.info("3720/16392 similarity: " + similarity2);
  //      assertTrue(similarity2 > 0.2d);

        double similarity3 = vec.similarity("DOC_6347", "DOC_3720");
        log.info("6347/3720 similarity: " + similarity3);
//        assertTrue(similarity3 > 0.6d);

        double similarityX = vec.similarity("DOC_3720", "DOC_9852");
        log.info("3720/9852 similarity: " + similarityX);
        assertTrue(similarityX < 0.5d);

    }


    @Test
    public void testParagraphVectorsWithWordVectorsModelling1() throws Exception {
        ClassPathResource resource = new ClassPathResource("/big/raw_sentences.txt");
        File file = resource.getFile();
        SentenceIterator iter = new BasicLineIterator(file);

//        InMemoryLookupCache cache = new InMemoryLookupCache(false);
        AbstractCache<VocabWord> cache = new AbstractCache.Builder<VocabWord>().build();

        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());

        LabelsSource source = new LabelsSource("DOC_");

        ParagraphVectors vec = new ParagraphVectors.Builder()
                .minWordFrequency(1)
                .iterations(3)
                .epochs(1)
                .layerSize(100)
                .learningRate(0.025)
                .labelsSource(source)
                .windowSize(5)
                .iterate(iter)
                .trainWordVectors(true)
                .vocabCache(cache)
                .tokenizerFactory(t)
                .sampling(0)
                .build();

        vec.fit();


        int cnt1 = cache.wordFrequency("day");
        int cnt2 = cache.wordFrequency("me");

        assertNotEquals(1, cnt1);
        assertNotEquals(1, cnt2);
        assertNotEquals(cnt1, cnt2);

        /*
            We have few lines that contain pretty close words invloved.
            These sentences should be pretty close to each other in vector space
         */
        // line 3721: This is my way .
        // line 6348: This is my case .
        // line 9836: This is my house .
        // line 12493: This is my world .
        // line 16393: This is my work .

        // this is special sentence, that has nothing common with previous sentences
        // line 9853: We now have one .

        assertTrue(vec.hasWord("DOC_3720"));

        double similarityD = vec.similarity("day", "night");
        log.info("day/night similarity: " + similarityD);

        double similarityW = vec.similarity("way", "work");
        log.info("way/work similarity: " + similarityW);

        double similarityH = vec.similarity("house", "world");
        log.info("house/world similarity: " + similarityH);

        double similarityC = vec.similarity("case", "way");
        log.info("case/way similarity: " + similarityC);

        double similarity1 = vec.similarity("DOC_9835", "DOC_12492");
        log.info("9835/12492 similarity: " + similarity1);
//        assertTrue(similarity1 > 0.7d);

        double similarity2 = vec.similarity("DOC_3720", "DOC_16392");
        log.info("3720/16392 similarity: " + similarity2);
//        assertTrue(similarity2 > 0.7d);

        double similarity3 = vec.similarity("DOC_6347", "DOC_3720");
        log.info("6347/3720 similarity: " + similarity3);
//        assertTrue(similarity2 > 0.7d);

        // likelihood in this case should be significantly lower
        // however, since corpus is small, and weight initialization is random-based, sometimes this test CAN fail
        double similarityX = vec.similarity("DOC_3720", "DOC_9852");
        log.info("3720/9852 similarity: " + similarityX);
        assertTrue(similarityX < 0.5d);


        double sim119 = vec.similarityToLabel("This is my case .", "DOC_6347");
        double sim120 = vec.similarityToLabel("This is my case .", "DOC_3720");
        log.info("1/2: " + sim119 + "/" + sim120);
        //assertEquals(similarity3, sim119, 0.001);
    }


    /**
     * This test is not indicative.
     * there's no need in this test within travis, use it manually only for problems detection
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testParagraphVectorsReducedLabels1() throws Exception {
        ClassPathResource resource = new ClassPathResource("/labeled");
        File file = resource.getFile();

        LabelAwareIterator iter = new FileLabelAwareIterator.Builder()
                .addSourceFolder(file)
                .build();

        TokenizerFactory t = new DefaultTokenizerFactory();

        /**
         * Please note: text corpus is REALLY small, and some kind of "results" could be received with HIGH epochs number, like 30.
         * But there's no reason to keep at that high
         */

        ParagraphVectors vec = new ParagraphVectors.Builder()
                .minWordFrequency(1)
                .epochs(3)
                .layerSize(100)
                .stopWords(new ArrayList<String>())
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();

        vec.fit();

        //WordVectorSerializer.writeWordVectors(vec, "vectors.txt");

        INDArray w1 = vec.lookupTable().vector("I");
        INDArray w2 = vec.lookupTable().vector("am");
        INDArray w3 = vec.lookupTable().vector("sad.");

        INDArray words = Nd4j.create(3, vec.lookupTable().layerSize());

        words.putRow(0, w1);
        words.putRow(1, w2);
        words.putRow(2, w3);


        INDArray mean = words.isMatrix() ? words.mean(0) : words;

        log.info("Mean" + Arrays.toString(mean.dup().data().asDouble()));
        log.info("Array" + Arrays.toString(vec.lookupTable().vector("negative").dup().data().asDouble()));

        double simN = Transforms.cosineSim(mean, vec.lookupTable().vector("negative"));
        log.info("Similarity negative: " + simN);


        double simP = Transforms.cosineSim(mean, vec.lookupTable().vector("neutral"));
        log.info("Similarity neutral: " + simP);

        double simV = Transforms.cosineSim(mean, vec.lookupTable().vector("positive"));
        log.info("Similarity positive: " + simV);
    }


    /*
        In this test we'll build w2v model, and will use it's vocab and weights for ParagraphVectors.
        there's no need in this test within travis, use it manually only for problems detection
    */
    @Test
    @Ignore
    public void testParagraphVectorsOverExistingWordVectorsModel() throws Exception {


        // we build w2v from multiple sources, to cover everything
        ClassPathResource resource_sentences = new ClassPathResource("/big/raw_sentences.txt");
        ClassPathResource resource_mixed = new ClassPathResource("/paravec");
        SentenceIterator iter = new AggregatingSentenceIterator.Builder()
                .addSentenceIterator(new BasicLineIterator(resource_sentences.getFile()))
                .addSentenceIterator(new FileSentenceIterator(resource_mixed.getFile()))
                .build();

        TokenizerFactory t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());

        Word2Vec wordVectors = new Word2Vec.Builder()
                .minWordFrequency(1)
                .batchSize(250)
                .iterations(3)
                .epochs(1)
                .learningRate(0.025)
                .layerSize(150)
                .minLearningRate(0.001)
                .windowSize(5)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();

        wordVectors.fit();

        INDArray vector_day1 = wordVectors.getWordVectorMatrix("day").dup();

        // At this moment we have ready w2v model. It's time to use it for ParagraphVectors

        FileLabelAwareIterator labelAwareIterator = new FileLabelAwareIterator.Builder()
                .addSourceFolder(new ClassPathResource("/paravec/labeled").getFile())
                .build();


        // documents from this iterator will be used for classification
        FileLabelAwareIterator unlabeledIterator = new FileLabelAwareIterator.Builder()
                .addSourceFolder(new ClassPathResource("/paravec/unlabeled").getFile())
                .build();


        // we're building classifier now, with pre-built w2v model passed in
        ParagraphVectors paragraphVectors = new ParagraphVectors.Builder()
                .iterate(labelAwareIterator)
                .learningRate(0.025)
                .minLearningRate(0.001)
                .iterations(1)
                .epochs(10)
                .layerSize(150)
                .tokenizerFactory(t)
                .trainWordVectors(true)
                .useExistingWordVectors(wordVectors)
                .build();

        paragraphVectors.fit();


        /*
        double similarityD = wordVectors.similarity("day", "night");
        log.info("day/night similarity: " + similarityD);
        assertTrue(similarityD > 0.5d);
        */

        INDArray vector_day2 = paragraphVectors.getWordVectorMatrix("day").dup();
        double crossDay = arraysSimilarity(vector_day1, vector_day2);
/*
        log.info("Day1: " + vector_day1);
        log.info("Day2: " + vector_day2);
        log.info("Cross-Day similarity: " + crossDay);
        log.info("Cross-Day similiarity 2: " + Transforms.cosineSim(vector_day1, vector_day2));

        assertTrue(crossDay > 0.9d);
*/
        /**
         *
         * Here we're checking cross-vocabulary equality
         *
         */
        /*
        Random rnd = new Random();
        VocabCache<VocabWord> cacheP = paragraphVectors.getVocab();
        VocabCache<VocabWord> cacheW = wordVectors.getVocab();
        for (int x = 0; x < 1000; x++) {
            int idx = rnd.nextInt(cacheW.numWords());

            String wordW = cacheW.wordAtIndex(idx);
            String wordP = cacheP.wordAtIndex(idx);

            assertEquals(wordW, wordP);

            INDArray arrayW = wordVectors.getWordVectorMatrix(wordW);
            INDArray arrayP = paragraphVectors.getWordVectorMatrix(wordP);

            double simWP = Transforms.cosineSim(arrayW, arrayP);
            assertTrue(simWP >= 0.9);
        }
        */

        log.info("Zfinance: " + paragraphVectors.getWordVectorMatrix("Zfinance"));
        log.info("Zhealth: " + paragraphVectors.getWordVectorMatrix("Zhealth"));
        log.info("Zscience: " + paragraphVectors.getWordVectorMatrix("Zscience"));

        LabelledDocument document = unlabeledIterator.nextDocument();

        log.info("Results for document '" + document.getLabel() + "'");

        List<String> results = new ArrayList<>(paragraphVectors.predictSeveral(document, 3));
        for (String result: results) {
            double sim = paragraphVectors.similarityToLabel(document, result);
            log.info("Similarity to ["+result+"] is ["+ sim +"]");
        }

        String topPrediction = paragraphVectors.predict(document);
        assertEquals("Zhealth", topPrediction);
    }

    /*
        Left as reference implementation, before stuff was changed in w2v
     */
    @Deprecated
    private double arraysSimilarity(@NonNull INDArray array1,@NonNull INDArray array2) {
        if (array1.equals(array2)) return 1.0;

        INDArray vector = Transforms.unitVec(array1);
        INDArray vector2 = Transforms.unitVec(array2);

        if(vector == null || vector2 == null)
            return -1;

        return  Nd4j.getBlasWrapper().dot(vector, vector2);

    }
}
