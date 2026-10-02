package com.scms.core.club;

import com.scms.core.club.domain.ClubMemberRole;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClubMemberRoleTest {

    @Test
    void higherRoleShouldManageLowerRoleOnly() {
        assertTrue(ClubMemberRole.PRESIDENT.canManage(ClubMemberRole.VICE_PRESIDENT));
        assertTrue(ClubMemberRole.VICE_PRESIDENT.canManage(ClubMemberRole.MINISTER));
        assertTrue(ClubMemberRole.VICE_PRESIDENT.canManage(ClubMemberRole.SECRETARY));
        assertTrue(ClubMemberRole.VICE_PRESIDENT.canManage(ClubMemberRole.TREASURER));
        assertTrue(ClubMemberRole.MINISTER.canManage(ClubMemberRole.MEMBER));
        assertTrue(ClubMemberRole.SECRETARY.canManage(ClubMemberRole.MEMBER));
        assertTrue(ClubMemberRole.TREASURER.canManage(ClubMemberRole.MEMBER));

        assertFalse(ClubMemberRole.PRESIDENT.canManage(ClubMemberRole.PRESIDENT));
        assertFalse(ClubMemberRole.MINISTER.canManage(ClubMemberRole.SECRETARY));
        assertFalse(ClubMemberRole.SECRETARY.canManage(ClubMemberRole.TREASURER));
        assertFalse(ClubMemberRole.TREASURER.canManage(ClubMemberRole.MINISTER));
        assertFalse(ClubMemberRole.MEMBER.canManage(ClubMemberRole.MEMBER));
    }

    @Test
    void displayOrderShouldFollowHierarchy() {
        List<ClubMemberRole> sortedRoles = List.of(
                ClubMemberRole.MEMBER,
                ClubMemberRole.TREASURER,
                ClubMemberRole.SECRETARY,
                ClubMemberRole.MINISTER,
                ClubMemberRole.VICE_PRESIDENT,
                ClubMemberRole.PRESIDENT
        ).stream().sorted(Comparator.comparingInt(ClubMemberRole::displayOrder)).toList();

        assertEquals(
                List.of(
                        ClubMemberRole.PRESIDENT,
                        ClubMemberRole.VICE_PRESIDENT,
                        ClubMemberRole.MINISTER,
                        ClubMemberRole.SECRETARY,
                        ClubMemberRole.TREASURER,
                        ClubMemberRole.MEMBER
                ),
                sortedRoles
        );
    }
}
