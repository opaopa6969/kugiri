package org.unlaxer.kugiri.building.demo;

import org.unlaxer.kugiri.building.eval.GoldPair;
import org.unlaxer.kugiri.building.eval.IdentityEvaluator;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.IdentityResolver;
import org.unlaxer.kugiri.building.identity.SampleCorpus;

import java.util.List;

/**
 * gold 対決ハーネス・デモ。gold pairs（resources/gold/identity_pairs.csv、サンプル）に対して
 * 3方式（contrastive / edit-normalized / edit-absolute）を採点して並べる。
 * 実運用では実ラベルの gold CSV に差し替える（実データはコミットしない）。
 */
public final class GoldArenaDemo {
    public static void main(String[] args) {
        BuildingLexicon lex = BuildingLexicon.learn(SampleCorpus.names());
        List<GoldPair> gold = GoldPair.loadCsv("/gold/identity_pairs.csv");

        System.out.printf("=== gold 対決（%d ペア・サンプル） ===%n", gold.size());
        for (String r : List.of("contrastive", "edit-normalized", "edit-absolute")) {
            var score = IdentityEvaluator.evaluate(gold, IdentityResolver.of(r, lex));
            System.out.println("  " + IdentityEvaluator.format(score));
        }
        System.out.println("\n※ サンプル gold。実ラベルの gold CSV（a,b,label）に差し替えて実運用評価。");
        System.out.println("   edit 系は NEEDS_REVIEW を出せない＝衝突ペアを必ず誤る（レビュー率0）。");
    }
}
