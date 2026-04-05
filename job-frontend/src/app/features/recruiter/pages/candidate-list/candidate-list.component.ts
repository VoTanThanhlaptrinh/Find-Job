import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

type CandidateStatus = 'new' | 'interviewing' | 'shortlisted' | 'rejected';

type CandidateRow = {
  name: string;
  role: string;
  experience: string;
  status: CandidateStatus;
};

@Component({
  selector: 'app-candidate-list',
  imports: [CommonModule, TranslatePipe],
  templateUrl: './candidate-list.component.html',
  styleUrl: './candidate-list.component.css'
})
export class CandidateListComponent {
  readonly candidates: CandidateRow[] = [
    {
      name: 'Nguyen Minh Anh',
      role: 'Frontend Developer',
      experience: '3 years',
      status: 'interviewing',
    },
    {
      name: 'Tran Bao Chau',
      role: 'UI/UX Designer',
      experience: '2 years',
      status: 'shortlisted',
    },
    {
      name: 'Le Hoang Long',
      role: 'Backend Developer',
      experience: '4 years',
      status: 'new',
    },
    {
      name: 'Pham Thu Ha',
      role: 'QA Engineer',
      experience: '2 years',
      status: 'rejected',
    },
  ];

  get interviewingCount(): number {
    return this.candidates.filter((candidate) => candidate.status === 'interviewing').length;
  }

  get newThisWeekCount(): number {
    return this.candidates.filter((candidate) => candidate.status === 'new').length;
  }

  trackByCandidate(index: number, candidate: CandidateRow): string {
    return `${candidate.name}-${candidate.role}-${index}`;
  }

  statusClass(status: CandidateStatus): string {
    switch (status) {
      case 'new':
        return 'bg-sky-100 text-sky-700';
      case 'interviewing':
        return 'bg-amber-100 text-amber-700';
      case 'shortlisted':
        return 'bg-emerald-100 text-emerald-700';
      case 'rejected':
      default:
        return 'bg-rose-100 text-rose-700';
    }
  }
}
