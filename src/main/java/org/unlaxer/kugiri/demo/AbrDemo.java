package org.unlaxer.kugiri.demo;

import org.unlaxer.kugiri.abr.Abr;
import org.unlaxer.kugiri.label.*;
import org.unlaxer.kugiri.model.*;
import org.unlaxer.kugiri.synth.Synth;
import org.unlaxer.kugiri.tagger.AddressParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** KEN_ALL->ABR でラベルを生成し、頭の階層分離(郡/政令区/特別区)を確認。 */
public final class AbrDemo {
    public static void main(String[] args) throws IOException {
        Abr.Result res = Abr.buildRecords(
                Res.open("ken_all_sample.csv"), StandardCharsets.UTF_8,
                Res.open("abr_town.csv"), Res.open("abr_blk.csv"),
                Res.open("abr_rsdt.csv"), Res.open("abr_parcel.csv"), 4);
        System.out.printf("KEN_ALL->ABR 生成教師レコード: %d 件 (未マッチ: %d)%n%n",
                res.records().size(), res.misses());
        for (List<Component> comps : res.records()) {
            Example ex = Synth.buildExample(comps);
            String text = CodePoints.join(ex.chars());
            StringBuilder sb = new StringBuilder();
            for (String[] tl : Bioes.decode(ex.chars(), ex.tags()))
                if (!tl[1].equals("O")) sb.append(tl[1]).append(":").append(tl[0]).append(" / ");
            System.out.println(text);
            System.out.println("  " + sb);
        }
        // 統合: 合成 + ABR由来 で学習 -> ABR住所を parse
        Random rng = new Random(7);
        List<Example> train = new ArrayList<>(Synth.makeDataset(1500, 1));
        for (List<Component> c : res.records())
            for (int i = 0; i < 30; i++) train.add(Synth.buildExample(Synth.augment(c, rng)));
        AddressParser p = new AddressParser().fit(train, 8);
        String target = CodePoints.join(Synth.buildExample(res.records().get(0)).chars());
        System.out.println("\n=== 統合学習 -> parse ===\n入力: " + target);
        for (Component c : p.parse(target))
            if (!c.label().equals("O")) System.out.printf("   %-8s %s%n", c.label(), c.surface());
    }
}
