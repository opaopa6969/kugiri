package org.unlaxer.kugiri.building.eval;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unlaxer.kugiri.building.eval.IdentityEvaluator.Score;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.IdentityResolver;
import org.unlaxer.kugiri.building.identity.SampleCorpus;

public class IdentityEvaluatorTest {

    private static BuildingLexicon lex;
    private static List<GoldPair> gold;

    @BeforeAll
    static void setup() {
        lex = BuildingLexicon.learn(SampleCorpus.names());
        gold = GoldPair.loadCsv("/gold/identity_pairs.csv");
    }

    @Test
    public void goldLoads() {
        assertFalse(gold.isEmpty());
    }

    @Test
    public void contrastiveBeatsEditBaselinesOnGold() {
        Score c = IdentityEvaluator.evaluate(gold, IdentityResolver.of("contrastive", lex));
        Score n = IdentityEvaluator.evaluate(gold, IdentityResolver.of("edit-normalized", lex));
        Score a = IdentityEvaluator.evaluate(gold, IdentityResolver.of("edit-absolute", lex));

        assertTrue(c.accuracy() >= 0.95, "contrastive 正答率が低い: " + c.accuracy());
        assertTrue(c.accuracy() > n.accuracy(), "contrastive > edit-normalized のはず");
        assertTrue(n.accuracy() >= a.accuracy(), "edit-normalized >= edit-absolute のはず");
        // SAME 再現率（recall）も contrastive が上（略称/種別語省略を包含で拾う）
        assertTrue(c.sameR() > n.sameR(), "SAME recall は contrastive が上のはず");
        // edit 系は NEEDS_REVIEW を出せない
        assertEquals(0.0, n.reviewRate(), 1e-9);
        assertEquals(0.0, a.reviewRate(), 1e-9);
    }
}
