import { SysAdminClubDutiesPage } from "../../../../../components/sys-admin/SysAdminClubDutiesPage";

type SysAdminClubDutiesRouteProps = {
  params: {
    clubId: string;
  };
};

export default function SysAdminClubDutyRoute({ params }: SysAdminClubDutiesRouteProps) {
  return <SysAdminClubDutiesPage clubId={Number(params.clubId)} />;
}
