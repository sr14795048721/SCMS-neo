import { TeacherClubAppWorkspacePage } from "../../../../../components/teacher/TeacherClubAppWorkspacePage";

type ClubAppWorkspacePageProps = {
  params: {
    clubId: string;
  };
};

export default function ClubAppWorkspacePage({ params }: ClubAppWorkspacePageProps) {
  return <TeacherClubAppWorkspacePage clubId={Number(params.clubId)} />;
}
