package org.unlaxer.kugiri.demo;

import org.unlaxer.kugiri.model.Example;
import org.unlaxer.kugiri.synth.Synth;
import org.unlaxer.kugiri.tagger.AddressParser;
import org.unlaxer.kugiri.tagger.GreedyTagger;
import org.unlaxer.kugiri.tagger.PerceptronTagger;
import org.unlaxer.kugiri.tagger.SelfTrainer;
import java.util.List;

/**
 * 差し替え層デモ（T5）。同じ {@link AddressParser} API のまま、系列ラベラ実装だけを
 * 差し替えて動くことを示す。MALLET CRF / 文字BERT+ONNX も同じ位置に差し込める。
 */
public final class TaggerSwapDemo {
    public static void main(String[] args) {
        List<Example> all = Synth.makeDataset(2000, 42);
        AddressParser.Split sp = AddressParser.holdout(all, 0.8, 7);

        // 既定: 構造化パーセプトロン（Viterbi, 信頼度対応）
        AddressParser perceptron = new AddressParser(new PerceptronTagger()).fit(sp.train(), 10);
        // 差し替え: 貪欲タガー（同じ API、信頼度非対応）
        AddressParser greedy = new AddressParser(new GreedyTagger()).fit(sp.train(), 10);

        System.out.println("=== 同一 API で実装を差し替え（test スパン micro-F1） ===");
        System.out.printf("  PerceptronTagger : F1=%.4f  信頼度対応=%b%n",
                SelfTrainer.microF1(perceptron, sp.test()), perceptron.supportsConfidence());
        System.out.printf("  GreedyTagger     : F1=%.4f  信頼度対応=%b%n",
                SelfTrainer.microF1(greedy, sp.test()), greedy.supportsConfidence());

        String target = "020-0021岩手県盛岡市上田1234番地";
        System.out.println("\n=== どちらも同じ parse() で動く ===\n入力: " + target);
        System.out.println("  [Perceptron] " + perceptron.parse(target));
        System.out.println("  [Greedy]     " + greedy.parse(target));
        System.out.println("\n※ 本番では同じ位置に MALLET CRF / 文字BERT(ONNX) を差し込む（API不変）。");
    }
}
