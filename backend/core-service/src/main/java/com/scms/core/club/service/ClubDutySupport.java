package com.scms.core.club.service;

import com.scms.core.club.domain.ClubDutyEntity;
import com.scms.core.club.domain.ClubDutyPermission;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.dto.ClubDutyResponse;
import com.scms.core.club.repository.ClubDutyRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClubDutySupport {

    private final ClubDutyRepository clubDutyRepository;

    public ClubDutySupport(ClubDutyRepository clubDutyRepository) {
        this.clubDutyRepository = clubDutyRepository;
    }

    public Map<Long, ClubDutyEntity> loadDutyMapByClubId(Long clubId) {
        if (clubId == null) {
            return Map.of();
        }
        return clubDutyRepository.findAllByClubIdOrderByCreatedAtAscIdAsc(clubId).stream()
                .collect(Collectors.toMap(ClubDutyEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    public Map<Long, ClubDutyEntity> loadDutyMapByIds(Collection<Long> dutyIds) {
        if (dutyIds == null || dutyIds.isEmpty()) {
            return Map.of();
        }
        return clubDutyRepository.findAllByIdIn(dutyIds).stream()
                .collect(Collectors.toMap(ClubDutyEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    public ClubDutyEntity resolveDuty(ClubStudentMemberEntity membership, Map<Long, ClubDutyEntity> dutyMap) {
        if (membership == null || membership.getDutyId() == null) {
            return null;
        }
        return dutyMap.get(membership.getDutyId());
    }

    public List<String> toPermissionValues(ClubDutyEntity duty) {
        return toPermissionValues(duty == null ? Set.of() : duty.getPermissions());
    }

    public List<String> toPermissionValues(Collection<ClubDutyPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        return permissions.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Enum::name))
                .map(Enum::name)
                .toList();
    }

    public List<ClubDutyResponse> toResponses(Collection<ClubDutyEntity> duties) {
        if (duties == null || duties.isEmpty()) {
            return List.of();
        }
        return duties.stream()
                .sorted(Comparator
                        .comparing(ClubDutyEntity::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(ClubDutyEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ClubDutyEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResponse)
                .toList();
    }

    public ClubDutyResponse toResponse(ClubDutyEntity duty) {
        return new ClubDutyResponse(
                duty.getId(),
                duty.getName(),
                toPermissionValues(duty),
                duty.getCreatedAt(),
                duty.getUpdatedAt()
        );
    }

    public String normalizeDutyName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public ClubDutyPermission parsePermission(String value) {
        String normalized = normalizeDutyName(value);
        if (normalized == null) {
            return null;
        }
        return ClubDutyPermission.valueOf(normalized.toUpperCase(Locale.ROOT));
    }
}
