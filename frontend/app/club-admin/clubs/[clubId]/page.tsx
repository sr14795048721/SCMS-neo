import { TeacherClubWorkspace } from "../../../../components/teacher/TeacherClubWorkspace";

type ClubWorkspacePageProps = {
  params: {
    clubId: string;
  };
};

export default function ClubWorkspacePage({ params }: ClubWorkspacePageProps) {
  return <TeacherClubWorkspace clubId={Number(params.clubId)} />;
}
