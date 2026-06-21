package org.unlaxer.kugiri.synth;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.label.Bioes;
import org.unlaxer.kugiri.label.CodePoints;
import org.unlaxer.kugiri.model.Component;
import org.unlaxer.kugiri.model.Example;

public class SynthTest {

    @Test
    public void datasetContainsBuildingAndHouseholdLabels() {
        List<Example> data = Synth.makeDataset(2000, 42);
        Set<String> labels = new HashSet<>();
        for (Example e : data) for (String t : e.tags())
            if (!t.equals("O")) labels.add(t.substring(t.indexOf('-') + 1));
        // T6 で追加した建物・方書き系ラベルが合成データに現れる
        assertTrue(labels.contains("棟"), "棟 が無い");
        assertTrue(labels.contains("階数"), "階数 が無い: " + labels);
        assertTrue(labels.contains("部屋番号"), "部屋番号 が無い");
        assertTrue(labels.contains("方書き"), "方書き が無い: " + labels);
    }

    @Test
    public void buildExampleAlignsCharsAndTags() {
        List<Component> comps = List.of(
                new Component("都道府県", "東京都"),
                new Component("方書き", "田中様方"));
        Example ex = Synth.buildExample(comps);
        // codepoint 数とタグ数が一致（アライメント不要で揃う）
        assertEquals(ex.chars().size(), ex.tags().size());
        assertEquals(CodePoints.of("東京都田中様方").size(), ex.chars().size());
        // decode で元のラベルに戻る
        List<String[]> decoded = Bioes.decode(ex.chars(), ex.tags());
        assertEquals("都道府県", decoded.get(0)[1]);
        assertEquals("方書き", decoded.get(1)[1]);
        assertEquals("田中様方", decoded.get(1)[0]);
    }
}
