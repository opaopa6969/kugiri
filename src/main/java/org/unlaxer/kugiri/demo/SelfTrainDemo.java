package org.unlaxer.kugiri.demo;

import org.unlaxer.kugiri.model.Example;
import org.unlaxer.kugiri.synth.Synth;
import org.unlaxer.kugiri.tagger.AddressParser;
import org.unlaxer.kugiri.tagger.SelfTrainer;
import java.util.*;

/**
 * self-training デモ（T3）。少数の seed（正解つき）＋大量の未ラベル住所から、
 * 高信頼の予測を擬似ラベルとして取り込み再学習して、未知 test のスパン F1 が
 * 上がるかを見る。
 *
 * <p>合成データは飽和しやすい（seed だけでも高得点）。効果が見えるよう seed を絞る。
 */
public final class SelfTrainDemo {
    public static void main(String[] args) {
        List<Example> all = Synth.makeDataset(4000, 123);
        // seed=少数 / unlabeled=大量 / test=hold-out
        List<Example> seed = all.subList(0, 40);
        List<Example> unlabeledEx = all.subList(40, 2000);
        List<Example> test = all.subList(2000, all.size());

        List<List<String>> unlabeled = new ArrayList<>();
        for (Example e : unlabeledEx) unlabeled.add(e.chars()); // ラベルは捨てる

        double threshold = Double.parseDouble(System.getProperty("margin", "8.0"));
        SelfTrainer.Result r = SelfTrainer.run(seed, unlabeled, test, threshold, 6, 10);

        System.out.printf("seed=%d  unlabeled=%d  test=%d  marginThreshold=%.1f%n",
                seed.size(), unlabeled.size(), test.size(), threshold);
        System.out.printf("seed のみ: test スパン micro-F1 = %.4f%n", r.seedOnlyF1());
        System.out.println("--- self-training 反復 ---");
        for (SelfTrainer.Round rd : r.history())
            System.out.printf("  iter %d: 擬似ラベル採用 %d 件 -> test micro-F1 = %.4f%n",
                    rd.iter(), rd.pseudoAdded(), rd.testMicroF1());
        System.out.printf("最終: test スパン micro-F1 = %.4f（seed のみ比 %+.4f）%n",
                r.bestF1(), r.bestF1() - r.seedOnlyF1());
    }
}
