package org.unlaxer.kugiri.tagger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.model.Example;
import org.unlaxer.kugiri.synth.Synth;

public class TaggerSwapTest {

    @Test
    public void addressParserWorksWithInjectedTaggers() {
        List<Example> train = Synth.makeDataset(800, 5);

        AddressParser perceptron = new AddressParser(new PerceptronTagger()).fit(train, 6);
        AddressParser greedy = new AddressParser(new GreedyTagger()).fit(train, 6);

        String text = "020-0021岩手県盛岡市上田1234番地";
        // どちらも同じ API で parse でき、非空の結果を返す
        assertFalse(perceptron.parse(text).isEmpty());
        assertFalse(greedy.parse(text).isEmpty());

        // 能力インターフェースの違い
        assertTrue(perceptron.supportsConfidence());
        assertFalse(greedy.supportsConfidence());
    }

    @Test
    public void greedyTaggerDoesNotSupportConfidence() {
        AddressParser greedy = new AddressParser(new GreedyTagger()).fit(Synth.makeDataset(200, 1), 3);
        assertThrows(UnsupportedOperationException.class,
                () -> greedy.predictConfidence(List.of("東", "京", "都")));
    }

    @Test
    public void defaultConstructorUsesPerceptron() {
        AddressParser p = new AddressParser().fit(Synth.makeDataset(200, 2), 3);
        assertTrue(p.supportsConfidence());
    }
}
