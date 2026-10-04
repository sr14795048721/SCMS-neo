ALTER TABLE club_app_workspace_project_demo_steps
    ADD COLUMN IF NOT EXISTS action_key VARCHAR(120);

UPDATE club_app_workspace_project_demo_steps AS step
SET action_key = 'qingyu-huxin-esp32-provision'
FROM club_app_workspace_project_demo_profiles AS profile
         JOIN club_app_workspace_projects AS project
              ON project.id = profile.project_id
WHERE step.demo_profile_id = profile.id
  AND project.project_key = 'qingyu-huxin'
  AND step.sort_order = 1
  AND LOWER(COALESCE(step.target_subsystem, '')) LIKE '%app%'
  AND (step.action_key IS NULL OR BTRIM(step.action_key) = '');
