package org.unlaxer.kugiri.demo;

import org.unlaxer.kugiri.aza.Aza;
import org.unlaxer.jaddress.aza.AzaInducer;
import org.unlaxer.kugiri.model.Component;
import java.util.*;

/** 教師なし字推定デモ: 字を隠して生成->ラベル無し推定->隠した正解で採点。 */
public final class AzaDemo {
    public static void main(String[] args) {
        Random rng = new Random(3);
        String[] OAZA = {"山目","真柴","萩荘","上田","高松"};
        String[] AZA = {"田中","横道","寺前","川端","中里","天神"};
        String[] KOAZA = {"北","上","本郷","前田"};

        record Case(String text, List<String> goldParts) {}
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
        AzaInducer inducer = new AzaInducer().fit(residuals);

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
            if (String.join("", pred).equals(String.join("", c.goldParts())) && pred.equals(c.goldParts())) spanOk++;
        }
        double pr = tp + fp == 0 ? 1 : (double) tp / (tp + fp);
        double rc = tp + fn == 0 ? 1 : (double) tp / (tp + fn);
        double f1 = pr + rc == 0 ? 0 : 2 * pr * rc / (pr + rc);
        System.out.printf("内部境界(字の切れ目) P=%.3f R=%.3f F1=%.3f%n", pr, rc, f1);
        System.out.printf("字スパン完全一致     %d/%d = %.3f%n%n", spanOk, corpus.size(), (double) spanOk / corpus.size());

        System.out.println("=== 推定例(教師ラベル不使用) ===");
        for (int i = 0; i < 6; i++) {
            Case c = corpus.get(i);
            StringBuilder sb = new StringBuilder();
            for (Component comp : Aza.inferComponents(c.text(), oazaDict, inducer))
                sb.append(comp.label()).append(":").append(comp.surface()).append(" / ");
            System.out.println("  " + c.text());
            System.out.println("     正解字=" + c.goldParts() + "  ->  推定: " + sb);
        }
    }

    private static Set<Integer> cuts(List<String> parts) {
        Set<Integer> pos = new HashSet<>(); int s = 0;
        for (int i = 0; i < parts.size() - 1; i++) { s += parts.get(i).length(); pos.add(s); }
        return pos;
    }
}
