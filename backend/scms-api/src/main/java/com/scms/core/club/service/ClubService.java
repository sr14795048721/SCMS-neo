package com.scms.core.club.service;

import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubStatus;
import com.scms.core.club.dto.ClubResponse;
import com.scms.core.club.repository.ClubRelationCountProjection;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;

    public ClubService(ClubRepository clubRepository, ClubStudentMemberRepository clubStudentMemberRepository) {
        this.clubRepository = clubRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<ClubResponse> list() {
        List<ClubEntity> clubs = clubRepository.findAllByStatusOrderByCreatedAtDescIdDesc(ClubStatus.ACTIVE);
        Map<Long, Long> memberCounts = loadMemberCounts(clubs.stream().map(ClubEntity::getId).toList());

        return clubs.stream()
                .map(club -> new ClubResponse(
                        club.getId(),
                        club.getName(),
                        club.getType(),
                        club.getDescription(),
                        memberCounts.getOrDefault(club.getId(), 0L),
                        club.getCreatedAt()
                ))
                .toList();
    }

    private Map<Long, Long> loadMemberCounts(List<Long> clubIds) {
        if (clubIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return clubStudentMemberRepository.countByClubIds(clubIds)
                .stream()
                .collect(Collectors.toMap(ClubRelationCountProjection::getClubId, ClubRelationCountProjection::getRelationCount));
    }
}
