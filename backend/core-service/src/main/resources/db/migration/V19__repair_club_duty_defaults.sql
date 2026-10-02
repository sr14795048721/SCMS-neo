UPDATE club_duties
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW())
WHERE created_at IS NULL
   OR updated_at IS NULL;

UPDATE club_duties duty
SET name = mapped.expected_name,
    updated_at = NOW()
FROM (
    SELECT DISTINCT
        member.duty_id,
        CASE COALESCE(NULLIF(member.role, ''), 'MEMBER')
            WHEN 'PRESIDENT' THEN '社长'
            WHEN 'VICE_PRESIDENT' THEN '副社长'
            WHEN 'MINISTER' THEN '部长'
            WHEN 'SECRETARY' THEN '秘书'
            WHEN 'TREASURER' THEN '财务'
            ELSE '普通社员'
        END AS expected_name
    FROM club_student_members member
    WHERE member.duty_id IS NOT NULL
) mapped
WHERE duty.id = mapped.duty_id
  AND duty.name IS DISTINCT FROM mapped.expected_name;
