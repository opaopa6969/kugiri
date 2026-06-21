package org.unlaxer.kugiri.demo;

import org.unlaxer.kugiri.aza.Aza;
import org.unlaxer.jaddress.aza.AzaInducer;
import org.unlaxer.kugiri.model.Component;
import java.util.*;

/**
 * 教師なし字推定デモ: 字を隠して生成 -> ラベル無し推定 -> 隠した正解で採点。
 *
 * <p>T4 の効果を見せるため、従来(boundaryWeight=0)と分岐エントロピー併用(0.5、既定)を
 * 並べて採点する。
 */
public final class AzaDemo {

    private record Case(String text, List<String> goldParts) {}

    public static void main(String[] args) {
        Random rng = new Random(3);
        String[] OAZA = {"山目","真柴","萩荘","上田","高松"};
        String[] AZA = {"田中","横道","寺前","川端","中里","天神"};
        String[] KOAZA = {"北","上","本郷","前田"};

        List<Case> corpus = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            String oaza = OAZA[rng.nextInt(OAZA.length)];
            List<String> parts = new ArrayList<>();
            parts.add(AZA[rng.nextInt(AZA.length)]);
            if (rng.nextDouble() < 0.4) parts.add(KOAZA[rng.nextInt(KOAZA.length)]);
            String mark = rng.nextDouble() < 0.5 ? "字" : "";
            String num = (1 + rng.nextInt(300)) + "番地";
            corpus.add(new Case(oaza + mark + String.join("", parts) + num, parts));
        }

        Set<String> oazaDict = new HashSet<>(Arrays.asList(OAZA));
        List<String> residuals = new ArrayList<>();
        for (Case c : corpus) residuals.add(Aza.peel(c.text(), oazaDict)[1]);

        // 従来(分岐エントロピー併用なし) と 既定(0.5) を比較
        AzaInducer legacy = new AzaInducer(3, 1, 6, 0.0, 0.5, 0.2, 0.0).fit(residuals);
        AzaInducer inducer = new AzaInducer().fit(residuals); // 既定 boundaryWeight=0.5

        System.out.println("=== 字推定スコア（教師ラベル不使用） ===");
        score("従来 (boundaryWeight=0)         ", corpus, oazaDict, legacy);
        score("分岐エントロピー併用 (0.5・既定) ", corpus, oazaDict, inducer);
        System.out.println();

        System.out.println("=== 推定例(既定設定・教師ラベル不使用) ===");
        for (int i = 0; i < 6; i++) {
            Case c = corpus.get(i);
            StringBuilder sb = new StringBuilder();
            for (Component comp : Aza.inferComponents(c.text(), oazaDict, inducer))
                sb.append(comp.label()).append(":").append(comp.surface()).append(" / ");
            System.out.println("  " + c.text());
            System.out.println("     正解字=" + c.goldParts() + "  ->  推定: " + sb);
        }
    }

    private static void score(String tag, List<Case> corpus, Set<String> oazaDict, AzaInducer inducer) {
        long tp = 0, fp = 0, fn = 0, spanOk = 0;
        for (Case c : corpus) {
            List<String> pred = new ArrayList<>();
            for (Component comp : Aza.inferComponents(c.text(), oazaDict, inducer))
                if (comp.label().equals("字小字")) pred.add(comp.surface());
            Set<Integer> gc = cuts(c.goldParts()), pc = cuts(pred);
            Set<Integer> inter = new HashSet<>(gc); inter.retainAll(pc);
            tp += inter.size();
            Set<Integer> fpS = new HashSet<>(pc); fpS.removeAll(gc); fp += fpS.size();
            Set<Integer> fnS = new HashSet<>(gc); fnS.removeAll(pc); fn += fnS.size();
            if (pred.equals(c.goldParts())) spanOk++;
        }
        double pr = tp + fp == 0 ? 1 : (double) tp / (tp + fp);
        double rc = tp + fn == 0 ? 1 : (double) tp / (tp + fn);
        double f1 = pr + rc == 0 ? 0 : 2 * pr * rc / (pr + rc);
        System.out.printf("  %s  P=%.3f R=%.3f F1=%.3f  字スパン完全一致 %d/%d=%.3f%n",
                tag, pr, rc, f1, spanOk, corpus.size(), (double) spanOk / corpus.size());
    }

    private static Set<Integer> cuts(List<String> parts) {
        Set<Integer> pos = new HashSet<>(); int s = 0;
        for (int i = 0; i < parts.size() - 1; i++) { s += parts.get(i).length(); pos.add(s); }
        return pos;
    }
}
