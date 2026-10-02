import { StudentCadreClubPage } from "../../../../../components/student/StudentCadreClubPage";

export default function StudentClubManagePage({
  params
}: {
  params: { clubId: string };
}) {
  return <StudentCadreClubPage clubId={Number(params.clubId)} />;
}
