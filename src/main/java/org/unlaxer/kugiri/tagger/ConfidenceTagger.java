package org.unlaxer.kugiri.tagger;

import java.util.List;

/**
 * 信頼度（self-training 等で使う）を出せる {@link SequenceTagger} の能力インターフェース。
 * すべてのタガーが確率/マージンを出せるわけではないので、別インターフェースに分ける。
 */
public interface ConfidenceTagger extends SequenceTagger {

    /** 信頼度つき推論。 */
    Confidence predictWithConfidence(List<String> chars);
}
