package org.unlaxer.kugiri.tagger;

import java.util.List;

/**
 * 信頼度つき推論の結果。Viterbi タグ列＋位置別マージン＋文平均マージン。
 * パーセプトロンは確率を出さないため、max-marginal のマージンを信頼度の代用に使う。
 */
public record Confidence(List<String> tags, double[] tokenMargin, double meanMargin) {}
