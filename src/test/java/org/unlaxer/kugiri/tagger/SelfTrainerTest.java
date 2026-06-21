package org.unlaxer.kugiri.tagger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.model.Example;
import org.unlaxer.kugiri.synth.Synth;

public class SelfTrainerTest {

    @Test
    public void selfTrainingDoesNotDegradeAndAddsPseudoLabels() {
        List<Example> all = Synth.makeDataset(1500, 123);
        List<Example> seed = all.subList(0, 40);
        List<Example> unlabeledEx = all.subList(40, 800);
        List<Example> test = all.subList(800, all.size());

        List<List<String>> unlabeled = new ArrayList<>();
        for (Example e : unlabeledEx) unlabeled.add(e.chars());

        SelfTrainer.Result r = SelfTrainer.run(seed, unlabeled, test, 5.0, 5, 8);

        // ドリフト監視により、最終 F1 は seed のみを下回らない
        assertTrue(r.bestF1() >= r.seedOnlyF1() - 1e-9,
                "self-training が悪化させた: seed=" + r.seedOnlyF1() + " best=" + r.bestF1());
        // 高信頼の擬似ラベルが少なくとも1反復で取り込まれている
        assertFalse(r.history().isEmpty());
        assertTrue(r.history().get(0).pseudoAdded() > 0, "擬似ラベルが取り込まれていない");
    }

    @Test
    public void confidenceMarginIsFiniteAndNonNegative() {
        List<Example> all = Synth.makeDataset(200, 1);
        AddressParser p = new AddressParser().fit(all.subList(0, 150), 5);
        var conf = p.predictConfidence(all.get(160).chars());
        assertEquals(all.get(160).chars().size(), conf.tags().size());
        assertTrue(conf.meanMargin() >= 0);
        assertTrue(Double.isFinite(conf.meanMargin()));
    }
}
