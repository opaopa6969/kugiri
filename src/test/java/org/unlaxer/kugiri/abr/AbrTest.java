package org.unlaxer.kugiri.abr;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.model.Component;

/** 弱教師(KEN_ALL→ABR)パイプラインの回帰。サンプルデータ(UTF-8)で頭の階層分離を確認。 */
public class AbrTest {

    private static InputStream res(String name) {
        InputStream in = AbrTest.class.getResourceAsStream("/sample_data/" + name);
        assertNotNull(in, "resource not found: " + name);
        return in;
    }

    @Test
    public void buildRecordsSeparatesHeadHierarchy() throws Exception {
        Abr.Result r = Abr.buildRecords(
                res("ken_all_sample.csv"), StandardCharsets.UTF_8,
                res("abr_town.csv"), res("abr_blk.csv"),
                res("abr_rsdt.csv"), res("abr_parcel.csv"), 4);

        assertFalse(r.records().isEmpty(), "教師レコードが生成されない");
        assertEquals(0, r.misses(), "サンプルは全件マッチするはず");

        // 出てくるラベル集合に頭の階層分離（東京23区/区/群/町村 など）が含まれる
        Set<String> labels = new HashSet<>();
        for (List<Component> rec : r.records())
            for (Component c : rec) labels.add(c.label());
        assertTrue(labels.contains("都道府県"));
        assertTrue(labels.contains("町または大字"));
        // 街区符号 or 地番 のいずれかは必ず付く
        assertTrue(labels.contains("街区符号") || labels.contains("地番"),
                "番地系ラベルが無い: " + labels);
    }

    @Test
    public void tokyo23WardIsLabeledDistinctly() throws Exception {
        Abr.Result r = Abr.buildRecords(
                res("ken_all_sample.csv"), StandardCharsets.UTF_8,
                res("abr_town.csv"), res("abr_blk.csv"),
                res("abr_rsdt.csv"), res("abr_parcel.csv"), 4);
        boolean has23 = r.records().stream().flatMap(List::stream)
                .anyMatch(c -> c.label().equals("東京23区"));
        // サンプルに千代田区(東京)が含まれる前提
        assertTrue(has23, "東京23区 ラベルが分離されていない");
    }
}
