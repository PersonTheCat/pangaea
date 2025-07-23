package personthecat.pangaea.serialization.codec;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static personthecat.pangaea.test.TestUtils.dynamic;

public final class TestPatternTest {

    @Test
    public void string_matchesAnyString() {
        assertTrue(TestPattern.STRING.test(dynamic(RandomStringUtils.random(32))));
    }

    @Test
    public void string_doesNotMatch_nonString() {
        assertFalse(TestPattern.STRING.test(dynamic(Math.random())));
    }

    @Test
    public void number_matchesAnyNumber() {
        assertTrue(TestPattern.NUMBER.test(dynamic(Math.random())));
    }

    @Test
    public void number_doesNotMatch_nonNumber() {
        assertFalse(TestPattern.NUMBER.test(dynamic(RandomStringUtils.random(32))));
    }

    @Test
    public void map_matchesAnyMap() {
        assertTrue(TestPattern.MAP.test(dynamic(Map.of("a", 1, "b", 2))));
    }

    @Test
    public void map_doesNotMatch_nonMap() {
        assertFalse(TestPattern.MAP.test(dynamic(List.of("a", 1, "b", 2))));
    }

    @Test
    public void list_matchesAnyList() {
        assertTrue(TestPattern.LIST.test(dynamic(List.of(1, 2, 3, 4))));
    }

    @Test
    public void list_doesNotMatch_nonList() {
        assertFalse(TestPattern.LIST.test(dynamic(Map.of("a", 1, "b", 2))));
    }

    @Test
    public void stringList_matchesListOfStrings() {
        assertTrue(TestPattern.STRING_LIST.test(dynamic(List.of("a", "b", "c"))));
    }

    @Test
    public void stringList_doesNotMatch_nonList() {
        assertFalse(TestPattern.STRING_LIST.test(dynamic(Map.of("a", "b"))));
    }

    @Test
    public void stringList_doesNotMatch_nonStrings() {
        assertFalse(TestPattern.STRING_LIST.test(dynamic(List.of("a", "b", 3))));
    }

    @Test
    public void numberList_matchesListOfStrings() {
        assertTrue(TestPattern.NUMBER_LIST.test(dynamic(List.of(1, 2, 3))));
    }

    @Test
    public void numberList_doesNotMatch_nonList() {
        assertFalse(TestPattern.NUMBER_LIST.test(dynamic(Map.of(1, 2))));
    }

    @Test
    public void numberList_doesNotMatch_nonStrings() {
        assertFalse(TestPattern.NUMBER_LIST.test(dynamic(List.of(1, 2, "c"))));
    }

    @Test
    public void mapList_matchesListOfMaps() {
        assertTrue(TestPattern.MAP_LIST.test(dynamic(List.of(Map.of(), Map.of()))));
    }

    @Test
    public void mapList_doesNotMatch_nonList() {
        assertFalse(TestPattern.MAP_LIST.test(dynamic(Map.of(Map.of(), Map.of()))));
    }

    @Test
    public void mapList_doesNotMatch_nonMaps() {
        assertFalse(TestPattern.MAP_LIST.test(dynamic(List.of(Map.of(), List.of()))));
    }

    @Test
    public void id_matchesValidResourceLocation() {
        assertTrue(TestPattern.ID.test(dynamic("minecraft:grass_block")));
    }

    @Test
    public void id_matchesSimpleResourceLocation() {
        assertTrue(TestPattern.ID.test(dynamic("grass_block")));
    }

    @Test
    public void id_doesNotMatch_invalidResourceLocation() {
        assertFalse(TestPattern.ID.test(dynamic("invalid-id:grass-block")));
    }

    @Test
    public void id_doesNotMatch_nonString() {
        assertFalse(TestPattern.ID.test(dynamic(1234)));
    }

    @Test
    public void state_matchesCommandStatePattern() {
        assertTrue(TestPattern.STATE.test(dynamic("minecraft:redstone_ore[lit=true]")));
    }

    @Test
    public void state_matchesMapWithNamePattern() {
        assertTrue(TestPattern.STATE.test(dynamic(Map.of("Name", "minecraft:redstone_ore"))));
    }

    @Test
    public void state_doesNotMatch_idOnly() {
        assertFalse(TestPattern.STATE.test(dynamic("minecraft:grass_block")));
    }

    @Test
    public void state_doesNotMatch_invalidIdPortion() {
        assertFalse(TestPattern.STATE.test(dynamic("invalid-id:grass-block[fake=true]")));
    }

    @Test
    public void state_doesNotMatch_mapWithoutName() {
        assertFalse(TestPattern.STATE.test(dynamic(Map.of("properties", Map.of()))));
    }

    @Test
    public void or_matchesLeftPattern() {
        assertTrue(TestPattern.STRING.or(TestPattern.NUMBER).test(dynamic(RandomStringUtils.random(32))));
    }

    @Test
    public void or_matchesRightPattern() {
        assertTrue(TestPattern.STRING.or(TestPattern.NUMBER).test(dynamic(Math.random())));
    }

    @Test
    public void or_doesNotMatch_otherPattern() {
        assertFalse(TestPattern.STRING.or(TestPattern.NUMBER).test(dynamic(List.of(1, 2, 3))));
    }

    @Test
    public void matching_matchesStringAndExpression() {
        assertTrue(TestPattern.matching("\\d+").test(dynamic("123")));
    }

    @Test
    public void matching_doesNotMatch_whenExpressionDoesNotMatch() {
        assertFalse(TestPattern.matching("\\d+").test(dynamic("abc")));
    }

    @Test
    public void matching_doesNotMatch_nonString() {
        assertFalse(TestPattern.matching("\\d+").test(dynamic(123)));
    }

    @Test
    public void mapContaining_matchesMapContainingGivenKeys() {
        assertTrue(TestPattern.mapContaining("a", "b").test(dynamic(Map.of("a", 1, "b", 2))));
    }

    @Test
    public void mapContaining_doesNotMatch_mapWithoutGivenKeys() {
        assertFalse(TestPattern.mapContaining("a", "b").test(dynamic(Map.of("a", 1, "z", 26))));
    }

    @Test
    public void mapContaining_doesNotMatch_nonMap() {
        assertFalse(TestPattern.mapContaining("a", "b").test(dynamic(List.of("a", 1, "b", 2))));
    }

    @Test
    public void forMap_matchesMapExpression() {
        assertTrue(TestPattern.forMap(m -> m.keyMatches("a", TestPattern.NUMBER)).test(dynamic(Map.of("a", 1))));
    }

    @Test
    public void forMap_doesMatch_mismatchingMapExpression() {
        assertFalse(TestPattern.forMap(m -> m.keyMatches("a", TestPattern.NUMBER)).test(dynamic(Map.of("a", "1"))));
    }

    @Test
    public void not_negatesPattern() {
        assertFalse(TestPattern.not(TestPattern.ALWAYS).test(dynamic("everything fails")));
    }
}
