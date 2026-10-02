import { TeacherClubAppWorkspaceDemoPage } from "../../../../../../../components/teacher/TeacherClubAppWorkspaceDemoPage";

type ClubAppWorkspaceProjectDemoPageProps = {
  params: {
    clubId: string;
    projectKey: string;
  };
};

export default function ClubAppWorkspaceProjectDemoPage({ params }: ClubAppWorkspaceProjectDemoPageProps) {
  return <TeacherClubAppWorkspaceDemoPage clubId={Number(params.clubId)} projectKey={params.projectKey} />;
}
