package org.unlaxer.kugiri.demo;

import org.unlaxer.kugiri.model.Example;
import org.unlaxer.kugiri.synth.Synth;
import org.unlaxer.kugiri.tagger.AddressParser;
import org.unlaxer.kugiri.model.Component;
import java.util.*;

/** 合成データで end-to-end(学習->評価->parse)。 */
public final class SynthDemo {
    public static void main(String[] args) {
        List<Example> train = Synth.makeDataset(1500, 1);
        List<Example> test = Synth.makeDataset(300, 99);
        AddressParser p = new AddressParser().fit(train, 8);
        System.out.println(p.evaluate(test));
        System.out.println("=== parse 例 ===");
        String[] samples = {
            "100-0005東京都千代田区丸の内一丁目9番1号朝日ビル1203号室",
            "020-0021岩手県盛岡市上田1234番地",
        };
        for (String s : samples) {
            System.out.println("\n入力: " + s);
            for (Component c : p.parse(s))
                if (!c.label().equals("O")) System.out.printf("   %-8s %s%n", c.label(), c.surface());
        }
    }
}
