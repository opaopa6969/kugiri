package org.unlaxer.kugiri.abr;

import org.unlaxer.kugiri.model.Component;
import java.io.*;
import java.nio.charset.*;
import java.util.*;

/**
 * ABR(アドレス・ベース・レジストリ) 弱教師ラベル生成アダプタ。
 *
 * <p>KEN_ALL の ZIP で大まかに引いた (都道府県,市区町村,町域) を ABR mt_town に文字列マッチ
 * して町字idを得る。ここで 郡/市/政令区/大字/丁目/小字 がフィールド分離される。さらに
 * (lg_code,町字id) で 住居/街区 or 地番 を引き、街区符号・住居番号・枝番 / 地番 の正解スパンを
 * 付与して Component 列を生成する。突合は文字列で行い団体コードの5桁/6桁差を回避する。
 *
 * <p>列名はデジタル庁 ABR 仕様 / KEN_ALL 仕様準拠(トリム版)。実ダンプの版で列が増減するため
 * ヘッダ名参照(get)で読む。実データで列名が違えば get(キー) の名前のみ直す。
 */
public final class Abr {
    private final List<Map<String, String>> towns = new ArrayList<>();
    private final Map<String, List<Map<String, String>>> townIndex = new HashMap<>();
    private final Map<String, List<String[]>> rsdt = new HashMap<>();   // key -> [(blk,num,num2)]
    private final Map<String, List<String>> blk = new HashMap<>();      // key -> [blk]
    private final Map<String, List<String[]>> parcel = new HashMap<>(); // key -> [(p1,p2,p3)]

    private static String g(Map<String, String> d, String k) {
        String v = d.get(k);
        return v == null ? "" : v.strip();
    }
    private static String key(String lg, String townId) { return lg + "\u0001" + townId; }
    private static String tkey(String pref, String combined, String oaza) {
        return pref + "\u0001" + combined + "\u0001" + oaza;
    }

    static String muniLabel(String surface, String pref) {
        if (surface.endsWith("市")) return "市";
        if (surface.endsWith("町") || surface.endsWith("村")) return "町村";
        if (surface.endsWith("区")) return pref.equals("東京都") ? "東京23区" : "区";
        return "市";
    }

    public Abr load(InputStream town, InputStream blkIn, InputStream rsdtIn, InputStream parcelIn) throws IOException {
        for (Map<String, String> d : dicts(town)) {
            towns.add(d);
            String pref = g(d, "都道府県名"), county = g(d, "郡名"),
                   city = g(d, "市区町村名"), ward = g(d, "政令市区名"), oaza = g(d, "大字・町名");
            String combined = county + city + ward;
            townIndex.computeIfAbsent(tkey(pref, combined, oaza), k -> new ArrayList<>()).add(d);
            townIndex.computeIfAbsent(tkey(pref, combined, ""), k -> new ArrayList<>()).add(d);
        }
        for (Map<String, String> d : dicts(blkIn))
            blk.computeIfAbsent(key(g(d, "全国地方公共団体コード"), g(d, "町字id")), k -> new ArrayList<>())
               .add(g(d, "街区符号"));
        for (Map<String, String> d : dicts(rsdtIn))
            rsdt.computeIfAbsent(key(g(d, "全国地方公共団体コード"), g(d, "町字id")), k -> new ArrayList<>())
                .add(new String[]{g(d, "街区符号"), g(d, "住居番号"), g(d, "住居番号2")});
        for (Map<String, String> d : dicts(parcelIn))
            parcel.computeIfAbsent(key(g(d, "全国地方公共団体コード"), g(d, "町字id")), k -> new ArrayList<>())
                  .add(new String[]{g(d, "地番1"), g(d, "地番2"), g(d, "地番3")});
        return this;
    }

    private static List<Map<String, String>> dicts(InputStream in) throws IOException {
        List<String[]> rows = Csv.read(in, StandardCharsets.UTF_8);
        List<Map<String, String>> out = new ArrayList<>();
        if (rows.isEmpty()) return out;
        String[] header = rows.get(0);
        for (int r = 1; r < rows.size(); r++) {
            String[] row = rows.get(r);
            Map<String, String> m = new HashMap<>();
            for (int c = 0; c < header.length; c++) m.put(header[c], c < row.length ? row[c] : "");
            out.add(m);
        }
        return out;
    }

    public List<Map<String, String>> findTowns(String pref, String cityward, String town) {
        List<Map<String, String>> hit = townIndex.get(tkey(pref, cityward, town));
        if (hit != null) return hit;
        return townIndex.getOrDefault(tkey(pref, cityward, ""), List.of());
    }

    private static String fmtZip(String z) {
        z = z.replace("-", "");
        return z.length() == 7 ? z.substring(0, 3) + "-" + z.substring(3) : z;
    }

    /** 町字行 + ZIP -> Component 列(番地違いで複数)。 */
    public List<List<Component>> emit(String zip7, Map<String, String> t, int maxUnits) {
        String pref = g(t, "都道府県名"), county = g(t, "郡名"), city = g(t, "市区町村名"),
               ward = g(t, "政令市区名"), oaza = g(t, "大字・町名"), chome = g(t, "丁目名"), koaza = g(t, "小字名");
        String k = key(g(t, "全国地方公共団体コード"), g(t, "町字id"));

        List<Component> head = new ArrayList<>();
        head.add(new Component("ZIP", fmtZip(zip7)));
        head.add(new Component("都道府県", pref));
        if (!county.isEmpty()) head.add(new Component("群", county));
        head.add(new Component(muniLabel(city, pref), city));
        if (!ward.isEmpty()) head.add(new Component("区", ward));
        head.add(new Component("町または大字", oaza));
        if (!chome.isEmpty()) head.add(new Component("丁目", chome));
        if (!koaza.isEmpty()) head.add(new Component("字小字", koaza));

        List<List<Component>> out = new ArrayList<>();
        boolean rsdtFlag = g(t, "住居表示フラグ").equals("1");
        if (rsdtFlag && rsdt.containsKey(k)) {
            for (String[] u : limit(rsdt.get(k), maxUnits)) {
                List<Component> c = new ArrayList<>(head);
                c.add(new Component("街区符号", u[0] + "番"));
                c.add(new Component("住居番号", u[1] + "号"));
                if (!u[2].isEmpty()) c.add(new Component("枝番号", "-" + u[2]));
                out.add(c);
            }
        } else if (parcel.containsKey(k)) {
            for (String[] p : limit(parcel.get(k), maxUnits)) {
                String num = p[0] + (p[1].isEmpty() ? "" : "-" + p[1]) + (p[2].isEmpty() ? "" : "-" + p[2]);
                List<Component> c = new ArrayList<>(head);
                c.add(new Component("地番", num + "番地"));
                out.add(c);
            }
        } else if (blk.containsKey(k)) {
            for (String b : limit(blk.get(k), maxUnits)) {
                List<Component> c = new ArrayList<>(head);
                c.add(new Component("街区符号", b + "番"));
                out.add(c);
            }
        } else {
            out.add(new ArrayList<>(head));
        }
        return out;
    }

    private static <T> List<T> limit(List<T> l, int n) { return l.size() <= n ? l : l.subList(0, n); }

    /** KEN_ALL -> ABR を辿って Component 列群を生成。 */
    public record Result(List<List<Component>> records, int misses) {}

    public static Result buildRecords(InputStream kenall, Charset kenallCs,
                                      InputStream town, InputStream blk, InputStream rsdt,
                                      InputStream parcel, int maxUnits) throws IOException {
        Abr abr = new Abr().load(town, blk, rsdt, parcel);
        List<List<Component>> records = new ArrayList<>();
        int misses = 0;
        for (KenAll.Row row : KenAll.load(kenall, kenallCs)) {
            List<Map<String, String>> ts = abr.findTowns(row.pref(), row.cityward(), row.town());
            if (ts.isEmpty()) { misses++; continue; }
            for (Map<String, String> t : ts) records.addAll(abr.emit(row.zip7(), t, maxUnits));
        }
        return new Result(records, misses);
    }
}
