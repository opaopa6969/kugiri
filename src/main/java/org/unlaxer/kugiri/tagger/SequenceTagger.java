package org.unlaxer.kugiri.tagger;

import org.unlaxer.kugiri.model.Example;
import java.util.List;

/**
 * 系列ラベラの差し替え層インターフェース。
 *
 * <p>CLAUDE.md の設計原則「ML 専用ライブラリは差し替え層(tagger)にのみ閉じ込める」を体現する
 * 継ぎ目。{@link PerceptronTagger}（純 JDK の参照実装）が既定。将来は同じ素性の MALLET CRF や、
 * 文字BERT を ONNX 化した onnxruntime 推論を、この interface の別実装として差し込む
 * （前処理は {@code CodePoints.of} のみ、公開窓口 {@link AddressParser} の API は不変）。
 */
public interface SequenceTagger {

    /** 学習。 */
    void fit(List<Example> data, int epochs);

    /** codepoint 列 -> BIOES タグ列。 */
    List<String> predict(List<String> chars);
}
