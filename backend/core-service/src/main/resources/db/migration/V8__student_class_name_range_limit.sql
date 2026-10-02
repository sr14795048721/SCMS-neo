UPDATE student_info
SET class_name = NULL
WHERE class_name IS NOT NULL
  AND (
    BTRIM(class_name) !~ '^[0-9]{1,3}$'
    OR CAST(BTRIM(class_name) AS INTEGER) < 1
    OR CAST(BTRIM(class_name) AS INTEGER) > 30
  );

UPDATE student_info
SET class_name = CAST(CAST(BTRIM(class_name) AS INTEGER) AS VARCHAR(2))
WHERE class_name IS NOT NULL
  AND BTRIM(class_name) ~ '^[0-9]{1,3}$'
  AND CAST(BTRIM(class_name) AS INTEGER) BETWEEN 1 AND 30;
