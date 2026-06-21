package org.unlaxer.kugiri.abr;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;

/**
 * 日本郵便 KEN_ALL.CSV(ヘッダ無し15列, 実ファイルはCP932)から
 * (zip7, 都道府県, 市区町村[郡/政令区を連結], 町域) を取り出す。
 */
public final class KenAll {
    private KenAll() {}
    private static final int ZIP7 = 2, PREF = 6, CITYWARD = 7, TOWN = 8;
    private static final Pattern PAREN = Pattern.compile("（.*?）|\\(.*?\\)");
    private static final Set<String> NOISE = Set.of("以下に掲載がない場合");

    public record Row(String zip7, String pref, String cityward, String town) {}

    static String cleanTown(String t) {
        if (NOISE.contains(t)) return "";
        return PAREN.matcher(t).replaceAll("").strip();
    }

    public static List<Row> load(InputStream in, Charset cs) throws IOException {
        List<Row> rows = new ArrayList<>();
        for (String[] r : Csv.read(in, cs)) {
            if (r.length < 9) continue;
            rows.add(new Row(r[ZIP7].strip(), r[PREF].strip(), r[CITYWARD].strip(),
                    cleanTown(r[TOWN].strip())));
        }
        return rows;
    }
}
