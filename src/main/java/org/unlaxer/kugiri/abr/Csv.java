package org.unlaxer.kugiri.abr;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/** 引用符対応の最小 CSV パーサ。 */
public final class Csv {
    private Csv() {}

    public static List<String[]> read(InputStream in, Charset cs) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, cs))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                rows.add(splitLine(line));
            }
        }
        return rows;
    }

    static String[] splitLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (q) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else q = false;
                } else cur.append(c);
            } else {
                if (c == '"') q = true;
                else if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
                else cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
