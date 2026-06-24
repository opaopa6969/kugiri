package org.unlaxer.addressbench;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unlaxer.addressbench.Benchmark.Row;
import org.unlaxer.addressbench.Benchmark.Result;

class BenchmarkTest {

    private static AddressSplitter bh, kugiri, hybrid;
    private static List<Row> gold;

    @BeforeAll
    static void setup() {
        bh = new BhSplitter();
        kugiri = new KugiriSplitter();   // 合成学習（数秒）
        hybrid = new HybridSplitter(bh, kugiri);
        gold = List.of(
                new Row("東京都千代田区千代田1-1サンハイツ101", "サンハイツ101"),
                new Row("大阪府大阪市北区梅田1-1グランドビル3階", "グランドビル3階"),
                new Row("高槻市柱本新町１番府営柱本団地Ｂ２−４０７", "府営柱本団地Ｂ２−４０７"),
                new Row("大阪市淀川区西三国３丁目２４−３", ""));
    }

    private Result resultOf(AddressSplitter s) {
        return Benchmark.run(gold, List.of(s)).get(0);
    }

    @Test
    void bhIsStrongReference() {
        Result r = resultOf(bh);
        assertEquals(gold.size(), r.exact(), "BH は明確な例で全一致のはず");
        assertEquals(1.0, r.exactRate(), 1e-9);
    }

    @Test
    void kugiriSynthTrainedIsWeakerOnRealStyle() {
        // 合成学習の kugiri は番地digit巻き込み等で BH を下回る（ベンチで可視化したい現象）
        assertTrue(resultOf(kugiri).exact() < resultOf(bh).exact());
    }

    @Test
    void hybridIsAtLeastAsGoodAsBh() {
        assertTrue(resultOf(hybrid).exact() >= resultOf(bh).exact());
    }

    @Test
    void csvParsingHandlesQuotesAndCommas() {
        var f = Benchmark.splitCsv("\"DUMMY\",\"6497174\",\"伊都郡かつらぎ町大字佐野８７８－３ガーデンハイツ１０４\",\"\",\"1\"");
        assertEquals(5, f.size());
        assertEquals("伊都郡かつらぎ町大字佐野８７８－３ガーデンハイツ１０４", f.get(2));
    }

    @Test
    void agreementIsComputable() {
        double a = Benchmark.agreementRate(gold, List.of(bh, kugiri, hybrid));
        assertTrue(a >= 0.0 && a <= 1.0);
    }
}
