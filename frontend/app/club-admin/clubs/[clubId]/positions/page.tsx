import { TeacherClubPositionsPage } from "../../../../../components/teacher/TeacherClubPositionsPage";

type ClubPositionsPageProps = {
  params: {
    clubId: string;
  };
};

export default function ClubPositionsPage({ params }: ClubPositionsPageProps) {
  return <TeacherClubPositionsPage clubId={Number(params.clubId)} />;
}
