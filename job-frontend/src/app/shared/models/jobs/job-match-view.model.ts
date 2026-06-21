import { JobCardModel } from "./job-card.model";

export interface JobMatchView extends JobCardModel {
    totalMatch: number;
    skillMatch: number;
    expMatch: number;
}
