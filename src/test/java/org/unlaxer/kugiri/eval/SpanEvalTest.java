package org.unlaxer.kugiri.eval;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.eval.SpanEval.Span;
import org.unlaxer.kugiri.eval.SpanEval.Counts;

public class SpanEvalTest {

    @Test
    public void spansFromBioes() {
        // 盛岡市 = B,I,E（市）/ 区 = S（区）/ 残り O
        List<String> tags = List.of("B-市", "I-市", "E-市", "S-区", "O");
        List<Span> spans = SpanEval.spans(tags);
        assertEquals(List.of(new Span("市", 0, 3), new Span("区", 3, 4)), spans);
    }

    @Test
    public void brokenTagsAreRecoverable() {
        // I- がいきなり始まる壊れた並びでも例外を出さずスパン化する
        List<String> tags = List.of("I-市", "E-市");
        List<Span> spans = SpanEval.spans(tags);
        assertEquals(1, spans.size());
        assertEquals("市", spans.get(0).label());
    }

    @Test
    public void perfectPredictionScoresOne() {
        List<String> g = List.of("B-市", "E-市", "S-区");
        var stat = SpanEval.spanCounts(List.of(g), List.of(g));
        assertEquals(1.0, stat.get("市").f1(), 1e-9);
        assertEquals(1.0, stat.get("区").f1(), 1e-9);
    }

    @Test
    public void boundaryErrorCountsAsFpAndFn() {
        // gold: 市[0,2)  pred: 市[0,3)（範囲ずれ）→ tp=0, fp=1, fn=1
        var gold = List.of(List.of("B-市", "E-市", "O"));
        var pred = List.of(List.of("B-市", "I-市", "E-市"));
        Map<String, Counts> stat = SpanEval.spanCounts(gold, pred);
        Counts c = stat.get("市");
        assertEquals(0, c.tp);
        assertEquals(1, c.fp);
        assertEquals(1, c.fn);
        assertEquals(0.0, c.f1(), 1e-9);
    }

    @Test
    public void confusionCountsTokenLabels() {
        var gold = List.of(List.of("S-市", "O"));
        var pred = List.of(List.of("S-区", "O"));
        var conf = SpanEval.confusion(gold, pred);
        assertEquals(1L, conf.get("市").get("区"));
        assertEquals(1L, conf.get("O").get("O"));
    }
}
