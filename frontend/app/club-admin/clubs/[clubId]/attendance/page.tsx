import { TeacherAttendancePage } from "../../../../../components/teacher/TeacherAttendancePage";

type TeacherAttendanceRoutePageProps = {
  params: {
    clubId: string;
  };
};

export default function TeacherAttendanceRoutePage({ params }: TeacherAttendanceRoutePageProps) {
  return <TeacherAttendancePage clubId={Number(params.clubId)} />;
}
