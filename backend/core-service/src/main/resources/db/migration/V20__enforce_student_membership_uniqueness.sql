DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM club_student_members
        GROUP BY student_user_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate club_student_members rows found for the same student. Clean up memberships before applying V20.';
    END IF;
END
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM club_join_requests
        WHERE status = 'PENDING'
        GROUP BY student_user_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate pending club_join_requests rows found for the same student. Clean up join requests before applying V20.';
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_club_student_member_user
    ON club_student_members(student_user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_club_join_requests_pending_student
    ON club_join_requests(student_user_id)
    WHERE status = 'PENDING';
