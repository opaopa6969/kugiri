package org.unlaxer.kugiri.building.store;

import org.flywaydb.core.Flyway;
import org.unlaxer.kugiri.building.hierarchy.HierarchyNode;
import org.unlaxer.kugiri.building.hierarchy.HierarchyNode.Level;

import java.sql.*;
import java.util.*;

/**
 * 本番用 Postgres ストア（JDBC＋Flyway）。{@code db/migration} のマイグレーションを適用してから使う。
 * 要・稼働中の Postgres（この環境では未実行。InMemoryStore と同契約なので統合テストで差し替え検証）。
 */
public final class PostgresStore implements BuildingStore {

    private final Connection conn;

    private PostgresStore(Connection conn) { this.conn = conn; }

    /** Flyway でマイグレーションを適用し、接続を開いてストアを返す。 */
    public static PostgresStore open(String jdbcUrl, String user, String password) {
        Flyway.configure().dataSource(jdbcUrl, user, password)
                .locations("classpath:db/migration").load().migrate();
        try {
            Connection c = DriverManager.getConnection(jdbcUrl, user, password);
            c.setAutoCommit(false);
            return new PostgresStore(c);
        } catch (SQLException e) {
            throw new RuntimeException("Postgres 接続に失敗: " + jdbcUrl, e);
        }
    }

    @Override
    public void save(HierarchyNode addressRoot) {
        String addressKey = addressRoot.label();
        try {
            // upsert = addressKey の既存建物を消して入れ直す（unit/alias は ON DELETE CASCADE）
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM building WHERE address_key = ?")) {
                del.setString(1, addressKey);
                del.executeUpdate();
            }
            try (PreparedStatement delR =
                         conn.prepareStatement("DELETE FROM identity_review WHERE address_key = ?")) {
                delR.setString(1, addressKey);
                delR.executeUpdate();
            }
            for (HierarchyNode building : addressRoot.children()) {
                if (building.level() != Level.BUILDING) continue;
                long buildingId = insertBuilding(addressKey, building.label(), building.needsReview());
                for (HierarchyNode child : building.children()) insertUnit(buildingId, null, child);
                if (building.needsReview()) insertReview(addressKey, building.label(),
                        "同定で衝突の疑い（型F）。住所/部屋証拠で確定");
            }
            conn.commit();
        } catch (SQLException e) {
            rollback();
            throw new RuntimeException("保存に失敗: " + addressKey, e);
        }
    }

    private long insertBuilding(String addressKey, String label, boolean review) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO building (address_key, label, needs_review) VALUES (?,?,?) RETURNING id")) {
            ps.setString(1, addressKey);
            ps.setString(2, label);
            ps.setBoolean(3, review);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); return rs.getLong(1); }
        }
    }

    private void insertUnit(long buildingId, Long parentId, HierarchyNode node) throws SQLException {
        long id;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO unit (building_id, parent_id, kind, label) VALUES (?,?,?,?) RETURNING id")) {
            ps.setLong(1, buildingId);
            if (parentId == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, parentId);
            ps.setString(3, node.level().name());
            ps.setString(4, node.label());
            try (ResultSet rs = ps.executeQuery()) { rs.next(); id = rs.getLong(1); }
        }
        for (HierarchyNode child : node.children()) insertUnit(buildingId, id, child);
    }

    private void insertReview(String addressKey, String label, String reason) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO identity_review (address_key, building_label, reason) VALUES (?,?,?)")) {
            ps.setString(1, addressKey);
            ps.setString(2, label);
            ps.setString(3, reason);
            ps.executeUpdate();
        }
    }

    @Override
    public Optional<HierarchyNode> address(String addressKey) {
        try {
            HierarchyNode addr = new HierarchyNode(Level.ADDRESS, addressKey);
            boolean any = false;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, label, needs_review FROM building WHERE address_key = ? ORDER BY id")) {
                ps.setString(1, addressKey);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        any = true;
                        HierarchyNode b = addr.child(Level.BUILDING, rs.getString("label"));
                        if (rs.getBoolean("needs_review")) b.markReview();
                        loadUnits(rs.getLong("id"), b);
                    }
                }
            }
            return any ? Optional.of(addr) : Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("取得に失敗: " + addressKey, e);
        }
    }

    /** building_id の unit を parent ごとにたどってノードへ復元。 */
    private void loadUnits(long buildingId, HierarchyNode building) throws SQLException {
        // parent_id -> [(id, kind, label)]
        record U(long id, Long parent, String kind, String label) {}
        List<U> units = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, parent_id, kind, label FROM unit WHERE building_id = ? ORDER BY id")) {
            ps.setLong(1, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long pid = rs.getLong("parent_id");
                    units.add(new U(rs.getLong("id"), rs.wasNull() ? null : pid,
                            rs.getString("kind"), rs.getString("label")));
                }
            }
        }
        Map<Long, HierarchyNode> byId = new HashMap<>();
        for (U u : units) {
            HierarchyNode parent = u.parent() == null ? building : byId.get(u.parent());
            HierarchyNode node = (parent == null ? building : parent)
                    .child(Level.valueOf(u.kind()), u.label());
            byId.put(u.id(), node);
        }
    }

    @Override
    public List<String> searchBuildings(String namePart) {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT label FROM building WHERE label LIKE ? ORDER BY label")) {
            ps.setString(1, "%" + namePart + "%");
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(rs.getString(1)); }
        } catch (SQLException e) {
            throw new RuntimeException("検索に失敗", e);
        }
        return out;
    }

    @Override
    public List<Review> reviews() {
        List<Review> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT address_key, building_label FROM identity_review WHERE resolved = FALSE ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(new Review(rs.getString(1), rs.getString(2)));
        } catch (SQLException e) {
            throw new RuntimeException("レビュー取得に失敗", e);
        }
        return out;
    }

    @Override
    public int buildingCount() {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM building");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("件数取得に失敗", e);
        }
    }

    private void rollback() {
        try { conn.rollback(); } catch (SQLException ignore) { /* best effort */ }
    }

    @Override
    public void close() {
        try { conn.close(); } catch (SQLException ignore) { /* best effort */ }
    }
}
