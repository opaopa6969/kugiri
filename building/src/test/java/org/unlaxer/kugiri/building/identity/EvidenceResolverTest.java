package org.unlaxer.kugiri.building.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.unlaxer.kugiri.building.identity.BuildingIdentity.Decision.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class EvidenceResolverTest {

    @Test
    public void sharedRoomsResolveToSame() {
        assertEquals(SAME, EvidenceResolver.resolve(NEEDS_REVIEW, Set.of("201", "202"), Set.of("202", "203")));
    }

    @Test
    public void disjointRoomsResolveToDistinct() {
        assertEquals(DISTINCT, EvidenceResolver.resolve(NEEDS_REVIEW, Set.of("101"), Set.of("201", "202")));
    }

    @Test
    public void missingRoomsStayReview() {
        assertEquals(NEEDS_REVIEW, EvidenceResolver.resolve(NEEDS_REVIEW, Set.of(), Set.of("201")));
        assertEquals(NEEDS_REVIEW, EvidenceResolver.resolve(NEEDS_REVIEW, Set.of("1"), Set.of()));
    }

    @Test
    public void confidentTextIsNotOverridden() {
        // テキストが確信している SAME/DISTINCT は証拠で覆さない
        assertEquals(SAME, EvidenceResolver.resolve(SAME, Set.of("1"), Set.of("9")));
        assertEquals(DISTINCT, EvidenceResolver.resolve(DISTINCT, Set.of("1"), Set.of("1")));
    }
}
