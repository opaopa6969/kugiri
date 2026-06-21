package org.unlaxer.kugiri.demo;

import org.unlaxer.kugiri.model.Example;
import org.unlaxer.kugiri.synth.Synth;
import org.unlaxer.kugiri.tagger.*;
import java.util.*;

/**
 * partial-CRF（近似）デモ（T7）。「頭（都道府県〜丁目）は既知・尻尾（番地/建物等）は潜在」
 * という弱教師状況で、尻尾のスパン F1 を 3 方式で比較する:
 *   (A) partial: 潜在変数perceptron（{@link PerceptronTagger#fitPartial}）＝部分ラベルで学習
 *   (B) naive  : 尻尾を全部 O とみなして通常学習（潜在を無視した素朴版）
 *   (C) self   : 少数の完全ラベル seed＋大量未ラベルで self-training
 *
 * <p>受け入れ条件は「尻尾の F1 が self-training と同等以上」。
 */
public final class PartialCrfDemo {
    public static void main(String[] args) {
        List<Example> all = Synth.makeDataset(4000, 99);
        AddressParser.Split sp = AddressParser.holdout(all, 0.75, 11);
        List<Example> train = sp.train(), test = sp.test();
        Set<String> TAIL = PartialLabels.TAIL;

        // 共通: 完全ラベル seed 60 件（アンカー）＋ 残りは「頭=既知・尻尾=潜在」
        List<Example> seed = new ArrayList<>(train.subList(0, 60));
        List<Example> rest = train.subList(60, train.size());

        // (A) partial: seed(完全) + 残り(頭既知/尻尾潜在) を潜在変数perceptronで学習
        List<Example> partialTrain = new ArrayList<>(seed);
        for (Example e : rest) partialTrain.add(PartialLabels.maskTail(e, TAIL));
        PerceptronTagger ptPartial = new PerceptronTagger();
        ptPartial.fitPartial(partialTrain, 10);
        AddressParser partial = new AddressParser(ptPartial);

        // (B) naive: seed(完全) + 残りは尻尾を O とみなして通常学習
        List<Example> naiveTrain = new ArrayList<>(seed);
        for (Example e : rest) naiveTrain.add(tailToO(e, TAIL));
        AddressParser naive = new AddressParser().fit(naiveTrain, 10);

        // (C) self-training: 同じ seed(完全) + 残りを未ラベル（頭ラベルは使わない）
        List<List<String>> unlabeled = new ArrayList<>();
        for (Example e : rest) unlabeled.add(e.chars());
        SelfTrainer.Result selfRes = SelfTrainer.run(seed, unlabeled, test, 5.0, 6, 10);
        AddressParser self = selfRes.parser();

        double fA = PartialLabels.tailMicroF1(partial, test, TAIL);
        double fB = PartialLabels.tailMicroF1(naive, test, TAIL);
        double fC = PartialLabels.tailMicroF1(self, test, TAIL);

        System.out.println("=== 尻尾（番地・建物等）のスパン micro-F1 ===");
        System.out.printf("  (A) partial 潜在変数perceptron : %.4f  ※頭ラベルのみ・全train%n", fA);
        System.out.printf("  (B) naive   尻尾=O 無視        : %.4f%n", fB);
        System.out.printf("  (C) self    完全seed60+未ラベル : %.4f%n", fC);
        System.out.printf("%n判定: partial >= self ? %s（%.4f vs %.4f）%n",
                (fA + 1e-9 >= fC) ? "YES" : "NO", fA, fC);
    }

    /** 尻尾ラベルのトークンを O に潰した事例（naive baseline 用）。 */
    private static Example tailToO(Example e, Set<String> tail) {
        List<String> tags = new ArrayList<>(e.tags().size());
        for (String t : e.tags()) {
            String lab = t.equals("O") ? "O" : t.substring(t.indexOf('-') + 1);
            tags.add(tail.contains(lab) ? "O" : t);
        }
        return new Example(e.chars(), tags);
    }
}
