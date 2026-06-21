package org.unlaxer.kugiri.demo;

import org.unlaxer.kugiri.model.Example;
import org.unlaxer.kugiri.synth.Synth;
import org.unlaxer.kugiri.tagger.AddressParser;
import java.util.*;

/**
 * 実評価基盤デモ（T2）。データを hold-out 分割し、未知の test 集合に対して
 * token-level と entity-level（スパン一致）の両方を出す。
 *
 * <p>注意: ここで使うのは合成データなので数値は高めに出る。これは「評価の配線が
 * 正しく動くこと」を示すデモであり、実力は実ラベルの hold-out（T1 の実データ）で測る。
 */
public final class EvalDemo {
    public static void main(String[] args) {
        List<Example> all = Synth.makeDataset(3000, 42);
        AddressParser.Split sp = AddressParser.holdout(all, 0.8, 7);
        System.out.printf("train=%d  test=%d（未知データ）%n%n", sp.train().size(), sp.test().size());

        AddressParser parser = new AddressParser().fit(sp.train(), 10);

        System.out.println("--- token-level（参考：O が多く高く出やすい） ---");
        System.out.print(parser.evaluate(sp.test()));
        System.out.println();
        System.out.print(parser.evaluateSpans(sp.test()));

        System.out.println("\n（合成データなので高得点。実評価は実ラベル hold-out が必要：issue T1/T2 参照）");
    }
}
