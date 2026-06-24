package org.unlaxer.kugiri.building.api;

import io.javalin.Javalin;
import org.unlaxer.kugiri.building.hierarchy.HierarchyAssembler.Row;
import org.unlaxer.kugiri.building.identity.BuildingLexicon;
import org.unlaxer.kugiri.building.identity.SampleCorpus;
import org.unlaxer.kugiri.building.ingest.BuildingIngestCli;
import org.unlaxer.kugiri.building.parser.BuildingParser;
import org.unlaxer.kugiri.building.store.BuildingStore;
import org.unlaxer.kugiri.building.store.InMemoryStore;
import org.unlaxer.kugiri.building.store.PersistencePipeline;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 建物階層 REST + 簡易 UI（Javalin, optional 依存）。既定はサンプルCSVを InMemoryStore に投入して起動。
 *
 * <pre>
 * mvn -q -f building/pom.xml exec:java -Dexec.mainClass=org.unlaxer.kugiri.building.api.BuildingApiServer
 *   GET /                      静的UI（resources/static/index.html）
 *   GET /api/parse?tail=...    建物テール分解
 *   GET /api/search?q=...      建物名検索
 *   GET /api/address/{key}     住所ツリー
 *   GET /api/reviews           要レビュー
 *   GET /api/stats             統計
 * </pre>
 */
public final class BuildingApiServer {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7072;
        BuildingLexicon lex = BuildingLexicon.learn(SampleCorpus.names());
        BuildingStore store = new InMemoryStore();
        loadSamples(lex, store);
        BuildingFacade facade = new BuildingFacade(store, lex, "lexicon");

        Javalin app = Javalin.create(cfg -> cfg.staticFiles.add("/static"));
        app.get("/api/parse", ctx -> ctx.json(facade.parse(ctx.queryParam("tail"))));
        app.get("/api/search", ctx -> ctx.json(facade.search(ctx.queryParam("q"))));
        app.get("/api/address/{key}", ctx -> {
            var t = facade.address(ctx.pathParam("key"));
            if (t == null) ctx.status(404).json(java.util.Map.of("error", "not found"));
            else ctx.json(t);
        });
        app.get("/api/reviews", ctx -> ctx.json(facade.reviews()));
        app.get("/api/stats", ctx -> ctx.json(facade.stats()));
        app.start(port);
        System.out.println("building API: http://localhost:" + port + "/");
    }

    private static void loadSamples(BuildingLexicon lex, BuildingStore store) throws IOException {
        InputStream in = BuildingApiServer.class.getResourceAsStream("/sample/building_rows.csv");
        if (in == null) return;
        BuildingParser parser = BuildingParser.of("lexicon", lex);
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            List<Row> rows = BuildingIngestCli.readRows(r, 0, 1, parser);
            PersistencePipeline.ingest(rows, lex, store);
        }
    }
}
