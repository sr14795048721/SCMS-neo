package com.scms.core.attendance.service;

import com.scms.core.attendance.domain.AttendanceRecordEntity;
import com.scms.core.attendance.domain.AttendanceRecordStatus;
import com.scms.core.attendance.domain.AttendanceSessionEntity;
import com.scms.core.attendance.domain.AttendanceSessionStatus;
import com.scms.core.attendance.dto.PublicAttendanceSessionResponse;
import com.scms.core.attendance.dto.PublicAttendanceSignRequest;
import com.scms.core.attendance.dto.PublicAttendanceSignResponse;
import com.scms.core.attendance.repository.AttendanceRecordRepository;
import com.scms.core.attendance.repository.AttendanceSessionRepository;
import com.scms.core.club.domain.ClubEntity;
import com.scms.core.club.domain.ClubMemberRole;
import com.scms.core.club.domain.ClubStudentMemberEntity;
import com.scms.core.club.repository.ClubRepository;
import com.scms.core.club.repository.ClubStudentMemberRepository;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.score.domain.ScoreRuleEntity;
import com.scms.core.score.repository.ScoreRuleRepository;
import com.scms.core.student.domain.StudentInfoEntity;
import com.scms.core.student.repository.StudentInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Public (no-login) attendance endpoints: session overview by share token and
 * handwritten-signature based check-in. Identity is confirmed by matching
 * display name + student number against club member profiles, which prevents
 * signing on behalf of someone else (the signature image is archived).
 */
@Service
public class PublicAttendanceService {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final ClubRepository clubRepository;
    private final ClubStudentMemberRepository clubStudentMemberRepository;
    private final ScoreRuleRepository scoreRuleRepository;
    private final StudentInfoRepository studentInfoRepository;
    private final AttendanceSignatureStorageService signatureStorageService;

    public PublicAttendanceService(AttendanceSessionRepository attendanceSessionRepository,
                                   AttendanceRecordRepository attendanceRecordRepository,
                                   ClubRepository clubRepository,
                                   ClubStudentMemberRepository clubStudentMemberRepository,
                                   ScoreRuleRepository scoreRuleRepository,
                                   StudentInfoRepository studentInfoRepository,
                                   AttendanceSignatureStorageService signatureStorageService) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.clubRepository = clubRepository;
        this.clubStudentMemberRepository = clubStudentMemberRepository;
        this.scoreRuleRepository = scoreRuleRepository;
        this.studentInfoRepository = studentInfoRepository;
        this.signatureStorageService = signatureStorageService;
    }

    @Transactional(readOnly = true)
    public PublicAttendanceSessionResponse getSession(String shareToken) {
        AttendanceSessionEntity session = getRequiredSession(shareToken);
        return buildSessionResponse(session);
    }

    @Transactional
    public PublicAttendanceSignResponse sign(String shareToken, PublicAttendanceSignRequest request) {
        AttendanceSessionEntity session = getRequiredSession(shareToken);
        if (session.getStatus() != AttendanceSessionStatus.OPEN) {
            throw new BusinessException(ErrorCode.CONFLICT, "本次签到已结束，无法再签到");
        }

        String displayName = normalizeRequired(request.displayName(), "姓名不能为空");
        String studentNo = normalizeRequired(request.studentNo(), "学号不能为空");

        List<ClubStudentMemberEntity> members = clubStudentMemberRepository.findAllByClubId(session.getClubId());
        Set<Long> memberUserIds = members.stream()
                .map(ClubStudentMemberEntity::getStudentUserId)
                .collect(Collectors.toSet());
        Map<Long, StudentInfoEntity> infoMap = loadStudentInfoMap(memberUserIds);

        ClubStudentMemberEntity matchedMember = null;
        StudentInfoEntity matchedInfo = null;
        for (ClubStudentMemberEntity member : members) {
            StudentInfoEntity info = infoMap.get(member.getStudentUserId());
            if (info == null) {
                continue;
            }
            String memberName = trimToNull(info.getDisplayName());
            String memberStudentNo = trimToNull(info.getStudentNo());
            if (memberName != null
                    && memberStudentNo != null
                    && memberName.equalsIgnoreCase(displayName)
                    && memberStudentNo.equalsIgnoreCase(studentNo)) {
                matchedMember = member;
                matchedInfo = info;
                break;
            }
        }
        if (matchedMember == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "姓名或学号与社团成员不匹配，请核对后重试");
        }

        AttendanceRecordEntity record = attendanceRecordRepository
                .findBySessionIdAndStudentUserId(session.getId(), matchedMember.getStudentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "你不在本次签到名单中，请联系社团干部"));
        if (record.getStatus() != AttendanceRecordStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "你已完成签到，请勿重复签到");
        }

        String signaturePath = signatureStorageService.storeSignature(request.signatureImage());

        Instant now = Instant.now();
        record.setCheckInAt(now);
        record.setCheckOutAt(null);
        record.setStatus(AttendanceRecordStatus.CHECKED_IN);
        record.setSignedName(displayName);
        record.setSignedRole(matchedMember.getRole() == null ? ClubMemberRole.MEMBER.name() : matchedMember.getRole().name());
        record.setSignaturePath(signaturePath);
        record.setSettled(false);
        attendanceRecordRepository.save(record);

        return new PublicAttendanceSignResponse(
                displayName,
                matchedInfo == null ? "" : defaultString(matchedInfo.getGrade()),
                matchedInfo == null ? "" : defaultString(matchedInfo.getClassName()),
                record.getSignedRole(),
                now
        );
    }

    private PublicAttendanceSessionResponse buildSessionResponse(AttendanceSessionEntity session) {
        ClubEntity club = clubRepository.findById(session.getClubId()).orElse(null);
        ScoreRuleEntity rule = session.getScoreRuleId() == null
                ? null
                : scoreRuleRepository.findById(session.getScoreRuleId()).orElse(null);
        return new PublicAttendanceSessionResponse(
                club == null ? "" : defaultString(club.getName()),
                defaultString(session.getTitle()),
                session.getStatus().name(),
                rule == null ? "" : defaultString(rule.getName()),
                rule == null ? 0 : rule.getScoreDelta(),
                session.getStartedAt()
        );
    }

    private AttendanceSessionEntity getRequiredSession(String shareToken) {
        String normalizedToken = trimToNull(shareToken);
        if (normalizedToken == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "签到会话不存在");
        }
        return attendanceSessionRepository.findByShareToken(normalizedToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "签到会话不存在或已失效"));
    }

    private String normalizeRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, message);
        }
        return normalized;
    }

    private Map<Long, StudentInfoEntity> loadStudentInfoMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, StudentInfoEntity> map = new LinkedHashMap<>();
        for (StudentInfoEntity item : studentInfoRepository.findAllByUserIdIn(userIds)) {
            StudentInfoEntity existing = map.get(item.getUserId());
            if (existing == null || isStudentInfoNewer(item, existing)) {
                map.put(item.getUserId(), item);
            }
        }
        return map;
    }

    private boolean isStudentInfoNewer(StudentInfoEntity candidate, StudentInfoEntity current) {
        if (candidate.getUpdatedAt() != null && current.getUpdatedAt() != null) {
            return candidate.getUpdatedAt().isAfter(current.getUpdatedAt());
        }
        if (candidate.getUpdatedAt() != null) {
            return true;
        }
        if (current.getUpdatedAt() != null) {
            return false;
        }
        return candidate.getId() != null && current.getId() != null && candidate.getId() > current.getId();
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
