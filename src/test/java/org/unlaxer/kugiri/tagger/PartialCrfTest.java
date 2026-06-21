package org.unlaxer.kugiri.tagger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.model.Example;
import org.unlaxer.kugiri.synth.Synth;

public class PartialCrfTest {

    @Test
    public void partialLabelLearningRecoversTailAtLeastAsWellAsSelfTraining() {
        List<Example> all = Synth.makeDataset(2000, 99);
        AddressParser.Split sp = AddressParser.holdout(all, 0.75, 11);
        List<Example> train = sp.train(), test = sp.test();
        Set<String> TAIL = PartialLabels.TAIL;

        List<Example> seed = new ArrayList<>(train.subList(0, 60));
        List<Example> rest = train.subList(60, train.size());

        // partial: seed(完全) + 残り(頭既知/尻尾潜在)
        List<Example> partialTrain = new ArrayList<>(seed);
        for (Example e : rest) partialTrain.add(PartialLabels.maskTail(e, TAIL));
        PerceptronTagger pt = new PerceptronTagger();
        pt.fitPartial(partialTrain, 8);
        AddressParser partial = new AddressParser(pt);

        // self-training: 同じ seed + 残りを未ラベル
        List<List<String>> unlabeled = new ArrayList<>();
        for (Example e : rest) unlabeled.add(e.chars());
        AddressParser self = SelfTrainer.run(seed, unlabeled, test, 5.0, 5, 8).parser();

        double fPartial = PartialLabels.tailMicroF1(partial, test, TAIL);
        double fSelf = PartialLabels.tailMicroF1(self, test, TAIL);

        // 尻尾を学習できている（崩壊していない）
        assertTrue(fPartial >= 0.7, "partial の尻尾F1が低すぎる: " + fPartial);
        // 受け入れ: self-training と同等以上（許容誤差つき）
        assertTrue(fPartial + 0.02 >= fSelf,
                "partial が self-training を下回った: partial=" + fPartial + " self=" + fSelf);
    }

    @Test
    public void constrainedViterbiRespectsKnownHead() {
        List<Example> all = Synth.makeDataset(400, 7);
        PerceptronTagger pt = new PerceptronTagger();
        // 完全ラベルで普通に学習してから、部分ラベルの整合補完が既知を壊さないことを確認
        List<Example> partial = new ArrayList<>();
        for (Example e : all.subList(0, 300)) partial.add(PartialLabels.maskTail(e, PartialLabels.TAIL));
        pt.fitPartial(partial, 5);
        AddressParser p = new AddressParser(pt);
        // 頭ラベル（都道府県）は壊れず取れる
        var comps = p.parse("020-0021岩手県盛岡市上田1234番地");
        assertTrue(comps.stream().anyMatch(c -> c.label().equals("都道府県") && c.surface().equals("岩手県")));
    }
}
