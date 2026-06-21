-- 建物階層 永続化スキーマ（kugiri-building, Phase1-4）
-- 住所(=敷地/site) → 建物 → 棟/階/部屋 を可変深さの unit で表す。
-- 同一住所に複数建物（基地/団地=型A）は building 複数で表現。

CREATE TABLE IF NOT EXISTS building (
    id           BIGSERIAL PRIMARY KEY,
    address_key  TEXT    NOT NULL,
    label        TEXT    NOT NULL,          -- 代表建物名（canonical）
    needs_review BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (address_key, label)
);
CREATE INDEX IF NOT EXISTS idx_building_address ON building (address_key);
CREATE INDEX IF NOT EXISTS idx_building_label   ON building (label);

-- 棟/階/部屋（建物内の入れ子）。parent_id が NULL なら建物直下。
CREATE TABLE IF NOT EXISTS unit (
    id          BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES building (id) ON DELETE CASCADE,
    parent_id   BIGINT REFERENCES unit (id) ON DELETE CASCADE,
    kind        TEXT   NOT NULL CHECK (kind IN ('WING', 'FLOOR', 'ROOM')),
    label       TEXT   NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_unit_building ON unit (building_id);
CREATE INDEX IF NOT EXISTS idx_unit_parent   ON unit (parent_id);

-- 表記ゆれ・略称・改名 → 代表建物への別名。
CREATE TABLE IF NOT EXISTS building_alias (
    id          BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES building (id) ON DELETE CASCADE,
    alias       TEXT   NOT NULL,
    kind        TEXT   NOT NULL CHECK (kind IN ('notation', 'abbrev', 'rename')),
    UNIQUE (building_id, alias)
);
CREATE INDEX IF NOT EXISTS idx_alias_alias ON building_alias (alias);

-- 同定の要レビュー（型F=衝突, 型C=改名疑い）。人手 must/cannot-link の入口。
CREATE TABLE IF NOT EXISTS identity_review (
    id           BIGSERIAL PRIMARY KEY,
    address_key  TEXT NOT NULL,
    building_label TEXT NOT NULL,
    reason       TEXT NOT NULL,
    resolved     BOOLEAN NOT NULL DEFAULT FALSE
);
