package org.unlaxer.kugiri.model;

import java.util.List;

/** 学習・評価の1事例: codepoint 列とその BIOES タグ列。 */
public record Example(List<String> chars, List<String> tags) {}
