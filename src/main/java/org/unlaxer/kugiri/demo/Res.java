package org.unlaxer.kugiri.demo;

import java.io.InputStream;

final class Res {
    private Res() {}
    static InputStream open(String name) {
        InputStream in = Res.class.getResourceAsStream("/sample_data/" + name);
        if (in == null) throw new IllegalStateException("resource not found: " + name);
        return in;
    }
}
