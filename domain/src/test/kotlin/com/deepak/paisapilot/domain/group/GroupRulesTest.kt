package com.deepak.paisapilot.domain.group

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupRulesTest {
    private fun member(name: String, active: Boolean = true, owner: Boolean = false) =
        Member("id-$name", "g", name, owner, active)

    @Test
    fun normalizeTrimsLowercasesAndCollapsesSpaces() {
        assertEquals("alex", normalizeMemberName("  Alex "))
        assertEquals("a b", normalizeMemberName("A    B"))
    }

    @Test
    fun groupNameBounds() {
        assertEquals(GroupNameResult.Valid("Flat"), validateGroupName("  Flat "))
        assertEquals(GroupNameResult.Blank, validateGroupName("   "))
        assertEquals(GroupNameResult.Valid("x".repeat(60)), validateGroupName("x".repeat(60)))
        assertEquals(GroupNameResult.TooLong, validateGroupName("x".repeat(61)))
    }

    // AC-GRP-03
    @Test
    fun duplicateMemberNameBlockedCaseInsensitiveTrimmed() {
        val existing = listOf(member("Alex"))
        assertEquals(MemberNameResult.Duplicate, validateMemberName(" alex ", existing))
    }

    // G6: inactive members still reserve the name
    @Test
    fun inactiveMemberStillReservesName() {
        val existing = listOf(member("Alex", active = false))
        assertEquals(MemberNameResult.Duplicate, validateMemberName("ALEX", existing))
    }

    @Test
    fun memberNameBounds() {
        assertEquals(MemberNameResult.Blank, validateMemberName(" ", emptyList()))
        assertEquals(MemberNameResult.Valid("y".repeat(40)), validateMemberName("y".repeat(40), emptyList()))
        assertEquals(MemberNameResult.TooLong, validateMemberName("y".repeat(41), emptyList()))
    }

    @Test
    fun memberLimitIs30() {
        val full = List(MAX_MEMBERS) { member("m$it") }
        assertEquals(MemberNameResult.GroupFull, validateMemberName("new", full))
        assertTrue(validateMemberName("new", full.drop(1)) is MemberNameResult.Valid)
    }

    // AC-GRP-02
    @Test
    fun newGroupValid() {
        val result = validateNewGroup("Flat", "Me", listOf("Alex"))
        assertEquals(
            NewGroupResult.Valid("Flat", listOf("Me", "Alex")),
            result,
        )
    }

    // AC-GRP-04
    @Test
    fun newGroupNeedsTwoMembers() {
        assertEquals(NewGroupResult.TooFewMembers, validateNewGroup("Flat", "Me", emptyList()))
    }

    @Test
    fun newGroupRejectsThirtyOneMembers() {
        val others = List(MAX_MEMBERS) { "p$it" }
        assertEquals(NewGroupResult.TooManyMembers, validateNewGroup("Flat", "Me", others))
        assertTrue(validateNewGroup("Flat", "Me", others.drop(1)) is NewGroupResult.Valid)
    }

    @Test
    fun newGroupRejectsDuplicatesIncludingOwner() {
        assertEquals(NewGroupResult.DuplicateMember, validateNewGroup("Flat", "Me", listOf("alex", " Alex")))
        assertEquals(NewGroupResult.DuplicateMember, validateNewGroup("Flat", "Me", listOf("me")))
    }

    @Test
    fun newGroupRejectsBadNames() {
        assertEquals(NewGroupResult.GroupNameBlank, validateNewGroup(" ", "Me", listOf("Alex")))
        assertEquals(NewGroupResult.GroupNameTooLong, validateNewGroup("x".repeat(61), "Me", listOf("Alex")))
        assertEquals(NewGroupResult.MemberNameBlank, validateNewGroup("Flat", "Me", listOf(" ")))
        assertEquals(NewGroupResult.MemberNameTooLong, validateNewGroup("Flat", "Me", listOf("z".repeat(41))))
    }

    @Test
    fun sharedExpenseNeedsTwoActiveMembers() {
        assertFalse(canRecordSharedExpense(listOf(member("Me"), member("Alex", active = false))))
        assertTrue(canRecordSharedExpense(listOf(member("Me"), member("Alex"))))
    }

    @Test
    fun deleteOnlyWithoutHistory() {
        assertTrue(canDeleteGroup(hasHistory = false))
        assertFalse(canDeleteGroup(hasHistory = true))
    }

    @Test
    fun deactivatingOwnerNotAllowed() {
        assertFalse(canDeactivate(member("Me", owner = true)))
        assertTrue(canDeactivate(member("Alex")))
    }
}
