package org.unlaxer.kugiri.building.eval;

import org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 同定の正解ペア（a, b, 期待判定）。gold pairs。 */
public record GoldPair(String a, String b, Decision expected) {

    /** クラスパス上の CSV（a,b,label / ヘッダ1行）を読む。 */
    public static List<GoldPair> loadCsv(String resource) {
        InputStream in = GoldPair.class.getResourceAsStream(resource);
        if (in == null) throw new IllegalStateException("gold resource not found: " + resource);
        List<GoldPair> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line; boolean header = true;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty()) continue;
                if (header) { header = false; continue; }
                String[] f = line.split(",", 3);
                if (f.length < 3) continue;
                out.add(new GoldPair(f[0].strip(), f[1].strip(), Decision.valueOf(f[2].strip())));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out;
    }
}
