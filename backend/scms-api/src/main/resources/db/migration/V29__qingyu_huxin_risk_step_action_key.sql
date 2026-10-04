UPDATE club_app_workspace_project_demo_steps AS step
SET target_subsystem = 'APP',
    action_key = 'qingyu-huxin-risk-review'
FROM club_app_workspace_project_demo_profiles AS profile
         JOIN club_app_workspace_projects AS project
              ON project.id = profile.project_id
WHERE step.demo_profile_id = profile.id
  AND project.project_key = 'qingyu-huxin'
  AND step.sort_order = 5;
